package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Permintaan pembuatan / perubahan penilaian SPM")
public class PenilaianSpmRequest {

    @NotNull
    @Schema(description = "ID koridor", example = "1")
    private Long koridorId;

    @Schema(description = "Hari penilaian", example = "Senin")
    private String hari;

    @NotNull
    @Schema(description = "Tanggal penilaian", example = "2026-06-10")
    private LocalDate tanggal;

    @Schema(description = "ID maker (Staf Operasional yang merekap)", example = "1")
    private Long makerId;

    @Valid
    @Schema(description = "Daftar detail penilaian")
    private List<PenilaianDetailRequest> details;
}
