package transpadang.spm.transpadang_final.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import transpadang.spm.transpadang_final.bean.ApiResponse;
import transpadang.spm.transpadang_final.bean.ChecklistDetailRequest;
import transpadang.spm.transpadang_final.bean.ChecklistHarianRequest;
import transpadang.spm.transpadang_final.bean.PageResponse;
import transpadang.spm.transpadang_final.entity.StatusChecklist;
import transpadang.spm.transpadang_final.service.ChecklistExportService;
import transpadang.spm.transpadang_final.service.ChecklistService;
import transpadang.spm.transpadang_final.view.ChecklistDetailView;
import transpadang.spm.transpadang_final.view.ChecklistHarianView;
import transpadang.spm.transpadang_final.view.ChecklistItemView;
import transpadang.spm.transpadang_final.view.ChecklistTemplateView;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/checklist")
@Tag(name = "Checklist Harian", description = "Form checklist operasional harian (template + header + detail)")
@SecurityRequirement(name = "Bearer Authentication")
public class ChecklistController {

    private final ChecklistService service;
    private final ChecklistExportService exportService;

    public ChecklistController(ChecklistService service, ChecklistExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping("/export-all")
    @Operation(summary = "Export SEMUA checklist ke Excel (4 sheet, layout form asli terisi)")
    public ResponseEntity<byte[]> exportAll() {
        byte[] data = exportService.exportAll();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rekap-checklist-harian.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/Template/Daftar")
    @Operation(summary = "Daftar template (jenis form checklist harian)")
    public ApiResponse<List<ChecklistTemplateView>> listTemplates() {
        return ApiResponse.ok(service.listTemplates());
    }

    @GetMapping("/Template/{kode}/Items")
    @Operation(summary = "Daftar item master sebuah template untuk merender form kosong")
    public ApiResponse<List<ChecklistItemView>> listItems(
            @Parameter(description = "Kode template: KENDARAAN/PRAMUGARA/BUS_DRIVER/KORLAP")
            @PathVariable String kode) {
        return ApiResponse.ok(service.listItemsByKode(kode));
    }

    @GetMapping("/Daftar")
    @Operation(summary = "Daftar checklist harian dengan paginasi (Blazebit)")
    public ApiResponse<PageResponse<ChecklistHarianView>> findAll(
            @Parameter(description = "Filter template") @RequestParam(required = false) Long templateId,
            @Parameter(description = "Filter koridor") @RequestParam(required = false) Long koridorId,
            @Parameter(description = "Filter tanggal") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.findAll(templateId, koridorId, tanggal, page, size));
    }

    @GetMapping("/get-checklist/{id}")
    @Operation(summary = "Ambil checklist lengkap dengan detail")
    public ApiResponse<ChecklistHarianView> findById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping("/new-checklist")
    @Operation(summary = "Buat checklist baru beserta detail (total denda dihitung otomatis)")
    public ApiResponse<ChecklistHarianView> create(@Valid @RequestBody ChecklistHarianRequest request) {
        return ApiResponse.ok("Checklist berhasil dibuat", service.create(request));
    }

    @PostMapping("/tambah-detail/{id}")
    @Operation(summary = "Tambah/ubah satu hasil item (upsert per checklist+item)")
    public ApiResponse<ChecklistDetailView> upsertDetail(@PathVariable Long id,
                                                         @Valid @RequestBody ChecklistDetailRequest req) {
        return ApiResponse.ok("Detail tersimpan", service.upsertDetail(id, req));
    }

    @DeleteMapping("/hapus-detail/{id}/{detailId}")
    @Operation(summary = "Hapus satu hasil item")
    public ApiResponse<Void> deleteDetail(@PathVariable Long id, @PathVariable Long detailId) {
        service.deleteDetail(id, detailId);
        return ApiResponse.ok("Detail dihapus", null);
    }

    @PatchMapping("/ubah-status/{id}")
    @Operation(summary = "Ubah status checklist (DRAFT -> SUBMITTED -> DIKETAHUI)")
    public ApiResponse<ChecklistHarianView> updateStatus(
            @PathVariable Long id,
            @Parameter(description = "Status baru") @RequestParam StatusChecklist status) {
        return ApiResponse.ok("Status berhasil diperbarui", service.updateStatus(id, status));
    }

    @DeleteMapping("/hapus/{id}")
    @Operation(summary = "Hapus checklist beserta detailnya")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Checklist berhasil dihapus", null);
    }
}
