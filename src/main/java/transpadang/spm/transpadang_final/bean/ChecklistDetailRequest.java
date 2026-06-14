package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Permintaan input satu baris hasil checklist")
public class ChecklistDetailRequest {

    @NotNull
    @Schema(description = "ID item checklist yang dicentang", example = "1")
    private Long itemId;

    @Schema(description = "ID bus (diisi untuk form objek Bus; null untuk Pramugara/Korlap)", example = "1")
    private Long busId;

    @Schema(description = "Hasil: true = Baik/Ada/OK, false = Rusak/Tidak/NotOK, null = belum diisi", example = "true")
    private Boolean hasil;

    @Schema(description = "Keterangan tambahan")
    private String keterangan;
}
