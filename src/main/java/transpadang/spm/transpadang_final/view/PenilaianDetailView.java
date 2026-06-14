package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.Bus;
import transpadang.spm.transpadang_final.entity.Halte;
import transpadang.spm.transpadang_final.entity.IndikatorSpm;
import transpadang.spm.transpadang_final.entity.PenilaianDetail;

import java.math.BigDecimal;

@EntityView(PenilaianDetail.class)
public interface PenilaianDetailView {

    @IdMapping
    Long getId();

    BusView getBus();

    @EntityView(Bus.class)
    interface BusView {
        @IdMapping
        Long getId();

        String getNoLambung();

    }

    HalteView getHalte();

    @EntityView(Halte.class)
    interface HalteView {
        @IdMapping
        Long getId();

        Integer getNomor();

        String getNama();
    }

    IndikatorView getIndikator();

    @EntityView(IndikatorSpm.class)
    interface IndikatorView {
        @IdMapping
        Long getId();

        String getUraian();

        BigDecimal getBobot();

    }

    BigDecimal getNilaiCapaian();

    BigDecimal getSkorTerbobot();

    String getCatatan();
}
