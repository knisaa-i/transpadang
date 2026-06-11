package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Data bus (unit kendaraan) pada suatu koridor")
public class BusDto {

    @Schema(description = "ID bus", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "ID koridor pemilik bus", example = "1")
    private Long koridorId;

    @Schema(description = "Nama koridor", accessMode = Schema.AccessMode.READ_ONLY)
    private String koridorNama;

    @NotBlank
    @Schema(description = "Nomor lambung bus", example = "46")
    private String noLambung;

    @Schema(description = "Plat nomor kendaraan", example = "BA 1234 AB")
    private String platNomor;

    @Schema(description = "Status aktif", example = "true")
    private Boolean aktif;
}
