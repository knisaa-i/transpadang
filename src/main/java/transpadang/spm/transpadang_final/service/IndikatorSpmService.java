package transpadang.spm.transpadang_final.service;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import transpadang.spm.transpadang_final.bean.IndikatorSpmDto;
import transpadang.spm.transpadang_final.bean.IndikatorSpmFilter;
import transpadang.spm.transpadang_final.bean.PageResponse;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;
import transpadang.spm.transpadang_final.entity.IndikatorSpm;
import transpadang.spm.transpadang_final.entity.SubKategori;
import transpadang.spm.transpadang_final.view.IndikatorSpmView;

/**
 * Service Indikator SPM.
 * Response berupa Blazebit entity view ({@link IndikatorSpmView}) dengan paginasi Blazebit.
 */
@Service
public class IndikatorSpmService {

    @PersistenceContext
    private EntityManager em;

    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    public IndikatorSpmService(CriteriaBuilderFactory cbf, EntityViewManager evm) {
        this.cbf = cbf;
        this.evm = evm;
    }

    @Transactional(readOnly = true)
    public PageResponse<IndikatorSpmView> search(IndikatorSpmFilter filter) {
        CriteriaBuilder<IndikatorSpm> cb = cbf.create(em, IndikatorSpm.class);

        if (filter.getAspekId() != null) {
            cb.where("aspek.id").eq(filter.getAspekId());
        }
        if (filter.getSubKategoriId() != null) {
            cb.where("subKategori.id").eq(filter.getSubKategoriId());
        }
        if (filter.getAktif() != null) {
            cb.where("aktif").eq(filter.getAktif());
        }
        if (StringUtils.hasText(filter.getKeyword())) {
            String like = "%" + filter.getKeyword().trim() + "%";
            cb.whereOr()
                    .where("uraian").like(false).value(like).noEscape()
                    .where("spmIndikator").like(false).value(like).noEscape()
                    .where("spmNilai").like(false).value(like).noEscape()
                    .endOr();
        }
        cb.orderByAsc("aspek.id").orderByAsc("id");

        int page = filter.pageOrDefault();
        int size = filter.sizeOrDefault();

        EntityViewSetting<IndikatorSpmView, PaginatedCriteriaBuilder<IndikatorSpmView>> setting =
                EntityViewSetting.create(IndikatorSpmView.class, page * size, size);
        PagedList<IndikatorSpmView> result = evm.applySetting(setting, cb).getResultList();
        return PageResponse.of(result, page, size, result.getTotalSize());
    }

    @Transactional(readOnly = true)
    public IndikatorSpmView findById(Long id) {
        IndikatorSpmView view = view(id);
        if (view == null) {
            throw new EntityNotFoundException("Indikator SPM tidak ditemukan: " + id);
        }
        return view;
    }

    @Transactional
    public IndikatorSpmView create(IndikatorSpmDto dto) {
        IndikatorSpm indikator = new IndikatorSpm();
        apply(indikator, dto);
        em.persist(indikator);
        em.flush();
        return view(indikator.getId());
    }

    @Transactional
    public IndikatorSpmView update(Long id, IndikatorSpmDto dto) {
        IndikatorSpm indikator = em.find(IndikatorSpm.class, id);
        if (indikator == null) {
            throw new EntityNotFoundException("Indikator SPM tidak ditemukan: " + id);
        }
        apply(indikator, dto);
        em.flush();
        return view(id);
    }

    @Transactional
    public void delete(Long id) {
        IndikatorSpm indikator = em.find(IndikatorSpm.class, id);
        if (indikator == null) {
            throw new EntityNotFoundException("Indikator SPM tidak ditemukan: " + id);
        }
        em.remove(indikator);
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

    private IndikatorSpmView view(Long id) {
        CriteriaBuilder<IndikatorSpm> cb = cbf.create(em, IndikatorSpm.class).where("id").eq(id);
        return evm.applySetting(EntityViewSetting.create(IndikatorSpmView.class), cb)
                .getResultList().stream().findFirst().orElse(null);
    }
}
