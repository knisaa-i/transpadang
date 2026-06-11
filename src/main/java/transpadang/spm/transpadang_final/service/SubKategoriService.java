package transpadang.spm.transpadang_final.service;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.bean.SubKategoriDto;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;
import transpadang.spm.transpadang_final.entity.SubKategori;
import transpadang.spm.transpadang_final.view.SubKategoriView;

import java.util.List;

/**
 * Service master Sub Kategori (Halte, Bus, Manusia) di bawah aspek.
 * Response berupa Blazebit entity view ({@link SubKategoriView}).
 */
@Service
public class SubKategoriService {

    @PersistenceContext
    private EntityManager em;

    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    public SubKategoriService(CriteriaBuilderFactory cbf, EntityViewManager evm) {
        this.cbf = cbf;
        this.evm = evm;
    }

    @Transactional(readOnly = true)
    public List<SubKategoriView> findAll() {
        CriteriaBuilder<SubKategori> cb = cbf.create(em, SubKategori.class)
                .orderByAsc("aspek.id").orderByAsc("urutan").orderByAsc("id");
        return evm.applySetting(EntityViewSetting.create(SubKategoriView.class), cb).getResultList();
    }

    @Transactional(readOnly = true)
    public List<SubKategoriView> findByAspek(Long aspekId) {
        CriteriaBuilder<SubKategori> cb = cbf.create(em, SubKategori.class);
        if (aspekId != null) {
            cb.where("aspek.id").eq(aspekId);
        }
        cb.orderByAsc("urutan").orderByAsc("id");
        return evm.applySetting(EntityViewSetting.create(SubKategoriView.class), cb).getResultList();
    }

    @Transactional(readOnly = true)
    public SubKategoriView findById(Long id) {
        SubKategoriView view = view(id);
        if (view == null) {
            throw new EntityNotFoundException("Sub kategori tidak ditemukan: " + id);
        }
        return view;
    }

    @Transactional
    public SubKategoriView create(SubKategoriDto dto) {
        SubKategori sub = new SubKategori();
        apply(sub, dto);
        em.persist(sub);
        em.flush();
        return view(sub.getId());
    }

    @Transactional
    public SubKategoriView update(Long id, SubKategoriDto dto) {
        SubKategori sub = em.find(SubKategori.class, id);
        if (sub == null) {
            throw new EntityNotFoundException("Sub kategori tidak ditemukan: " + id);
        }
        apply(sub, dto);
        em.flush();
        return view(id);
    }

    @Transactional
    public void delete(Long id) {
        SubKategori sub = em.find(SubKategori.class, id);
        if (sub == null) {
            throw new EntityNotFoundException("Sub kategori tidak ditemukan: " + id);
        }
        em.remove(sub);
    }

    private void apply(SubKategori sub, SubKategoriDto dto) {
        sub.setAspek(em.getReference(AspekPelayanan.class, dto.getAspekId()));
        sub.setNama(dto.getNama());
        sub.setUrutan(dto.getUrutan());
    }

    private SubKategoriView view(Long id) {
        CriteriaBuilder<SubKategori> cb = cbf.create(em, SubKategori.class).where("id").eq(id);
        return evm.applySetting(EntityViewSetting.create(SubKategoriView.class), cb)
                .getResultList().stream().findFirst().orElse(null);
    }
}
