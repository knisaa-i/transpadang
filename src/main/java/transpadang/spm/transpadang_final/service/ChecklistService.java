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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import transpadang.spm.transpadang_final.bean.ChecklistDetailRequest;
import transpadang.spm.transpadang_final.bean.ChecklistHarianRequest;
import transpadang.spm.transpadang_final.bean.PageResponse;
import transpadang.spm.transpadang_final.entity.*;
import transpadang.spm.transpadang_final.view.ChecklistDetailView;
import transpadang.spm.transpadang_final.view.ChecklistHarianView;
import transpadang.spm.transpadang_final.view.ChecklistItemView;
import transpadang.spm.transpadang_final.view.ChecklistTemplateView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Service modul Checklist Harian (template + item master, header + detail transaksi).
 * Query memakai CriteriaBuilderFactory + QueryDSL Q-class (path type-safe);
 * response berupa Blazebit entity view, persis pola {@code PenilaianSpmService}.
 */
@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;
    private final JPAQueryFactory queryFactory;

    // ================= MASTER (template & item) =================

    @Transactional(readOnly = true)
    public List<ChecklistTemplateView> listTemplates() {
        var q = new QChecklistTemplate("t");
        CriteriaBuilder<ChecklistTemplate> query = cbf.create(em, ChecklistTemplate.class)
                .from(ChecklistTemplate.class, q.getMetadata().getName())
                .orderByAsc(q.id.toString());
        return evm.applySetting(EntityViewSetting.create(ChecklistTemplateView.class), query).getResultList();
    }

    /** Daftar item master untuk merender form kosong, urut sesuai tampilan. */
    @Transactional(readOnly = true)
    public List<ChecklistItemView> listItemsByKode(String kode) {
        var q = new QChecklistItem("i");
        CriteriaBuilder<ChecklistItem> query = cbf.create(em, ChecklistItem.class)
                .from(ChecklistItem.class, q.getMetadata().getName())
                .where(q.template.kode.toString()).eq(kode)
                .where(q.aktif.toString()).eq(Boolean.TRUE)
                .orderByAsc(q.urutan.toString()).orderByAsc(q.id.toString());
        return evm.applySetting(EntityViewSetting.create(ChecklistItemView.class), query).getResultList();
    }

    // ================= TRANSAKSI (header & detail) =================

    @Transactional(readOnly = true)
    public PageResponse<ChecklistHarianView> findAll(Long templateId, Long koridorId, LocalDate tanggal,
                                                     int page, int size) {
        var q = new QChecklistHarian("h");
        CriteriaBuilder<ChecklistHarian> query = cbf.create(em, ChecklistHarian.class)
                .from(ChecklistHarian.class, q.getMetadata().getName());
        if (templateId != null) {
            query.where(q.template.id.toString()).eq(templateId);
        }
        if (koridorId != null) {
            query.where(q.koridor.id.toString()).eq(koridorId);
        }
        if (tanggal != null) {
            query.where(q.tanggal.toString()).eq(tanggal);
        }
        query.orderByDesc(q.tanggal.toString()).orderByDesc(q.id.toString());

        EntityViewSetting<ChecklistHarianView, PaginatedCriteriaBuilder<ChecklistHarianView>> setting =
                EntityViewSetting.create(ChecklistHarianView.class, page * size, size);
        PagedList<ChecklistHarianView> result = evm.applySetting(setting, query).getResultList();
        return PageResponse.of(result, page, size, result.getTotalSize());
    }

    @Transactional(readOnly = true)
    public ChecklistHarianView findById(Long id) {
        var q = new QChecklistHarian("h");
        var query = cbf.create(em, ChecklistHarian.class).from(ChecklistHarian.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        var result = evm.applySetting(EntityViewSetting.create(ChecklistHarianView.class), query).getResultList();
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Checklist tidak ditemukan: " + id);
        }
        return result.getFirst();
    }

    @Transactional
    public ChecklistHarianView create(ChecklistHarianRequest request) {
        ChecklistTemplate template = findTemplate(request.getTemplateId());
        validateSubjek(template, request);

        var h = new ChecklistHarian();
        h.setTemplate(template);
        h.setKoridor(em.getReference(Koridor.class, request.getKoridorId()));
        h.setHari(request.getHari());
        h.setTanggal(request.getTanggal());
        applyIdentity(h, template, request);
        h.setStatus(StatusChecklist.DRAFT);
        h.setTotalDenda(BigDecimal.ZERO);
        User pengisi = currentUserOrNull();
        if (pengisi != null) {
            h.setDibuatOleh(pengisi);
        }
        LocalDateTime now = LocalDateTime.now();
        h.setCreatedAt(now);
        h.setUpdatedAt(now);
        em.persist(h);

        if (request.getDetails() != null) {
            for (ChecklistDetailRequest dr : request.getDetails()) {
                em.merge(buildDetail(h, dr));
            }
        }
        em.flush();
        recomputeDenda(h);
        em.flush();
        return findById(h.getId());
    }

    /** Tambah/ubah SATU hasil item (upsert per checklist+item). */
    @Transactional
    public ChecklistDetailView upsertDetail(Long checklistId, ChecklistDetailRequest dr) {
        ChecklistHarian h = findEntity(checklistId);
        ensureEditable(h);
        ChecklistItem item = findItem(dr.getItemId());

        var builder = cbf.create(em, ChecklistDetail.class, "d")
                .where("d.checklistHarian.id").eq(checklistId)
                .where("d.item.id").eq(dr.getItemId());

        if (dr.getBusId() != null) {
            builder.where("d.bus.id").eq(dr.getBusId());
        } else {
            builder.where("d.bus").isNull();
        }

        var list = builder.getResultList();
        ChecklistDetail detail = list.isEmpty() ? null : list.getFirst();

        if (detail == null) {
            detail = new ChecklistDetail();
            detail.setChecklistHarian(h);
            detail.setItem(item);
            if (dr.getBusId() != null) {
                detail.setBus(em.getReference(Bus.class, dr.getBusId()));
            }
        }
        detail.setHasil(dr.getHasil());
        detail.setKeterangan(dr.getKeterangan());
        detail = em.merge(detail);

        h.setUpdatedAt(LocalDateTime.now());
        em.flush();
        recomputeDenda(h);
        em.flush();
        return detailView(detail.getId());
    }

    @Transactional
    public void deleteDetail(Long checklistId, Long detailId) {
        var list = cbf.create(em, ChecklistDetail.class, "d")
                .fetch("checklistHarian")
                .where("d.id").eq(detailId)
                .where("d.checklistHarian.id").eq(checklistId)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Detail tidak ditemukan: " + detailId);
        }
        ChecklistDetail detail = list.getFirst();
        ChecklistHarian h = detail.getChecklistHarian();
        ensureEditable(h);
        em.remove(detail);
        em.flush();
        recomputeDenda(h);
    }

    @Transactional
    public ChecklistHarianView updateStatus(Long id, StatusChecklist status) {
        ChecklistHarian h = findEntity(id);
        User current = currentUserOrNull();
        if (current == null) {
            throw new AccessDeniedException("Tidak terautentikasi");
        }
        if (status == StatusChecklist.SUBMITTED) {
            ensureComplete(h);
        }
        h.setStatus(status);
        if (status == StatusChecklist.DIKETAHUI) {
            h.setDiketahuiOleh(current);
        }
        h.setUpdatedAt(LocalDateTime.now());
        em.flush();
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        ChecklistHarian h = findEntity(id);
        cbf.delete(em, ChecklistDetail.class, "d")
                .where("d.checklistHarian.id").eq(id)
                .executeUpdate();
        em.remove(h);
    }

    // ================= helper =================

    /** Akumulasi denda = jumlah nilai_denda item yang NOT OK (hasil=false). Form non-denda tetap 0. */
    private void recomputeDenda(ChecklistHarian h) {
        if (h.getTemplate() == null || !Boolean.TRUE.equals(h.getTemplate().getPakaiDenda())) {
            h.setTotalDenda(BigDecimal.ZERO);
            return;
        }
        var t = new QChecklistDetail("w");
        var nilaiDenda = cbf.create(em, BigDecimal.class)
                .from(ChecklistDetail.class, t.getMetadata().getName())
                .select(t.item.nilaiDenda.toString())
                .where(t.checklistHarian.id.toString()).eq(h.getId())
                .where(t.hasil.toString()).eq(false)
                .where(t.item.nilaiDenda.toString()).isNotNull()
                .getResultList();

        BigDecimal total = nilaiDenda.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        h.setTotalDenda(total);
    }

    /**
     * Pastikan SEMUA item wajib (leaf — bukan item induk seperti "Pakaian Pramugara")
     * sudah dijawab (hasil tidak null) sebelum boleh SUBMITTED.
     */
    private void ensureComplete(ChecklistHarian h) {
        Long tplId = h.getTemplate() != null ? h.getTemplate().getId() : null;

        // id item yang menjadi induk (punya anak) pada template ini
        List<Long> parentIds = cbf.create(em, Long.class)
                .from(ChecklistItem.class, "i")
                .select("i.parent.id")
                .distinct()
                .where("i.template.id").eq(tplId)
                .where("i.parent").isNotNull()
                .getResultList();

        // jumlah leaf item aktif (item yang BUKAN induk)
        var leafBuilder = cbf.create(em, Long.class)
                .from(ChecklistItem.class, "i")
                .select("COUNT(i)")
                .where("i.template.id").eq(tplId)
                .where("i.aktif").eq(true);
        if (!parentIds.isEmpty()) {
            leafBuilder.where("i.id").notIn(parentIds);
        }
        long leafCount = nz(leafBuilder.getSingleResult());

        // jumlah leaf yang sudah dijawab (hasil tidak null)
        var ansBuilder = cbf.create(em, Long.class)
                .from(ChecklistDetail.class, "d")
                .select("COUNT(d)")
                .where("d.checklistHarian.id").eq(h.getId())
                .where("d.hasil").isNotNull();
        if (!parentIds.isEmpty()) {
            ansBuilder.where("d.item.id").notIn(parentIds);
        }
        long answered = nz(ansBuilder.getSingleResult());

        // Form objek Bus: target = jumlah bus aktif di koridor × jumlah leaf item
        long target = leafCount;
        if (h.getTemplate() != null && h.getTemplate().getSubjek() == SubjekChecklist.BUS) {
            Long koridorId = h.getKoridor() != null ? h.getKoridor().getId() : null;
            long busCount = nz(cbf.create(em, Long.class)
                    .from(Bus.class, "b")
                    .select("COUNT(b)")
                    .where("b.koridor.id").eq(koridorId)
                    .where("b.aktif").eq(true)
                    .getSingleResult());
            target = busCount * leafCount;
        }

        if (answered < target) {
            throw new IllegalArgumentException(
                    "Masih ada data belum diisi (" + answered + " dari " + target +
                            " terisi). Semua bus & item harus terisi sebelum submit.");
        }
    }

    private long nz(Long v) {
        return v == null ? 0L : v;
    }

    private void applyIdentity(ChecklistHarian h, ChecklistTemplate template, ChecklistHarianRequest req) {
        // Form objek Bus = sesi per koridor (mencakup semua bus); bus diisi di level detail.
        if (template.getSubjek() == SubjekChecklist.PRAMUGARA) {
            h.setNamaPramugara(req.getNamaPramugara());
            h.setShift(req.getShift());
        }
    }

    /** Validasi field identitas wajib sesuai subjek template. */
    private void validateSubjek(ChecklistTemplate template, ChecklistHarianRequest req) {
        switch (template.getSubjek()) {
            case BUS -> { /* sesi mencakup semua bus di koridor; bus dipilih per detail */ }
            case PRAMUGARA -> {
                if (!StringUtils.hasText(req.getNamaPramugara())) {
                    throw new IllegalArgumentException("namaPramugara wajib diisi untuk form Pramugara.");
                }
            }
            case KORIDOR -> { /* cukup koridor yang sudah divalidasi di request */ }
        }
    }

    private ChecklistDetail buildDetail(ChecklistHarian h, ChecklistDetailRequest dr) {
        var detail = new ChecklistDetail();
        detail.setChecklistHarian(h);
        detail.setItem(findItem(dr.getItemId()));
        if (dr.getBusId() != null) {
            detail.setBus(em.getReference(Bus.class, dr.getBusId()));
        }
        detail.setHasil(dr.getHasil());
        detail.setKeterangan(dr.getKeterangan());
        return detail;
    }

    private void ensureEditable(ChecklistHarian h) {
        if (h.getStatus() != StatusChecklist.DRAFT) {
            throw new IllegalArgumentException(
                    "Checklist berstatus " + h.getStatus() + " tidak bisa diubah (hanya DRAFT).");
        }
    }

    private ChecklistHarian findEntity(Long id) {
        var q = new QChecklistHarian("h");
        var list = cbf.create(em, ChecklistHarian.class).from(ChecklistHarian.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Checklist tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private ChecklistTemplate findTemplate(Long id) {
        var r = new QChecklistTemplate("p");
        var list = cbf.create(em, ChecklistTemplate.class).from(ChecklistTemplate.class, r.getMetadata().getName())
                .where(r.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Template tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private ChecklistItem findItem(Long id) {
        var u = new QChecklistItem("s");
        var list = cbf.create(em, ChecklistItem.class).from(ChecklistItem.class, u.getMetadata().getName())
                .where(u.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Item checklist tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private ChecklistDetailView detailView(Long id) {
        var q = new QChecklistDetail("d");
        var query = cbf.create(em, ChecklistDetail.class).from(ChecklistDetail.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        return evm.applySetting(EntityViewSetting.create(ChecklistDetailView.class), query)
                .getResultList().stream().findFirst().orElse(null);
    }

    /** User yang sedang login (dari token), atau null bila tidak ada. */
    private User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        var f = new QUser("k");
        var list = cbf.create(em, User.class).from(User.class,f.getMetadata().getName())
                .where(f.username.toString()).eq(auth.getName())
                .getResultList();
        return list.isEmpty() ? null : list.getFirst();
    }
}
