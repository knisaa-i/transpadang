package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import transpadang.spm.transpadang_final.entity.IndikatorSpm;

import java.math.BigDecimal;

@EntityView(IndikatorSpm.class)
public interface IndikatorSpmView {

    @IdMapping
    Long getId();

    @Mapping("aspek.id")
    Long getAspekId();

    @Mapping("aspek.nama")
    String getAspekNama();

    @Mapping("subKategori.id")
    Long getSubKategoriId();

    @Mapping("subKategori.nama")
    String getSubKategoriNama();

    String getNomorUrut();

    String getUraian();

    String getSpmIndikator();

    String getSpmNilai();

    BigDecimal getTargetCapaian();

    BigDecimal getBobot();

    Boolean getAktif();
}
