package transpadang.spm.transpadang_final.service;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.bean.HalteDto;
import transpadang.spm.transpadang_final.entity.Halte;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.entity.QHalte;
import transpadang.spm.transpadang_final.view.HalteView;

import java.util.List;

/**
 * Service master Halte (unit halte per koridor).
 * Query memakai CriteriaBuilderFactory + QueryDSL Q-class (path type-safe),
 * response berupa Blazebit entity view ({@link HalteView}).
 */
@Service
@RequiredArgsConstructor
public class HalteService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    @Transactional(readOnly = true)
    public List<HalteView> findAll() {
        return search(null, null);
    }

    @Transactional(readOnly = true)
    public List<HalteView> search(Long koridorId, Boolean aktif) {
        var q = new QHalte("h");
        CriteriaBuilder<Halte> query = cbf.create(em, Halte.class)
                .from(Halte.class, q.getMetadata().getName());
        if (koridorId != null) {
            query.where(q.koridor.id.toString()).eq(koridorId);
        }
        if (aktif != null) {
            query.where(q.aktif.toString()).eq(aktif);
        }
        query.orderByAsc(q.nomor.toString()).orderByAsc(q.id.toString());
        return evm.applySetting(EntityViewSetting.create(HalteView.class), query).getResultList();
    }

    @Transactional(readOnly = true)
    public HalteView findById(Long id) {
        var q = new QHalte("h");
        var query = cbf.create(em, Halte.class).from(Halte.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        var result = evm.applySetting(EntityViewSetting.create(HalteView.class), query).getResultList();
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Halte tidak ditemukan: " + id);
        }
        return result.getFirst();
    }

    @Transactional
    public HalteView create(HalteDto dto) {
        var halte = new Halte();
        apply(halte, dto);
        em.persist(halte);
        em.flush();
        return findById(halte.getId());
    }

    @Transactional
    public HalteView update(Long id, HalteDto dto) {
        var halte = findEntity(id);
        apply(halte, dto);
        em.flush();
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        em.remove(findEntity(id));
    }

    /** Ambil entity (managed) via cbf + QueryDSL Q-class, untuk update/delete. */
    private Halte findEntity(Long id) {
        var q = new QHalte("h");
        var list = cbf.create(em, Halte.class).from(Halte.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Halte tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private void apply(Halte halte, HalteDto dto) {
        halte.setKoridor(em.getReference(Koridor.class, dto.getKoridorId()));
        halte.setNomor(dto.getNomor());
        halte.setNama(dto.getNama());
        halte.setAktif(dto.getAktif() != null ? dto.getAktif() : Boolean.TRUE);
    }
}
