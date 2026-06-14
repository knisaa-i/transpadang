package transpadang.spm.transpadang_final.service;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.bean.AspekPelayananDto;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;
import transpadang.spm.transpadang_final.entity.QAspekPelayanan;
import transpadang.spm.transpadang_final.view.AspekPelayananView;

import java.util.List;

/**
 * Service master Aspek Pelayanan.
 * Query memakai CriteriaBuilderFactory + QueryDSL Q-class (path type-safe),
 * response berupa Blazebit entity view ({@link AspekPelayananView}).
 */
@Service
@RequiredArgsConstructor
public class AspekPelayananService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    @Transactional(readOnly = true)
    public List<AspekPelayananView> findAll() {
        var q = new QAspekPelayanan("a");
        var query = cbf.create(em, AspekPelayanan.class).from(AspekPelayanan.class, q.getMetadata().getName())
                .orderByAsc(q.urutan.toString())
                .orderByAsc(q.id.toString());
        return evm.applySetting(EntityViewSetting.create(AspekPelayananView.class), query).getResultList();
    }

    @Transactional(readOnly = true)
    public AspekPelayananView findById(Long id) {
        var q = new QAspekPelayanan("a");
        var query = cbf.create(em, AspekPelayanan.class).from(AspekPelayanan.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        var result = evm.applySetting(EntityViewSetting.create(AspekPelayananView.class), query).getResultList();
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Aspek pelayanan tidak ditemukan: " + id);
        }
        return result.getFirst();
    }

    @Transactional
    public AspekPelayananView create(AspekPelayananDto dto) {
        var aspek = new AspekPelayanan();
        apply(aspek, dto);
        em.persist(aspek);
        em.flush();
        return findById(aspek.getId());
    }

    @Transactional
    public AspekPelayananView update(Long id, AspekPelayananDto dto) {
        var aspek = findEntity(id);
        apply(aspek, dto);
        em.flush();
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        em.remove(findEntity(id));
    }

    /** Ambil entity (managed) via cbf + QueryDSL Q-class, untuk update/delete. */
    private AspekPelayanan findEntity(Long id) {
        var q = new QAspekPelayanan("a");
        var list = cbf.create(em, AspekPelayanan.class).from(AspekPelayanan.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Aspek pelayanan tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private void apply(AspekPelayanan aspek, AspekPelayananDto dto) {
        aspek.setNama(dto.getNama());
        aspek.setUrutan(dto.getUrutan());
    }
}