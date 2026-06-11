package transpadang.spm.transpadang_final.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Konfigurasi keamanan + pembatasan role.
 *
 * <p>Ringkasan wewenang:
 * <ul>
 *   <li>Login &amp; Swagger: terbuka.</li>
 *   <li>Registrasi user &amp; ubah data master (koridor/bus/indikator/sub-kategori/aspek): hanya ADMIN.</li>
 *   <li>Buat penilaian: MAKER atau ADMIN.</li>
 *   <li>Ubah status penilaian: semua user login, tetapi dibatasi per-status di service
 *       (SUBMITTED=MAKER, CHECKED=CHECKER, APPROVED=APPROVER).</li>
 *   <li>Hapus penilaian: hanya ADMIN.</li>
 *   <li>Endpoint baca lain: cukup login.</li>
 * </ul>
 */
@Configuration
public class SecurityConfiguration {

    private static final String[] MASTER = {
            "/api/koridor", "/api/bus", "/api/indikator-spm", "/api/sub-kategori", "/api/aspek-pelayanan"
    };
    private static final String[] MASTER_ID = {
            "/api/koridor/**", "/api/bus/**", "/api/indikator-spm/**", "/api/sub-kategori/**", "/api/aspek-pelayanan/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/**"
                        ).permitAll()
                        // Registrasi user baru: hanya ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")
                        // Data master: hanya ADMIN yang boleh menambah/mengubah/menghapus
                        .requestMatchers(HttpMethod.POST, MASTER).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, MASTER_ID).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, MASTER_ID).hasRole("ADMIN")
                        // Penilaian: buat & input/edit detail = MAKER/ADMIN ; ubah status dicek di service
                        .requestMatchers(HttpMethod.POST, "/api/penilaian").hasAnyRole("MAKER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/penilaian/*/detail").hasAnyRole("MAKER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/penilaian/*/detail/**").hasAnyRole("MAKER", "ADMIN")
                        // Hapus seluruh penilaian = ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/api/penilaian/**").hasRole("ADMIN")
                        // Sisanya (termasuk semua GET dan PATCH status) cukup login
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
