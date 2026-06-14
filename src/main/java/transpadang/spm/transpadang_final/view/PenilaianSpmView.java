package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.Koridor;
import transpadang.spm.transpadang_final.entity.PenilaianSpm;
import transpadang.spm.transpadang_final.entity.StatusPenilaian;
import transpadang.spm.transpadang_final.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@EntityView(PenilaianSpm.class)
public interface PenilaianSpmView {

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

    String getHari();

    LocalDate getTanggal();

    StatusPenilaian getStatus();

    UserRefView getMaker();

    UserRefView getChecker();

    UserRefView getApprover();

    @EntityView(User.class)
    interface UserRefView {
        @IdMapping
        Long getId();

        String getNama();

    }

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
