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

@Configuration
public class SecurityConfiguration {

    // Semua sub-path modul master (create/update/delete pakai sub-path bernama, mis. /new-koridor, /buat)
    private static final String[] MASTER_ID = {
            "/api/koridor/**", "/api/bus/**", "/api/halte/**", "/api/indikator-spm/**", "/api/sub-kategori/**", "/api/aspek-pelayanan/**"
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
                                "/actuator/**",
                                "/ws/**"
                        ).permitAll()
                        // Registrasi user baru: hanya ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")

                        // Data master: baca = semua user login; buat/ubah/hapus = ADMIN
                        .requestMatchers(HttpMethod.POST, MASTER_ID).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, MASTER_ID).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, MASTER_ID).hasRole("ADMIN")

                        // Penilaian SPM: buat & input/edit detail = MAKER/ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/penilaian/**").hasAnyRole("MAKER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/penilaian/hapus-detail/**").hasAnyRole("MAKER", "ADMIN")
                        // Hapus seluruh penilaian = ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/api/penilaian/**").hasRole("ADMIN")
                        // PATCH ubah-status penilaian: dibatasi per-status di service (enforceStatusRole)

                        // Checklist Harian: isi/tambah/hapus = MAKER (Korlap) / ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/checklist/**").hasAnyRole("MAKER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/checklist/**").hasAnyRole("MAKER", "ADMIN")

                        // Sisanya (semua GET, PATCH ubah-status, /api/auth/me) cukup login
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
