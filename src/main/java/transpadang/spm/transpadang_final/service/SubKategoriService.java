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
import transpadang.spm.transpadang_final.bean.SubKategoriDto;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;
import transpadang.spm.transpadang_final.entity.QSubKategori;
import transpadang.spm.transpadang_final.entity.SubKategori;
import transpadang.spm.transpadang_final.helper.CurrentUser;
import transpadang.spm.transpadang_final.view.SubKategoriView;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubKategoriService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<SubKategoriView> findAll() {
        var q = new QSubKategori("s");
        CriteriaBuilder<SubKategori> query = cbf.create(em, SubKategori.class)
                .from(SubKategori.class, q.getMetadata().getName())
                .orderByAsc(q.aspek.id.toString())
                .orderByAsc(q.urutan.toString())
                .orderByAsc(q.id.toString());
        return evm.applySetting(EntityViewSetting.create(SubKategoriView.class), query).getResultList();
    }

    @Transactional(readOnly = true)
    public List<SubKategoriView> findByAspek(Long aspekId) {
        var q = new QSubKategori("s");
        CriteriaBuilder<SubKategori> query = cbf.create(em, SubKategori.class)
                .from(SubKategori.class, q.getMetadata().getName());
        if (aspekId != null) {
            query.where(q.aspek.id.toString()).eq(aspekId);
        }
        query.orderByAsc(q.urutan.toString()).orderByAsc(q.id.toString());
        return evm.applySetting(EntityViewSetting.create(SubKategoriView.class), query).getResultList();
    }

    @Transactional(readOnly = true)
    public SubKategoriView findById(Long id) {
        var q = new QSubKategori("s");
        var query = cbf.create(em, SubKategori.class).from(SubKategori.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        var result = evm.applySetting(EntityViewSetting.create(SubKategoriView.class), query).getResultList();
        if (result.isEmpty()) {
            throw new EntityNotFoundException("Sub kategori tidak ditemukan: " + id);
        }
        return result.getFirst();
    }

    @Transactional
    public SubKategoriView create(SubKategoriDto dto) {
        var currUser = currentUser.getCurrentUser();
        if (currUser == null){
            throw new RuntimeException("User Belum Login");
        }
        var sub = new SubKategori();
        apply(sub, dto);
        em.persist(sub);
        em.flush();
        return findById(sub.getId());
    }

    @Transactional
    public SubKategoriView update(Long id, SubKategoriDto dto) {
        var currUser = currentUser.getCurrentUser();
        if (currUser == null){
            throw new RuntimeException("User Belum Login");
        }
        var sub = findEntity(id);
        apply(sub, dto);
        em.flush();
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        var currUser = currentUser.getCurrentUser();
        if (currUser == null){
            throw new RuntimeException("User Belum Login");
        }
        em.remove(findEntity(id));
    }

    /** Ambil entity (managed) via cbf + QueryDSL Q-class, untuk update/delete. */
    private SubKategori findEntity(Long id) {
        var q = new QSubKategori("s");
        var list = cbf.create(em, SubKategori.class).from(SubKategori.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id)
                .getResultList();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("Sub kategori tidak ditemukan: " + id);
        }
        return list.getFirst();
    }

    private void apply(SubKategori sub, SubKategoriDto dto) {
        sub.setAspek(em.getReference(AspekPelayanan.class, dto.getAspekId()));
        sub.setNama(dto.getNama());
        sub.setUrutan(dto.getUrutan());
    }
}