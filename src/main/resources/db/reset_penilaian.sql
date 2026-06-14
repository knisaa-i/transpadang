-- =====================================================================
-- RESET DATA TRANSAKSI PENILAIAN (untuk uji coba input ulang)
--
-- Menghapus SEMUA penilaian + detail, reset id ke 1.
-- Master TIDAK terhapus: koridor, bus, halte, indikator_spm,
-- sub_kategori, aspek_pelayanan, users.
--
-- DBeaver: paste -> jalankan -> pastikan Commit.
-- =====================================================================

TRUNCATE penilaian_detail, penilaian_spm RESTART IDENTITY CASCADE;

-- Verifikasi:
-- SELECT COUNT(*) FROM penilaian_spm;     -- 0
-- SELECT COUNT(*) FROM penilaian_detail;  -- 0
