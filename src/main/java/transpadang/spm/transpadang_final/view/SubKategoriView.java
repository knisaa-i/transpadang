package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import transpadang.spm.transpadang_final.entity.SubKategori;

@EntityView(SubKategori.class)
public interface SubKategoriView {

    @IdMapping
    Long getId();

    @Mapping("aspek.id")
    Long getAspekId();

    @Mapping("aspek.nama")
    String getAspekNama();

    String getNama();

    Integer getUrutan();
}
