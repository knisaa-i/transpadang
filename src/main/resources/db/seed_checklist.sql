-- =====================================================================
-- SEED MASTER CHECKLIST HARIAN TRANS PADANG
--   checklist_template -> 4 jenis form (KENDARAAN, PRAMUGARA, BUS_DRIVER, KORLAP)
--   checklist_item     -> item per form (transkrip dari "Form Checklist Harian.xlsx")
--
-- Jalankan SETELAH aplikasi pernah start (tabel sudah dibuat Hibernate).
-- DBeaver: paste -> Alt+X.  psql: psql -d transpadang_spm -f seed_checklist.sql
--
-- CATATAN: AMAN DIJALANKAN BERULANG. TRUNCATE mengosongkan dulu template & item
-- (CASCADE ikut menghapus checklist_harian & checklist_detail).
-- Hapus baris TRUNCATE bila tidak ingin transaksi checklist lama terhapus.
-- =====================================================================

DO $$
DECLARE
    v_kendaraan BIGINT;
    v_pramugara BIGINT;
    v_busdriver BIGINT;
    v_korlap    BIGINT;
    v_p3        BIGINT;  -- id item "3 Pakaian Pramugara" (induk sub-item)
BEGIN
    TRUNCATE checklist_template, checklist_item RESTART IDENTITY CASCADE;

    -- =================================================================
    -- 1. TEMPLATE
    -- =================================================================
    INSERT INTO checklist_template (kode, nama, tipe_jawaban, subjek, pakai_denda, judul_ttd, aktif)
    VALUES ('KENDARAAN', 'Checklist Harian Kelaikan Bus Trans Padang', 'BAIK_RUSAK', 'BUS', FALSE, 'Dibuat Oleh', TRUE)
    RETURNING id INTO v_kendaraan;

    INSERT INTO checklist_template (kode, nama, tipe_jawaban, subjek, pakai_denda, judul_ttd, aktif)
    VALUES ('PRAMUGARA', 'Checklist Harian Pramugara', 'ADA_TIDAK', 'PRAMUGARA', FALSE, 'Koordinator Lapangan', TRUE)
    RETURNING id INTO v_pramugara;

    INSERT INTO checklist_template (kode, nama, tipe_jawaban, subjek, pakai_denda, judul_ttd, aktif)
    VALUES ('BUS_DRIVER', 'Checklist Harian Bus & Driver', 'OK_NOTOK', 'BUS', TRUE, 'Diketahui Oleh', TRUE)
    RETURNING id INTO v_busdriver;

    INSERT INTO checklist_template (kode, nama, tipe_jawaban, subjek, pakai_denda, judul_ttd, aktif)
    VALUES ('KORLAP', 'Laporan Harian Pengawasan Korlap', 'ADA_TIDAK', 'KORIDOR', FALSE, 'Koordinator Lapangan', TRUE)
    RETURNING id INTO v_korlap;

    -- =================================================================
    -- 2. ITEM KENDARAAN (12 item, Baik/Rusak)
    -- =================================================================
    INSERT INTO checklist_item (template_id, nomor_urut, uraian, urutan, aktif) VALUES
    (v_kendaraan, '1',  'Kebersihan Kendaraan',                 1,  TRUE),
    (v_kendaraan, '2',  'Ban',                                  2,  TRUE),
    (v_kendaraan, '3',  'Lampu',                                3,  TRUE),
    (v_kendaraan, '4',  'Rem depan',                            4,  TRUE),
    (v_kendaraan, '5',  'Rem Belakang',                         5,  TRUE),
    (v_kendaraan, '6',  'Lampu Sen Kiri Kanan, Muka Belakang',  6,  TRUE),
    (v_kendaraan, '7',  'Klason',                               7,  TRUE),
    (v_kendaraan, '8',  'Pintu',                                8,  TRUE),
    (v_kendaraan, '9',  'AC',                                   9,  TRUE),
    (v_kendaraan, '10', 'APAR',                                 10, TRUE),
    (v_kendaraan, '11', 'Kotak P3K',                            11, TRUE),
    (v_kendaraan, '12', 'Palu Pemecah Kaca',                    12, TRUE);

    -- =================================================================
    -- 3. ITEM PRAMUGARA (Ada/Tidak); item 3 punya sub-item dua grup
    -- =================================================================
    INSERT INTO checklist_item (template_id, nomor_urut, uraian, urutan, aktif) VALUES
    (v_pramugara, '1', 'Kehadiran Tepat waktu',              10, TRUE),
    (v_pramugara, '2', 'Kondisi Fisik Sehat dan siap kerja', 20, TRUE);

    INSERT INTO checklist_item (template_id, nomor_urut, uraian, urutan, aktif)
    VALUES (v_pramugara, '3', 'Pakaian Pramugara', 30, TRUE)
    RETURNING id INTO v_p3;

    -- 3.a Pakaian Pramugara Laki-laki
    INSERT INTO checklist_item (template_id, parent_id, grup, nomor_urut, uraian, urutan, aktif) VALUES
    (v_pramugara, v_p3, 'Pakaian Pramugara Laki-laki', 'a', 'Menggunakan baju seragam pegawai pramugara', 31, TRUE),
    (v_pramugara, v_p3, 'Pakaian Pramugara Laki-laki', 'b', 'Menggunakan celana dasar berwarna hitam (tidak boleh celana jeans) dan ikat pinggang berwarna hitam', 32, TRUE),
    (v_pramugara, v_p3, 'Pakaian Pramugara Laki-laki', 'c', 'Menggunakan sepatu jenis "pantofel" berwarna hitam dan kaos kaki berwarna hitam.', 33, TRUE),
    (v_pramugara, v_p3, 'Pakaian Pramugara Laki-laki', 'd', 'Potongan rambut rapi tidak boleh melebihi dari telinga dan kerah baju (disarankan menggunakan minyak rambut)', 34, TRUE);

    -- 3.b Pakaian Pramugara Wanita
    INSERT INTO checklist_item (template_id, parent_id, grup, nomor_urut, uraian, urutan, aktif) VALUES
    (v_pramugara, v_p3, 'Pakaian Pramugara Wanita', 'a', 'Menggunakan baju seragam pegawai pramugara', 35, TRUE),
    (v_pramugara, v_p3, 'Pakaian Pramugara Wanita', 'b', 'Menggunakan rok panjang atau celana dasar berwarna hitam (longgar dan tidak ketat) dan ikat pinggang berwarna hitam', 36, TRUE),
    (v_pramugara, v_p3, 'Pakaian Pramugara Wanita', 'c', 'Menggunakan sepatu jenis "pantofel" berwarna hitam dan kaos kaki berwarna hitam.', 37, TRUE),
    (v_pramugara, v_p3, 'Pakaian Pramugara Wanita', 'd', 'Menggunakan "manset tangan" berwarna hitam', 38, TRUE),
    (v_pramugara, v_p3, 'Pakaian Pramugara Wanita', 'e', 'Menggunakan hijab atau hijab polos berwarna hitam', 39, TRUE),
    (v_pramugara, v_p3, 'Pakaian Pramugara Wanita', 'f', 'Menggunakan sepatu jenis "pasus" berwarna hitam dan kaos kaki berwarna hitam', 40, TRUE);

    -- =================================================================
    -- 4. ITEM BUS & DRIVER (OK/Not OK) + sanksi/denda
    --    nilai_denda diisi bila sanksi berupa Rupiah (untuk akumulasi)
    -- =================================================================
    INSERT INTO checklist_item (template_id, nomor_urut, uraian, sanksi_denda, nilai_denda, urutan, aktif) VALUES
    (v_busdriver, '1',  'Kilometer/Spidometer Bus berfungsi dengan baik',
        'Bus dikeluarkan dari Trayek Transpadang yaitu setelah mencapai Halte terdekat dalam arah perjalanannya untuk digantikan armada bus cadangan', NULL, 1, TRUE),
    (v_busdriver, '2',  'Pengemudi Menaikan/Menurunkan Penumpang di Lokasi Halte Transpadang',
        'Denda sebesar Rp. 300.000,- tiap pelanggaran', 300000, 2, TRUE),
    (v_busdriver, '3',  'Bus dalam keadaan bersih bagian luar/dalam',
        'Denda sebesar Rp. 300.000,- /hari', 300000, 3, TRUE),
    (v_busdriver, '4',  'Melakukan operasi dan layanan sesuai waktu operasi dan persetujuan Perumda PSM dan petugas operasinya.',
        'Pengurangan kilometer tempuh bus sebesar 70 km perpelanggaran', NULL, 4, TRUE),
    (v_busdriver, '5',  'Suhu udara dalam ruang/ kabin penumpang bus Maksimal 27o C',
        'Kilometer tempuh bus pada hari itu diperhitungkan 70% dari kilometer tempuh yang telah dicapai pada rit/putaran terjadinya pelanggaran atau denda minimal Rp. 300.000,- perbus', 300000, 5, TRUE),
    (v_busdriver, '6',  'Pelayanan operasi sesuai dengan jam mulai operasi',
        'Pengurangan kilometer tempuh sebesar 1 Round Trip tiap pelanggaran pada hari itu.', NULL, 6, TRUE),
    (v_busdriver, '7',  'Pengemudi mengemudikan bus sesuai dengan SOP',
        'Bus dikeluarkan dari trayek Transpadang. Denda sebesar Rp. 500.000,- per bus tiap pelanggaran', 500000, 7, TRUE),
    (v_busdriver, '8',  'Pengemudi tidak makan/ minum/ merokok di dalam bus',
        'Denda sebesar Rp. 500.000,- per bus tiap pelanggaran', 500000, 8, TRUE),
    (v_busdriver, '9',  'Pengemudi tidak membawa/ menggunakan narkoba/ obat-obat berbahaya/ minuman keras',
        'Denda sebesar Rp. 1.000.000,- tiap pelanggaran. Pengemudi diproses sesuai ketentuan hukum yang berlaku', 1000000, 9, TRUE),
    (v_busdriver, '10', 'Pengemudi mengemudikan bus sesuai dengan kecepatan yang telah ditentukan 30-50 km/jam',
        'Denda sebesar Rp. 300.000,- tiap pelanggaran', 300000, 10, TRUE),
    (v_busdriver, '11', 'Pengemudi mengenakan identitas pribadi atau identitas bus',
        'Denda sebesar Rp. 300.000,- perpelanggaran', 300000, 11, TRUE),
    (v_busdriver, '12', 'Pengemudi mengenakan seragam dan perlengkapan standar sebagaimana tercantum dalam standar pengemudi',
        'Denda sebesar Rp. 300.000,- perpelanggaran', 300000, 12, TRUE),
    (v_busdriver, '13', 'Pengemudi berprilaku sopan kepada penumpang',
        'Denda sebesar Rp. 300.000,- perpelanggaran', 300000, 13, TRUE),
    (v_busdriver, '14', 'Pengemudi mengangkut penumpang yang telah berada di dalam halte Transpadang',
        'Pengurangan kilometer tempuh bus sebesar 70% pada rit/putaran terjadinya pelanggaran atau denda minimal Rp. 300.000,- per bus.', 300000, 14, TRUE),
    (v_busdriver, '15', 'Pengemudi berhenti di Halte Transpadang pada jadwal dan rute yang telah ditentukan',
        'Denda sebesar Rp. 300.000,- per bus per Halte dimana bus tidak berhenti.', 300000, 15, TRUE);

    -- =================================================================
    -- 5. ITEM LAPORAN KORLAP (10 item, Ada/Tidak)
    -- =================================================================
    INSERT INTO checklist_item (template_id, nomor_urut, uraian, urutan, aktif) VALUES
    (v_korlap, '1',  'Armada beroperasi sesuai rute & jadwal (Time Table)',                                          1,  TRUE),
    (v_korlap, '2',  'Pengemudi menaikan/ menurunkan penumpang pada Halte Trans Padang',                             2,  TRUE),
    (v_korlap, '3',  'Suhu udara dalam ruang/kabin 25o - 27o',                                                       3,  TRUE),
    (v_korlap, '4',  'Kelengkapan atribut pramugara',                                                                4,  TRUE),
    (v_korlap, '5',  'Kelengkapan atribut pengemudi',                                                                5,  TRUE),
    (v_korlap, '6',  'Pramugara aktif melayani penumpang',                                                           6,  TRUE),
    (v_korlap, '7',  'Semua penumpang telah melakukan pembayaran tiket maksimal pada halte ke-3 dari titik 0',       7,  TRUE),
    (v_korlap, '8',  'Pengecekan jumlah penumpang',                                                                  8,  TRUE),
    (v_korlap, '9',  'Pengecekan penggunaan asesoris tambahan pada bus yang tidak sesuai dengan ketentuan SPM',      9,  TRUE),
    (v_korlap, '10', 'Pengecekan musik dalam bus (dilarang suara keras)',                                            10, TRUE);

END $$;

-- Verifikasi:
-- SELECT id, kode, nama, tipe_jawaban, subjek, pakai_denda FROM checklist_template ORDER BY id;          -- 4 template
-- SELECT t.kode, count(*) FROM checklist_item i JOIN checklist_template t ON t.id=i.template_id GROUP BY t.kode;
--   KENDARAAN=12, PRAMUGARA=13, BUS_DRIVER=15, KORLAP=10
