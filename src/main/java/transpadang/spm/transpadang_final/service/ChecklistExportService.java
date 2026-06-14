package transpadang.spm.transpadang_final.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transpadang.spm.transpadang_final.entity.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChecklistExportService {

    private final JPAQueryFactory queryFactory;

    public ChecklistExportService(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Transactional(readOnly = true)
    public byte[] exportAll() {
        var qt = new QChecklistTemplate("t");
        List<ChecklistTemplate> templates = queryFactory.selectFrom(qt).orderBy(qt.id.asc()).fetch();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles st = new Styles(wb);
            for (ChecklistTemplate t : templates) {
                Sheet sheet = wb.createSheet(t.getKode());
                writeTemplateSheet(sheet, t, st);
                sheet.setColumnWidth(1, 18000); // Uraian
            }
            if (templates.isEmpty()) {
                wb.createSheet("Kosong").createRow(0).createCell(0)
                        .setCellValue("Belum ada master checklist.");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat Excel: " + e.getMessage(), e);
        }
    }

    private void writeTemplateSheet(Sheet sheet, ChecklistTemplate t, Styles st) {
        var qi = new QChecklistItem("i");
        List<ChecklistItem> items = queryFactory.selectFrom(qi)
                .leftJoin(qi.parent).fetchJoin()
                .where(qi.template.id.eq(t.getId()))
                .orderBy(qi.urutan.asc(), qi.id.asc())
                .fetch();

        var qh = new QChecklistHarian("h");
        List<ChecklistHarian> sessions = queryFactory.selectFrom(qh)
                .leftJoin(qh.koridor).fetchJoin()
                .leftJoin(qh.dibuatOleh).fetchJoin()
                .leftJoin(qh.diketahuiOleh).fetchJoin()
                .where(qh.template.id.eq(t.getId()))
                .orderBy(qh.tanggal.desc(), qh.id.desc())
                .fetch();

        int r = 0;
        if (sessions.isEmpty()) {
            setC(sheet.createRow(r), 0, t.getNama() + " — belum ada data.", st.bold);
            return;
        }
        for (ChecklistHarian h : sessions) {
            r = (t.getSubjek() == SubjekChecklist.BUS)
                    ? writeBusBlock(sheet, r, t, items, h, st)
                    : writeSingleBlock(sheet, r, t, items, h, st);
            r += 2; // jarak antar sesi
        }
    }

    // ---- Form objek Bus: bus jadi kolom ----
    private int writeBusBlock(Sheet sheet, int r, ChecklistTemplate t, List<ChecklistItem> items,
                              ChecklistHarian h, Styles st) {
        boolean denda = Boolean.TRUE.equals(t.getPakaiDenda());

        Long koridorId = h.getKoridor() != null ? h.getKoridor().getId() : null;
        var qb = new QBus("b");
        List<Bus> buses = queryFactory.selectFrom(qb)
                .where(qb.koridor.id.eq(koridorId).and(qb.aktif.isTrue()))
                .orderBy(qb.noLambung.asc(), qb.id.asc()).fetch();

        var qd = new QChecklistDetail("d");
        List<ChecklistDetail> details = queryFactory.selectFrom(qd)
                .leftJoin(qd.bus).fetchJoin()
                .leftJoin(qd.item).fetchJoin()
                .where(qd.checklistHarian.id.eq(h.getId())).fetch();
        Map<String, Boolean> ans = new HashMap<>();
        for (ChecklistDetail d : details) {
            if (d.getBus() != null && d.getItem() != null) {
                ans.put(d.getBus().getId() + ":" + d.getItem().getId(), d.getHasil());
            }
        }

        final int cNo = 0, cUraian = 1, firstBus = 2;
        int n = buses.size();
        int cSanksi = firstBus + n;
        int lastCol = denda ? cSanksi : Math.max(firstBus, firstBus + n - 1);

        r = title(sheet, r, t.getNama(), st.bold, lastCol);
        Row idr = sheet.createRow(r++);
        setC(idr, 0, "Koridor: " + koridorNama(h) + "      Hari: " + nz(h.getHari())
                + "      Tanggal: " + tgl(h) + "      Status: " + status(h), null);
        sheet.addMergedRegion(new CellRangeAddress(idr.getRowNum(), idr.getRowNum(), 0, lastCol));

        Row hr = sheet.createRow(r++);
        setC(hr, cNo, "No", st.hdr);
        setC(hr, cUraian, "Uraian", st.hdr);
        int c = firstBus;
        for (Bus b : buses) setC(hr, c++, "Lambung " + b.getNoLambung(), st.hdr);
        if (denda) setC(hr, cSanksi, "Sanksi / Denda", st.hdr);

        for (ChecklistItem it : items) {
            Row row = sheet.createRow(r++);
            setC(row, cNo, it.getNomorUrut(), st.cell);
            setC(row, cUraian, it.getUraian(), st.cell);
            c = firstBus;
            for (Bus b : buses) {
                setC(row, c++, label(t.getTipeJawaban(), ans.get(b.getId() + ":" + it.getId())), st.cellCenter);
            }
            if (denda) setC(row, cSanksi, nz(it.getSanksiDenda()), st.cell);
        }

        if (denda) {
            Row tr = sheet.createRow(r++);
            setC(tr, cUraian, "TOTAL DENDA", st.bold);
            c = firstBus;
            for (Bus b : buses) {
                BigDecimal sum = BigDecimal.ZERO;
                for (ChecklistItem it : items) {
                    Boolean hv = ans.get(b.getId() + ":" + it.getId());
                    if (Boolean.FALSE.equals(hv) && it.getNilaiDenda() != null) sum = sum.add(it.getNilaiDenda());
                }
                setN(tr, c++, sum, st.cellNum);
            }
        }

        return signature(sheet, r, t, h, cUraian);
    }

    // ---- Form Pramugara / Korlap: satu kolom hasil + keterangan ----
    private int writeSingleBlock(Sheet sheet, int r, ChecklistTemplate t, List<ChecklistItem> items,
                                 ChecklistHarian h, Styles st) {
        var qd = new QChecklistDetail("d");
        List<ChecklistDetail> details = queryFactory.selectFrom(qd)
                .leftJoin(qd.item).fetchJoin()
                .where(qd.checklistHarian.id.eq(h.getId())).fetch();
        Map<Long, ChecklistDetail> byItem = new HashMap<>();
        for (ChecklistDetail d : details) if (d.getItem() != null) byItem.put(d.getItem().getId(), d);

        Set<Long> parentIds = new HashSet<>();
        for (ChecklistItem it : items) if (it.getParent() != null) parentIds.add(it.getParent().getId());

        final int cNo = 0, cUraian = 1, cHasil = 2, cKet = 3, lastCol = 3;

        r = title(sheet, r, t.getNama(), st.bold, lastCol);
        String ident = "Koridor: " + koridorNama(h) + "      Hari: " + nz(h.getHari()) + "      Tanggal: " + tgl(h);
        if (t.getSubjek() == SubjekChecklist.PRAMUGARA) {
            ident += "      Pramugara: " + nz(h.getNamaPramugara()) + "      Shift: " + nz(h.getShift());
        }
        Row idr = sheet.createRow(r++);
        setC(idr, 0, ident, null);
        sheet.addMergedRegion(new CellRangeAddress(idr.getRowNum(), idr.getRowNum(), 0, lastCol));

        Row hr = sheet.createRow(r++);
        setC(hr, cNo, "No", st.hdr);
        setC(hr, cUraian, "Uraian", st.hdr);
        setC(hr, cHasil, "Hasil", st.hdr);
        setC(hr, cKet, "Keterangan", st.hdr);

        String lastGrup = "__";
        for (ChecklistItem it : items) {
            if (parentIds.contains(it.getId())) { // item induk = heading
                Row row = sheet.createRow(r++);
                setC(row, cNo, it.getNomorUrut(), st.bold);
                setC(row, cUraian, it.getUraian(), st.bold);
                lastGrup = "__";
                continue;
            }
            if (it.getGrup() != null && !it.getGrup().equals(lastGrup)) {
                lastGrup = it.getGrup();
                setC(sheet.createRow(r++), cUraian, "   " + lastGrup, st.bold);
            }
            ChecklistDetail d = byItem.get(it.getId());
            Row row = sheet.createRow(r++);
            setC(row, cNo, it.getNomorUrut(), st.cell);
            setC(row, cUraian, it.getUraian(), st.cell);
            setC(row, cHasil, label(t.getTipeJawaban(), d != null ? d.getHasil() : null), st.cellCenter);
            setC(row, cKet, d != null ? nz(d.getKeterangan()) : "", st.cell);
        }

        return signature(sheet, r, t, h, cUraian);
    }

    private int signature(Sheet sheet, int r, ChecklistTemplate t, ChecklistHarian h, int col) {
        r += 1;
        setC(sheet.createRow(r++), col, nz(t.getJudulTtd()) + ",", null);
        r += 2;
        User ttd = h.getDiketahuiOleh() != null ? h.getDiketahuiOleh() : h.getDibuatOleh();
        setC(sheet.createRow(r++), col, name(ttd), bold(sheet));
        setC(sheet.createRow(r++), col, jab(ttd), null);
        return r;
    }

    // ---- helper ----
    private String label(TipeJawaban tipe, Boolean h) {
        if (h == null) return "";
        if (tipe == TipeJawaban.BAIK_RUSAK) return h ? "Baik" : "Rusak";
        if (tipe == TipeJawaban.OK_NOTOK) return h ? "OK" : "Not OK";
        return h ? "Ada" : "Tidak";
    }

    private int title(Sheet sheet, int r, String text, CellStyle style, int lastCol) {
        setC(sheet.createRow(r), 0, text, style);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, Math.max(1, lastCol)));
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

    private CellStyle bold(Sheet sheet) {
        CellStyle s = sheet.getWorkbook().createCellStyle();
        Font f = sheet.getWorkbook().createFont(); f.setBold(true); s.setFont(f);
        return s;
    }

    private String koridorNama(ChecklistHarian h) { return h.getKoridor() != null ? h.getKoridor().getNama() : "-"; }
    private String tgl(ChecklistHarian h) { return h.getTanggal() != null ? h.getTanggal().toString() : "-"; }
    private String status(ChecklistHarian h) { return h.getStatus() != null ? h.getStatus().name() : "-"; }
    private String nz(String s) { return s == null ? "" : s; }
    private String name(User u) { return u == null ? "" : nz(u.getNama()); }
    private String jab(User u) { return u == null ? "" : nz(u.getJabatan()); }

    /** Kumpulan style yang dipakai berulang dalam satu workbook. */
    private static class Styles {
        final CellStyle bold, hdr, cell, cellCenter, cellNum;
        Styles(Workbook wb) {
            bold = wb.createCellStyle();
            Font fb = wb.createFont(); fb.setBold(true); bold.setFont(fb);

            hdr = wb.createCellStyle();
            hdr.cloneStyleFrom(bold);
            border(hdr);
            hdr.setAlignment(HorizontalAlignment.CENTER);
            hdr.setVerticalAlignment(VerticalAlignment.CENTER);
            hdr.setWrapText(true);

            cell = wb.createCellStyle();
            border(cell);
            cell.setVerticalAlignment(VerticalAlignment.TOP);
            cell.setWrapText(true);

            cellCenter = wb.createCellStyle();
            cellCenter.cloneStyleFrom(cell);
            cellCenter.setAlignment(HorizontalAlignment.CENTER);

            cellNum = wb.createCellStyle();
            cellNum.cloneStyleFrom(cell);
            cellNum.setAlignment(HorizontalAlignment.RIGHT);
        }
        private static void border(CellStyle s) {
            s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        }
    }
}
