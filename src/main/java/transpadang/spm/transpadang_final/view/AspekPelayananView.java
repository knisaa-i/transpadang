package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.AspekPelayanan;

@EntityView(AspekPelayanan.class)
public interface AspekPelayananView {

    @IdMapping
    Long getId();

    String getNama();

    Integer getUrutan();
}
