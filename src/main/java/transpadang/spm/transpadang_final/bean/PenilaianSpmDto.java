package transpadang.spm.transpadang_final.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Penilaian SPM (header laporan capaian) beserta detailnya")
public class PenilaianSpmDto {

    @Schema(description = "ID penilaian", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "ID koridor", example = "1")
    private Long koridorId;

    @Schema(description = "Nama koridor", accessMode = Schema.AccessMode.READ_ONLY)
    private String koridorNama;

    @Schema(description = "Hari penilaian", example = "Senin")
    private String hari;

    @Schema(description = "Tanggal penilaian", example = "2026-06-10")
    private LocalDate tanggal;

    @Schema(description = "Status penilaian", example = "DRAFT")
    private String status;

    @Schema(description = "ID maker (Direkap Oleh / Staf Operasional)")
    private Long makerId;

    @Schema(description = "Nama maker", accessMode = Schema.AccessMode.READ_ONLY)
    private String makerNama;

    @Schema(description = "ID checker (Diketahui Oleh / Kepala Divisi)")
    private Long checkerId;

    @Schema(description = "Nama checker", accessMode = Schema.AccessMode.READ_ONLY)
    private String checkerNama;

    @Schema(description = "ID approver (Disetujui Oleh / Manager Operasional)")
    private Long approverId;

    @Schema(description = "Nama approver", accessMode = Schema.AccessMode.READ_ONLY)
    private String approverNama;

    @Schema(description = "Total skor terbobot seluruh detail (rata-rata capaian SPM)",
            accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal totalCapaian;

    @Schema(description = "Waktu dibuat", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Waktu diperbarui", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @Schema(description = "Daftar detail penilaian per bus per indikator")
    private List<PenilaianDetailDto> details;
}
