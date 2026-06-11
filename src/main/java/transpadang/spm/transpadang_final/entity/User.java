package transpadang.spm.transpadang_final.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", unique = true)
    private String username;

    // password ter-hash (BCrypt)
    @Column(name = "password")
    private String password;

    // nama lengkap (mis. "Aulil Amri, S.E.")
    @Column(name = "nama")
    private String nama;

    // jabatan (mis. "Staf Operasional", "Kepala Divisi", "Manager Operasional")
    @Column(name = "jabatan")
    private String jabatan;

    // peran untuk otorisasi (mis. "MAKER", "CHECKER", "APPROVER", "ADMIN")
    @Column(name = "role")
    private String role;

    @Column(name = "aktif")
    private Boolean aktif;
}
