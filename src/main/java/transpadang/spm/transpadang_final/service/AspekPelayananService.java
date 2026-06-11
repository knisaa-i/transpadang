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
import transpadang.spm.transpadang_final.bean.AspekPelayananDto;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;
import transpadang.spm.transpadang_final.view.AspekPelayananView;

import java.util.List;

/**
 * Service master Aspek Pelayanan.
 * Response berupa Blazebit entity view ({@link AspekPelayananView}).
 */
@Service
public class AspekPelayananService {

    @PersistenceContext
    private EntityManager em;

    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    public AspekPelayananService(CriteriaBuilderFactory cbf, EntityViewManager evm) {
        this.cbf = cbf;
        this.evm = evm;
    }

    @Transactional(readOnly = true)
    public List<AspekPelayananView> findAll() {
        CriteriaBuilder<AspekPelayanan> cb = cbf.create(em, AspekPelayanan.class)
                .orderByAsc("urutan").orderByAsc("id");
        return evm.applySetting(EntityViewSetting.create(AspekPelayananView.class), cb).getResultList();
    }

    @Transactional(readOnly = true)
    public AspekPelayananView findById(Long id) {
        AspekPelayananView view = view(id);
        if (view == null) {
            throw new EntityNotFoundException("Aspek pelayanan tidak ditemukan: " + id);
        }
        return view;
    }

    @Transactional
    public AspekPelayananView create(AspekPelayananDto dto) {
        AspekPelayanan aspek = new AspekPelayanan();
        apply(aspek, dto);
        em.persist(aspek);
        em.flush();
        return view(aspek.getId());
    }

    @Transactional
    public AspekPelayananView update(Long id, AspekPelayananDto dto) {
        AspekPelayanan aspek = em.find(AspekPelayanan.class, id);
        if (aspek == null) {
            throw new EntityNotFoundException("Aspek pelayanan tidak ditemukan: " + id);
        }
        apply(aspek, dto);
        em.flush();
        return view(id);
    }

    @Transactional
    public void delete(Long id) {
        AspekPelayanan aspek = em.find(AspekPelayanan.class, id);
        if (aspek == null) {
            throw new EntityNotFoundException("Aspek pelayanan tidak ditemukan: " + id);
        }
        em.remove(aspek);
    }

    private void apply(AspekPelayanan aspek, AspekPelayananDto dto) {
        aspek.setNama(dto.getNama());
        aspek.setUrutan(dto.getUrutan());
    }

    private AspekPelayananView view(Long id) {
        CriteriaBuilder<AspekPelayanan> cb = cbf.create(em, AspekPelayanan.class).where("id").eq(id);
        return evm.applySetting(EntityViewSetting.create(AspekPelayananView.class), cb)
                .getResultList().stream().findFirst().orElse(null);
    }
}
