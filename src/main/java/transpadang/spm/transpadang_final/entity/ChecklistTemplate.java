package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Master jenis form checklist harian (4 baris: KENDARAAN, PRAMUGARA, BUS_DRIVER, KORLAP).
 * Menentukan label jawaban, subjek yang dinilai, dan apakah form memakai denda.
 */
@Getter
@Setter
@Entity
@Table(name = "checklist_template")
public class ChecklistTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // kode unik: KENDARAAN, PRAMUGARA, BUS_DRIVER, KORLAP
    @Column(name = "kode", unique = true)
    private String kode;

    // nama form lengkap, mis. "Checklist Harian Kelaikan Bus"
    @Column(name = "nama")
    private String nama;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipe_jawaban")
    private TipeJawaban tipeJawaban;

    @Enumerated(EnumType.STRING)
    @Column(name = "subjek")
    private SubjekChecklist subjek;

    // true hanya untuk BUS_DRIVER (punya kolom sanksi/denda)
    @Column(name = "pakai_denda")
    private Boolean pakaiDenda;

    // judul blok tanda tangan, mis. "Dibuat Oleh" / "Diketahui Oleh"
    @Column(name = "judul_ttd")
    private String judulTtd;

    @Column(name = "aktif")
    private Boolean aktif;
}
