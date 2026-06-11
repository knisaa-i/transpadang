# Frontend SPM Trans Padang (statis)

FE sederhana (HTML + CSS + JavaScript murni, **tanpa build / tanpa npm**) untuk mencoba alur:
**login → input penilaian → daftar penilaian → alur status (maker/checker/approver)**.

Terhubung ke backend Spring Boot di `http://localhost:8080` (lihat konstanta `API` di `app.js`).
Folder ini **terpisah** dari kode BE — menghapusnya tidak memengaruhi BE.

## Prasyarat
1. Backend **sedang jalan** di port 8080.
2. Backend sudah **di-restart** setelah penambahan CORS (`CorsConfig.java`).
3. Data master & user sudah di-seed (koridor, bus, indikator, users).

## Cara menjalankan FE
Buka folder ini lalu jalankan salah satu:

**Opsi A — Python**
```bash
cd frontend
python -m http.server 5500
```
Lalu buka `http://localhost:5500`.

**Opsi B — VS Code Live Server**
Klik kanan `index.html` → "Open with Live Server".

> Jalankan lewat server (bukan dobel-klik `file://...`) supaya `fetch` + CORS bekerja normal.

## Login contoh
| username | role | yang bisa dilakukan |
|----------|------|---------------------|
| `staf01` | MAKER | buat penilaian, submit |
| `kadiv01` | CHECKER | periksa (CHECKED) / tolak |
| `manager01` | APPROVER | setujui (APPROVED) / tolak |
| `admin` | ADMIN | semua |

Password semua: `trans12345`.

## Catatan
- Token JWT disimpan di `localStorage`; tombol "Keluar" menghapusnya.
- Tab "Input Penilaian" hanya muncul untuk MAKER/ADMIN.
- Tombol aksi status di "Daftar Penilaian" muncul sesuai role & status.
- Ganti `const API` di `app.js` bila BE berjalan di host/port lain.
