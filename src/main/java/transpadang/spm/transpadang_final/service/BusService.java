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
import transpadang.spm.transpadang_final.bean.BusDto;
import transpadang.spm.transpadang_final.entity.Bus;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.entity.QBus;
import transpadang.spm.transpadang_final.view.BusView;

import java.util.List;

/**
 * Service master Bus.
 * Query memakai CriteriaBuilderFactory + QueryDSL Q-class (path type-safe),
 * response berupa Blazebit entity view ({@link BusView}).
 */

@Service
@RequiredArgsConstructor
public class BusService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    @Transactional(readOnly = true)
    public List<BusView> findAll() {
        return search(null, null);
    }

    @Transactional(readOnly = true)
    public List<BusView> search(Long koridorId, Boolean aktif) {
        var q = new QBus("b");
        CriteriaBuilder<Bus> query = cbf.create(em, Bus.class)
                .from(Bus.class, q.getMetadata().getName());
        if (koridorId != null) {
            query.where(q.koridor.id.toString()).eq(koridorId);
        }
        if (aktif != null) {
            query.where(q.aktif.toString()).eq(aktif);
        }
        query.orderByAsc(q.noLambung.toString()).orderByAsc(q.id.toString());
        return evm.applySetting(EntityViewSetting.create(BusView.class), query).getResultList();
    }

    @Transactional(readOnly = true)
    public BusView findById(Long id) {
        var q = new QBus("b");
        var query = cbf.create(em, Bus.class).from(Bus.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        var result = evm.applySetting(EntityViewSetting.create(BusView.class), query).getResultList();
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Bus tidak ditemukan: " + id);
        }
        return result.getFirst();
    }

    @Transactional
    public BusView create(BusDto dto) {
        var bus = new Bus();
        apply(bus, dto);
        em.persist(bus);
        em.flush();
        return findById(bus.getId());
    }

    @Transactional
    public BusView update(Long id, BusDto dto) {
        var bus = findEntity(id);
        apply(bus, dto);
        em.flush();
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        em.remove(findEntity(id));
    }

    /** Ambil entity (managed) via cbf + QueryDSL Q-class, untuk update/delete. */
    private Bus findEntity(Long id) {
        var q = new QBus("b");
        var list = cbf.create(em, Bus.class).from(Bus.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Bus tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private void apply(Bus bus, BusDto dto) {
        bus.setKoridor(em.getReference(Koridor.class, dto.getKoridorId()));
        bus.setNoLambung(dto.getNoLambung());
        bus.setPlatNomor(dto.getPlatNomor());
        bus.setAktif(dto.getAktif() != null ? dto.getAktif() : Boolean.TRUE);
    }
}