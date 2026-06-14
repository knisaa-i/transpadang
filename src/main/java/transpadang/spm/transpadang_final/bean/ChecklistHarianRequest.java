package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Permintaan pembuatan / perubahan checklist harian")
public class ChecklistHarianRequest {

    @NotNull
    @Schema(description = "ID template (jenis form)", example = "1")
    private Long templateId;

    @NotNull
    @Schema(description = "ID koridor", example = "1")
    private Long koridorId;

    @Schema(description = "Hari", example = "Senin")
    private String hari;

    @NotNull
    @Schema(description = "Tanggal", example = "2026-06-12")
    private LocalDate tanggal;

    @Schema(description = "ID bus (wajib bila subjek = BUS)", example = "1")
    private Long busId;

    @Schema(description = "Nama pramugara (wajib bila subjek = PRAMUGARA)")
    private String namaPramugara;

    @Schema(description = "Shift (wajib bila subjek = PRAMUGARA)", example = "Pagi")
    private String shift;

    @Valid
    @Schema(description = "Daftar hasil checklist per item")
    private List<ChecklistDetailRequest> details;
}
