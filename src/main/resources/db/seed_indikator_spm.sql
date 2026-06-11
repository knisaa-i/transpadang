-- =====================================================================
-- SEED DATA SPM TRANS PADANG  (sesuai ERD: spm_penilaian_bus_erd)
-- Sumber: "Form Bobot SPM TP Perwako New.xlsx" (Perwako No. 127 Tahun 2021)
--
-- Mengisi master panduan penilaian yang dilihat penilai SEBELUM memberi nilai:
--   aspek_pelayanan  -> Jenis Pelayanan Dasar (Keamanan, Keselamatan, dst)
--   sub_kategori     -> kelompok (Halte / Bus / Manusia)
--   indikator_spm    -> uraian (judul + penjelasan, multi-line),
--                       spm_indikator, spm_nilai, target_capaian, bobot
--
-- Total bobot seluruh indikator = 1.0000 ; jumlah indikator = 31
-- Jalankan di PostgreSQL (DBeaver: paste -> Alt+X, atau psql -f).
--
-- CATATAN: skrip ini AMAN DIJALANKAN BERULANG. Baris TRUNCATE di bawah
-- mengosongkan dulu data master + penilaian_detail (CASCADE) lalu mengisi ulang.
-- Hapus baris TRUNCATE bila tidak ingin data lama terhapus.
-- =====================================================================

DO $$
DECLARE
    v_keamanan      BIGINT;
    v_keselamatan   BIGINT;
    v_kenyamanan    BIGINT;
    v_kesetaraan    BIGINT;
    v_keteraturan   BIGINT;

    v_kea_halte     BIGINT;
    v_kea_bus       BIGINT;
    v_kes_manusia   BIGINT;
    v_kes_bus       BIGINT;
    v_ken_halte     BIGINT;
    v_ken_bus       BIGINT;
