package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pembungkus standar respons API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pembungkus standar respons API")
public class ApiResponse<T> {

    @Schema(description = "Status keberhasilan request", example = "true")
    private boolean success;

    @Schema(description = "Pesan informatif", example = "OK")
    private String message;

    @Schema(description = "Data payload")
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
