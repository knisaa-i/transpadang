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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;
import transpadang.spm.transpadang_final.bean.IndikatorSpmDto;
import transpadang.spm.transpadang_final.bean.IndikatorSpmFilter;
import transpadang.spm.transpadang_final.bean.PageResponse;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;
import transpadang.spm.transpadang_final.entity.IndikatorSpm;
import transpadang.spm.transpadang_final.entity.QIndikatorSpm;
import transpadang.spm.transpadang_final.entity.SubKategori;
import transpadang.spm.transpadang_final.view.IndikatorSpmView;

/**
 * Service Indikator SPM.
 * Query memakai CriteriaBuilderFactory + QueryDSL Q-class (path type-safe),
 * response berupa Blazebit entity view ({@link IndikatorSpmView}) dengan paginasi Blazebit.
 */
@Service
@RequiredArgsConstructor
public class IndikatorSpmService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;


    @Transactional(readOnly = true)
    public PageResponse<IndikatorSpmView> search(IndikatorSpmFilter filter) {
        var q = new QIndikatorSpm("i");
        CriteriaBuilder<IndikatorSpm> query = cbf.create(em, IndikatorSpm.class)
                .from(IndikatorSpm.class, q.getMetadata().getName());

        if (filter.getAspekId() != null) {
            query.where(q.aspek.id.toString()).eq(filter.getAspekId());
        }
        if (filter.getSubKategoriId() != null) {
            query.where(q.subKategori.id.toString()).eq(filter.getSubKategoriId());
        }
        if (filter.getKategori() != null && !filter.getKategori().isBlank()) {
            String kat = filter.getKategori().trim().toUpperCase();
            if ("HALTE".equals(kat)) {
                query.where(q.subKategori.nama.toString()).eq("Halte");
            } else if ("BUS".equals(kat)) {
                query.whereOr()
                        .where(q.subKategori.nama.toString()).in(List.of("Bus", "Manusia"))
                        .where(q.subKategori.id.toString()).isNull()
                        .endOr();
            }
        }
        if (filter.getAktif() != null) {
            query.where(q.aktif.toString()).eq(filter.getAktif());
        }
        if (StringUtils.hasText(filter.getKeyword())) {
            String like = "%" + filter.getKeyword().trim() + "%";
            query.whereOr()
                    .where(q.uraian.toString()).like(false).value(like).noEscape()
                    .where(q.spmIndikator.toString()).like(false).value(like).noEscape()
                    .where(q.spmNilai.toString()).like(false).value(like).noEscape()
                    .endOr();
        }
        query.orderByAsc(q.aspek.id.toString()).orderByAsc(q.id.toString());

        int page = filter.pageOrDefault();
        int size = filter.sizeOrDefault();

        EntityViewSetting<IndikatorSpmView, PaginatedCriteriaBuilder<IndikatorSpmView>> setting =
                EntityViewSetting.create(IndikatorSpmView.class, page * size, size);
        PagedList<IndikatorSpmView> result = evm.applySetting(setting, query).getResultList();
        return PageResponse.of(result, page, size, result.getTotalSize());
    }

    @Transactional(readOnly = true)
    public IndikatorSpmView findById(Long id) {
        var q = new QIndikatorSpm("i");
        var query = cbf.create(em, IndikatorSpm.class).from(IndikatorSpm.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        var result = evm.applySetting(EntityViewSetting.create(IndikatorSpmView.class), query).getResultList();
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Indikator SPM tidak ditemukan: " + id);
        }
        return result.getFirst();
    }

    @Transactional
    public IndikatorSpmView create(IndikatorSpmDto dto) {
        var indikator = new IndikatorSpm();
        apply(indikator, dto);
        em.persist(indikator);
        em.flush();
        return findById(indikator.getId());
    }

    @Transactional
    public IndikatorSpmView update(Long id, IndikatorSpmDto dto) {
        var indikator = findEntity(id);
        apply(indikator, dto);
        em.flush();
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        em.remove(findEntity(id));
    }

    /** Ambil entity (managed) via cbf + QueryDSL Q-class, untuk update/delete. */
    private IndikatorSpm findEntity(Long id) {
        var q = new QIndikatorSpm("i");
        var list = cbf.create(em, IndikatorSpm.class).from(IndikatorSpm.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Indikator SPM tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private void apply(IndikatorSpm indikator, IndikatorSpmDto dto) {
        indikator.setAspek(em.getReference(AspekPelayanan.class, dto.getAspekId()));
        indikator.setSubKategori(dto.getSubKategoriId() != null
                ? em.getReference(SubKategori.class, dto.getSubKategoriId())
                : null);
        indikator.setNomorUrut(dto.getNomorUrut());
        indikator.setUraian(dto.getUraian());
        indikator.setSpmIndikator(dto.getSpmIndikator());
        indikator.setSpmNilai(dto.getSpmNilai());
        indikator.setTargetCapaian(dto.getTargetCapaian());
        indikator.setBobot(dto.getBobot());
        indikator.setAktif(dto.getAktif() != null ? dto.getAktif() : Boolean.TRUE);
    }
}