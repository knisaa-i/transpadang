package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import transpadang.spm.transpadang_final.entity.PenilaianDetail;

import java.math.BigDecimal;

@EntityView(PenilaianDetail.class)
public interface PenilaianDetailView {

    @IdMapping
    Long getId();

    @Mapping("bus.id")
    Long getBusId();

    @Mapping("bus.noLambung")
    String getNoLambung();

    @Mapping("indikator.id")
    Long getIndikatorId();

    @Mapping("indikator.uraian")
    String getIndikatorUraian();

    @Mapping("indikator.bobot")
    BigDecimal getBobot();

    BigDecimal getNilaiCapaian();

    BigDecimal getSkorTerbobot();

    String getCatatan();
}
