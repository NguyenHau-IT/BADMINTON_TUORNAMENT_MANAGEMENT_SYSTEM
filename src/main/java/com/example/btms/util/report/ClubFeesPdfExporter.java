package com.example.btms.util.report;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.example.btms.service.fee.ClubFeesService;
import com.example.btms.util.fees.FeesCalculator.ClubFeeInfo;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Xuất báo cáo lệ phí theo câu lạc bộ ra PDF
 */
public final class ClubFeesPdfExporter {

    private ClubFeesPdfExporter() {
    }

    public static void export(File outputFile, String tournamentName,
            Map<Integer, ClubFeeInfo> clubFees, ClubFeesService clubFeesService, Integer tournamentId)
            throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            Document doc = new Document(PageSize.A4, 36f, 36f, 90f, 36f);
            PdfWriter writer = PdfWriter.getInstance(doc, fos);

            // Set up header
            writer.setPageEvent(new HeaderEvent(tournamentName));
            doc.open();

            // Thêm tiêu đề
            // Sử dụng font Arial từ hệ thống để hỗ trợ tiếng Việt
            String fontPath = "C:/Windows/Fonts/arial.ttf";
            // Nếu không tìm thấy trên Windows, thử đường dẫn khác
            if (!new java.io.File(fontPath).exists()) {
                fontPath = "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf";
            }
            if (!new java.io.File(fontPath).exists()) {
                // Fallback: sử dụng font mặc định
                fontPath = null;
            }

            BaseFont baseFont = null;
            Font titleFont, headerFont, normalFont, boldFont;

            try {
                if (fontPath != null) {
                    baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } else {
                    baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                }
            } catch (Exception e) {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
            }

            titleFont = new Font(baseFont, 16, Font.BOLD);
            headerFont = new Font(baseFont, 11, Font.BOLD);
            normalFont = new Font(baseFont, 10);
            boldFont = new Font(baseFont, 10, Font.BOLD);

            Paragraph title = new Paragraph("BÁO CÁO LỆ PHÍ THEO CÂU LẠC BỘ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph subtitle = new Paragraph("Giải: " + tournamentName, normalFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            doc.add(subtitle);

            Paragraph date = new Paragraph("Ngày xuất: " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    normalFont);
            date.setAlignment(Element.ALIGN_CENTER);
            doc.add(date);

            doc.add(new Paragraph("\n"));

            // Tạo bảng
            PdfPTable table = createTable(headerFont, normalFont, boldFont);

            int rowNum = 1;
            int grandTotal = 0;

            for (ClubFeeInfo clubInfo : clubFees.values()) {
                // Hàng thông tin CLB
                PdfPCell numCell = new PdfPCell(new Phrase(String.valueOf(rowNum), boldFont));
                numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                numCell.setPadding(10);
                numCell.setMinimumHeight(25);
                table.addCell(numCell);

                PdfPCell clubCell = new PdfPCell(new Phrase(clubInfo.getClubName(), boldFont));
                clubCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                clubCell.setPadding(10);
                clubCell.setMinimumHeight(25);
                table.addCell(clubCell);

                PdfPCell countCell = new PdfPCell(new Phrase(String.valueOf(clubInfo.getPlayerCount()), boldFont));
                countCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                countCell.setPadding(10);
                countCell.setMinimumHeight(25);
                table.addCell(countCell);

                PdfPCell feeCell = new PdfPCell(new Phrase(formatMoney(clubInfo.getTotalFee()), boldFont));
                feeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                feeCell.setPadding(10);
                feeCell.setMinimumHeight(25);
                table.addCell(feeCell);

                grandTotal += clubInfo.getTotalFee();
                rowNum++;
            }

            // Hàng tổng cộng
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TỔNG CỘNG", boldFont));
            totalLabelCell.setColspan(3);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setBackgroundColor(new java.awt.Color(220, 220, 220));
            totalLabelCell.setPadding(10);
            totalLabelCell.setMinimumHeight(25);
            table.addCell(totalLabelCell);

            PdfPCell totalFeeCell = new PdfPCell(new Phrase(formatMoney(grandTotal), boldFont));
            totalFeeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalFeeCell.setBackgroundColor(new java.awt.Color(220, 220, 220));
            totalFeeCell.setPadding(10);
            totalFeeCell.setMinimumHeight(25);
            table.addCell(totalFeeCell);

            doc.add(table);

            // Thêm chú thích
            doc.add(new Paragraph("\n"));
            Paragraph notes = new Paragraph(
                    "Chú thích: Nội dung đầu tiên: 200 000 đ/VĐV | Nội dung thứ 2 trở đi: 100 000 đ/nội dung/VĐV",
                    normalFont);
            notes.setAlignment(Element.ALIGN_LEFT);
            doc.add(notes);

            // Thêm chi tiết từng câu lạc bộ (trang 2 trở đi)
            if (clubFeesService != null && tournamentId != null) {
                addClubDetails(doc, clubFees, clubFeesService, tournamentId, titleFont, headerFont, normalFont,
                        boldFont);
            }

            doc.close();
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static PdfPTable createTable(Font headerFont, Font normalFont, Font boldFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.2f, 3f, 1.5f, 1.5f });
        table.setSpacingAfter(0);

