package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import transpadang.spm.transpadang_final.entity.Bus;

@EntityView(Bus.class)
public interface BusView {

    @IdMapping
    Long getId();

    @Mapping("koridor.id")
    Long getKoridorId();

    @Mapping("koridor.nama")
    String getKoridorNama();

    String getNoLambung();

    String getPlatNomor();

    Boolean getAktif();
}
