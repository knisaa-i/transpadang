package transpadang.spm.transpadang_final.entity;

/**
 * Objek yang dinilai sebuah form, menentukan field identitas wajib pada
 * {@link ChecklistHarian}:
 * - BUS       : wajib bus (no. lambung & polisi diambil dari relasi Bus)
 * - PRAMUGARA : wajib nama pramugara + shift
 * - KORIDOR   : cukup koridor (laporan pengawasan korlap)
 */
public enum SubjekChecklist {
    BUS,
    PRAMUGARA,
    KORIDOR
}
