package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Indikator SPM beserta standar dan bobot penilaian")
public class IndikatorSpmDto {

    @Schema(description = "ID indikator", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "ID aspek pelayanan", example = "1")
    private Long aspekId;

    @Schema(description = "Nama aspek", accessMode = Schema.AccessMode.READ_ONLY)
    private String aspekNama;

    @Schema(description = "ID sub kategori (boleh kosong untuk Kesetaraan/Keteraturan)", example = "1")
    private Long subKategoriId;

    @Schema(description = "Nama sub kategori", accessMode = Schema.AccessMode.READ_ONLY)
    private String subKategoriNama;

    @Schema(description = "Nomor urut dalam kelompok", example = "1")
    private String nomorUrut;

    @Schema(description = "Uraian indikator (judul + penjelasan, panduan penilai)",
            example = "Informasi gangguan keamanan")
    private String uraian;

    @Schema(description = "Indikator standar pelayanan minimal")
    private String spmIndikator;

    @Schema(description = "Nilai standar pelayanan minimal", example = "Minimal 2 (dua) stiker")
    private String spmNilai;

    @Schema(description = "Target capaian (skala 10-100)", example = "100.00")
    private BigDecimal targetCapaian;

    @Schema(description = "Bobot indikator (total seluruh indikator = 1.0000)", example = "0.0200")
    private BigDecimal bobot;

    @Schema(description = "Status aktif", example = "true")
    private Boolean aktif;
}
