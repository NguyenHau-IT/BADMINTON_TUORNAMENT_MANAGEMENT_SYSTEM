package com.example.btms.util.report;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.btms.config.Prefs;
import com.example.btms.model.category.NoiDung;
import com.example.btms.model.club.CauLacBo;
import com.example.btms.model.player.DangKiCaNhan;
import com.example.btms.model.player.VanDongVien;
import com.example.btms.model.team.ChiTietDoi;
import com.example.btms.model.team.DangKiDoi;
import com.example.btms.repository.category.NoiDungRepository;
import com.example.btms.repository.club.CauLacBoRepository;
import com.example.btms.repository.player.DangKiCaNhanRepository;
import com.example.btms.repository.player.VanDongVienRepository;
import com.example.btms.repository.team.ChiTietDoiRepository;
import com.example.btms.repository.team.DangKiDoiRepository;
import com.example.btms.service.category.NoiDungService;
import com.example.btms.service.club.CauLacBoService;
import com.example.btms.service.player.DangKiCaNhanService;
import com.example.btms.service.player.VanDongVienService;
import com.example.btms.service.team.ChiTietDoiService;
import com.example.btms.service.team.DangKiDoiService;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Export danh sách đăng ký ĐƠN + ĐÔI ra PDF: tất cả, theo CLB, theo nội dung.
 * (Giữ tên lớp lịch sử, nhưng giờ bao gồm cả ĐƠN.)
 */
public final class RegistrationPdfExporter {
    private static final Integer NO_CLUB = -1;

    private RegistrationPdfExporter() {
    }

