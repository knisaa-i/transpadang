-- =====================================================================
-- SEED HALTE — unit halte per koridor (mirip Bus, untuk penilaian kategori Halte)
--
-- Jalankan SETELAH aplikasi di-restart dengan entity Halte (tabel 'halte' terbentuk).
-- DBeaver: paste -> jalankan (Ctrl+Enter di dalam blok DO).
--
-- CATATAN: AMAN DIJALANKAN BERULANG. TRUNCATE mengosongkan dulu halte
-- (CASCADE ikut menghapus penilaian_detail yang menunjuk halte, bila kolomnya sudah ada).
-- =====================================================================

DO $$
DECLARE
    v_koridor6 BIGINT;
BEGIN
    SELECT id INTO v_koridor6 FROM koridor WHERE nomor = 6 LIMIT 1;
    IF v_koridor6 IS NULL THEN
        RAISE EXCEPTION 'Koridor dengan nomor 6 belum ada. Jalankan seed_master_pendukung.sql dulu.';
    END IF;

    TRUNCATE halte RESTART IDENTITY CASCADE;

    INSERT INTO halte (koridor_id, nomor, nama, aktif) VALUES
    (v_koridor6, 1, 'Halte 1', TRUE),
    (v_koridor6, 2, 'Halte 2', TRUE),
    (v_koridor6, 3, 'Halte 3', TRUE);
END $$;

-- Verifikasi:
-- SELECT id, nomor, nama FROM halte ORDER BY nomor;   -- harapkan 3 baris
