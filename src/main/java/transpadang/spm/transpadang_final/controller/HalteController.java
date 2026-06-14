package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.HalteDto;
import transpadang.spm.transpadang_final.service.HalteService;
import transpadang.spm.transpadang_final.view.HalteView;

import java.util.List;

@RestController
@RequestMapping("/api/halte")
@Tag(name = "Halte", description = "Manajemen data halte (unit) per koridor")
@SecurityRequirement(name = "Bearer Authentication")
public class HalteController {

    private final HalteService service;

    public HalteController(HalteService service) {
        this.service = service;
    }

    @GetMapping("/daftar")
    @Operation(summary = "Daftar halte dengan filter opsional (per koridor / status aktif)")
    public ApiResponse<List<HalteView>> findAll(
            @Parameter(description = "Filter koridor") @RequestParam(required = false) Long koridorId,
            @Parameter(description = "Filter status aktif") @RequestParam(required = false) Boolean aktif) {
        if (koridorId != null || aktif != null) {
            return ApiResponse.ok(service.search(koridorId, aktif));
        }
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/ambil/{id}")
    @Operation(summary = "Ambil halte berdasarkan ID")
    public ApiResponse<HalteView> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping("/buat")
    @Operation(summary = "Buat halte baru")
    public ApiResponse<HalteView> create(@Valid @RequestBody HalteDto dto) {
        return ApiResponse.ok("Halte berhasil dibuat", service.create(dto));
    }

    @PutMapping("/perbarui/{id}")
    @Operation(summary = "Perbarui halte")
    public ApiResponse<HalteView> update(@PathVariable Long id, @Valid @RequestBody HalteDto dto) {
        return ApiResponse.ok("Halte berhasil diperbarui", service.update(id, dto));
    }

    @DeleteMapping("/hapus/{id}")
    @Operation(summary = "Hapus halte")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Halte berhasil dihapus", null);
    }
}
