package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Hasil pengisian satu item pada satu checklist harian.
 * Unik per (checklist_harian, item) sehingga mendukung upsert per item.
 */
@Getter
@Setter
@Entity
@Table(name = "checklist_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"checklist_harian_id", "bus_id", "item_id"}))
public class ChecklistDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_harian_id")
    private ChecklistHarian checklistHarian;

    // diisi untuk form objek Bus (sesi mencakup semua bus); null untuk Pramugara/Korlap
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id")
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ChecklistItem item;

    // true = Baik/Ada/OK ; false = Rusak/Tidak/NotOK ; null = belum diisi
    @Column(name = "hasil")
    private Boolean hasil;

    @Column(name = "keterangan", columnDefinition = "TEXT")
    private String keterangan;
}
