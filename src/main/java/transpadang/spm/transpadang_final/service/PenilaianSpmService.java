package transpadang.spm.transpadang_final.service;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.bean.PageResponse;
import transpadang.spm.transpadang_final.bean.PenilaianDetailRequest;
import transpadang.spm.transpadang_final.bean.PenilaianSpmRequest;
import transpadang.spm.transpadang_final.entity.Bus;
import transpadang.spm.transpadang_final.entity.IndikatorSpm;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.entity.PenilaianDetail;
import transpadang.spm.transpadang_final.entity.PenilaianSpm;
import transpadang.spm.transpadang_final.entity.QPenilaianDetail;
import transpadang.spm.transpadang_final.entity.QUser;
import transpadang.spm.transpadang_final.entity.StatusPenilaian;
import transpadang.spm.transpadang_final.entity.User;
import transpadang.spm.transpadang_final.view.PenilaianDetailView;
import transpadang.spm.transpadang_final.view.PenilaianSpmView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Service transaksi Penilaian SPM (header + detail).
 * Response berupa Blazebit entity view ({@link PenilaianSpmView});
 * QueryDSL dipakai untuk lookup user yang login dan penghapusan detail.
 */
@Service
public class PenilaianSpmService {

    @PersistenceContext
    private EntityManager em;

    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;
    private final JPAQueryFactory queryFactory;

    public PenilaianSpmService(CriteriaBuilderFactory cbf, EntityViewManager evm, JPAQueryFactory queryFactory) {
        this.cbf = cbf;
        this.evm = evm;
        this.queryFactory = queryFactory;
    }

    @Transactional(readOnly = true)
    public PageResponse<PenilaianSpmView> findAll(Long koridorId, int page, int size) {
        CriteriaBuilder<PenilaianSpm> cb = cbf.create(em, PenilaianSpm.class);
        if (koridorId != null) {
            cb.where("koridor.id").eq(koridorId);
        }
        cb.orderByDesc("tanggal").orderByDesc("id");

        EntityViewSetting<PenilaianSpmView, PaginatedCriteriaBuilder<PenilaianSpmView>> setting =
                EntityViewSetting.create(PenilaianSpmView.class, page * size, size);
        PagedList<PenilaianSpmView> result = evm.applySetting(setting, cb).getResultList();
        return PageResponse.of(result, page, size, result.getTotalSize());
    }

    @Transactional(readOnly = true)
    public PenilaianSpmView findById(Long id) {
        PenilaianSpmView view = view(id);
        if (view == null) {
            throw new EntityNotFoundException("Penilaian tidak ditemukan: " + id);
        }
        return view;
    }

