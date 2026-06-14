package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.Bus;
import transpadang.spm.transpadang_final.entity.ChecklistHarian;
import transpadang.spm.transpadang_final.entity.ChecklistTemplate;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.entity.StatusChecklist;
import transpadang.spm.transpadang_final.entity.SubjekChecklist;
import transpadang.spm.transpadang_final.entity.TipeJawaban;
import transpadang.spm.transpadang_final.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@EntityView(ChecklistHarian.class)
public interface ChecklistHarianView {

    @IdMapping
    Long getId();

    TemplateRefView getTemplate();

    @EntityView(ChecklistTemplate.class)
    interface TemplateRefView {
        @IdMapping
        Long getId();

        String getKode();

        String getNama();

        TipeJawaban getTipeJawaban();

        SubjekChecklist getSubjek();

        Boolean getPakaiDenda();

        String getJudulTtd();
    }

    KoridorRefView getKoridor();

    @EntityView(Koridor.class)
    interface KoridorRefView {
        @IdMapping
        Long getId();

        Integer getNomor();

        String getNama();
    }

    String getHari();

    LocalDate getTanggal();

    BusRefView getBus();

    @EntityView(Bus.class)
    interface BusRefView {
        @IdMapping
        Long getId();

        String getNoLambung();

        String getPlatNomor();
    }

    String getNamaPramugara();

    String getShift();

    StatusChecklist getStatus();

    BigDecimal getTotalDenda();

    UserRefView getDibuatOleh();

    UserRefView getDiketahuiOleh();

    @EntityView(User.class)
    interface UserRefView {
        @IdMapping
        Long getId();

        String getNama();
    }

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    List<ChecklistDetailView> getDetails();
}
