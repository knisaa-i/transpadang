package transpadang.spm.transpadang_final.service;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.bean.AuthResponse;
import transpadang.spm.transpadang_final.bean.LoginRequest;
import transpadang.spm.transpadang_final.bean.RegisterRequest;
import transpadang.spm.transpadang_final.config.JwtService;
import transpadang.spm.transpadang_final.entity.QUser;
import transpadang.spm.transpadang_final.entity.User;
import transpadang.spm.transpadang_final.view.UserView;

/**
 * Service autentikasi & manajemen User.
 * <ul>
 *     <li>Pencarian user memakai QueryDSL ({@link JPAQueryFactory}).</li>
 *     <li>Proyeksi data user memakai Blazebit entity view ({@link EntityViewManager}).</li>
 *     <li>Token memakai {@link JwtService}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserView register(RegisterRequest request) {
        if (findEntityByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("Username sudah digunakan: " + request.getUsername());
        }
        var user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNama(request.getNama());
        user.setJabatan(request.getJabatan());
        user.setRole(request.getRole() != null ? request.getRole() : "MAKER");
        user.setAktif(Boolean.TRUE);
        em.persist(user);
        em.flush();
        return loadView(user.getId());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var user = findEntityByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Username atau password salah");
        }
        if (Boolean.FALSE.equals(user.getAktif())) {
            throw new BadCredentialsException("User tidak aktif");
        }
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), loadView(user.getId()));
    }

    @Transactional(readOnly = true)
    public UserView getByUsername(String username) {
        var user = findEntityByUsername(username);
        if (user == null) {
            throw new EntityNotFoundException("User tidak ditemukan: " + username);
        }
        return loadView(user.getId());
    }

    /** Pencarian entity user via QueryDSL. */
    private User findEntityByUsername(String username) {
        var list = cbf.create(em, User.class, "u")
                .where("u.username").eq(username)
                .getResultList();
        return list.isEmpty() ? null : list.getFirst();
    }

    /** Proyeksi UserView via Blazebit entity view (cbf + QueryDSL Q-class path). */
    private UserView loadView(Long id) {
        var q = new QUser("u");
        CriteriaBuilder<User> cb = cbf.create(em, User.class).from(User.class, q.getMetadata().getName())
                .where(q.id.toString()).eq(id);
        return evm.applySetting(EntityViewSetting.create(UserView.class), cb)
                .getResultList().stream().findFirst().orElse(null);
    }
}