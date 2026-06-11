package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Aspek pelayanan dasar (Keamanan, Keselamatan, Kenyamanan, Kesetaraan, Keteraturan)")
public class AspekPelayananDto {

    @Schema(description = "ID aspek", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank
    @Schema(description = "Nama aspek", example = "Keamanan")
    private String nama;

    @Schema(description = "Urutan tampil", example = "1")
    private Integer urutan;
}
