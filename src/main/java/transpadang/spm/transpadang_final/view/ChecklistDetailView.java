package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.Bus;
import transpadang.spm.transpadang_final.entity.ChecklistDetail;
import transpadang.spm.transpadang_final.entity.ChecklistItem;
import java.math.BigDecimal;

@EntityView(ChecklistDetail.class)
public interface ChecklistDetailView {

    @IdMapping
    Long getId();

    BusRefView getBus();

    @EntityView(Bus.class)
    interface BusRefView {
        @IdMapping
        Long getId();

        String getNoLambung();
    }

    ItemRefView getItem();

    @EntityView(ChecklistItem.class)
    interface ItemRefView {
        @IdMapping
        Long getId();

        String getNomorUrut();

        String getUraian();

        String getGrup();

        String getSanksiDenda();

        BigDecimal getNilaiDenda();
    }

    Boolean getHasil();

    String getKeterangan();
}
