package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sub_kategori")
public class SubKategori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aspek_id")
    private AspekPelayanan aspek;

    // Halte / Bus / Manusia
    @Column(name = "nama")
    private String nama;

    @Column(name = "urutan")
    private Integer urutan;
}
