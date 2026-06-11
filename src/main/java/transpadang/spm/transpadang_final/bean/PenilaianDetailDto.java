package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Detail nilai capaian SPM per bus per indikator")
public class PenilaianDetailDto {

    @Schema(description = "ID detail", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "ID bus", example = "1")
    private Long busId;

    @Schema(description = "Nomor lambung bus", example = "46", accessMode = Schema.AccessMode.READ_ONLY)
    private String noLambung;

    @Schema(description = "ID indikator", example = "1")
    private Long indikatorId;

    @Schema(description = "Uraian indikator", accessMode = Schema.AccessMode.READ_ONLY)
    private String indikatorUraian;

    @Schema(description = "Bobot indikator", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal bobot;

    @Schema(description = "Nilai capaian (skala 10-100)", example = "100.00")
    private BigDecimal nilaiCapaian;

    @Schema(description = "Skor terbobot = nilai capaian * bobot", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal skorTerbobot;

    @Schema(description = "Catatan penilai")
    private String catatan;
}
