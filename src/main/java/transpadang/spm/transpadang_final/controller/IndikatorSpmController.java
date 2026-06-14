package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.IndikatorSpmDto;
import transpadang.spm.transpadang_final.bean.IndikatorSpmFilter;
import transpadang.spm.transpadang_final.bean.PageResponse;
import transpadang.spm.transpadang_final.service.IndikatorSpmService;
import transpadang.spm.transpadang_final.view.IndikatorSpmView;

@RestController
@RequestMapping("/api/indikator-spm")
@Tag(name = "Indikator SPM", description = "Master indikator Standar Pelayanan Minimal")
@SecurityRequirement(name = "Bearer Authentication")
public class IndikatorSpmController {

    private final IndikatorSpmService service;

    public IndikatorSpmController(IndikatorSpmService service) {
        this.service = service;
    }

    @GetMapping("/indikator-filter")
    @Operation(summary = "Cari indikator dengan filter dinamis + paginasi (Blazebit)")
    public ApiResponse<PageResponse<IndikatorSpmView>> search(@ParameterObject IndikatorSpmFilter filter) {
        return ApiResponse.ok(service.search(filter));
    }

    @GetMapping("/indikator/{id}")
    @Operation(summary = "Ambil indikator berdasarkan ID (QueryDSL)")
    public ApiResponse<IndikatorSpmView> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping("/new-indikator")
    @Operation(summary = "Buat indikator baru")
    public ApiResponse<IndikatorSpmView> create(@Valid @RequestBody IndikatorSpmDto dto) {
        return ApiResponse.ok("Indikator berhasil dibuat", service.create(dto));
    }

    @PutMapping("/perbarui/{id}")
    @Operation(summary = "Perbarui indikator")
    public ApiResponse<IndikatorSpmView> update(@PathVariable Long id, @Valid @RequestBody IndikatorSpmDto dto) {
        return ApiResponse.ok("Indikator berhasil diperbarui", service.update(id, dto));
    }

    @DeleteMapping("/hapus/{id}")
    @Operation(summary = "Hapus indikator")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Indikator berhasil dihapus", null);
    }
}
