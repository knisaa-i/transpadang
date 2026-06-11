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
import transpadang.spm.transpadang_final.bean.BusDto;
import transpadang.spm.transpadang_final.entity.Bus;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.view.BusView;

import java.util.List;

/**
 * Service master Bus.
 * Response berupa Blazebit entity view ({@link BusView}); filter dinamis via CriteriaBuilder.
 */
@Service
public class BusService {

    @PersistenceContext
    private EntityManager em;

    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    public BusService(CriteriaBuilderFactory cbf, EntityViewManager evm) {
        this.cbf = cbf;
        this.evm = evm;
    }

    @Transactional(readOnly = true)
    public List<BusView> findAll() {
        return search(null, null);
    }

    @Transactional(readOnly = true)
    public List<BusView> search(Long koridorId, Boolean aktif) {
        CriteriaBuilder<Bus> cb = cbf.create(em, Bus.class);
        if (koridorId != null) {
            cb.where("koridor.id").eq(koridorId);
        }
        if (aktif != null) {
            cb.where("aktif").eq(aktif);
        }
        cb.orderByAsc("noLambung").orderByAsc("id");
        return evm.applySetting(EntityViewSetting.create(BusView.class), cb).getResultList();
    }

    @Transactional(readOnly = true)
    public BusView findById(Long id) {
        BusView view = view(id);
        if (view == null) {
            throw new EntityNotFoundException("Bus tidak ditemukan: " + id);
        }
        return view;
    }

    @Transactional
    public BusView create(BusDto dto) {
        Bus bus = new Bus();
        apply(bus, dto);
        em.persist(bus);
        em.flush();
        return view(bus.getId());
    }

    @Transactional
    public BusView update(Long id, BusDto dto) {
        Bus bus = em.find(Bus.class, id);
        if (bus == null) {
            throw new EntityNotFoundException("Bus tidak ditemukan: " + id);
        }
        apply(bus, dto);
        em.flush();
        return view(id);
    }

    @Transactional
    public void delete(Long id) {
        Bus bus = em.find(Bus.class, id);
        if (bus == null) {
            throw new EntityNotFoundException("Bus tidak ditemukan: " + id);
        }
        em.remove(bus);
    }

    private void apply(Bus bus, BusDto dto) {
        bus.setKoridor(em.getReference(Koridor.class, dto.getKoridorId()));
        bus.setNoLambung(dto.getNoLambung());
        bus.setPlatNomor(dto.getPlatNomor());
        bus.setAktif(dto.getAktif() != null ? dto.getAktif() : Boolean.TRUE);
    }

    private BusView view(Long id) {
        CriteriaBuilder<Bus> cb = cbf.create(em, Bus.class).where("id").eq(id);
        return evm.applySetting(EntityViewSetting.create(BusView.class), cb)
                .getResultList().stream().findFirst().orElse(null);
    }
}
