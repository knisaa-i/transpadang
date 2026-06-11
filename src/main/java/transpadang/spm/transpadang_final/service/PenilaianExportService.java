package transpadang.spm.transpadang_final.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.entity.Bus;
import transpadang.spm.transpadang_final.entity.IndikatorSpm;
import transpadang.spm.transpadang_final.entity.PenilaianDetail;
import transpadang.spm.transpadang_final.entity.PenilaianSpm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Export satu penilaian SPM ke Excel (semua bus sebagai kolom), mirip
 * "LAPORAN CAPAIAN SPM KORIDOR ..." pada form Perwako.
 */
@Service
public class PenilaianExportService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public byte[] export(Long penilaianId) {
        PenilaianSpm p = em.find(PenilaianSpm.class, penilaianId);
        if (p == null) {
            throw new EntityNotFoundException("Penilaian tidak ditemukan: " + penilaianId);
        }

        List<IndikatorSpm> indikators = em.createQuery(
                "select i from IndikatorSpm i left join fetch i.aspek a left join fetch i.subKategori s " +
                        "order by a.urutan, s.urutan, i.id", IndikatorSpm.class).getResultList();

        List<PenilaianDetail> details = em.createQuery(
                "select d from PenilaianDetail d join fetch d.bus b join fetch d.indikator i " +
                        "where d.penilaian.id = :pid order by b.noLambung, b.id", PenilaianDetail.class)
                .setParameter("pid", penilaianId).getResultList();

