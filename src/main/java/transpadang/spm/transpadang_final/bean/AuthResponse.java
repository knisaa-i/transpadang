package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import transpadang.spm.transpadang_final.view.UserView;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respons autentikasi berisi token JWT")
public class AuthResponse {

    @Schema(description = "Token JWT yang dipakai pada header Authorization")
    private String accessToken;

    @Schema(description = "Tipe token", example = "Bearer")
    private String tokenType;

    @Schema(description = "Masa berlaku token (ms)", example = "86400000")
    private long expiresIn;

    @Schema(description = "Data user yang login (Blazebit entity view)")
    private UserView user;
}
