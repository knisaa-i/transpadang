package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Permintaan input satu baris detail penilaian")
public class PenilaianDetailRequest {

    @Schema(description = "ID bus (diisi untuk kategori Bus)", example = "1")
    private Long busId;

    @Schema(description = "ID halte (diisi untuk kategori Halte)", example = "1")
    private Long halteId;

    @NotNull
    @Schema(description = "ID indikator yang dinilai", example = "1")
    private Long indikatorId;

    @NotNull
    @Schema(description = "Nilai capaian (skala 10-100)", example = "100.00")
    private BigDecimal nilaiCapaian;

    @Schema(description = "Catatan penilai")
    private String catatan;
}
