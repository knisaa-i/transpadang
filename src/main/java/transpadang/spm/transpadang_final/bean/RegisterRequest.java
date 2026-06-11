package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Permintaan registrasi user baru")
public class RegisterRequest {

    @NotBlank
    @Schema(description = "Username unik", example = "staf01")
    private String username;

    @NotBlank
    @Size(min = 6, message = "Password minimal 6 karakter")
    @Schema(description = "Password", example = "rahasia123")
    private String password;

    @NotBlank
    @Schema(description = "Nama lengkap", example = "Aulil Amri, S.E.")
    private String nama;

    @Schema(description = "Jabatan", example = "Staf Operasional")
    private String jabatan;

    @Schema(description = "Peran/role (MAKER, CHECKER, APPROVER, ADMIN)", example = "MAKER")
    private String role;
}
