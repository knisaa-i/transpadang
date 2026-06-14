package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.ChecklistTemplate;
import transpadang.spm.transpadang_final.entity.SubjekChecklist;
import transpadang.spm.transpadang_final.entity.TipeJawaban;

@EntityView(ChecklistTemplate.class)
public interface ChecklistTemplateView {

    @IdMapping
    Long getId();

    String getKode();

    String getNama();

    TipeJawaban getTipeJawaban();

    SubjekChecklist getSubjek();

    Boolean getPakaiDenda();

    String getJudulTtd();

    Boolean getAktif();
}
