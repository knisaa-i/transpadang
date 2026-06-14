package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.AspekPelayananDto;
import transpadang.spm.transpadang_final.service.AspekPelayananService;
import transpadang.spm.transpadang_final.view.AspekPelayananView;

import java.util.List;

@RestController
@RequestMapping("/api/aspek-pelayanan")
@Tag(name = "Aspek Pelayanan", description = "Master aspek pelayanan dasar SPM")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class AspekPelayananController {

    private final AspekPelayananService service;

    @GetMapping("/daftar")
    @Operation(summary = "Daftar seluruh aspek pelayanan (Blazebit)")
    public ApiResponse<List<AspekPelayananView>> findAll() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/ambil/{id}")
    @Operation(summary = "Ambil aspek berdasarkan ID (QueryDSL)")
    public ApiResponse<AspekPelayananView> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping("/buat")
    @Operation(summary = "Buat aspek baru")
    public ApiResponse<AspekPelayananView> create(@Valid @RequestBody AspekPelayananDto dto) {
        return ApiResponse.ok("Aspek berhasil dibuat", service.create(dto));
    }

    @PutMapping("/perbarui/{id}")
    @Operation(summary = "Perbarui aspek")
    public ApiResponse<AspekPelayananView> update(@PathVariable Long id, @Valid @RequestBody AspekPelayananDto dto) {
        return ApiResponse.ok("Aspek berhasil diperbarui", service.update(id, dto));
    }

    @DeleteMapping("/hapus/{id}")
    @Operation(summary = "Hapus aspek")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Aspek berhasil dihapus", null);
    }
}
