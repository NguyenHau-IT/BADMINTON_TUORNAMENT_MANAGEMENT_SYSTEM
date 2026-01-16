package com.example.btms.util.report;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import com.example.btms.config.Prefs;
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
            Map<Integer, ClubFeeInfo> clubFees, ClubFeesService clubFeesService, Integer tournamentId,
            long firstEventFee, long subsequentEventFee)
            throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            Document doc = new Document(PageSize.A4, 36f, 36f, 150f, 36f); // Tăng top margin lên 150f
            PdfWriter writer = PdfWriter.getInstance(doc, fos);

            // Set up header
            writer.setPageEvent(new HeaderEvent(tournamentName, "BÁO CÁO LỆ PHÍ THEO CÂU LẠC BỘ"));
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
                    "Chú thích: Nội dung đầu tiên: " + formatMoney(firstEventFee) + " đ/VĐV | Nội dung thứ 2 trở đi: "
                            + formatMoney(subsequentEventFee) + " đ/nội dung/VĐV",
                    normalFont);
            notes.setAlignment(Element.ALIGN_LEFT);
            doc.add(notes);

            // Thêm chi tiết từng câu lạc bộ (trang 2 trở đi)
            if (clubFeesService != null && tournamentId != null) {
                addClubDetails(doc, clubFees, clubFeesService, tournamentId, titleFont, headerFont, normalFont,
                        boldFont, firstEventFee, subsequentEventFee);
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
            Font titleFont, Font headerFont, Font normalFont, Font boldFont,
            long firstEventFee, long subsequentEventFee) throws Exception {

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
                    Map<String, java.util.List<String>> playerContents = new java.util.LinkedHashMap<>();
                    Map<String, String> playerOrder = new java.util.LinkedHashMap<>();

                    for (Map<String, Object> item : clubDetails) {
                        String playerName = (String) item.get("playerName");
                        if (playerName == null)
                            playerName = "Unknown";
                        String contentName = (String) item.get("contentName");
                        if (contentName == null)
                            contentName = "Unknown";

                        playerOrder.put(playerName, playerName);
                        playerContents.computeIfAbsent(playerName, k -> new java.util.ArrayList<>()).add(contentName);
                    }

                    // Chi tiết - hiển thị từng nội dung cho từng VĐV
                    int rowNum = 1;
                    long clubTotal = 0;

                    for (String playerName : playerOrder.keySet()) {
                        java.util.List<String> contents = playerContents.get(playerName);
                        int eventCount = contents.size();

                        // Tính phí cho VĐV này
                        long playerTotalFee = com.example.btms.util.fees.FeesCalculator.calculateFeeForPlayer(
                                eventCount,
                                firstEventFee, subsequentEventFee);

                        // Hiển thị từng nội dung
                        for (int i = 0; i < contents.size(); i++) {
                            String contentName = contents.get(i);
                            long contentFee = i == 0 ? firstEventFee : subsequentEventFee;

                            // STT
                            PdfPCell numCell = new PdfPCell(new Phrase(String.valueOf(rowNum), normalFont));
                            numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            numCell.setPadding(8);
                            numCell.setMinimumHeight(20);
                            detailTable.addCell(numCell);

                            // Tên VĐV
                            PdfPCell nameCell = new PdfPCell(new Phrase(playerName, normalFont));
                            nameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                            nameCell.setPadding(8);
                            nameCell.setMinimumHeight(20);
                            detailTable.addCell(nameCell);

                            // Nội dung
                            PdfPCell contentCell = new PdfPCell(new Phrase(contentName, normalFont));
                            contentCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                            contentCell.setPadding(8);
                            contentCell.setMinimumHeight(20);
                            detailTable.addCell(contentCell);

                            // Phí từng nội dung
                            PdfPCell feeCell = new PdfPCell(new Phrase(formatMoney(contentFee), normalFont));
                            feeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            feeCell.setPadding(8);
                            feeCell.setMinimumHeight(20);
                            detailTable.addCell(feeCell);

                            rowNum++;
                        }

                        clubTotal += playerTotalFee;
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
        private String subtitle;
        private Image leftLogo;
        private Image rightLogo;
        private Font titleFont;
        private Font subtitleFont;

        public HeaderEvent(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;

            // Load logos từ preferences
            try {
                Prefs prefs = new Prefs();
                String leftPath = prefs.get("report.logo.path", "");
                if (leftPath != null && !leftPath.isBlank()) {
                    this.leftLogo = Image.getInstance(leftPath);
                }
            } catch (Exception ignore) {
                this.leftLogo = null;
            }

            try {
                Prefs prefs = new Prefs();
                String rightPath = prefs.get("report.sponsor.logo.path", "");
                if (rightPath != null && !rightPath.isBlank()) {
                    this.rightLogo = Image.getInstance(rightPath);
                }
            } catch (Exception ignore) {
                this.rightLogo = null;
            }

            // Setup fonts
            this.titleFont = docFont(15f, Font.BOLD); // Giảm từ 16pt xuống 15pt
            this.subtitleFont = docFont(10f, Font.BOLD); // Giảm từ 12pt xuống 10pt
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                com.lowagie.text.Rectangle page = document.getPageSize();
                float pageWidth = page.getWidth();
                float leftMargin = document.leftMargin();
                float rightMargin = document.rightMargin();
                float topY = page.getHeight() - 5f; // 12pt from top edge
                float maxLogoWLeft = (pageWidth - leftMargin - rightMargin) * 1f; // Logo trái to hơn
                float maxLogoWRight = (pageWidth - leftMargin - rightMargin) * 3f; // Logo phải bình thường
                float maxLogoH = 70f;

                // Left logo (tournament/organization) - top left, larger
                if (leftLogo != null) {
                    try {
                        Image img = Image.getInstance(leftLogo);
                        img.scaleToFit(maxLogoWLeft, maxLogoH);
                        float x = leftMargin;
                        float y = topY - img.getScaledHeight();
                        img.setAbsolutePosition(x, y);
                        writer.getDirectContent().addImage(img);
                    } catch (Exception ignore) {
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
                    } catch (Exception ignore) {
                    }
                }

                // Title centered (middle of page, below logos)
                float textStartY = topY - 90f; // Space after logos
                com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(),
                        Element.ALIGN_CENTER,
                        new Phrase(this.title, this.titleFont), pageWidth / 2f, textStartY, 0);

                // Subtitle centered (below title)
                com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(),
                        Element.ALIGN_CENTER,
                        new Phrase(this.subtitle, this.subtitleFont), pageWidth / 2f, textStartY - 18f, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static Font docFont(float size, int style) {
            try {
                com.lowagie.text.FontFactory.registerDirectories();
                Font f = com.lowagie.text.FontFactory.getFont("Times New Roman", BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED, size, style);
                if (f != null && f.getBaseFont() != null)
                    return f;
            } catch (Exception ignore) {
            }
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                return new Font(bf, size, style);
            } catch (Exception e) {
                return new Font(Font.HELVETICA, size, style);
            }
        }
    }
}
