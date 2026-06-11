package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "penilaian_spm")
public class PenilaianSpm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "koridor_id")
    private Koridor koridor;

    @Column(name = "hari")
    private String hari;

    @Column(name = "tanggal")
    private LocalDate tanggal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusPenilaian status;

    // Maker-Checker-Approver: referensi ke user
    // maker    = Direkap Oleh (Staf Operasional)
    // checker  = Diketahui Oleh (Kepala Divisi)
    // approver = Disetujui Oleh (Manager Operasional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maker_id")
    private User maker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checker_id")
    private User checker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // inverse: detail penilaian per bus per indikator (dipakai entity view)
    @OneToMany(mappedBy = "penilaian", fetch = FetchType.LAZY)
    private List<PenilaianDetail> details;
}
