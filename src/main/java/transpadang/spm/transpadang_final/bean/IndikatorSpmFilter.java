package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Filter pencarian indikator SPM (dipakai QueryDSL secara dinamis)")
public class IndikatorSpmFilter {

    @Schema(description = "Filter berdasarkan aspek", example = "1")
    private Long aspekId;

    @Schema(description = "Filter berdasarkan sub kategori", example = "1")
    private Long subKategoriId;

    @Schema(description = "Kata kunci pada uraian / indikator / nilai", example = "CCTV")
    private String keyword;

    @Schema(description = "Filter status aktif", example = "true")
    private Boolean aktif;

    @Schema(description = "Nomor halaman (0-based)", example = "0")
    private Integer page = 0;

    @Schema(description = "Jumlah data per halaman", example = "10")
    private Integer size = 10;

    public int pageOrDefault() {
        return page == null || page < 0 ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null || size <= 0 ? 10 : size;
    }
}
