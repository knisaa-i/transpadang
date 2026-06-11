package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.SubKategoriDto;
import transpadang.spm.transpadang_final.service.SubKategoriService;
import transpadang.spm.transpadang_final.view.SubKategoriView;

import java.util.List;

@RestController
@RequestMapping("/api/sub-kategori")
@Tag(name = "Sub Kategori", description = "Master sub kategori (Halte, Bus, Manusia)")
public class SubKategoriController {

    private final SubKategoriService service;

    public SubKategoriController(SubKategoriService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Daftar sub kategori, opsional difilter per aspek (Blazebit/QueryDSL)")
    public ApiResponse<List<SubKategoriView>> findAll(
            @Parameter(description = "Filter aspek") @RequestParam(required = false) Long aspekId) {
        if (aspekId != null) {
            return ApiResponse.ok(service.findByAspek(aspekId));
        }
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ambil sub kategori berdasarkan ID")
    public ApiResponse<SubKategoriView> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Buat sub kategori baru")
    public ApiResponse<SubKategoriView> create(@Valid @RequestBody SubKategoriDto dto) {
        return ApiResponse.ok("Sub kategori berhasil dibuat", service.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Perbarui sub kategori")
    public ApiResponse<SubKategoriView> update(@PathVariable Long id, @Valid @RequestBody SubKategoriDto dto) {
        return ApiResponse.ok("Sub kategori berhasil diperbarui", service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus sub kategori")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Sub kategori berhasil dihapus", null);
    }
}
