package transpadang.spm.transpadang_final.entity;

/**
 * Alur status pengisian checklist harian (lebih ringan dari penilaian SPM):
 * DRAFT (diisi Korlap) -> SUBMITTED (disubmit) -> DIKETAHUI (diketahui atasan/korlap).
 */
public enum StatusChecklist {
    DRAFT,       // sedang/baru diisi Korlap, belum disubmit
    SUBMITTED,   // disubmit, menunggu diketahui
    DIKETAHUI    // sudah diketahui (ditandatangani "Diketahui Oleh")
}