    public static void exportAll(Connection conn, int idGiai, File out, String giaiName) throws Exception {
        Data d = loadData(conn, idGiai);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            Document doc = new Document(PageSize.A4, 36f, 36f, 120f, 36f);
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            writer.setPageEvent(new HeaderEvent(giaiName, "DANH SÁCH ĐĂNG KÝ"));
            doc.open();
            PdfPTable table = buildTable(d.fontHeader, true);
            List<Row> rows = new ArrayList<>();
            for (NoiDung nd : d.noiDungs) {
                // ĐÔI: mỗi thành viên 1 hàng
                for (DangKiDoi t : d.teamsByNoiDung.getOrDefault(nd.getId(), List.of())) {
                    List<ChiTietDoi> members = d.chiTietSvc.listMembers(t.getIdTeam());
                    members.sort(java.util.Comparator.comparing(ChiTietDoi::getIdVdv));
                    for (ChiTietDoi m : members) {
                        VanDongVien v = d.vdvById.get(m.getIdVdv());
                        if (v != null) {
                            Row r = new Row();
                            r.noiDung = safe(nd.getTenNoiDung());
                            r.tenvdv = safe(v.getHoTen());
                            r.ngaySinh = v.getNgaySinh();
                            r.clbId = v.getIdClb();
                            r.clbName = Optional.ofNullable(d.clbById.get(r.clbId))
                                    .map(CauLacBo::getTenClb).orElse("");
                            r.tenTeam = safe(t.getTenTeam());
                            rows.add(r);
                        }
                    }
                }
                // ĐƠN: mỗi đăng ký là 1 hàng
                for (DangKiCaNhan r1 : d.singlesByNoiDung.getOrDefault(nd.getId(), List.of())) {
                    VanDongVien v = d.vdvById.get(r1.getIdVdv());
                    if (v != null) {
                        Row r = new Row();
                        r.noiDung = safe(nd.getTenNoiDung());
                        r.tenvdv = safe(v.getHoTen());
                        r.ngaySinh = v.getNgaySinh();
                        r.clbId = v.getIdClb();
                        r.clbName = Optional.ofNullable(d.clbById.get(r.clbId))
                                .map(CauLacBo::getTenClb).orElse("");
                        rows.add(r);
                    }
                }
            }
            // Sắp xếp theo ngày sinh từ mới tới cũ
            rows.sort((a, b) -> {
                if (a.ngaySinh == null && b.ngaySinh == null)
                    return 0;
                if (a.ngaySinh == null)
                    return 1;
                if (b.ngaySinh == null)
                    return -1;
                return b.ngaySinh.compareTo(a.ngaySinh);
            });
            fillRows(table, rows, d.fontNormal, true);
            doc.add(table);
            doc.close();
        }
    }

    public static void exportByClub(Connection conn, int idGiai, File out, String giaiName) throws Exception {
        Data d = loadData(conn, idGiai);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            Document doc = new Document(PageSize.A4, 36f, 36f, 120f, 36f); // giấy dọc (portrait) + top margin for
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            writer.setPageEvent(new HeaderEvent(giaiName, "DANH SÁCH ĐĂNG KÝ THEO CLB"));
            doc.open();
            // Group by CLB, each row is a VĐV in an event (even if same VĐV in 2 events, 2
            // rows)
            Map<Integer, List<Row>> byClub = new LinkedHashMap<>();
            for (NoiDung nd : d.noiDungs) {
                // ĐÔI: mỗi thành viên 1 hàng
                for (DangKiDoi t : d.teamsByNoiDung.getOrDefault(nd.getId(), List.of())) {
                    List<ChiTietDoi> members = d.chiTietSvc.listMembers(t.getIdTeam());
                    members.sort(java.util.Comparator.comparing(ChiTietDoi::getIdVdv));
                    for (ChiTietDoi m : members) {
                        VanDongVien v = d.vdvById.get(m.getIdVdv());
                        if (v != null) {
                            Row r = new Row();
                            r.noiDung = safe(nd.getTenNoiDung());
                            r.tenvdv = safe(v.getHoTen());
                            r.ngaySinh = v.getNgaySinh();
                            r.clbId = v.getIdClb();
                            r.clbName = Optional.ofNullable(d.clbById.get(r.clbId))
                                    .map(CauLacBo::getTenClb).orElse("");
                            r.tenTeam = safe(t.getTenTeam());
                            final Integer key = Objects.requireNonNullElse(r.clbId, NO_CLUB);
                            byClub.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
                        }
                    }
                }
                // ĐƠN: mỗi đăng ký là 1 hàng
                for (DangKiCaNhan r1 : d.singlesByNoiDung.getOrDefault(nd.getId(), List.of())) {
                    VanDongVien v = d.vdvById.get(r1.getIdVdv());
                    if (v != null) {
                        Row r = new Row();
                        r.noiDung = safe(nd.getTenNoiDung());
                        r.tenvdv = safe(v.getHoTen());
                        r.ngaySinh = v.getNgaySinh();
                        r.clbId = v.getIdClb();
                        r.clbName = Optional.ofNullable(d.clbById.get(r.clbId))
                                .map(CauLacBo::getTenClb).orElse("");
                        r.tenTeam = ""; // Đơn: không có danh sách thành viên
                        final Integer key = Objects.requireNonNullElse(r.clbId, NO_CLUB);
                        byClub.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
                    }
                }
            }
            List<Map.Entry<Integer, List<Row>>> clubEntries = new ArrayList<>(byClub.entrySet());
            for (int idx = 0; idx < clubEntries.size(); idx++) {
                Map.Entry<Integer, List<Row>> e = clubEntries.get(idx);
                if (idx > 0) {
                    doc.newPage();
                }
                boolean noClub = NO_CLUB.equals(e.getKey());
                String clbName = noClub ? "(Không có CLB)"
                        : Optional.ofNullable(d.clbById.get(e.getKey())).map(CauLacBo::getTenClb).orElse("(N/A)");
                Font sectionFont = new Font(d.fontSection);
                sectionFont.setSize(16f);
                Paragraph sec = new Paragraph(clbName, sectionFont);
                // cho sec nằm giữa
                sec.setAlignment(Element.ALIGN_CENTER);
                sec.setSpacingAfter(3f);
                doc.add(sec);
                PdfPTable table = buildTable(d.fontHeader, false);
                fillRows(table, e.getValue(), d.fontNormal, false);
                doc.add(table);
            }
            doc.close();
        }
    }

    public static void exportByNoiDung(Connection conn, int idGiai, File out, String giaiName) throws Exception {
        Data d = loadData(conn, idGiai);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            Document doc = new Document(PageSize.A4, 36f, 36f, 120f, 36f);
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            writer.setPageEvent(new HeaderEvent(giaiName, "DANH SÁCH ĐĂNG KÝ THEO NỘI DUNG"));
            doc.open();
            boolean isFirstPage = true;
            for (int idx = 0; idx < d.noiDungs.size(); idx++) {
                NoiDung nd = d.noiDungs.get(idx);
                List<Row> rows = new ArrayList<>();
                // ĐÔI: mỗi thành viên 1 hàng
                for (DangKiDoi t : d.teamsByNoiDung.getOrDefault(nd.getId(), List.of())) {
                    List<ChiTietDoi> members = d.chiTietSvc.listMembers(t.getIdTeam());
                    members.sort(java.util.Comparator.comparing(ChiTietDoi::getIdVdv));
                    for (ChiTietDoi m : members) {
                        VanDongVien v = d.vdvById.get(m.getIdVdv());
                        if (v != null) {
                            Row r = new Row();
                            r.noiDung = safe(nd.getTenNoiDung());
                            r.tenvdv = safe(v.getHoTen());
                            r.ngaySinh = v.getNgaySinh();
                            r.clbId = v.getIdClb();
                            r.clbName = Optional.ofNullable(d.clbById.get(r.clbId))
                                    .map(CauLacBo::getTenClb).orElse("");
                            r.tenTeam = safe(t.getTenTeam());
                            rows.add(r);
                        }
                    }
                }
                // ĐƠN: mỗi đăng ký là 1 hàng
                for (DangKiCaNhan r1 : d.singlesByNoiDung.getOrDefault(nd.getId(), List.of())) {
                    VanDongVien v = d.vdvById.get(r1.getIdVdv());
                    if (v != null) {
                        Row r = new Row();
                        r.noiDung = safe(nd.getTenNoiDung());
                        r.tenvdv = safe(v.getHoTen());
                        r.ngaySinh = v.getNgaySinh();
                        r.clbId = v.getIdClb();
                        r.clbName = Optional.ofNullable(d.clbById.get(r.clbId))
                                .map(CauLacBo::getTenClb).orElse("");
                        r.tenTeam = "";
                        rows.add(r);
                    }
                }
                // Sắp xếp theo ngày sinh từ mới tới cũ
                rows.sort((a, b) -> {
                    if (a.ngaySinh == null && b.ngaySinh == null)
                        return 0;
                    if (a.ngaySinh == null)
                        return 1;
                    if (b.ngaySinh == null)
                        return -1;
                    return b.ngaySinh.compareTo(a.ngaySinh);
                });
                // Skip nội dung không có VĐV nào
                if (rows.isEmpty()) {
                    continue;
                }
                if (!isFirstPage) {
                    doc.newPage();
                }
                isFirstPage = false;
                Paragraph sec = new Paragraph("• " + safe(nd.getTenNoiDung()) + " (" + rows.size() + ")",
                        d.fontSection);
                sec.setSpacingBefore(8f);
                sec.setSpacingAfter(3f);
                doc.add(sec);
                PdfPTable table = buildTable(d.fontHeader, true);
                fillRows(table, rows, d.fontNormal, true);
                doc.add(table);
            }
            doc.close();
        }
    }

    /* -------------------- Internals -------------------- */

    private static class Data {
        final List<NoiDung> noiDungs; // gồm cả nội dung ĐƠN và ĐÔI
        final Map<Integer, List<DangKiDoi>> teamsByNoiDung; // ĐÔI
        final Map<Integer, List<DangKiCaNhan>> singlesByNoiDung; // ĐƠN
        final Map<Integer, CauLacBo> clbById;
        final Map<Integer, VanDongVien> vdvById;
        final ChiTietDoiService chiTietSvc;
        final Font fontHeader, fontNormal, fontSection;

        Data(List<NoiDung> noiDungs,
                Map<Integer, List<DangKiDoi>> teamsByNoiDung,
                Map<Integer, List<DangKiCaNhan>> singlesByNoiDung,
                Map<Integer, CauLacBo> clbById, Map<Integer, VanDongVien> vdvById,
                ChiTietDoiService chiTietSvc,
                Font fontHeader, Font fontNormal, Font fontSection) {
            this.noiDungs = noiDungs;
            this.teamsByNoiDung = teamsByNoiDung;
            this.singlesByNoiDung = singlesByNoiDung;
            this.clbById = clbById;
            this.vdvById = vdvById;
            this.chiTietSvc = chiTietSvc;
            this.fontHeader = fontHeader;
            this.fontNormal = fontNormal;
            this.fontSection = fontSection;
        }
    }

    private static class Row {
        Integer clbId;
        String clbName;
        LocalDate ngaySinh;
        String noiDung;
        String tenvdv;
        String tenTeam; // "A & B"
    }

    private static Data loadData(Connection conn, int idGiai) throws Exception {
        // Load registered contents: both SINGLES and DOUBLES
        NoiDungService ndSvc = new NoiDungService(new NoiDungRepository(conn));
        List<NoiDung> ndAll = ndSvc.getAllNoiDung();
        Map<String, Integer>[] maps = new NoiDungRepository(conn).loadCategories();
        java.util.Set<Integer> singleIds = new java.util.LinkedHashSet<>(maps[0].values());
        java.util.Set<Integer> doubleIds = new java.util.LinkedHashSet<>(maps[1].values());
        List<NoiDung> singles = ndAll.stream()
                .filter(nd -> nd.getId() != null && singleIds.contains(nd.getId()))
                .collect(Collectors.toList());
        List<NoiDung> doubles = ndAll.stream()
                .filter(nd -> nd.getId() != null && doubleIds.contains(nd.getId()))
                .collect(Collectors.toList());
        // Union (preserve order by id)
        Map<Integer, NoiDung> union = new LinkedHashMap<>();
        java.util.function.Consumer<NoiDung> addNd = nd -> {
            if (nd != null && nd.getId() != null)
                union.putIfAbsent(nd.getId(), nd);
        };
        singles.stream().sorted(java.util.Comparator.comparing(NoiDung::getId)).forEach(addNd);
        doubles.stream().sorted(java.util.Comparator.comparing(NoiDung::getId)).forEach(addNd);
        List<NoiDung> allRelevant = new ArrayList<>(union.values());

        DangKiDoiRepository doiRepo = new DangKiDoiRepository(conn);
        DangKiDoiService doiSvc = new DangKiDoiService(doiRepo);
        ChiTietDoiService chiTietSvc = new ChiTietDoiService(conn, doiRepo, new ChiTietDoiRepository(conn));

        DangKiCaNhanService dkcnSvc = new DangKiCaNhanService(new DangKiCaNhanRepository(conn));

        CauLacBoService clbSvc = new CauLacBoService(new CauLacBoRepository(conn));
        Map<Integer, CauLacBo> clbById = new LinkedHashMap<>();
        for (CauLacBo c : clbSvc.findAll()) {
            if (c.getId() != null)
                clbById.put(c.getId(), c);
        }
        VanDongVienService vdvSvc = new VanDongVienService(new VanDongVienRepository(conn));
        Map<Integer, VanDongVien> vdvById = new LinkedHashMap<>();
        for (VanDongVien v : vdvSvc.findAll()) {
            if (v.getId() != null)
                vdvById.put(v.getId(), v);
        }

        Map<Integer, List<DangKiDoi>> teamsByNd = new LinkedHashMap<>();
        for (NoiDung nd : doubles) {
            List<DangKiDoi> teams = doiSvc.listTeams(idGiai, nd.getId());
            teamsByNd.put(nd.getId(), teams);
        }

        Map<Integer, List<DangKiCaNhan>> singlesByNd = new LinkedHashMap<>();
        for (NoiDung nd : singles) {
            List<DangKiCaNhan> regs = dkcnSvc.listByGiaiAndNoiDung(idGiai, nd.getId(), null);
            singlesByNd.put(nd.getId(), regs);
        }

        // Fonts (attempt to support Unicode via registered system fonts)
        Font fontHeader, fontNormal, fontSection;
        try {
            com.lowagie.text.FontFactory.registerDirectories();
            Font base = com.lowagie.text.FontFactory.getFont("Times New Roman", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    12);
            if (base == null || base.getBaseFont() == null) {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                fontNormal = new Font(bf, 12);
            } else {
                fontNormal = base;
            }
            fontHeader = new Font(fontNormal);
            fontHeader.setStyle(Font.BOLD);
            fontSection = new Font(fontNormal);
            fontSection.setStyle(Font.BOLD);
        } catch (DocumentException | IOException ex) {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            fontNormal = new Font(bf, 12);
            fontHeader = new Font(fontNormal);
            fontHeader.setStyle(Font.BOLD);
            fontSection = new Font(fontNormal);
            fontSection.setStyle(Font.BOLD);
        }

        return new Data(allRelevant, teamsByNd, singlesByNd, clbById, vdvById, chiTietSvc,
                fontHeader, fontNormal, fontSection);
    }

    /** Page event to draw header on every page. */
    private static class HeaderEvent extends PdfPageEventHelper {
        private final String tournamentName;
        private final String exportTitle;
        private final Font tournamentFont;
        private final Font exportFont;
        private Image leftLogo; // report.logo.path
        private Image rightLogo; // report.sponsor.logo.path

        HeaderEvent(String giaiName, String title) {
            this.tournamentName = safe(giaiName);
            this.exportTitle = safe(title);
            this.tournamentFont = docFont(15f, Font.BOLD); // Giảm từ 18pt xuống 15pt
            this.exportFont = docFont(10f, Font.BOLD); // Giảm từ 12pt xuống 10pt
            try {
                Prefs prefs = new Prefs();
                String leftPath = prefs.get("report.logo.path", "");
                if (leftPath != null && !leftPath.isBlank()) {
                    this.leftLogo = Image.getInstance(leftPath);
                }
            } catch (BadElementException | IOException ignore) {
                this.leftLogo = null;
            }
            try {
                Prefs prefs = new Prefs();
                String rightPath = prefs.get("report.sponsor.logo.path", "");
                if (rightPath != null && !rightPath.isBlank()) {
                    this.rightLogo = Image.getInstance(rightPath);
                }
            } catch (BadElementException | IOException ignore) {
                this.rightLogo = null;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            float pageWidth = page.getWidth();
            float leftMargin = document.leftMargin();
            float rightMargin = document.rightMargin();
            float topY = page.getHeight() - 5f; // 12pt from top edge
            float maxLogoWLeft = (pageWidth - leftMargin - rightMargin) * 1f; // Logo trái to hơn
            float maxLogoWRight = (pageWidth - leftMargin - rightMargin) * 3f; // Logo phải bình thường
            float maxLogoH = 70f;

            if (leftLogo != null) {
                try {
                    Image img = Image.getInstance(leftLogo);
                    img.scaleToFit(maxLogoWLeft, maxLogoH);
                    float x = leftMargin;
                    float y = topY - img.getScaledHeight();
                    img.setAbsolutePosition(x, y);
                    writer.getDirectContent().addImage(img);
                } catch (DocumentException ignore) {
                }
            }

            // Right logo (sponsor) - top right
            if (rightLogo != null) {
                try {
                    Image img = Image.getInstance(rightLogo);
                    img.scaleToFit(maxLogoWRight, maxLogoH);
                    float x = pageWidth - rightMargin - img.getScaledWidth();
                    float y = topY - img.getScaledHeight();
                    img.setAbsolutePosition(x, y);
                    writer.getDirectContent().addImage(img);
                } catch (DocumentException ignore) {
                }
            }

            // Tournament name centered (middle of page, below logos)
            float textStartY = topY - 90f; // Space after logos
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(this.tournamentName, this.tournamentFont), pageWidth / 2f, textStartY, 0);
            // Export title centered (below tournament name)
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(this.exportTitle, this.exportFont), pageWidth / 2f, textStartY - 18f, 0);

            // Page number at footer (bottom center)
            int pageNum = writer.getPageNumber();
            String pageText = "Trang " + pageNum;
            Font pageFont = docFont(10f, Font.NORMAL);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(pageText, pageFont), pageWidth - rightMargin, document.bottomMargin() - 10f, 0);
        }
    }

    private static Font docFont(float size, int style) {
        try {
            com.lowagie.text.FontFactory.registerDirectories();
            Font f = com.lowagie.text.FontFactory.getFont("Times New Roman", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    size, style);
            if (f != null && f.getBaseFont() != null)
                return f;
        } catch (DocumentException ignore) {
        }
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style);
        } catch (DocumentException | IOException e) {
            return new Font(Font.HELVETICA, size, style);
        }
    }

    private static PdfPTable buildTable(Font fontHeader, boolean clbNameColumn) {
        // Điều chỉnh tỉ lệ cột cho giấy dọc: tổng = 100
        if (clbNameColumn) {
            float[] widths = { 5f, 25f, 20f, 15f, 20f, 15f }; // STT, Nội dung, CLB, Tên đội, Thành viên
            PdfPTable t = new PdfPTable(widths);
            t.setWidthPercentage(100);
            t.setHeaderRows(1);
            addHeaderCell(t, "STT", fontHeader);
            addHeaderCell(t, "Nội dung", fontHeader);
            addHeaderCell(t, "Câu lạc bộ", fontHeader);
            addHeaderCell(t, "Ngày sinh", fontHeader);
            addHeaderCell(t, "Vận động viên", fontHeader);
            addHeaderCell(t, "Tên đội", fontHeader);
            return t;
        } else {
            float[] widths = { 5f, 35f, 18f, 22f, 20f }; // STT, Nội dung, CLB, Tên đội, Thành viên
            PdfPTable t = new PdfPTable(widths);
            t.setWidthPercentage(100);
            t.setHeaderRows(1);
            addHeaderCell(t, "STT", fontHeader);
            addHeaderCell(t, "Nội dung", fontHeader);
            addHeaderCell(t, "Ngày sinh", fontHeader);
            addHeaderCell(t, "Vận động viên", fontHeader);
            addHeaderCell(t, "Tên đội", fontHeader);
            return t;
        }
    }

    private static void addHeaderCell(PdfPTable t, String txt, Font fontHeader) {
        PdfPCell c = new PdfPCell(new Phrase(safe(txt), fontHeader));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBackgroundColor(new Color(33, 150, 243)); // Blue 500
        t.addCell(c);
    }

    private static void fillRows(PdfPTable t, List<Row> rows, Font fontNormal, boolean clbNameColumn) {
        if (clbNameColumn) {
            int i = 1;
            for (Row r : rows) {
                addCell(t, String.valueOf(i++), fontNormal, Element.ALIGN_CENTER);
                addCell(t, r.noiDung, fontNormal, Element.ALIGN_LEFT);
                addCell(t, r.clbName, fontNormal, Element.ALIGN_LEFT);
                addCell(t, r.ngaySinh.toString(), fontNormal, Element.ALIGN_CENTER);
                addCell(t, r.tenvdv, fontNormal, Element.ALIGN_LEFT);
                addCell(t, r.tenTeam, fontNormal, Element.ALIGN_LEFT);
            }
        } else {
            int i = 1;
            for (Row r : rows) {
                addCell(t, String.valueOf(i++), fontNormal, Element.ALIGN_CENTER);
                addCell(t, r.noiDung, fontNormal, Element.ALIGN_LEFT);
                addCell(t, r.ngaySinh.toString(), fontNormal, Element.ALIGN_CENTER);
                addCell(t, r.tenvdv, fontNormal, Element.ALIGN_LEFT);
                addCell(t, r.tenTeam, fontNormal, Element.ALIGN_LEFT);
            }
        }
    }

    private static void addCell(PdfPTable t, String txt, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(safe(txt), f));
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private static Row toRow(Data d, NoiDung nd, DangKiDoi t) {
        Row r = new Row();
        r.noiDung = safe(nd.getTenNoiDung());
        r.tenTeam = safe(t.getTenTeam());
        try {
            List<ChiTietDoi> members = d.chiTietSvc.listMembers(t.getIdTeam());
            members.sort(java.util.Comparator.comparing(ChiTietDoi::getIdVdv));
            List<String> names = new ArrayList<>();
            for (ChiTietDoi m : members) {
                VanDongVien v = d.vdvById.get(m.getIdVdv());
                if (v != null)
                    names.add(safe(v.getHoTen()));
            }
            r.tenvdv = names.isEmpty() ? "" : String.join(" & ", names);
            r.ngaySinh = members.isEmpty() ? null : d.vdvById.get(members.get(0).getIdVdv()).getNgaySinh();
        } catch (RuntimeException ex) {
            r.tenTeam = "";
            r.ngaySinh = null;
        }
        return r;
    }

    /** Hàng cho ĐƠN: dùng tên VĐV ở cột "Tên đội", để giữ cấu trúc bảng sẵn có. */
    private static Row toRowSingle(Data d, NoiDung nd, DangKiCaNhan reg) {
        Row r = new Row();
        r.noiDung = safe(nd.getTenNoiDung());
        VanDongVien v = d.vdvById.get(reg.getIdVdv());
        if (v != null) {
            r.clbId = v.getIdClb();
            r.tenvdv = safe(v.getHoTen());
            r.ngaySinh = v.getNgaySinh();
        } else {
            r.tenvdv = "";
            r.ngaySinh = null;
        }
        r.tenTeam = ""; // Đơn: không có danh sách thành viên
        return r;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
