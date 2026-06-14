-- =====================================================================
-- PARTIAL UNIQUE INDEX penilaian_detail — anti-duplikat nilai
--
-- Mencegah baris detail ganda untuk kombinasi yang sama:
--   - kategori Bus   : (penilaian_id, bus_id, indikator_id) unik
--   - kategori Halte : (penilaian_id, halte_id, indikator_id) unik
--
-- Pakai PARTIAL index karena salah satu dari bus_id/halte_id selalu NULL;
-- unique biasa tidak menutup duplikat saat ada NULL (di PostgreSQL NULL dianggap berbeda).
--
-- Jalankan SETELAH kolom halte_id terbentuk (restart app dengan entity baru).
-- DBeaver: paste -> jalankan.
-- =====================================================================

CREATE UNIQUE INDEX IF NOT EXISTS ux_detail_bus
    ON penilaian_detail (penilaian_id, bus_id, indikator_id)
    WHERE bus_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_detail_halte
    ON penilaian_detail (penilaian_id, halte_id, indikator_id)
    WHERE halte_id IS NOT NULL;
