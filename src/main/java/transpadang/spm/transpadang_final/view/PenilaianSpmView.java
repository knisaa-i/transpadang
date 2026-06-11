package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import transpadang.spm.transpadang_final.entity.PenilaianSpm;
import transpadang.spm.transpadang_final.entity.StatusPenilaian;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@EntityView(PenilaianSpm.class)
public interface PenilaianSpmView {

    @IdMapping
    Long getId();

    @Mapping("koridor.id")
    Long getKoridorId();

    @Mapping("koridor.nama")
    String getKoridorNama();

    String getHari();

    LocalDate getTanggal();

    StatusPenilaian getStatus();

    @Mapping("maker.id")
    Long getMakerId();

    @Mapping("maker.nama")
    String getMakerNama();

    @Mapping("checker.id")
    Long getCheckerId();

    @Mapping("checker.nama")
    String getCheckerNama();

    @Mapping("approver.id")
    Long getApproverId();

    @Mapping("approver.nama")
    String getApproverNama();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    List<PenilaianDetailView> getDetails();

    /** Total skor terbobot dihitung dari seluruh detail (tidak disimpan). */
    default BigDecimal getTotalCapaian() {
        List<PenilaianDetailView> d = getDetails();
        if (d == null) {
            return BigDecimal.ZERO;
        }
        return d.stream()
                .map(PenilaianDetailView::getSkorTerbobot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
