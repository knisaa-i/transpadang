package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Data koridor Trans Padang")
public class KoridorDto {

    @Schema(description = "ID koridor", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Nomor koridor", example = "6")
    private Integer nomor;

    @Schema(description = "Nama / rute koridor", example = "Koridor 6")
    private String nama;
}
