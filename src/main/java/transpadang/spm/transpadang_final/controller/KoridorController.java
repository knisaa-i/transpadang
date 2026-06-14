package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.KoridorDto;
import transpadang.spm.transpadang_final.service.KoridorService;
import transpadang.spm.transpadang_final.view.KoridorView;

import java.util.List;

@RestController
@RequestMapping("/api/koridor")
@Tag(name = "Koridor", description = "Manajemen data koridor Trans Padang")
@SecurityRequirement(name = "Bearer Authentication")
public class KoridorController {

    private final KoridorService service;

    public KoridorController(KoridorService service) {
        this.service = service;
    }

    @GetMapping("/daftar")
    @Operation(summary = "Daftar seluruh koridor (Blazebit)")
    public ApiResponse<List<KoridorView>> findAll() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/get-koridor/{id}")
    @Operation(summary = "Ambil koridor berdasarkan ID (QueryDSL)")
    public ApiResponse<KoridorView> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping("/new-koridor")
    @Operation(summary = "Buat koridor baru")
    public ApiResponse<KoridorView> create(@Valid @RequestBody KoridorDto dto) {
        return ApiResponse.ok("Koridor berhasil dibuat", service.create(dto));
    }

    @PutMapping("/perbarui-koridor/{id}")
    @Operation(summary = "Perbarui koridor")
    public ApiResponse<KoridorView> update(@PathVariable Long id, @Valid @RequestBody KoridorDto dto) {
        return ApiResponse.ok("Koridor berhasil diperbarui", service.update(id, dto));
    }

    @DeleteMapping("/hapus/{id}")
    @Operation(summary = "Hapus koridor")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Koridor berhasil dihapus", null);
    }
}
