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
import transpadang.spm.transpadang_final.bean.KoridorDto;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.view.KoridorView;

import java.util.List;

/**
 * Service master Koridor.
 * Response berupa Blazebit entity view ({@link KoridorView}).
 */
@Service
public class KoridorService {

    @PersistenceContext
    private EntityManager em;

    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    public KoridorService(CriteriaBuilderFactory cbf, EntityViewManager evm) {
        this.cbf = cbf;
        this.evm = evm;
    }

    @Transactional(readOnly = true)
    public List<KoridorView> findAll() {
        CriteriaBuilder<Koridor> cb = cbf.create(em, Koridor.class)
                .orderByAsc("nomor").orderByAsc("id");
        return evm.applySetting(EntityViewSetting.create(KoridorView.class), cb).getResultList();
    }

    @Transactional(readOnly = true)
    public KoridorView findById(Long id) {
        KoridorView view = view(id);
        if (view == null) {
            throw new EntityNotFoundException("Koridor tidak ditemukan: " + id);
        }
        return view;
    }

    @Transactional
    public KoridorView create(KoridorDto dto) {
        Koridor koridor = new Koridor();
        apply(koridor, dto);
        em.persist(koridor);
        em.flush();
        return view(koridor.getId());
    }

    @Transactional
    public KoridorView update(Long id, KoridorDto dto) {
        Koridor koridor = em.find(Koridor.class, id);
        if (koridor == null) {
            throw new EntityNotFoundException("Koridor tidak ditemukan: " + id);
        }
        apply(koridor, dto);
        em.flush();
        return view(id);
    }

    @Transactional
    public void delete(Long id) {
        Koridor koridor = em.find(Koridor.class, id);
        if (koridor == null) {
            throw new EntityNotFoundException("Koridor tidak ditemukan: " + id);
        }
        em.remove(koridor);
    }

    private void apply(Koridor koridor, KoridorDto dto) {
        koridor.setNomor(dto.getNomor());
        koridor.setNama(dto.getNama());
    }

    private KoridorView view(Long id) {
        CriteriaBuilder<Koridor> cb = cbf.create(em, Koridor.class).where("id").eq(id);
        return evm.applySetting(EntityViewSetting.create(KoridorView.class), cb)
                .getResultList().stream().findFirst().orElse(null);
    }
}
