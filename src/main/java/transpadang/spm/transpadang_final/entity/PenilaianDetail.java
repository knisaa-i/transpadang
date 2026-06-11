package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "penilaian_detail")
public class PenilaianDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penilaian_id")
    private PenilaianSpm penilaian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id")
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indikator_id")
    private IndikatorSpm indikator;

    // nilai capaian skala 10-100 (diisi petugas)
    @Column(name = "nilai_capaian", precision = 5, scale = 2)
    private BigDecimal nilaiCapaian;

    // skor terbobot = nilai_capaian * bobot indikator (disimpan, bukan dihitung on-the-fly)
    @Column(name = "skor_terbobot", precision = 7, scale = 4)
    private BigDecimal skorTerbobot;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;
}
