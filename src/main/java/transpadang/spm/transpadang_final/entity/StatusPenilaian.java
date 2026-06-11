package transpadang.spm.transpadang_final.entity;

public enum StatusPenilaian {
    DRAFT,       // dibuat staf (maker), belum disubmit
    SUBMITTED,   // disubmit, menunggu diketahui kadiv
    CHECKED,     // sudah diketahui kadiv (checker), menunggu persetujuan manager
    APPROVED,    // disetujui manager (approver)
    REJECTED     // ditolak, dikembalikan ke maker
}