        // Header cells
        String[] headers = { "STT", "Tên Câu Lạc Bộ", "Số VĐV", "Tổng Lệ Phí" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new java.awt.Color(100, 149, 237)); // Cornflower blue
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(12);
            cell.setMinimumHeight(30);
            table.addCell(cell);
        }

        return table;
    }

    /**
     * Format tiền tệ theo định dạng Việt Nam (với khoảng trắng làm dấu phân tách)
     */
    private static String formatMoney(long amount) {
        String formatted = String.format("%,d", amount);
        return formatted.replace(",", " ");
    }

    /**
     * Thêm chi tiết từng câu lạc bộ trên các trang tiếp theo
     */
    private static void addClubDetails(Document doc, Map<Integer, ClubFeeInfo> clubFees,
            ClubFeesService clubFeesService, Integer tournamentId,
            Font titleFont, Font headerFont, Font normalFont, Font boldFont) throws Exception {

        for (Map.Entry<Integer, ClubFeeInfo> entry : clubFees.entrySet()) {
            Integer clubId = entry.getKey();
            ClubFeeInfo clubInfo = entry.getValue();

            // Thêm page break
            doc.newPage();

            // Tiêu đề câu lạc bộ
            Paragraph clubTitle = new Paragraph("Chi tiết: " + clubInfo.getClubName(), titleFont);
            clubTitle.setAlignment(Element.ALIGN_CENTER);
            doc.add(clubTitle);

            Paragraph clubInfo2 = new Paragraph(
                    "Số VĐV: " + clubInfo.getPlayerCount() + " | Tổng lệ phí: " + formatMoney(clubInfo.getTotalFee())
                            + " đ",
                    normalFont);
            clubInfo2.setAlignment(Element.ALIGN_CENTER);
            doc.add(clubInfo2);

            doc.add(new Paragraph("\n"));

            try {
                // Lấy chi tiết các VĐV của câu lạc bộ
                List<Map<String, Object>> clubDetails = clubFeesService.getClubDetails(clubId, tournamentId);

                if (clubDetails != null && !clubDetails.isEmpty()) {
                    // Tạo bảng chi tiết - 4 cột
                    PdfPTable detailTable = new PdfPTable(4);
                    detailTable.setWidthPercentage(100);
                    detailTable.setWidths(new float[] { 1f, 2.5f, 2.5f, 1.5f });
                    detailTable.setSpacingAfter(0);

                    // Headers
                    String[] headers = { "STT", "Tên VĐV", "Nội dung", "Phí (đ)" };
                    for (String header : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                        cell.setBackgroundColor(new java.awt.Color(100, 149, 237));
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.setPadding(10);
                        cell.setMinimumHeight(25);
                        detailTable.addCell(cell);
                    }

                    // Tính phí cho mỗi VĐV (200k cho nội dung đầu, 100k cho nội dung tiếp theo)
                    Map<String, Integer> playerEventCount = new java.util.LinkedHashMap<>();
                    Map<String, Long> playerFees = new java.util.LinkedHashMap<>();
                    java.util.List<String> playerOrder = new java.util.ArrayList<>();

                    for (Map<String, Object> item : clubDetails) {
                        String playerName = (String) item.get("playerName");
                        if (playerName == null)
                            playerName = "Unknown";

                        if (!playerEventCount.containsKey(playerName)) {
                            playerEventCount.put(playerName, 0);
                            playerFees.put(playerName, 0L);
                            playerOrder.add(playerName);
                        }

                        int eventCount = playerEventCount.get(playerName);
                        long currentFee = playerFees.get(playerName);

                        // Tính phí: 200k cho nội dung đầu, 100k cho nội dung tiếp theo
                        long eventFee = eventCount == 0 ? 200000L : 100000L;
                        playerFees.put(playerName, currentFee + eventFee);
                        playerEventCount.put(playerName, eventCount + 1);
                    }

                    // Chi tiết - hiển thị từng nội dung cho từng VĐV
                    int rowNum = 1;
                    String lastPlayerName = null;
                    int playerRowStart = 0;
                    int playerRowCount = 0;

                    for (int i = 0; i < clubDetails.size(); i++) {
                        Map<String, Object> item = clubDetails.get(i);
                        String playerName = (String) item.get("playerName");
                        if (playerName == null)
                            playerName = "Unknown";

                        String contentName = (String) item.get("contentName");
                        if (contentName == null)
                            contentName = "Unknown";

                        // Thêm số thứ tự
                        PdfPCell numCell = new PdfPCell(new Phrase(String.valueOf(rowNum), normalFont));
                        numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        numCell.setPadding(8);
                        numCell.setMinimumHeight(20);
                        detailTable.addCell(numCell);

                        // Thêm tên VĐV
                        PdfPCell nameCell = new PdfPCell(new Phrase(playerName, normalFont));
                        nameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        nameCell.setPadding(8);
                        nameCell.setMinimumHeight(20);
                        detailTable.addCell(nameCell);

                        // Thêm nội dung
                        PdfPCell contentCell = new PdfPCell(new Phrase(contentName, normalFont));
                        contentCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        contentCell.setPadding(8);
                        contentCell.setMinimumHeight(20);
                        detailTable.addCell(contentCell);

                        // Thêm phí (sẽ merge sau cho VĐV có nhiều nội dung)
                        PdfPCell feeCell = new PdfPCell(
                                new Phrase(formatMoney(playerFees.get(playerName)), normalFont));
                        feeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        feeCell.setRowspan(playerEventCount.get(playerName));
                        feeCell.setPadding(8);
                        feeCell.setMinimumHeight(20);
                        detailTable.addCell(feeCell);

                        rowNum++;
                    }

                    // Hàng tổng cộng
                    PdfPCell totalLabelCell = new PdfPCell(new Phrase("TỔNG CỘNG", boldFont));
                    totalLabelCell.setColspan(3);
                    totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabelCell.setBackgroundColor(new java.awt.Color(220, 220, 220));
                    totalLabelCell.setPadding(8);
                    totalLabelCell.setMinimumHeight(20);
                    detailTable.addCell(totalLabelCell);

                    PdfPCell totalFeeCell = new PdfPCell(
                            new Phrase(formatMoney(clubInfo.getTotalFee()), boldFont));
                    totalFeeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalFeeCell.setBackgroundColor(new java.awt.Color(220, 220, 220));
                    totalFeeCell.setPadding(8);
                    totalFeeCell.setMinimumHeight(20);
                    detailTable.addCell(totalFeeCell);

                    doc.add(detailTable);
                } else {
                    doc.add(new Paragraph("Không có thông tin chi tiết", normalFont));
                }
            } catch (Exception e) {
                doc.add(new Paragraph("Lỗi khi lấy chi tiết câu lạc bộ: " + e.getMessage(), normalFont));
            }
        }
    }

    /**
     * Inner class để xử lý header
     */
    private static class HeaderEvent extends PdfPageEventHelper {
        private String title;

        public HeaderEvent(String title) {
            this.title = title;
        }

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            try {
                // Tạo bảng header với logo và tiêu đề
                PdfPTable headerTable = new PdfPTable(3);
                headerTable.setTotalWidth(document.getPageSize().getWidth() - 72); // 36pt margin mỗi bên
                headerTable.setLockedWidth(true);
                headerTable.setWidths(new float[] { 1, 2, 1 });

                // Logo ứng dụng bên trái
                try {
                    String appLogoPath = getLogoPath("report.logo.path", "src/main/resources/icons/btms.png");
                    File logoFile = new File(appLogoPath);

                    if (logoFile.exists()) {
                        Image appLogo = Image.getInstance(logoFile.getAbsolutePath());
                        appLogo.scaleToFit(110, 110);
                        PdfPCell logoCell = new PdfPCell(appLogo);
                        logoCell.setBorder(0);
                        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        headerTable.addCell(logoCell);
                    } else {
                        PdfPCell emptyCell = new PdfPCell();
                        emptyCell.setBorder(0);
                        headerTable.addCell(emptyCell);
                    }
                } catch (Exception e) {
                    PdfPCell emptyCell = new PdfPCell();
                    emptyCell.setBorder(0);
                    headerTable.addCell(emptyCell);
                }

                // Tiêu đề ở giữa
                Phrase titlePhrase = new Phrase(title, new Font(Font.HELVETICA, 12, Font.BOLD));
                PdfPCell titleCell = new PdfPCell(titlePhrase);
                titleCell.setBorder(0);
                titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerTable.addCell(titleCell);

                // Logo nhà tài trợ bên phải
                try {
                    String sponsorLogoPath = getLogoPath("report.sponsor.logo.path",
                            "src/main/resources/icons/vuidokan.png");
                    File sponsorFile = new File(sponsorLogoPath);

                    if (sponsorFile.exists()) {
                        Image sponsorLogo = Image.getInstance(sponsorFile.getAbsolutePath());
                        sponsorLogo.scaleToFit(80, 80);
                        PdfPCell sponsorCell = new PdfPCell(sponsorLogo);
                        sponsorCell.setBorder(0);
                        sponsorCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        sponsorCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        headerTable.addCell(sponsorCell);
                    } else {
                        PdfPCell emptyCell = new PdfPCell();
                        emptyCell.setBorder(0);
                        headerTable.addCell(emptyCell);
                    }
                } catch (Exception e) {
                    PdfPCell emptyCell = new PdfPCell();
                    emptyCell.setBorder(0);
                    headerTable.addCell(emptyCell);
                }

                // Vẽ header
                headerTable.writeSelectedRows(0, -1, 36, document.getPageSize().getHeight() - 30,
                        writer.getDirectContent());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                // Thêm số trang ở footer
                int pageNumber = writer.getPageNumber();
                String text = "Trang " + pageNumber;

                // Tạo font cho số trang
                BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                Font pageFont = new Font(baseFont, 9);

                Phrase pagePhrase = new Phrase(text, pageFont);

                // Vị trí footer: giữa trang, cách đáy 20pt
                float x = (document.getPageSize().getLeft() + document.getPageSize().getRight()) / 2;
                float y = document.getPageSize().getBottom() + 20;

                com.lowagie.text.pdf.ColumnText.showTextAligned(
                        writer.getDirectContent(),
                        Element.ALIGN_CENTER,
                        pagePhrase,
                        x, y, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Lấy đường dẫn logo từ preferences hoặc sử dụng đường dẫn mặc định
         */
        private String getLogoPath(String prefKey, String defaultPath) {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot()
                    .node("com/example/btms");
            return prefs.get(prefKey, defaultPath);
        }
    }
}
