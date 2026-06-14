-- =====================================================================
-- RESET TABEL TRANSAKSI CHECKLIST (bukan master)
--
-- Diperlukan setelah ChecklistDetail mendapat kolom bus_id + unique baru
-- (checklist_harian_id, bus_id, item_id). Hibernate ddl-auto=update TIDAK
-- mengubah unique constraint lama, jadi tabel transaksi dibuat ulang.
--
-- LANGKAH:
--   1) Jalankan skrip ini (menghapus data transaksi checklist — data uji saja).
--   2) RESTART aplikasi → Hibernate membuat ulang kedua tabel dgn skema baru.
--
-- Master (checklist_template, checklist_item) TIDAK terpengaruh.
-- DBeaver: paste -> Alt+X.  psql: -f reset_checklist_transaksi.sql
-- =====================================================================

DROP TABLE IF EXISTS checklist_detail CASCADE;
DROP TABLE IF EXISTS checklist_harian CASCADE;