BEGIN
    -- Reset agar bisa dijalankan ulang tanpa duplikasi
    TRUNCATE indikator_spm, sub_kategori, aspek_pelayanan RESTART IDENTITY CASCADE;

    -- ---------------------------------------------------------------
    -- 1. ASPEK PELAYANAN
    -- ---------------------------------------------------------------
    INSERT INTO aspek_pelayanan (nama, urutan) VALUES ('Keamanan', 1)   RETURNING id INTO v_keamanan;
    INSERT INTO aspek_pelayanan (nama, urutan) VALUES ('Keselamatan', 2) RETURNING id INTO v_keselamatan;
    INSERT INTO aspek_pelayanan (nama, urutan) VALUES ('Kenyamanan', 3)  RETURNING id INTO v_kenyamanan;
    INSERT INTO aspek_pelayanan (nama, urutan) VALUES ('Kesetaraan', 4)  RETURNING id INTO v_kesetaraan;
    INSERT INTO aspek_pelayanan (nama, urutan) VALUES ('Keteraturan', 5) RETURNING id INTO v_keteraturan;

    -- ---------------------------------------------------------------
    -- 2. SUB KATEGORI
    -- ---------------------------------------------------------------
    INSERT INTO sub_kategori (aspek_id, nama, urutan) VALUES (v_keamanan, 'Halte', 1)      RETURNING id INTO v_kea_halte;
    INSERT INTO sub_kategori (aspek_id, nama, urutan) VALUES (v_keamanan, 'Bus', 2)        RETURNING id INTO v_kea_bus;
    INSERT INTO sub_kategori (aspek_id, nama, urutan) VALUES (v_keselamatan, 'Manusia', 1) RETURNING id INTO v_kes_manusia;
    INSERT INTO sub_kategori (aspek_id, nama, urutan) VALUES (v_keselamatan, 'Bus', 2)     RETURNING id INTO v_kes_bus;
    INSERT INTO sub_kategori (aspek_id, nama, urutan) VALUES (v_kenyamanan, 'Halte', 1)    RETURNING id INTO v_ken_halte;
    INSERT INTO sub_kategori (aspek_id, nama, urutan) VALUES (v_kenyamanan, 'Bus', 2)      RETURNING id INTO v_ken_bus;

    -- ---------------------------------------------------------------
    -- 3. INDIKATOR SPM
    --    (aspek_id, sub_kategori_id, nomor_urut, uraian,
    --     spm_indikator, spm_nilai, target_capaian, bobot, aktif)
    --    uraian = judul + penjelasan (multi-line, panduan penilai)
    -- ---------------------------------------------------------------

    -- 1. KEAMANAN / Halte
    INSERT INTO indikator_spm (aspek_id, sub_kategori_id, nomor_urut, uraian, spm_indikator, spm_nilai, target_capaian, bobot, aktif) VALUES
    (v_keamanan, v_kea_halte, '1',
     E'Informasi gangguan keamanan\nInformasi yang disampaikan penumpang apabila mendapat gangguan keamanan berupa stiker yang mencantumkan nomor telpon dan atau SMS pengaduan ditempel pada tempat yang stategis dan mudah terlihat.',
     'Jumlah stiker per halte tertempel dalam kondisi baik', 'Minimal 2 (dua) stiker', 100.00, 0.0200, TRUE),
    (v_keamanan, v_kea_halte, '2',
     E'CCTV\na. Sebagai sarana pengawasan terhadap aktifitas selama di halte\nb. CCTV berfungsi dan merekam aktifitas di Halte',
     'Jumlah CCTV per halte', 'Minimal 1 (satu) CCTV', 100.00, 0.0300, TRUE);

    -- 1. KEAMANAN / Bus
    INSERT INTO indikator_spm (aspek_id, sub_kategori_id, nomor_urut, uraian, spm_indikator, spm_nilai, target_capaian, bobot, aktif) VALUES
    (v_keamanan, v_kea_bus, '1',
     E'Identitas Kendaraan\na. Papan trayek/rute berupa display LED yang dispasang pada bagian depan kendaraan\nb. Nomor body kendaraan yang dipasang pada sisi depan, belakang, kiri, dan kanan kendaraan.',
     'Jumlah display LED yang berfungsi dan jumlah nomor body per kendaraan', 'Minimal 1 (satu) display LED dan 3 (tiga) nomor body', 100.00, 0.0200, TRUE),
    (v_keamanan, v_kea_bus, '2',
     E'Tanda Darurat pengenal pengemudi dan seragam awak kendaraan\na. Berbentuk tanda pengenal nama pengemudi yang ditempatkan di ruang pengemudi.\nb. Seragam awak yang dilengkapi dengan tanda pengenal diri',
     'Tersedia Tanda Pengenal Pengemudi dan dikenakannya Seragam oleh awak kendaraan',
     E'a. 1 (satu) tanda pengenal pengemudi.\nb. Wajib mengenakan seragam dan tanda pengenal diri.', 100.00, 0.0400, TRUE),
    (v_keamanan, v_kea_bus, '3',
     E'Lampu Isyarat Tanda Darurat\nLampu informasi sebagai tanda Darurat berupa tombol yang ditempatkan di ruang pengemudi',
     'Lampu isyarat tanda darurat', 'Berfungsi', 100.00, 0.0200, TRUE),
    (v_keamanan, v_kea_bus, '4',
     E'Petugas Dalam Bus\nOrang yang bertugas menjaga keamanan, dalam bus',
     'Jumlah petugas', 'Minimal 1 (satu) orang per unit bus', 100.00, 0.0200, TRUE),
    (v_keamanan, v_kea_bus, '5',
     E'Kegelapan kaca film\nLapisan pada kaca samping kendaraan guna mengurangi cahaya matahari secara langsung',
     'Persentase kegelapan', 'Maksimal 60%', 100.00, 0.0200, TRUE),
    (v_keamanan, v_kea_bus, '6',
     E'Televisi Sirkuit Tertutup (Closed Circuit Television/CCTV)\na. Sebagai sarana pengawasan terhadap aktifitas selama di dalam bus.\nb. CCTV berfungsi dan merekam aktifitas di dalam bus',
     'Jumlah CCTV per kendaraan', 'Minimal 1 (satu) unit CCTV', 100.00, 0.0300, TRUE);

    -- 2. KESELAMATAN / Manusia
    INSERT INTO indikator_spm (aspek_id, sub_kategori_id, nomor_urut, uraian, spm_indikator, spm_nilai, target_capaian, bobot, aktif) VALUES
    (v_keselamatan, v_kes_manusia, '1',
     E'SOP Pengeoperasian Kendaraan\nTata tertip mengoperasikan kendaran yang wajib dipatuhi oleh awak kendaraan sekurang-kurangnya yang memuat :\na. Tata tertip mengemudi/operasional kendaraan.\nb. Tata tertip menaikan dan menurukan penumpang',
     E'a. Ketersedian dokumen SOP di dalam bus.\nb. Penerapan SOP oleh pengemudi dan petugas dalam bus.',
     E'a. Minimal 1 (satu) dokumen SOP.\nb. Paramater perhitungan kelalaian penerapan SOP diatur dalam Naskah Perjanjian Subsidi', 100.00, 0.0300, TRUE),
    (v_keselamatan, v_kes_manusia, '2',
     E'SOP Penanganan Keadaan Darurat\nTata tertip penanganan keadaan darurat untuk keselamatan awak kendaraan dan penumpang',
     E'a. Ketersedian dokumen SOP di dalam bus.\nb. Penerapan SOP oleh pengemudi dan petugas dalam bus.',
     E'a. Minimal 1 (satu) dokumen SOP.\nb. Paramater perhitungan kelalaian penerapan SOP diatur dalam Naskah Perjanjian Subsidi', 100.00, 0.0300, TRUE);

    -- 2. KESELAMATAN / Bus
    INSERT INTO indikator_spm (aspek_id, sub_kategori_id, nomor_urut, uraian, spm_indikator, spm_nilai, target_capaian, bobot, aktif) VALUES
    (v_keselamatan, v_kes_bus, '1',
     E'Kelaikan Kendaraan\nKendaraan yang dioperasikan wajib laik jalan',
     'Dilengkapi dengan Kartu Uji, Tanda Uji dan Plat Uji per kendaraan', 'Masa berlaku Uji Berkala masih akif', 100.00, 0.0300, TRUE),
    (v_keselamatan, v_kes_bus, '2',
     E'Peralatan Keselamatan\nFasilitas penyelamatan darurat dalam bahaya, dipasang di tempat yang mudah dicapai dilengkapi dengan keterangan tata cara penggunaan berbentuk stiker dan paling sedikit meliputi :\na. Palu pemecah kaca.\nb. Tabung pemadam kebakaran : dan\nc. Lampu Senter',
     'Jumlah fasilitas dan berfungsi dengan baik',
     E'a. Palu pemecah 2 (dua) buah.\nb. Tabung pemadam kebakaran 1 (satu) buah.\nc. Lampu senter 1 (satu) buah.', 100.00, 0.0300, TRUE),
    (v_keselamatan, v_kes_bus, '3',
     E'Fasilitas Kesehatan\nFasilitas kesehatan yang digunakan untuk penanganan darurat kecelakaan dalam bus, berupa set perlengkapan P3K (Pertolongan Pertama Pada Kecelakaan) yang paling sedikit terdiri dari Kapas bersih, larutan Iodine, gunting kecil, plaster, elastis, kasa steril, alkohol 70%',
     'Tersedianya Perlengkapan P3K per kendaraan yang tidak melebihi masa kadarluasa', '1 (satu) set', 100.00, 0.0300, TRUE),
    (v_keselamatan, v_kes_bus, '4',
     E'Informasi nomor pengaduan\nInformasi yang disampaikan penumpang/masyarakat apabila terjadi kondisi darurat berisi nomor telpon dan atau SMS pengaduan ditempel pada tempat yang strategis dan mudah dilihat',
     'Jumlah tampilan nomor pengaduan di dalam dan di luar bus',
     E'a. Di dalam bus : 2 (dua) buah.\nb. Di luar bus : 1 (satu) buah.', 100.00, 0.0200, TRUE),
    (v_keselamatan, v_kes_bus, '5',
     E'Fasilitas pegangan pengguna jasa berdiri\nAlat bantu pengguna jasa yang berdiri di dalam bus',
     E'a. Ketersedian sesuai spesifikasi teknis bus.\nb. Berfungsi dengan baik.', 'Tersedia dan berfungsi', 100.00, 0.0200, TRUE),
    (v_keselamatan, v_kes_bus, '6',
     E'Pintu keluar dan/atau masuk pengguna Jasa\na. Pintu Berfungsi\nb. Pintu keluar dan/atau masuk pengguna jasa harus tertutup pada saat kendaraan berjalan',
     'Berfungsi dan tertutup pada saat berjalan', 'Dapat berfungsi dengan baik dan tertutup pada saat berjalan', 100.00, 0.0200, TRUE);

    -- 3. KENYAMANAN / Halte
    INSERT INTO indikator_spm (aspek_id, sub_kategori_id, nomor_urut, uraian, spm_indikator, spm_nilai, target_capaian, bobot, aktif) VALUES
    (v_kenyamanan, v_ken_halte, '1',
     E'Lampu Penerangan\nBerfungsi sebagai sumber cahaya di halte untuk memberikan rasa nyaman bagi penumpang',
     'Tingkat pencahayaan per halte', 'Minimal 1 (satu) lampu per halte', 100.00, 0.0300, TRUE),
    (v_kenyamanan, v_ken_halte, '2',
     E'Kebersihan Halte\nHalte bebas dari sampah organik dan non organik',
     'Bersih dari sampah', E'a. Minimal 1 (satu) tempat sampah per halte.\nb. Bebas dari sampah.', 100.00, 0.0300, TRUE),
    (v_kenyamanan, v_ken_halte, '3',
     E'Kemudahan naik dan turun pengguna jasa\nMemberikan kemudahan penumpang untuk naik dan turun dari bus',
     'Tinggi lantai halte sama dengan tinggi lantai bus', 'Tinggi lantai halte dan bus dengan toleransi 10 cm', 100.00, 0.0300, TRUE);

    -- 3. KENYAMANAN / Bus
    INSERT INTO indikator_spm (aspek_id, sub_kategori_id, nomor_urut, uraian, spm_indikator, spm_nilai, target_capaian, bobot, aktif) VALUES
    (v_kenyamanan, v_ken_bus, '1',
     E'Lampu Penerangan\nBerfungsi sebagai sumber cahaya di dalam bus (ruang penumpang) untuk memberikan kenyamanan bagi penumpang',
     'Pencahayaan di dalam bus', 'Tersedia dan berfungsi dengan baik', 100.00, 0.0500, TRUE),
    (v_kenyamanan, v_ken_bus, '2',
     E'Faktor Muat\nPerbandingan antara jumlah penumpang yang diangkut dengan kapasitas angkut',
     'Faktor Muat Maksimum', '1', 100.00, 0.0500, TRUE),
    (v_kenyamanan, v_ken_bus, '3',
     E'Fasilitas pengatur suhu ruangan\nFasilitas pengatur suhu di dalam bus menggunakan AC (air conditioner)',
     'Suhu di dalam bus', 'Maksimum 25° C', 100.00, 0.0500, TRUE);

    -- 4. KESETARAAN (tanpa sub kategori)
    INSERT INTO indikator_spm (aspek_id, sub_kategori_id, nomor_urut, uraian, spm_indikator, spm_nilai, target_capaian, bobot, aktif) VALUES
    (v_kesetaraan, NULL, 'a',
     E'Kursi perioritas\nTempat duduk bus yang diperuntukan bagi penyandang stabilitas, lanjut usia, ibu membawa balita dan wanita hamil',
     'Jumlah kursi prioritas', 'Minimal 2 (dua)', 100.00, 0.0100, TRUE),
    (v_kesetaraan, NULL, 'b',
     E'Ruang Khusus untuk Kursi roda\nRuang di dalam bus yang diperuntukan bagi penumpang yang menggunakan kursi roda',
     'Junlah ruang khusus', 'Minimal 1 (satu)', 100.00, 0.0100, TRUE);

    -- 5. KETERATURAN (tanpa sub kategori)
    INSERT INTO indikator_spm (aspek_id, sub_kategori_id, nomor_urut, uraian, spm_indikator, spm_nilai, target_capaian, bobot, aktif) VALUES
    (v_keteraturan, NULL, 'a',
     E'Waktu kedatangan bus di Halte\nKedatangan bus di Halte akhir koridor',
     'Ketepatan waktu kedatangan bus di halte dengan jadwal keberangkatan (time table)', 'Maksimal 10 menit', 100.00, 0.1000, TRUE),
    (v_keteraturan, NULL, 'b',
     E'Kecepatan perjalanan\nKecepatan perjalan bus normal',
     'Kecepatan maksimal', '50 km/jam', 100.00, 0.0300, TRUE),
    (v_keteraturan, NULL, 'c',
     E'Waktu berhenti di Halte\nWaktu berhenti bus normal',
     'Waktu berhenti maksimal', '90 detik', 100.00, 0.0200, TRUE),
    (v_keteraturan, NULL, 'd',
     E'Informasi Pelayanan\nInformasi di dalam bus (berupa audio dan atau visual) untuk memperjelas penumpang yang akan turun di suatu Halte',
     'Informasi yang jelas', 'Harus tersedia', 100.00, 0.0200, TRUE),
    (v_keteraturan, NULL, 'e',
     E'Sistem Pembayaran\nMetode pembelian tiket yang praktis, mudah dan transparan',
     'Tersedianya sistem E-Ticketing', 'Semua sistem pembayaran berfungsi baik di dalam bus', 100.00, 0.0300, TRUE),
    (v_keteraturan, NULL, 'f',
     E'Dokumen perjalanan\nDokumen perjalanan kendaraan yang melekat pada seluruh kendaraan yang dioperasikan. Lengkapnya dokumen perjalanan yang terdiri dari :\na. SIM Umum sesuai jenis kendaraan.\nb. STNK.\nc. Kartu Pengawasan.',
     NULL, 'Masih berlaku', 100.00, 0.1000, TRUE),
    (v_keteraturan, NULL, 'g',
     E'Sitem Pemosisi Global (Global Postioning System/GPS)\nTerintegrasi dengan ruang kendali utama',
     'Tersedianya sistem GPS di setiap bus', 'Berfungsi dengan baik', 100.00, 0.0300, TRUE);

END $$;

-- Verifikasi:
-- SELECT SUM(bobot) AS total_bobot FROM indikator_spm;        -- harapkan 1.0000
-- SELECT COUNT(*) AS jumlah_indikator FROM indikator_spm;     -- harapkan 31
-- SELECT COUNT(*) FROM aspek_pelayanan;                       -- harapkan 5
-- SELECT i.nomor_urut, a.nama AS aspek, s.nama AS sub_kategori, i.uraian
-- FROM indikator_spm i
-- JOIN aspek_pelayanan a ON i.aspek_id = a.id
-- LEFT JOIN sub_kategori s ON i.sub_kategori_id = s.id
-- ORDER BY a.urutan, s.urutan, i.id;