        // bus unik (urut sesuai no lambung) + map (indikator,bus) -> detail
        Map<Long, Bus> buses = new LinkedHashMap<>();
        Map<Long, Map<Long, PenilaianDetail>> byIndikator = new LinkedHashMap<>();
        Map<Long, BigDecimal> totalPerBus = new LinkedHashMap<>();
        for (PenilaianDetail d : details) {
            buses.putIfAbsent(d.getBus().getId(), d.getBus());
            byIndikator.computeIfAbsent(d.getIndikator().getId(), k -> new LinkedHashMap<>())
                    .put(d.getBus().getId(), d);
            totalPerBus.merge(d.getBus().getId(),
                    d.getSkorTerbobot() != null ? d.getSkorTerbobot() : BigDecimal.ZERO, BigDecimal::add);
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Capaian SPM");

            CellStyle bold = wb.createCellStyle();
            Font fb = wb.createFont(); fb.setBold(true); bold.setFont(fb);
            CellStyle hdr = wb.createCellStyle();
            hdr.cloneStyleFrom(bold);
            hdr.setBorderBottom(BorderStyle.THIN); hdr.setBorderTop(BorderStyle.THIN);
            hdr.setBorderLeft(BorderStyle.THIN); hdr.setBorderRight(BorderStyle.THIN);
            hdr.setAlignment(HorizontalAlignment.CENTER);
            hdr.setVerticalAlignment(VerticalAlignment.CENTER);
            hdr.setWrapText(true);
            CellStyle cell = wb.createCellStyle();
            cell.setBorderBottom(BorderStyle.THIN); cell.setBorderTop(BorderStyle.THIN);
            cell.setBorderLeft(BorderStyle.THIN); cell.setBorderRight(BorderStyle.THIN);
            cell.setVerticalAlignment(VerticalAlignment.TOP);
            cell.setWrapText(true);
            CellStyle cellNum = wb.createCellStyle();
            cellNum.cloneStyleFrom(cell);
            cellNum.setAlignment(HorizontalAlignment.CENTER);

            int nomorKoridor = p.getKoridor() != null && p.getKoridor().getNomor() != null
                    ? p.getKoridor().getNomor() : 0;

            int r = 0;
            r = title(sheet, r, "LAPORAN CAPAIAN SPM KORIDOR " + nomorKoridor, bold);
            r = title(sheet, r, "SESUAI PERWAKO NOMOR 127 TAHUN 2021", bold);
            r = title(sheet, r, "HARI    : " + nz(p.getHari()), null);
            r = title(sheet, r, "TANGGAL : " + (p.getTanggal() != null ? p.getTanggal().toString() : "-"), null);
            r++; // baris kosong

            // ---- header tabel ----
            int busCount = buses.size();
            int firstBusCol = 7;            // kolom mulai untuk bus
            int rataCol = firstBusCol + busCount;       // Rata-2 capaian
            int rataBobotCol = rataCol + 1;             // Rata-2 terbobot

            Row h = sheet.createRow(r++);
            String[] heads = {"No", "Aspek", "Sub Kategori", "Uraian", "Indikator", "Nilai (Standar)", "Bobot"};
            for (int c = 0; c < heads.length; c++) setC(h, c, heads[c], hdr);
            int bc = firstBusCol;
            for (Bus b : buses.values()) setC(h, bc++, "Lambung " + b.getNoLambung(), hdr);
            setC(h, rataCol, "Rata-2 Capaian", hdr);
            setC(h, rataBobotCol, "Rata-2 Terbobot", hdr);

            // ---- baris indikator ----
            for (IndikatorSpm i : indikators) {
                Row row = sheet.createRow(r++);
                setC(row, 0, i.getNomorUrut(), cell);
                setC(row, 1, i.getAspek() != null ? i.getAspek().getNama() : "", cell);
                setC(row, 2, i.getSubKategori() != null ? i.getSubKategori().getNama() : "", cell);
                setC(row, 3, i.getUraian(), cell);
                setC(row, 4, i.getSpmIndikator(), cell);
                setC(row, 5, i.getSpmNilai(), cell);
                setN(row, 6, i.getBobot(), cellNum);

                Map<Long, PenilaianDetail> perBus = byIndikator.getOrDefault(i.getId(), Map.of());
                BigDecimal sum = BigDecimal.ZERO; int cnt = 0;
                int col = firstBusCol;
                for (Bus b : buses.values()) {
                    PenilaianDetail d = perBus.get(b.getId());
                    if (d != null && d.getNilaiCapaian() != null) {
                        setN(row, col, d.getNilaiCapaian(), cellNum);
                        sum = sum.add(d.getNilaiCapaian()); cnt++;
                    } else {
                        setC(row, col, "", cellNum);
                    }
                    col++;
                }
                BigDecimal rata = cnt > 0 ? sum.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                setN(row, rataCol, rata, cellNum);
                BigDecimal rataBobot = i.getBobot() != null
                        ? rata.multiply(i.getBobot()).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                setN(row, rataBobotCol, rataBobot, cellNum);
            }

            // ---- total per bus ----
            Row totalRow = sheet.createRow(r++);
            setC(totalRow, 0, "TOTAL PENCAPAIAN SPM", bold);
            int col = firstBusCol;
            for (Bus b : buses.values()) {
                setN(totalRow, col++, totalPerBus.getOrDefault(b.getId(), BigDecimal.ZERO), cellNum);
            }
            r += 2;

            // ---- tanda tangan ----
            Row sigTitle = sheet.createRow(r++);
            setC(sigTitle, 1, "Diketahui Oleh,", null);
            setC(sigTitle, 3, "Disetujui Oleh,", null);
            setC(sigTitle, 5, "Direkap Oleh,", null);
            r += 3;
            Row sigName = sheet.createRow(r++);
            setC(sigName, 1, nz(name(p.getChecker())), bold);
            setC(sigName, 3, nz(name(p.getApprover())), bold);
            setC(sigName, 5, nz(name(p.getMaker())), bold);
            Row sigJab = sheet.createRow(r++);
            setC(sigJab, 1, nz(jab(p.getChecker())), null);
            setC(sigJab, 3, nz(jab(p.getApprover())), null);
            setC(sigJab, 5, nz(jab(p.getMaker())), null);

            // lebar kolom
            sheet.setColumnWidth(1, 4500);
            sheet.setColumnWidth(2, 3500);
            sheet.setColumnWidth(3, 14000);
            sheet.setColumnWidth(4, 9000);
            sheet.setColumnWidth(5, 9000);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat Excel: " + e.getMessage(), e);
        }
    }

    /** Export SEMUA penilaian: sheet "Rekap" (1 baris per penilaian) + sheet "Detail" (semua baris). */
    @Transactional(readOnly = true)
    public byte[] exportAll() {
        List<PenilaianSpm> penilaians = em.createQuery(
                "select p from PenilaianSpm p left join fetch p.koridor left join fetch p.maker " +
                        "left join fetch p.checker left join fetch p.approver order by p.tanggal desc, p.id desc",
                PenilaianSpm.class).getResultList();
        List<PenilaianDetail> details = em.createQuery(
                "select d from PenilaianDetail d join fetch d.penilaian pp join fetch d.bus b " +
                        "join fetch d.indikator i left join fetch i.aspek left join fetch i.subKategori " +
                        "order by pp.id, b.noLambung, i.id", PenilaianDetail.class).getResultList();

        Map<Long, BigDecimal> totalPerPenilaian = new LinkedHashMap<>();
        for (PenilaianDetail d : details) {
            totalPerPenilaian.merge(d.getPenilaian().getId(),
                    d.getSkorTerbobot() != null ? d.getSkorTerbobot() : BigDecimal.ZERO, BigDecimal::add);
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font fb = wb.createFont(); fb.setBold(true); bold.setFont(fb);

            // ---- Sheet Rekap ----
            Sheet rekap = wb.createSheet("Rekap");
            String[] rh = {"ID", "Koridor", "Hari", "Tanggal", "Status", "Maker", "Checker", "Approver", "Total Capaian"};
            Row rhead = rekap.createRow(0);
            for (int c = 0; c < rh.length; c++) setC(rhead, c, rh[c], bold);
            int ri = 1;
            for (PenilaianSpm p : penilaians) {
                Row row = rekap.createRow(ri++);
                setN(row, 0, BigDecimal.valueOf(p.getId()), null);
                setC(row, 1, p.getKoridor() != null ? p.getKoridor().getNama() : "", null);
                setC(row, 2, nz(p.getHari()), null);
                setC(row, 3, p.getTanggal() != null ? p.getTanggal().toString() : "", null);
                setC(row, 4, p.getStatus() != null ? p.getStatus().name() : "", null);
                setC(row, 5, name(p.getMaker()), null);
                setC(row, 6, name(p.getChecker()), null);
                setC(row, 7, name(p.getApprover()), null);
                setN(row, 8, totalPerPenilaian.getOrDefault(p.getId(), BigDecimal.ZERO), null);
            }
            for (int c = 0; c < rh.length; c++) rekap.autoSizeColumn(c);

            // ---- Sheet Detail ----
            Sheet det = wb.createSheet("Detail");
            String[] dh = {"ID Penilaian", "Tanggal", "Koridor", "Lambung", "Aspek", "Sub Kategori",
                    "No", "Uraian", "Indikator", "Nilai Standar", "Nilai Capaian", "Bobot", "Skor Terbobot", "Catatan"};
            Row dhead = det.createRow(0);
            for (int c = 0; c < dh.length; c++) setC(dhead, c, dh[c], bold);
            int di = 1;
            for (PenilaianDetail d : details) {
                PenilaianSpm p = d.getPenilaian();
                IndikatorSpm i = d.getIndikator();
                Row row = det.createRow(di++);
                setN(row, 0, BigDecimal.valueOf(p.getId()), null);
                setC(row, 1, p.getTanggal() != null ? p.getTanggal().toString() : "", null);
                setC(row, 2, p.getKoridor() != null ? p.getKoridor().getNama() : "", null);
                setC(row, 3, d.getBus() != null ? d.getBus().getNoLambung() : "", null);
                setC(row, 4, i.getAspek() != null ? i.getAspek().getNama() : "", null);
                setC(row, 5, i.getSubKategori() != null ? i.getSubKategori().getNama() : "", null);
                setC(row, 6, i.getNomorUrut(), null);
                setC(row, 7, i.getUraian(), null);
                setC(row, 8, i.getSpmIndikator(), null);
                setC(row, 9, i.getSpmNilai(), null);
                setN(row, 10, d.getNilaiCapaian(), null);
                setN(row, 11, i.getBobot(), null);
                setN(row, 12, d.getSkorTerbobot(), null);
                setC(row, 13, d.getCatatan(), null);
            }
            det.setColumnWidth(7, 14000);
            det.setColumnWidth(8, 9000);
            det.setColumnWidth(9, 9000);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat Excel: " + e.getMessage(), e);
        }
    }

    private int title(Sheet sheet, int r, String text, CellStyle style) {
        Row row = sheet.createRow(r);
        setC(row, 0, text, style);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 5));
        return r + 1;
    }

    private void setC(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val == null ? "" : val);
        if (style != null) c.setCellStyle(style);
    }

    private void setN(Row row, int col, BigDecimal val, CellStyle style) {
        Cell c = row.createCell(col);
        if (val != null) c.setCellValue(val.doubleValue());
        if (style != null) c.setCellStyle(style);
    }

    private String nz(String s) { return s == null ? "" : s; }
    private String name(transpadang.spm.transpadang_final.entity.User u) { return u == null ? "" : u.getNama(); }
    private String jab(transpadang.spm.transpadang_final.entity.User u) { return u == null ? "" : u.getJabatan(); }
}
