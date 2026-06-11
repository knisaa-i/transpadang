package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "aspek_pelayanan")
public class AspekPelayanan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // Keamanan, Keselamatan, Kenyamanan, Kesetaraan, Keteraturan
    @Column(name = "nama")
    private String nama;

    @Column(name = "urutan")
    private Integer urutan;
}
