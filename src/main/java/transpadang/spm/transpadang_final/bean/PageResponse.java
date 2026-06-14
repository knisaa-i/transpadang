package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Pembungkus hasil paginasi (dipakai bersama Blazebit PaginatedCriteriaBuilder).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hasil data dengan paginasi")
public class PageResponse<T> {

    @Schema(description = "Isi data pada halaman ini")
    private List<T> content;

    @Schema(description = "Nomor halaman (0-based)", example = "0")
    private int page;

    @Schema(description = "Jumlah data per halaman", example = "10")
    private int size;

    @Schema(description = "Total seluruh data", example = "42")
    private long totalElements;

    @Schema(description = "Total halaman", example = "5")
    private int totalPages;

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
