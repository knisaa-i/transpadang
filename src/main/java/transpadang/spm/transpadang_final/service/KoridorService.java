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
import transpadang.spm.transpadang_final.bean.KoridorDto;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.entity.QKoridor;
import transpadang.spm.transpadang_final.helper.CurrentUser;
import transpadang.spm.transpadang_final.view.KoridorView;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KoridorService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<KoridorView> findAll() {
        var q = new QKoridor("k");
        CriteriaBuilder<Koridor> query = cbf.create(em, Koridor.class).from(Koridor.class, q.getMetadata().getName())
                .orderByAsc(q.nomor.toString())
                .orderByAsc(q.id.toString());
        return evm.applySetting(EntityViewSetting.create(KoridorView.class), query).getResultList();
    }

    @Transactional(readOnly = true)
    public KoridorView findById(Long id) {
        var q = new QKoridor("k");
        var query = cbf.create(em, Koridor.class).from(Koridor.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        var result = evm.applySetting(EntityViewSetting.create(KoridorView.class), query).getResultList();
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Koridor tidak ditemukan: " + id);
        }
        return result.getFirst();
    }

    @Transactional
    public KoridorView create(KoridorDto dto) {
        var currUser = currentUser.getCurrentUser();
        if (currUser == null){
            throw new RuntimeException("User Belum Login");
        }
        var koridor = new Koridor();
        apply(koridor, dto);
        em.persist(koridor);
        em.flush();
        return findById(koridor.getId());
    }

    @Transactional
    public KoridorView update(Long id, KoridorDto dto) {
        var currUser = currentUser.getCurrentUser();
        if (currUser == null){
            throw new RuntimeException("User Belum Login");
        }
        var koridor = findEntity(id);
        apply(koridor, dto);
        em.flush();
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        em.remove(findEntity(id));
    }

    /** Ambil entity (managed) via cbf + QueryDSL Q-class, untuk update/delete. */
    private Koridor findEntity(Long id) {
        var q = new QKoridor("k");
        var list = cbf.create(em, Koridor.class).from(Koridor.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Koridor tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private void apply(Koridor koridor, KoridorDto dto) {
        koridor.setNomor(dto.getNomor());
        koridor.setNama(dto.getNama());
    }
}