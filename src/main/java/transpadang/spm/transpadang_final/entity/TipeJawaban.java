package transpadang.spm.transpadang_final.entity;

/**
 * Jenis kolom jawaban pada form checklist harian.
 * Hanya mengubah LABEL tampilan; nilai disimpan seragam sebagai Boolean
 * pada {@link ChecklistDetail#getHasil()} (true = sisi positif, false = sisi negatif).
 */
public enum TipeJawaban {
    BAIK_RUSAK,   // Baik / Rusak     (Checklist Kelaikan Bus)
    ADA_TIDAK,    // Ada / Tidak      (Pramugara, Laporan Korlap)
    OK_NOTOK      // OK / Not OK      (Checklist Bus & Driver)
}
