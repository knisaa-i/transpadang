package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.PageResponse;
import transpadang.spm.transpadang_final.bean.PenilaianDetailRequest;
import transpadang.spm.transpadang_final.bean.PenilaianSpmRequest;
import transpadang.spm.transpadang_final.entity.StatusPenilaian;
import transpadang.spm.transpadang_final.service.PenilaianExportService;
import transpadang.spm.transpadang_final.service.PenilaianSpmService;
import transpadang.spm.transpadang_final.view.PenilaianDetailView;
import transpadang.spm.transpadang_final.view.PenilaianSpmView;

@RestController
@RequestMapping("/api/penilaian")
@Tag(name = "Penilaian SPM", description = "Transaksi penilaian capaian SPM (header + detail)")
public class PenilaianSpmController {

    private final PenilaianSpmService service;
    private final PenilaianExportService exportService;

    public PenilaianSpmController(PenilaianSpmService service, PenilaianExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping
    @Operation(summary = "Daftar penilaian dengan paginasi (Blazebit)")
    public ApiResponse<PageResponse<PenilaianSpmView>> findAll(
            @Parameter(description = "Filter koridor") @RequestParam(required = false) Long koridorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.findAll(koridorId, page, size));
    }

    @GetMapping("/export-all")
    @Operation(summary = "Export SEMUA penilaian ke Excel (sheet Rekap + Detail)")
    public ResponseEntity<byte[]> exportAll() {
        byte[] data = exportService.exportAll();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rekap-penilaian-spm.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ambil penilaian lengkap dengan detail (QueryDSL)")
    public ApiResponse<PenilaianSpmView> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Buat penilaian baru beserta detail (skor terbobot dihitung otomatis)")
    public ApiResponse<PenilaianSpmView> create(@Valid @RequestBody PenilaianSpmRequest request) {
        return ApiResponse.ok("Penilaian berhasil dibuat", service.create(request));
    }

    @PostMapping("/{id}/detail")
    @Operation(summary = "Tambah/ubah satu detail penilaian (upsert per bus+indikator) — input/edit per item")
    public ApiResponse<PenilaianDetailView> upsertDetail(@PathVariable Long id,
                                                         @Valid @RequestBody PenilaianDetailRequest req) {
        return ApiResponse.ok("Detail tersimpan", service.upsertDetail(id, req));
    }

    @DeleteMapping("/{id}/detail/{detailId}")
    @Operation(summary = "Hapus satu detail penilaian")
    public ApiResponse<Void> deleteDetail(@PathVariable Long id, @PathVariable Long detailId) {
        service.deleteDetail(id, detailId);
        return ApiResponse.ok("Detail dihapus", null);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ubah status penilaian (alur maker-checker-approver)")
    public ApiResponse<PenilaianSpmView> updateStatus(
            @PathVariable Long id,
            @Parameter(description = "Status baru") @RequestParam StatusPenilaian status) {
        return ApiResponse.ok("Status berhasil diperbarui", service.updateStatus(id, status));
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Export penilaian ke Excel (semua bus sebagai kolom)")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] data = exportService.export(id);
        String filename = "penilaian-spm-" + id + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus penilaian beserta detailnya")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Penilaian berhasil dihapus", null);
    }
}
