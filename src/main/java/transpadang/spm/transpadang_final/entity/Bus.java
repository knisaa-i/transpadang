package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bus")
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "koridor_id")
    private Koridor koridor;

    @Column(name = "no_lambung")
    private String noLambung;

    @Column(name = "plat_nomor")
    private String platNomor;

    @Column(name = "aktif")
    private Boolean aktif;
}
