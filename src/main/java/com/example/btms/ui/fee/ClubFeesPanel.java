package com.example.btms.ui.fee;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.sql.SQLException;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.btms.model.tournament.GiaiDau;
import com.example.btms.service.fee.ClubFeesService;
import com.example.btms.util.fees.FeesCalculator.ClubFeeInfo;
import com.example.btms.util.report.ClubFeesPdfExporter;
import com.example.btms.util.log.Log;

/**
 * UI Panel tính lệ phí theo câu lạc bộ
 */
@Component
public class ClubFeesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    @Autowired
    private ClubFeesService clubFeesService;

    private JLabel tournamentLabel;
    private JTable feesTable;
    private DefaultTableModel tableModel;
    private JButton calculateBtn;
    private JButton exportPdfBtn;
    private JLabel totalLabel;
    private Log log = new Log();
    private Map<Integer, ClubFeeInfo> currentClubFees;
    private GiaiDau currentTournament; // giải đấu hiện tại

    public ClubFeesPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Panel trên - hiển thị giải đấu
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.add(new JLabel("Giải đấu:"));

        tournamentLabel = new JLabel("Chưa chọn giải");
        java.awt.Font font = tournamentLabel.getFont();
        tournamentLabel.setFont(new java.awt.Font(font.getName(), java.awt.Font.BOLD, font.getSize() + 2));
        topPanel.add(tournamentLabel);

        calculateBtn = new JButton("Tính lệ phí");
        calculateBtn.addActionListener(e -> calculateFees());
        topPanel.add(calculateBtn);

        exportPdfBtn = new JButton("Xuất PDF");
        exportPdfBtn.setEnabled(false);
        exportPdfBtn.addActionListener(e -> exportPdf());
        topPanel.add(exportPdfBtn);

        add(topPanel, BorderLayout.NORTH);

        // Panel giữa - bảng hiển thị
        setupTable();
        JScrollPane scrollPane = new JScrollPane(feesTable);
        add(scrollPane, BorderLayout.CENTER);

        // Panel dưới - tổng cộng
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        totalLabel = new JLabel("Tổng cộng: 0 đ");
        bottomPanel.add(totalLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupTable() {
        String[] columnNames = { "STT", "Tên Câu Lạc Bộ", "Số VĐV", "Tổng Lệ Phí" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        feesTable = new JTable(tableModel);
        feesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        TableColumnModel columnModel = feesTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40); // STT
        columnModel.getColumn(1).setPreferredWidth(300); // Tên CLB
        columnModel.getColumn(2).setPreferredWidth(80); // Số VĐV
        columnModel.getColumn(3).setPreferredWidth(120); // Tổng lệ phí
        
        // Thêm double-click listener để xem chi tiết CLB
        feesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = feesTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        showClubDetails(row);
                    }
                }
            }
        });
    }

    /**
     * Set ClubFeesService (dùng khi autowiring không hoạt động)
     */
    public void setClubFeesService(ClubFeesService service) {
        this.clubFeesService = service;
    }

    /**
     * Set giải đấu hiện tại và hiển thị tên giải
     */
    public void setSelectedTournament(GiaiDau tournament) {
        this.currentTournament = tournament;
        if (tournament != null && tournament.getTenGiai() != null) {
            tournamentLabel.setText(tournament.getTenGiai());
        } else {
            tournamentLabel.setText("Chưa chọn giải");
        }
    }

    /**
     * Set giải đấu hiện tại theo ID
     */
    public void setSelectedTournamentById(Integer tournamentId) {
        // Method không còn cần thiết vì không dùng combobox nữa
        // Giữ lại để tương thích nhưng không làm gì
    }

    private void calculateFees() {
        if (currentTournament == null) {
            log.logTs("Vui lòng chọn giải đấu");
            javax.swing.JOptionPane.showMessageDialog(this, "Vui lòng chọn giải đấu", "Thông báo", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        calculateBtn.setEnabled(false);
        new Thread(() -> {
            try {
                if (clubFeesService == null) {
                    String msg = "Lỗi: ClubFeesService chưa được khởi tạo";
                    log.logTs(msg);
                    javax.swing.JOptionPane.showMessageDialog(null, msg, "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
                    SwingUtilities.invokeLater(() -> calculateBtn.setEnabled(true));
                    return;
                }
                int tournamentId = currentTournament.getId();
                String startMsg = String.format("Bắt đầu tính lệ phí cho giải ID: %d", tournamentId);
                log.logTs(startMsg);
                System.out.println("[ClubFeesPanel] " + startMsg);
                
                currentClubFees = clubFeesService.calculateClubFees(tournamentId);
                String resultMsg = String.format("Tính lệ phí xong, số CLB: %d", currentClubFees == null ? 0 : currentClubFees.size());
                log.logTs(resultMsg);
                System.out.println("[ClubFeesPanel] " + resultMsg);
                
                SwingUtilities.invokeLater(() -> {
                    displayClubFees();
                    exportPdfBtn.setEnabled(true);
                    calculateBtn.setEnabled(true);
                    javax.swing.JOptionPane.showMessageDialog(this, resultMsg, "Thành công", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (SQLException e) {
                String errMsg = String.format("Lỗi tính lệ phí (SQL): %s", e.getMessage());
                log.logTs(errMsg);
                System.out.println("[ClubFeesPanel] " + errMsg);
                e.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(null, errMsg, "Lỗi SQL", javax.swing.JOptionPane.ERROR_MESSAGE);
                SwingUtilities.invokeLater(() -> calculateBtn.setEnabled(true));
            } catch (Exception e) {
                String errMsg = String.format("Lỗi tính lệ phí: %s", e.getMessage());
                log.logTs(errMsg);
                System.out.println("[ClubFeesPanel] " + errMsg);
                e.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(null, errMsg, "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
                SwingUtilities.invokeLater(() -> calculateBtn.setEnabled(true));
            }
        }).start();
    }

    private void displayClubFees() {
        tableModel.setRowCount(0);

        if (currentClubFees == null || currentClubFees.isEmpty()) {
            totalLabel.setText("Tổng cộng: 0 đ");
            return;
        }

        int rowNum = 1;
        int grandTotal = 0;

        for (ClubFeeInfo clubInfo : currentClubFees.values()) {
            Object[] row = {
                    rowNum,
                    clubInfo.getClubName(),
                    clubInfo.getPlayerCount(),
                    formatMoney(clubInfo.getTotalFee())
            };
            tableModel.addRow(row);
            grandTotal += clubInfo.getTotalFee();
            rowNum++;
        }

        totalLabel.setText(String.format("Tổng cộng: %s đ", formatMoney(grandTotal)));
    }

    /**
     * Format tiền với dấu cách (200 000 thay vì 200,000)
     */
    private String formatMoney(int amount) {
        return String.format("%,d", amount).replace(",", " ");
    }

    /**
     * Hiển thị dialog chi tiết về lệ phí của một CLB
     */
    private void showClubDetails(int rowIndex) {
        if (currentClubFees == null || rowIndex < 0 || rowIndex >= tableModel.getRowCount()) {
            return;
        }
        
        // Lấy tên CLB từ hàng
        String clubName = (String) tableModel.getValueAt(rowIndex, 1);
        
        // Tìm ClubFeeInfo tương ứng
        com.example.btms.util.fees.FeesCalculator.ClubFeeInfo clubInfo = null;
        Integer clubId = null;
        for (var entry : currentClubFees.entrySet()) {
            if (clubName.equals(entry.getValue().getClubName())) {
                clubInfo = entry.getValue();
                clubId = entry.getKey();
                break;
            }
        }
        
        if (clubInfo == null || clubId == null || currentTournament == null) {
            return;
        }
        
        // Tạo dialog
        javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                "Chi tiết lệ phí - " + clubName, true);
        dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        // Panel chứa bảng chi tiết
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
        panel.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));
        
        // Load chi tiết từ DB
        String[] columns = {"STT", "Tên VĐV", "Nội dung đăng ký", "Lệ phí"};
        javax.swing.table.DefaultTableModel detailModel = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        try {
            // Lấy chi tiết từ service
            var details = clubFeesService.getClubDetails(clubId, currentTournament.getId());
            
            // Map VĐV -> tổng lệ phí
            java.util.Map<Integer, Integer> playerTotalFees = new java.util.LinkedHashMap<>();
            java.util.Map<Integer, String> playerNames = new java.util.LinkedHashMap<>();
            
            for (var detail : details) {
                Integer playerId = (Integer) detail.get("playerId");
                String playerName = (String) detail.get("playerName");
                String contentName = (String) detail.get("contentName");
                
                playerNames.put(playerId, playerName);
                playerTotalFees.merge(playerId, 1, Integer::sum); // Count events
            }
            
            // Tính lệ phí cho từng VĐV dựa trên số nội dung
            int stt = 1;
            int totalFee = 0;
            for (var entry : playerTotalFees.entrySet()) {
                int playerId = entry.getKey();
                int eventCount = entry.getValue();
                String playerName = playerNames.get(playerId);
                
                int playerFee = com.example.btms.util.fees.FeesCalculator.calculateFeeForPlayer(eventCount);
                
                Object[] row = {
                        stt,
                        playerName,
                        eventCount + " nội dung",
                        formatMoney(playerFee)
                };
                detailModel.addRow(row);
                totalFee += playerFee;
                stt++;
            }
            
            javax.swing.JTable detailTable = new javax.swing.JTable(detailModel);
            detailTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(detailTable);
            panel.add(scrollPane, java.awt.BorderLayout.CENTER);
            
            // Panel dưới - tổng cộng
            javax.swing.JPanel bottomPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
            javax.swing.JLabel totalLabel = new javax.swing.JLabel(
                    String.format("Tổng lệ phí CLB: %s đ", formatMoney(totalFee)));
            java.awt.Font font = totalLabel.getFont();
            totalLabel.setFont(new java.awt.Font(font.getName(), java.awt.Font.BOLD, font.getSize() + 2));
            bottomPanel.add(totalLabel);
            panel.add(bottomPanel, java.awt.BorderLayout.SOUTH);
            
        } catch (Exception e) {
            log.logTs("Lỗi load chi tiết CLB: %s", e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(dialog, 
                    "Lỗi load chi tiết: " + e.getMessage(), 
                    "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void exportPdf() {
        if (currentClubFees == null || currentClubFees.isEmpty()) {
            log.logTs("Không có dữ liệu để xuất");
            return;
        }

        if (currentTournament == null) {
            log.logTs("Vui lòng chọn giải đấu");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu báo cáo lệ phí");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
        String defaultFileName = "BaoCao_LePhi_" + currentTournament.getTenGiai()
                + "_" + System.currentTimeMillis() + ".pdf";
        fileChooser.setSelectedFile(new File(defaultFileName));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            if (!outputFile.getName().endsWith(".pdf")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".pdf");
            }

            try {
                ClubFeesPdfExporter.export(outputFile, currentTournament.getTenGiai(), currentClubFees, clubFeesService, currentTournament.getId());
                log.logTs("Xuất PDF thành công: %s", outputFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                log.logTs("Lỗi xuất PDF: %s", e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        return "Tính lệ phí theo CLB";
    }
}
