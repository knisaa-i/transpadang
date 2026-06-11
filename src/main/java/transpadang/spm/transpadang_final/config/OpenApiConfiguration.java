package transpadang.spm.transpadang_final.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurasi metadata Swagger / OpenAPI + skema keamanan Bearer JWT
 * sehingga tombol "Authorize" muncul di Swagger UI untuk memasukkan token.
 * UI dapat diakses melalui /swagger-ui.html (lihat application.properties).
 */
@Configuration
public class OpenApiConfiguration {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI transpadangOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistem SPM Trans Padang")
                        .description("API penilaian Standar Pelayanan Minimal (SPM) Trans Padang " +
                                "sesuai Perwako Nomor 127 Tahun 2021.")
                        .version("v1.0.0")
                        .contact(new Contact().name("Divisi Trans Padang"))
                        .license(new License().name("Internal Use")))
                // Membuat tombol Authorize global di Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Masukkan token JWT hasil login (tanpa prefix 'Bearer ').")));
    }
}
