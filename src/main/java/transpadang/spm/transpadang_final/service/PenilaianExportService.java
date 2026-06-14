package transpadang.spm.transpadang_final.service;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.entity.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Export penilaian SPM ke Excel. Query memakai QueryDSL (JPAQueryFactory).
 * export(id) menghasilkan 2 sheet: "Bus" dan "Halte".
 */
@Service
@RequiredArgsConstructor
public class PenilaianExportService {

    private final JPAQueryFactory queryFactory;
    private final CriteriaBuilderFactory cbf;
    private final EntityManager em;


    @Transactional(readOnly = true)
    public byte[] export(Long penilaianId) {
        var qp = new QPenilaianSpm("p");
        PenilaianSpm p = queryFactory.selectFrom(qp)
                .leftJoin(qp.koridor).fetchJoin()
                .leftJoin(qp.maker).fetchJoin()
                .leftJoin(qp.checker).fetchJoin()
                .leftJoin(qp.approver).fetchJoin()
                .where(qp.id.eq(penilaianId))
                .fetchOne();
        if (p == null) {
            throw new EntityNotFoundException("Penilaian tidak ditemukan: " + penilaianId);
        }

        var qi = new QIndikatorSpm("i");
        var qa = new QAspekPelayanan("a");
        var qs = new QSubKategori("s");
        List<IndikatorSpm> indikators = queryFactory.selectFrom(qi)
                .leftJoin(qi.aspek, qa).fetchJoin()
                .leftJoin(qi.subKategori, qs).fetchJoin()
                .orderBy(qa.urutan.asc(), qs.urutan.asc(), qi.id.asc())
                .fetch();

        var qd = new QPenilaianDetail("d");
        var qb = new QBus("b");
        var qh = new QHalte("h");
        List<PenilaianDetail> busDetails = queryFactory.selectFrom(qd)
                .join(qd.bus, qb).fetchJoin()
                .join(qd.indikator).fetchJoin()
                .where(qd.penilaian.id.eq(penilaianId))
                .orderBy(qb.noLambung.asc(), qb.id.asc())
                .fetch();
        List<PenilaianDetail> halteDetails = queryFactory.selectFrom(qd)
                .join(qd.halte, qh).fetchJoin()
                .join(qd.indikator).fetchJoin()
                .where(qd.penilaian.id.eq(penilaianId))
                .orderBy(qh.nomor.asc(), qh.id.asc())
                .fetch();

        // semua unit (master) koridor ini → jadi kolom, walau belum dinilai
        Long koridorId = p.getKoridor() != null ? p.getKoridor().getId() : null;
        var qbAll = new QBus("ba");
        List<Bus> busUnits = queryFactory.selectFrom(qbAll)
                .where(qbAll.koridor.id.eq(koridorId).and(qbAll.aktif.isTrue()))
                .orderBy(qbAll.noLambung.asc(), qbAll.id.asc()).fetch();
        var qhAll = new QHalte("ha");
        List<Halte> halteUnits = queryFactory.selectFrom(qhAll)
                .where(qhAll.koridor.id.eq(koridorId).and(qhAll.aktif.isTrue()))
                .orderBy(qhAll.nomor.asc(), qhAll.id.asc()).fetch();

        // pisahkan indikator: kategori Halte (sub_kategori = Halte) vs Bus (sisanya)
        List<IndikatorSpm> busInd = new ArrayList<>();
        List<IndikatorSpm> halteInd = new ArrayList<>();
        for (IndikatorSpm i : indikators) {
            boolean isHalte = i.getSubKategori() != null
                    && "Halte".equalsIgnoreCase(i.getSubKategori().getNama());
            (isHalte ? halteInd : busInd).add(i);
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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

            writeSheet(wb, "Bus", "NO LAMBUNG", p, busInd, prepareBus(busUnits, busDetails), bold, hdr, cell, cellNum);
            writeSheet(wb, "Halte", "HALTE", p, halteInd, prepareHalte(halteUnits, halteDetails), bold, hdr, cell, cellNum);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat Excel: " + e.getMessage(), e);
        }
    }

    /** Kolom = SEMUA bus master; nilai diisi dari detail bila ada. */
    private SheetData prepareBus(List<Bus> units, List<PenilaianDetail> details) {
        var data = new SheetData();
        for (Bus u : units) data.units.put(u.getId(), "Lambung " + u.getNoLambung());
        for (PenilaianDetail d : details) {
            if (d.getBus() == null) continue;
            Long uid = d.getBus().getId();
            data.units.putIfAbsent(uid, "Lambung " + d.getBus().getNoLambung());
            fill(data, uid, d);
        }
        return data;
    }

    /** Kolom = SEMUA halte master; nilai diisi dari detail bila ada. */
    private SheetData prepareHalte(List<Halte> units, List<PenilaianDetail> details) {
        var data = new SheetData();
        for (Halte u : units) data.units.put(u.getId(), "Halte " + u.getNomor());
        for (PenilaianDetail d : details) {
            if (d.getHalte() == null) continue;
            Long uid = d.getHalte().getId();
            data.units.putIfAbsent(uid, "Halte " + d.getHalte().getNomor());
            fill(data, uid, d);
        }
        return data;
    }

