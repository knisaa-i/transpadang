package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.AuthResponse;
import transpadang.spm.transpadang_final.bean.LoginRequest;
import transpadang.spm.transpadang_final.bean.RegisterRequest;
import transpadang.spm.transpadang_final.service.UserService;
import transpadang.spm.transpadang_final.view.UserView;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autentikasi: registrasi, login, dan profil")
@SecurityRequirement(name = "Bearer Authentication")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Registrasi user baru")
    public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Registrasi berhasil", userService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login dan dapatkan token JWT")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login berhasil", userService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil user yang sedang login (perlu Bearer token)")
    public ApiResponse<UserView> me(Authentication authentication) {
        return ApiResponse.ok(userService.getByUsername(authentication.getName()));
    }
}
