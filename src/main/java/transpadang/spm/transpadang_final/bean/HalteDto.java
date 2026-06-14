package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Data halte (unit) pada suatu koridor")
public class HalteDto {

    @Schema(description = "ID halte", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "ID koridor pemilik halte", example = "1")
    private Long koridorId;

    @Schema(description = "Nama koridor", accessMode = Schema.AccessMode.READ_ONLY)
    private String koridorNama;

    @Schema(description = "Nomor halte", example = "1")
    private Integer nomor;

    @Schema(description = "Nama halte", example = "Halte 1")
    private String nama;

    @Schema(description = "Status aktif", example = "true")
    private Boolean aktif;
}