    private void fill(SheetData data, Long uid, PenilaianDetail d) {
        data.byIndikator.computeIfAbsent(d.getIndikator().getId(), k -> new LinkedHashMap<>()).put(uid, d);
        data.totalPerUnit.merge(uid,
                d.getSkorTerbobot() != null ? d.getSkorTerbobot() : BigDecimal.ZERO, BigDecimal::add);
    }

    /** Tulis satu sheet laporan (Bus/Halte) mengikuti format form Perwako asli. */
    private void writeSheet(Workbook wb, String sheetName, String unitHeader, PenilaianSpm p,
                            List<IndikatorSpm> indikators, SheetData data,
                            CellStyle bold, CellStyle hdr, CellStyle cell, CellStyle cellNum) {
        Sheet sheet = wb.createSheet(sheetName);
        List<Long> unitIds = new ArrayList<>(data.units.keySet());
        int n = unitIds.size();

        final int cNo = 0, cJenis = 1, cUraian = 2, cInd = 3, cNilai = 4, cTarget = 5, cBobot = 6;
        int firstUnit = 7;
        int rataCap = firstUnit + n;
        int rataBob = rataCap + 1;
        int lastCol = rataBob;

        int nomorKoridor = p.getKoridor() != null && p.getKoridor().getNomor() != null
                ? p.getKoridor().getNomor() : 0;

        int r = 0;
        r = titleWide(sheet, r, "LAPORAN CAPAIAN SPM KORIDOR " + nomorKoridor + " — " + sheetName.toUpperCase(), bold, lastCol);
        r = titleWide(sheet, r, "SESUAI PERWAKO NOMOR 127 TAHUN 2021", bold, lastCol);
        r = titleWide(sheet, r, "HARI    : " + nz(p.getHari()), null, lastCol);
        r = titleWide(sheet, r, "TANGGAL : " + (p.getTanggal() != null ? p.getTanggal().toString() : "-"), null, lastCol);
        r++; // baris kosong

        // ---- header 2 baris (mirip form asli) ----
        int h1 = r, h2 = r + 1;
        Row rowH1 = sheet.createRow(h1);
        Row rowH2 = sheet.createRow(h2);
        setC(rowH1, cNo, "No", hdr);                       mergeV(sheet, h1, h2, cNo);
        setC(rowH1, cJenis, "Jenis Pelayanan Dasar", hdr); mergeV(sheet, h1, h2, cJenis);
        setC(rowH1, cUraian, "Uraian", hdr);               mergeV(sheet, h1, h2, cUraian);
        setC(rowH1, cInd, "Standar Pelayanan Minimal", hdr);
        sheet.addMergedRegion(new CellRangeAddress(h1, h1, cInd, cNilai));
        setC(rowH2, cInd, "Indikator", hdr);
        setC(rowH2, cNilai, "Nilai", hdr);
        setC(rowH1, cTarget, "Target Capaian", hdr);       mergeV(sheet, h1, h2, cTarget);
        setC(rowH1, cBobot, "Bobot", hdr);                 mergeV(sheet, h1, h2, cBobot);
        if (n > 0) {
            setC(rowH1, firstUnit, unitHeader, hdr);
            sheet.addMergedRegion(new CellRangeAddress(h1, h1, firstUnit, firstUnit + n - 1));
        }
        int uc = firstUnit;
        for (Long uid : unitIds) setC(rowH2, uc++, data.units.get(uid), hdr);
        setC(rowH1, rataCap, "RATA-2", hdr);
        sheet.addMergedRegion(new CellRangeAddress(h1, h1, rataCap, rataBob));
        setC(rowH2, rataCap, "Capaian SPM", hdr);
        setC(rowH2, rataBob, "Bobot", hdr);
        r = h2 + 1;

        // ---- baris data dikelompokkan per aspek → sub ----
        String curAspek = null, curSub = "__";
        BigDecimal bobotSheet = BigDecimal.ZERO;
        for (IndikatorSpm i : indikators) {
            String aspekNama = i.getAspek() != null ? i.getAspek().getNama() : "-";
            Integer aspekUrut = i.getAspek() != null ? i.getAspek().getUrutan() : null;
            String subNama = i.getSubKategori() != null ? i.getSubKategori().getNama() : null;

            if (!aspekNama.equals(curAspek)) {
                curAspek = aspekNama;
                curSub = "__";
                Row ar = sheet.createRow(r++);
                setC(ar, cNo, aspekUrut != null ? String.valueOf(aspekUrut) : "", bold);
                setC(ar, cJenis, aspekNama, bold);
            }
            if (subNama != null && !subNama.equals(curSub)) {
                curSub = subNama;
                Row sr = sheet.createRow(r++);
                setC(sr, cJenis, "   " + subNama, bold);
            }

            Row row = sheet.createRow(r++);
            setC(row, cNo, i.getNomorUrut(), cell);
            setC(row, cUraian, i.getUraian(), cell);
            setC(row, cInd, i.getSpmIndikator(), cell);
            setC(row, cNilai, i.getSpmNilai(), cell);
            setN(row, cTarget, i.getTargetCapaian(), cellNum);
            setN(row, cBobot, i.getBobot(), cellNum);
            if (i.getBobot() != null) bobotSheet = bobotSheet.add(i.getBobot());

            Map<Long, PenilaianDetail> perUnit = data.byIndikator.getOrDefault(i.getId(), Map.of());
            BigDecimal sum = BigDecimal.ZERO;
            int cnt = 0, col = firstUnit;
            for (Long uid : unitIds) {
                PenilaianDetail d = perUnit.get(uid);
                if (d != null && d.getNilaiCapaian() != null) {
                    setN(row, col, d.getNilaiCapaian(), cellNum);
                    sum = sum.add(d.getNilaiCapaian());
                    cnt++;
                } else {
                    setC(row, col, "", cellNum);
                }
                col++;
            }
            BigDecimal rata = cnt > 0 ? sum.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            setN(row, rataCap, rata, cellNum);
            setN(row, rataBob, i.getBobot() != null
                    ? rata.multiply(i.getBobot()).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO, cellNum);
        }

        // ---- TOTAL PENCAPAIAN SPM ----
        BigDecimal maxScore = bobotSheet.multiply(BigDecimal.valueOf(100));
        Row totalRow = sheet.createRow(r++);
        setC(totalRow, cJenis, "TOTAL PENCAPAIAN SPM", bold);
        int col = firstUnit;
        for (Long uid : unitIds) setN(totalRow, col++, data.totalPerUnit.getOrDefault(uid, BigDecimal.ZERO), cellNum);

        // ---- KETIDAKTERCAPAIAN SPM (maksimum sheet − total) ----
        Row gapRow = sheet.createRow(r++);
        setC(gapRow, cJenis, "KETIDAKTERCAPAIAN SPM", bold);
        col = firstUnit;
        for (Long uid : unitIds) {
            BigDecimal tot = data.totalPerUnit.getOrDefault(uid, BigDecimal.ZERO);
            setN(gapRow, col++, maxScore.subtract(tot).setScale(4, RoundingMode.HALF_UP), cellNum);
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

        sheet.setColumnWidth(cJenis, 6000);
        sheet.setColumnWidth(cUraian, 16000);
        sheet.setColumnWidth(cInd, 10000);
        sheet.setColumnWidth(cNilai, 10000);
    }

    /** Export SEMUA penilaian: sheet "Rekap" + sheet "Detail". */
    @Transactional(readOnly = true)
    public byte[] exportAll() {
        List<PenilaianSpm> penilaians = cbf.create(em, PenilaianSpm.class, "p")
                .fetch("koridor", "maker", "checker", "approver")
                .orderByAsc("tanggal")
                .orderByDesc("id")
                .getResultList();

        List<PenilaianDetail> details = cbf.create(em, PenilaianDetail.class, "d")
                .fetch("penilaian", "penilaian.koridor", "bus", "halte",
                        "indikator", "indikator.aspek", "indikator.subKategori")
                .orderByAsc("penilaian.id")
                .orderByAsc("indikator.id")
                .getResultList();

        Map<Long, BigDecimal> totalPerPenilaian = new LinkedHashMap<>();
        for (PenilaianDetail d : details) {
            totalPerPenilaian.merge(d.getPenilaian().getId(),
                    d.getSkorTerbobot() != null ? d.getSkorTerbobot() : BigDecimal.ZERO, BigDecimal::add);
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font fb = wb.createFont(); fb.setBold(true); bold.setFont(fb);

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

            Sheet det = wb.createSheet("Detail");
            String[] dh = {"ID Penilaian", "Tanggal", "Koridor", "Unit", "Aspek", "Sub Kategori",
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
                setC(row, 3, unitLabel(d), null);
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

    private String unitLabel(PenilaianDetail d) {
        if (d.getBus() != null) {
            return "Bus " + d.getBus().getNoLambung();
        }
        if (d.getHalte() != null) {
            return "Halte " + d.getHalte().getNomor();
        }
        return "";
    }

    private int titleWide(Sheet sheet, int r, String text, CellStyle style, int lastCol) {
        Row row = sheet.createRow(r);
        setC(row, 0, text, style);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, Math.max(5, lastCol)));
        return r + 1;
    }

    private void mergeV(Sheet sheet, int r1, int r2, int col) {
        sheet.addMergedRegion(new CellRangeAddress(r1, r2, col, col));
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
    private String name(User u) { return u == null ? "" : u.getNama(); }
    private String jab(User u) { return u == null ? "" : u.getJabatan(); }

    /** Holder data satu sheet (unit + matriks nilai). */
    private static class SheetData {
        final Map<Long, String> units = new LinkedHashMap<>();
        final Map<Long, Map<Long, PenilaianDetail>> byIndikator = new LinkedHashMap<>();
        final Map<Long, BigDecimal> totalPerUnit = new LinkedHashMap<>();
    }
}
