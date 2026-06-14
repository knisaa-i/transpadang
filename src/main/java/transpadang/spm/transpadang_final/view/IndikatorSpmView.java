package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;
import transpadang.spm.transpadang_final.entity.IndikatorSpm;
import transpadang.spm.transpadang_final.entity.SubKategori;
import java.math.BigDecimal;

@EntityView(IndikatorSpm.class)
public interface IndikatorSpmView {

    @IdMapping
    Long getId();

    AspekView getAspek();

    @EntityView(AspekPelayanan.class)
    interface AspekView {
        @IdMapping
        Long getId();

        String getNama();
    }

    SubKategoriRefView getSubKategori();

    @EntityView(SubKategori.class)
    interface SubKategoriRefView {
        @IdMapping
        Long getId();

        String getNama();
    }

    String getNomorUrut();

    String getUraian();

    String getSpmIndikator();

    String getSpmNilai();

    BigDecimal getTargetCapaian();

    BigDecimal getBobot();

    Boolean getAktif();
}
