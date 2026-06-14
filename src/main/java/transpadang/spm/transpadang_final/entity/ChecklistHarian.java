package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Header transaksi: satu pengisian checklist = satu hari, satu objek (bus/pramugara/koridor).
 * Field identitas yang dipakai bergantung pada {@link ChecklistTemplate#getSubjek()}.
 */
@Getter
@Setter
@Entity
@Table(name = "checklist_harian")
public class ChecklistHarian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ChecklistTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "koridor_id")
    private Koridor koridor;

    @Column(name = "hari")
    private String hari;

    @Column(name = "tanggal")
    private LocalDate tanggal;

    // diisi bila subjek = BUS (no. lambung & polisi diambil dari relasi)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id")
    private Bus bus;

    // diisi bila subjek = PRAMUGARA
    @Column(name = "nama_pramugara")
    private String namaPramugara;

    @Column(name = "shift")
    private String shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusChecklist status;

    // akumulasi denda dari item NOT OK (form Bus & Driver); 0 untuk form lain
    @Column(name = "total_denda", precision = 12, scale = 2)
    private BigDecimal totalDenda;

    // "Dibuat Oleh" = Korlap pengisi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dibuat_oleh_id")
    private User dibuatOleh;

    // "Diketahui Oleh"; nullable sampai status DIKETAHUI
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diketahui_oleh_id")
    private User diketahuiOleh;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "checklistHarian", fetch = FetchType.LAZY)
    private List<ChecklistDetail> details;
}
