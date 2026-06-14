package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.BusDto;
import transpadang.spm.transpadang_final.service.BusService;
import transpadang.spm.transpadang_final.view.BusView;

import java.util.List;

@RestController
@RequestMapping("/api/bus")
@Tag(name = "Bus", description = "Manajemen data bus (unit kendaraan)")
@SecurityRequirement(name = "Bearer Authentication")
public class BusController {

    private final BusService service;

    public BusController(BusService service) {
        this.service = service;
    }

    @GetMapping("/daftar")
    @Operation(summary = "Daftar bus dengan filter opsional (Blazebit/QueryDSL)")
    public ApiResponse<List<BusView>> findAll(
            @Parameter(description = "Filter koridor") @RequestParam(required = false) Long koridorId,
            @Parameter(description = "Filter status aktif") @RequestParam(required = false) Boolean aktif) {
        if (koridorId != null || aktif != null) {
            return ApiResponse.ok(service.search(koridorId, aktif));
        }
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/ambil/{id}")
    @Operation(summary = "Ambil bus berdasarkan ID")
    public ApiResponse<BusView> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping("/buat")
    @Operation(summary = "Buat bus baru")
    public ApiResponse<BusView> create(@Valid @RequestBody BusDto dto) {
        return ApiResponse.ok("Bus berhasil dibuat", service.create(dto));
    }

    @PutMapping("/perbarui/{id}")
    @Operation(summary = "Perbarui bus")
    public ApiResponse<BusView> update(@PathVariable Long id, @Valid @RequestBody BusDto dto) {
        return ApiResponse.ok("Bus berhasil diperbarui", service.update(id, dto));
    }

    @DeleteMapping("/hapus/{id}")
    @Operation(summary = "Hapus bus")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Bus berhasil dihapus", null);
    }
}
