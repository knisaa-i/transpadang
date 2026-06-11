package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Sub kategori di bawah aspek (mis. Halte, Bus, Manusia)")
public class SubKategoriDto {

    @Schema(description = "ID sub kategori", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "ID aspek induk", example = "1")
    private Long aspekId;

    @Schema(description = "Nama aspek induk", accessMode = Schema.AccessMode.READ_ONLY)
    private String aspekNama;

    @NotBlank
    @Schema(description = "Nama sub kategori", example = "Halte")
    private String nama;

    @Schema(description = "Urutan tampil", example = "1")
    private Integer urutan;
}
