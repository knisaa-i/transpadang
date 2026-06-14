package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;
import transpadang.spm.transpadang_final.entity.SubKategori;

@EntityView(SubKategori.class)
public interface SubKategoriView {

        @IdMapping
        Long getId();

        AspekView getAspek();

        @EntityView(AspekPelayanan.class)
        interface AspekView {
            @IdMapping
            Long getId();

            String getNama();
        }

    String getNama();

    Integer getUrutan();
}
