-- =====================================================================
-- SEED MASTER PENDUKUNG SPM TRANS PADANG
--   users    -> maker / checker / approver + admin
--   koridor  -> Koridor 6
--   bus      -> No. Lambung 46 s/d 55 (10 unit)
--
-- Jalankan SETELAH aplikasi pernah start (tabel sudah dibuat Hibernate),
-- boleh sebelum/sesudah seed_indikator_spm.sql.
-- DBeaver: paste -> Alt+X.  psql: psql -d transpadang_spm -f seed_master_pendukung.sql
--
-- LOGIN: semua user password = "trans12345"
--   (hash BCrypt di bawah; ganti lewat API bila perlu)
--
-- CATATAN: AMAN DIJALANKAN BERULANG. TRUNCATE mengosongkan dulu
-- users/koridor/bus (CASCADE ikut menghapus penilaian_spm & penilaian_detail).
-- Hapus baris TRUNCATE bila tidak ingin data lama terhapus.
-- =====================================================================

DO $$
DECLARE
    pwd        TEXT := '$2b$10$wpb8RlVXX1jzwIEoNV8ufezbsWY0rBtuptESw5Zs3L.VczxOEZwlu'; -- = "trans12345"
    v_koridor6 BIGINT;
    i          INT;
BEGIN
    -- Reset agar bisa dijalankan ulang tanpa duplikasi
    TRUNCATE users, koridor, bus RESTART IDENTITY CASCADE;

    -- ---------------------------------------------------------------
    -- 1. USERS  (maker / checker / approver + admin)
    -- ---------------------------------------------------------------
    INSERT INTO users (username, password, nama, jabatan, role, aktif) VALUES
    ('staf01',    pwd, 'Staf Operasional Trans Padang', 'Staf Operasional',    'MAKER',    TRUE),
    ('kadiv01',   pwd, 'Aulil Amri, S.E.',              'Kepala Divisi Trans Padang', 'CHECKER',  TRUE),
    ('manager01', pwd, 'Efrijon, S.T.',                 'Manager Operasional Trans Padang', 'APPROVER', TRUE),
    ('admin',     pwd, 'Administrator',                 'Administrator',       'ADMIN',    TRUE);

    -- ---------------------------------------------------------------
    -- 2. KORIDOR
    -- ---------------------------------------------------------------
    INSERT INTO koridor (nomor, nama) VALUES (6, 'Koridor 6')
    RETURNING id INTO v_koridor6;

    -- ---------------------------------------------------------------
    -- 3. BUS  (No. Lambung 46 s/d 55)
    -- ---------------------------------------------------------------
    FOR i IN 46..55 LOOP
        INSERT INTO bus (koridor_id, no_lambung, plat_nomor, aktif)
        VALUES (v_koridor6, i::text, 'BA ' || (7000 + i)::text || ' PA', TRUE);
    END LOOP;

END $$;

-- Verifikasi:
-- SELECT id, username, nama, role FROM users ORDER BY id;          -- 4 user
-- SELECT id, nomor, nama FROM koridor;                             -- Koridor 6
-- SELECT id, no_lambung, plat_nomor FROM bus ORDER BY id;          -- 10 bus (46..55)
