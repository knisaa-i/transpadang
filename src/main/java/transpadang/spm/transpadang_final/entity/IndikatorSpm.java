package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "indikator_spm")
public class IndikatorSpm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aspek_id")
    private AspekPelayanan aspek;

    // nullable: Kesetaraan & Keteraturan tidak punya sub-kategori
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_kategori_id")
    private SubKategori subKategori;

    // nomor urut dalam kelompok: "1", "2", "a", "b", dst
    @Column(name = "nomor_urut")
    private String nomorUrut;

    // uraian indikator: judul + penjelasan lengkap sebagai panduan penilai
    // (poin a/b/c disimpan apa adanya sebagai teks panjang multi-line)
    @Column(name = "uraian", columnDefinition = "TEXT")
    private String uraian;

    @Column(name = "spm_indikator", columnDefinition = "TEXT")
    private String spmIndikator;

    @Column(name = "spm_nilai", columnDefinition = "TEXT")
    private String spmNilai;

    // target capaian = 100 (skala 10-100)
    @Column(name = "target_capaian", precision = 5, scale = 2)
    private BigDecimal targetCapaian;

    // bobot, total seluruh indikator = 1.0000
    @Column(name = "bobot", precision = 6, scale = 4)
    private BigDecimal bobot;

    @Column(name = "aktif")
    private Boolean aktif;
}
