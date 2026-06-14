package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.Halte;
import transpadang.spm.transpadang_final.entity.Koridor;

@EntityView(Halte.class)
public interface HalteView {

    @IdMapping
    Long getId();

    KoridorView getKoridor();

    @EntityView(Koridor.class)
    interface KoridorView {
        @IdMapping
        Long getId();

        Integer getNomor();

        String getNama();
    }

    Integer getNomor();

    String getNama();

    Boolean getAktif();
}
