package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "halte")
public class Halte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "koridor_id")
    private Koridor koridor;

    // nomor urut halte dalam koridor (1, 2, 3, ...)
    @Column(name = "nomor")
    private Integer nomor;

    @Column(name = "nama")
    private String nama;

    @Column(name = "aktif")
    private Boolean aktif;
}
