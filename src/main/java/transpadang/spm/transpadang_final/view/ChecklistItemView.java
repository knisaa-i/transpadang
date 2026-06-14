package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.ChecklistItem;
import java.math.BigDecimal;

@EntityView(ChecklistItem.class)
public interface ChecklistItemView {

    @IdMapping
    Long getId();

    String getNomorUrut();

    String getUraian();

    String getGrup();

    String getSanksiDenda();

    BigDecimal getNilaiDenda();

    Integer getUrutan();

    Boolean getAktif();

    ParentRefView getParent();

    @EntityView(ChecklistItem.class)
    interface ParentRefView {
        @IdMapping
        Long getId();

        String getNomorUrut();
    }
}
