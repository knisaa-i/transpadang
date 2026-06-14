package transpadang.spm.transpadang_final.service;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.bean.PageResponse;
import transpadang.spm.transpadang_final.bean.PenilaianDetailRequest;
import transpadang.spm.transpadang_final.bean.PenilaianSpmRequest;
import transpadang.spm.transpadang_final.entity.Bus;
import transpadang.spm.transpadang_final.entity.Halte;
import transpadang.spm.transpadang_final.entity.IndikatorSpm;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.entity.PenilaianDetail;
import transpadang.spm.transpadang_final.entity.PenilaianSpm;
import transpadang.spm.transpadang_final.entity.QPenilaianDetail;
import transpadang.spm.transpadang_final.entity.QPenilaianSpm;
import transpadang.spm.transpadang_final.entity.StatusPenilaian;
import transpadang.spm.transpadang_final.entity.User;
import transpadang.spm.transpadang_final.view.PenilaianDetailView;
import transpadang.spm.transpadang_final.view.PenilaianSpmView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Service transaksi Penilaian SPM (header + detail).
 * Query memakai CriteriaBuilderFactory + QueryDSL Q-class (path type-safe);
 * response berupa Blazebit entity view ({@link PenilaianSpmView}).
 */
@Service
@RequiredArgsConstructor
public class PenilaianSpmService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    @Transactional(readOnly = true)
    public PageResponse<PenilaianSpmView> findAll(Long koridorId, int page, int size) {
        var q = new QPenilaianSpm("p");
        CriteriaBuilder<PenilaianSpm> query = cbf.create(em, PenilaianSpm.class)
                .from(PenilaianSpm.class, q.getMetadata().getName());
        if (koridorId != null) {
            query.where(q.koridor.id.toString()).eq(koridorId);
        }
        query.orderByDesc(q.tanggal.toString()).orderByDesc(q.id.toString());

        EntityViewSetting<PenilaianSpmView, PaginatedCriteriaBuilder<PenilaianSpmView>> setting =
                EntityViewSetting.create(PenilaianSpmView.class, page * size, size);
        PagedList<PenilaianSpmView> result = evm.applySetting(setting, query).getResultList();
        return PageResponse.of(result, page, size, result.getTotalSize());
    }

    @Transactional(readOnly = true)
    public PenilaianSpmView findById(Long id) {
        var q = new QPenilaianSpm("p");
        var query = cbf.create(em, PenilaianSpm.class).from(PenilaianSpm.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        var result = evm.applySetting(EntityViewSetting.create(PenilaianSpmView.class), query).getResultList();
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Penilaian tidak ditemukan: " + id);
        }
        return result.getFirst();
    }

    @Transactional
    public PenilaianSpmView create(PenilaianSpmRequest request) {
        var penilaian = new PenilaianSpm();
        penilaian.setKoridor(em.getReference(Koridor.class, request.getKoridorId()));
        penilaian.setHari(request.getHari());
        penilaian.setTanggal(request.getTanggal());
        penilaian.setStatus(StatusPenilaian.DRAFT);
        // maker diambil dari user yang sedang login; fallback ke makerId bila ada
        User maker = currentUserOrNull();
        if (maker != null) {
            penilaian.setMaker(maker);
        } else if (request.getMakerId() != null) {
            penilaian.setMaker(em.getReference(User.class, request.getMakerId()));
        }
        LocalDateTime now = LocalDateTime.now();
        penilaian.setCreatedAt(now);
        penilaian.setUpdatedAt(now);
        em.persist(penilaian);

        if (request.getDetails() != null) {
            for (PenilaianDetailRequest dr : request.getDetails()) {
                em.merge(buildDetail(penilaian, dr));
            }
        }
        em.flush();
        return findById(penilaian.getId());
    }

    @Transactional
    public PenilaianSpmView updateStatus(Long id, StatusPenilaian status) {
        PenilaianSpm penilaian = findEntity(id);
        User current = currentUserOrNull();
        if (current == null) {
            throw new AccessDeniedException("Tidak terautentikasi");
        }
        enforceStatusRole(status, current.getRole());
        if (status == StatusPenilaian.SUBMITTED) {
            ensureComplete(penilaian);
        }

        penilaian.setStatus(status);
        if (status == StatusPenilaian.CHECKED) {
            penilaian.setChecker(current);
        } else if (status == StatusPenilaian.APPROVED) {
            penilaian.setApprover(current);
        }
        penilaian.setUpdatedAt(LocalDateTime.now());
        em.flush();
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        PenilaianSpm penilaian = findEntity(id);
        cbf.delete(em, PenilaianDetail.class, "d")
                .where("d.penilaian.id").eq(id)
                .executeUpdate();
        em.remove(penilaian);
    }

    /**
     * Tambah/ubah SATU detail (upsert: penilaian + bus + indikator). Untuk input/edit per item.
     */
    @Transactional
    public PenilaianDetailView upsertDetail(Long penilaianId, PenilaianDetailRequest dr) {
        validateUnit(dr);
        PenilaianSpm p = findEntity(penilaianId);
        ensureEditable(p);
        IndikatorSpm indikator = findIndikator(dr.getIndikatorId());

        var q = new QPenilaianDetail("d");
        var builder = cbf.create(em, PenilaianDetail.class)
                .from(PenilaianDetail.class, "d")
                .where(q.penilaian.id.toString()).eq(penilaianId)
                .where(q.indikator.id.toString()).eq(dr.getIndikatorId());

        if (dr.getBusId() != null) {
            builder.where(q.bus.id.toString()).eq(dr.getBusId());
        } else {
            builder.where(q.halte.id.toString()).eq(dr.getHalteId());
        }

        var list = builder.getResultList();
        PenilaianDetail detail = list.isEmpty() ? null : list.getFirst();

        if (detail == null) {
            detail = new PenilaianDetail();
            detail.setPenilaian(p);
            applyUnit(detail, dr);
            detail.setIndikator(indikator);
        }
        detail.setNilaiCapaian(dr.getNilaiCapaian());
        detail.setSkorTerbobot(hitungSkor(dr.getNilaiCapaian(), indikator.getBobot()));
        detail.setCatatan(dr.getCatatan());
        detail = em.merge(detail);
        p.setUpdatedAt(LocalDateTime.now());
        em.flush();
        return detailView(detail.getId());
    }

    @Transactional
    public void deleteDetail(Long penilaianId, Long detailId) {
        var list = cbf.create(em, PenilaianDetail.class, "d")
                .fetch("penilaian")
                .where("d.id").eq(detailId)
                .where("d.penilaian.id").eq(penilaianId)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Detail tidak ditemukan: " + detailId);
        }
        PenilaianDetail detail = list.getFirst();
        ensureEditable(detail.getPenilaian());
        em.remove(detail);
    }

    /**
     * Pastikan SEMUA bus & halte sudah dinilai sebelum submit:
     * target = (bus aktif × indikator Bus) + (halte aktif × indikator Halte) == jumlah detail tersimpan.
     */
    private void ensureComplete(PenilaianSpm penilaian) {
        Long koridorId = penilaian.getKoridor() != null ? penilaian.getKoridor().getId() : null;

        long busCount = nz(cbf.create(em, Long.class, "b")
                .from(Bus.class, "b")
                .select("COUNT(b)")
                .where("b.koridor.id").eq(koridorId)
                .where("b.aktif").eq(true)
                .getSingleResult());

        long halteCount = nz(cbf.create(em, Long.class, "h")
                .from(Halte.class, "h")
                .select("COUNT(h)")
                .where("h.koridor.id").eq(koridorId)
                .where("h.aktif").eq(true)
                .getSingleResult());

        long busInd = nz(cbf.create(em, Long.class, "i")
                .from(IndikatorSpm.class, "i")
                .select("COUNT(i)")
                .where("i.aktif").eq(true)
                .whereOr()
                .where("i.subKategori.nama").in("Bus", "Manusia")
                .where("i.subKategori").isNull()
                .endOr()
                .getSingleResult());

        long halteInd = nz(cbf.create(em, Long.class, "i")
                .from(IndikatorSpm.class, "i")
                .select("COUNT(i)")
                .where("i.aktif").eq(true)
                .where("i.subKategori.nama").eq("Halte")
                .getSingleResult());

        long target = busCount * busInd + halteCount * halteInd;

        long saved = nz(cbf.create(em, Long.class, "d")
                .from(PenilaianDetail.class, "d")
                .select("COUNT(d)")
                .where("d.penilaian.id").eq(penilaian.getId())
                .getSingleResult());

        if (saved < target) {
            throw new IllegalArgumentException(
                    "Masih ada data yang belum dinilai (" + saved + " dari " + target +
                            " terisi). Semua bus & halte harus dinilai dulu sebelum submit.");
        }
    }

    private long nz(Long v) {
        return v == null ? 0L : v;
    }

    private void ensureEditable(PenilaianSpm p) {
        StatusPenilaian s = p.getStatus();
        if (s != StatusPenilaian.DRAFT && s != StatusPenilaian.REJECTED) {
            throw new IllegalArgumentException(
                    "Penilaian berstatus " + s + " tidak bisa diubah (hanya DRAFT/REJECTED).");
        }
    }

    // ---- helper ----

    /** Ambil entity penilaian (managed) via cbf + QueryDSL Q-class. */
    private PenilaianSpm findEntity(Long id) {
        var q = new QPenilaianSpm("p");
        var list = cbf.create(em, PenilaianSpm.class).from(PenilaianSpm.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Penilaian tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private IndikatorSpm findIndikator(Long id) {
        var list = cbf.create(em, IndikatorSpm.class, "i")
                .where("i.id").eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Indikator tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private PenilaianDetail buildDetail(PenilaianSpm penilaian, PenilaianDetailRequest dr) {
        validateUnit(dr);
        var indikator = findIndikator(dr.getIndikatorId());
        var detail = new PenilaianDetail();
        detail.setPenilaian(penilaian);
        applyUnit(detail, dr);
        detail.setIndikator(indikator);
        detail.setNilaiCapaian(dr.getNilaiCapaian());
        detail.setSkorTerbobot(hitungSkor(dr.getNilaiCapaian(), indikator.getBobot()));
        detail.setCatatan(dr.getCatatan());
        return detail;
    }

    /** Isi bus ATAU halte ke detail sesuai request. */
    private void applyUnit(PenilaianDetail detail, PenilaianDetailRequest dr) {
        if (dr.getBusId() != null) {
            detail.setBus(em.getReference(Bus.class, dr.getBusId()));
            detail.setHalte(null);
        } else {
            detail.setHalte(em.getReference(Halte.class, dr.getHalteId()));
            detail.setBus(null);
        }
    }

    /** Wajib tepat satu: busId (kategori Bus) ATAU halteId (kategori Halte). */
    private void validateUnit(PenilaianDetailRequest dr) {
        boolean hasBus = dr.getBusId() != null;
        boolean hasHalte = dr.getHalteId() != null;
        if (hasBus == hasHalte) {
            throw new IllegalArgumentException(
                    "Isi tepat satu: busId (kategori Bus) ATAU halteId (kategori Halte).");
        }
    }

    private PenilaianDetailView detailView(Long id) {
        var q = new QPenilaianDetail("d");
        var query = cbf.create(em, PenilaianDetail.class).from(PenilaianDetail.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        return evm.applySetting(EntityViewSetting.create(PenilaianDetailView.class), query)
                .getResultList().stream().findFirst().orElse(null);
    }

    private BigDecimal hitungSkor(BigDecimal nilai, BigDecimal bobot) {
        if (nilai == null || bobot == null) {
            return BigDecimal.ZERO;
        }
        return nilai.multiply(bobot).setScale(4, RoundingMode.HALF_UP);
    }

    /** Hanya role tertentu yang boleh memindahkan ke status tertentu (ADMIN bebas). */
    private void enforceStatusRole(StatusPenilaian status, String role) {
        if ("ADMIN".equals(role)) {
            return;
        }
        boolean ok = switch (status) {
            case DRAFT, SUBMITTED -> "MAKER".equals(role);
            case CHECKED -> "CHECKER".equals(role);
            case APPROVED -> "APPROVER".equals(role);
            case REJECTED -> "CHECKER".equals(role) || "APPROVER".equals(role);
        };
        if (!ok) {
            throw new AccessDeniedException(
                    "Role " + role + " tidak berwenang mengubah status menjadi " + status);
        }
    }

    /** User yang sedang login (dari token), atau null bila tidak ada. */
    private User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        var list = cbf.create(em, User.class, "u")
                .where("u.username").eq(auth.getName())
                .getResultList();
        return list.isEmpty() ? null : list.getFirst();
    }
}