    @Transactional
    public PenilaianSpmView create(PenilaianSpmRequest request) {
        PenilaianSpm penilaian = new PenilaianSpm();
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
                em.persist(buildDetail(penilaian, dr));
            }
        }
        em.flush();
        return view(penilaian.getId());
    }

    @Transactional
    public PenilaianSpmView updateStatus(Long id, StatusPenilaian status) {
        PenilaianSpm penilaian = em.find(PenilaianSpm.class, id);
        if (penilaian == null) {
            throw new EntityNotFoundException("Penilaian tidak ditemukan: " + id);
        }
        User current = currentUserOrNull();
        if (current == null) {
            throw new AccessDeniedException("Tidak terautentikasi");
        }
        enforceStatusRole(status, current.getRole());

        penilaian.setStatus(status);
        if (status == StatusPenilaian.CHECKED) {
            penilaian.setChecker(current);
        } else if (status == StatusPenilaian.APPROVED) {
            penilaian.setApprover(current);
        }
        penilaian.setUpdatedAt(LocalDateTime.now());
        em.flush();
        return view(id);
    }

    @Transactional
    public void delete(Long id) {
        PenilaianSpm penilaian = em.find(PenilaianSpm.class, id);
        if (penilaian == null) {
            throw new EntityNotFoundException("Penilaian tidak ditemukan: " + id);
        }
        QPenilaianDetail d = QPenilaianDetail.penilaianDetail;
        queryFactory.delete(d).where(d.penilaian.id.eq(id)).execute();
        em.remove(penilaian);
    }

    /**
     * Tambah/ubah SATU detail (upsert berdasarkan kombinasi penilaian + bus + indikator).
     * Dipakai untuk input/edit per item.
     */
    @Transactional
    public PenilaianDetailView upsertDetail(Long penilaianId, PenilaianDetailRequest dr) {
        PenilaianSpm p = em.find(PenilaianSpm.class, penilaianId);
        if (p == null) {
            throw new EntityNotFoundException("Penilaian tidak ditemukan: " + penilaianId);
        }
        ensureEditable(p);
        IndikatorSpm indikator = em.find(IndikatorSpm.class, dr.getIndikatorId());
        if (indikator == null) {
            throw new EntityNotFoundException("Indikator tidak ditemukan: " + dr.getIndikatorId());
        }

        QPenilaianDetail q = QPenilaianDetail.penilaianDetail;
        PenilaianDetail detail = queryFactory.selectFrom(q)
                .where(q.penilaian.id.eq(penilaianId)
                        .and(q.bus.id.eq(dr.getBusId()))
                        .and(q.indikator.id.eq(dr.getIndikatorId())))
                .fetchFirst();
        if (detail == null) {
            detail = new PenilaianDetail();
            detail.setPenilaian(p);
            detail.setBus(em.getReference(Bus.class, dr.getBusId()));
            detail.setIndikator(indikator);
        }
        detail.setNilaiCapaian(dr.getNilaiCapaian());
        detail.setSkorTerbobot(hitungSkor(dr.getNilaiCapaian(), indikator.getBobot()));
        detail.setCatatan(dr.getCatatan());
        if (detail.getId() == null) {
            em.persist(detail);
        }
        p.setUpdatedAt(LocalDateTime.now());
        em.flush();
        return detailView(detail.getId());
    }

    @Transactional
    public void deleteDetail(Long penilaianId, Long detailId) {
        PenilaianDetail detail = em.find(PenilaianDetail.class, detailId);
        if (detail == null || detail.getPenilaian() == null
                || !detail.getPenilaian().getId().equals(penilaianId)) {
            throw new EntityNotFoundException("Detail tidak ditemukan: " + detailId);
        }
        ensureEditable(detail.getPenilaian());
        em.remove(detail);
    }

    private void ensureEditable(PenilaianSpm p) {
        StatusPenilaian s = p.getStatus();
        if (s != StatusPenilaian.DRAFT && s != StatusPenilaian.REJECTED) {
            throw new IllegalArgumentException(
                    "Penilaian berstatus " + s + " tidak bisa diubah (hanya DRAFT/REJECTED).");
        }
    }

    // ---- helper ----

    private PenilaianSpmView view(Long id) {
        CriteriaBuilder<PenilaianSpm> cb = cbf.create(em, PenilaianSpm.class).where("id").eq(id);
        return evm.applySetting(EntityViewSetting.create(PenilaianSpmView.class), cb)
                .getResultList().stream().findFirst().orElse(null);
    }

    private PenilaianDetailView detailView(Long id) {
        CriteriaBuilder<PenilaianDetail> cb = cbf.create(em, PenilaianDetail.class).where("id").eq(id);
        return evm.applySetting(EntityViewSetting.create(PenilaianDetailView.class), cb)
                .getResultList().stream().findFirst().orElse(null);
    }

    private PenilaianDetail buildDetail(PenilaianSpm penilaian, PenilaianDetailRequest dr) {
        IndikatorSpm indikator = em.find(IndikatorSpm.class, dr.getIndikatorId());
        if (indikator == null) {
            throw new EntityNotFoundException("Indikator tidak ditemukan: " + dr.getIndikatorId());
        }
        PenilaianDetail detail = new PenilaianDetail();
        detail.setPenilaian(penilaian);
        detail.setBus(em.getReference(Bus.class, dr.getBusId()));
        detail.setIndikator(indikator);
        detail.setNilaiCapaian(dr.getNilaiCapaian());
        detail.setSkorTerbobot(hitungSkor(dr.getNilaiCapaian(), indikator.getBobot()));
        detail.setCatatan(dr.getCatatan());
        return detail;
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
        QUser u = QUser.user;
        return queryFactory.selectFrom(u).where(u.username.eq(auth.getName())).fetchFirst();
    }
}
