package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.Koridor;

@EntityView(Koridor.class)
public interface KoridorView {

    @IdMapping
    Long getId();

    Integer getNomor();

    String getNama();
}
