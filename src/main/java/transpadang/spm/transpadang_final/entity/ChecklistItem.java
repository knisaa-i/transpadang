package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Master item per template (baris-baris yang dicentang di form).
 * Mendukung sub-item a/b/c via self-relation {@link #parent}
 * (dipakai form Pramugara: seragam laki-laki/wanita).
 */
@Getter
@Setter
@Entity
@Table(name = "checklist_item")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ChecklistTemplate template;

    // null = item utama; terisi = sub-item (a/b/c) dari item induk
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ChecklistItem parent;

    // nomor urut dalam kelompok: "1","2",... atau "a","b","c"
    @Column(name = "nomor_urut")
    private String nomorUrut;

    @Column(name = "uraian", columnDefinition = "TEXT")
    private String uraian;

    // judul kelompok (heading) mis. "Pakaian Pramugara Laki-laki" / "...Wanita"; nullable
    @Column(name = "grup")
    private String grup;

    // teks sanksi/denda apa adanya (form Bus & Driver); nullable
    @Column(name = "sanksi_denda", columnDefinition = "TEXT")
    private String sanksiDenda;

    // nominal denda Rp bila bisa diparse (untuk akumulasi); null bila sanksi non-rupiah
    @Column(name = "nilai_denda", precision = 12, scale = 2)
    private BigDecimal nilaiDenda;

    // urutan tampil (supaya sub-item & heading tampil berurutan)
    @Column(name = "urutan")
    private Integer urutan;

    @Column(name = "aktif")
    private Boolean aktif;
}
