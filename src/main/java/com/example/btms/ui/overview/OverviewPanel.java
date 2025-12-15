package com.example.btms.ui.overview;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.example.btms.service.overview.OverviewService;
import com.example.btms.web.dto.OverviewStatsDTO;
import com.example.btms.web.dto.ContentStatsDTO;
import com.example.btms.model.tournament.GiaiDau;

/**
 * Panel hiển thị tổng quan thống kê của giải đấu
 */
public class OverviewPanel extends JPanel {

    private Connection connection;
    private GiaiDau currentTournament;
    private OverviewService overviewService;

    // UI Components
    private JLabel lblTournamentName;
    private JLabel lblTournamentStatus;
    private JLabel lblTotalContents;
    private JLabel lblTotalPlayers;
    private JLabel lblTotalClubs;

    private JTable contentStatsTable;
    private JTable clubStatsTable;
    private DefaultTableModel contentTableModel;
    private DefaultTableModel clubTableModel;

    public OverviewPanel() {
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("TỔNG QUAN GIẢI ĐẤU", SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.SERIF, Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 153));
        add(titleLabel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new GridLayout(2, 2, 15, 15));

        // Tournament info panel
        JPanel tournamentPanel = createTournamentInfoPanel();
        mainPanel.add(tournamentPanel);

        // General stats panel
        JPanel generalStatsPanel = createGeneralStatsPanel();
        mainPanel.add(generalStatsPanel);

        // Content stats panel
        JPanel contentPanel = createContentStatsPanel();
        mainPanel.add(contentPanel);

        // Club stats panel
        JPanel clubPanel = createClubStatsPanel();
        mainPanel.add(clubPanel);

        add(mainPanel, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("🔄 Làm mới");
        refreshButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        refreshButton.setBackground(new Color(0, 123, 255));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.addActionListener(e -> refreshData());

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createTournamentInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(new TitledBorder("Thông tin giải đấu"));
        panel.setBackground(new Color(240, 248, 255));

        lblTournamentName = new JLabel("Tên giải: Chưa chọn");
        lblTournamentName.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        lblTournamentStatus = new JLabel("Trạng thái: --");
        lblTournamentStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        JLabel lblInfo = new JLabel("Cập nhật: " + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lblInfo.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        lblInfo.setForeground(Color.GRAY);

        panel.add(lblTournamentName);
        panel.add(lblTournamentStatus);
        panel.add(lblInfo);

        return panel;
    }

    private JPanel createGeneralStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(new TitledBorder("Thống kê chung"));
        panel.setBackground(new Color(255, 248, 240));

        lblTotalContents = new JLabel("📋 Số nội dung: 0");
        lblTotalContents.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        lblTotalPlayers = new JLabel("👤 Tổng VĐV: 0");
        lblTotalPlayers.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        lblTotalClubs = new JLabel("🏢 Số CLB: 0");
        lblTotalClubs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        JLabel lblEmpty = new JLabel("");

        panel.add(lblTotalContents);
        panel.add(lblTotalPlayers);
        panel.add(lblTotalClubs);
        panel.add(lblEmpty);

        return panel;
    }

    private JPanel createContentStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Thống kê nội dung thi đấu"));
        panel.setBackground(new Color(248, 255, 240));

        // Tạo table model với 5 cột
        String[] columnNames = { "Nội dung", "Số VĐV", "Số trận DK", "Số trận đã thi", "Trạng thái" };
        contentTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa
            }
        };

        contentStatsTable = new JTable(contentTableModel);
        contentStatsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        contentStatsTable.setRowHeight(25);
        contentStatsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        contentStatsTable.getTableHeader().setBackground(new Color(220, 240, 220));
        contentStatsTable.setGridColor(new Color(200, 200, 200));
        contentStatsTable.setSelectionBackground(new Color(230, 245, 230));

        // Căn giữa các cột số liệu
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        contentStatsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Số VĐV
        contentStatsTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Số trận DK
        contentStatsTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Số trận đã thi
        contentStatsTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Trạng thái

        // Điều chỉnh độ rộng cột
        contentStatsTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Nội dung
        contentStatsTable.getColumnModel().getColumn(1).setPreferredWidth(80); // Số VĐV
        contentStatsTable.getColumnModel().getColumn(2).setPreferredWidth(90); // Số trận DK
        contentStatsTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Số trận đã thi
        contentStatsTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Trạng thái

        JScrollPane scrollPane = new JScrollPane(contentStatsTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createClubStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("VĐV theo câu lạc bộ"));
        panel.setBackground(new Color(255, 240, 248));

        // Tạo table model với 2 cột
        String[] columnNames = { "Câu lạc bộ", "Số VĐV" };
        clubTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa
            }
        };

        clubStatsTable = new JTable(clubTableModel);
        clubStatsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        clubStatsTable.setRowHeight(25);
        clubStatsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        clubStatsTable.getTableHeader().setBackground(new Color(245, 220, 240));
        clubStatsTable.setGridColor(new Color(200, 200, 200));
        clubStatsTable.setSelectionBackground(new Color(250, 230, 245));

        // Căn giữa cột số VĐV
        clubStatsTable.getColumnModel().getColumn(1).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    {
                        setHorizontalAlignment(SwingConstants.CENTER);
                    }
                });

        JScrollPane scrollPane = new JScrollPane(clubStatsTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Cập nhật connection và tournament
     */
    public void updateConnection(Connection connection, GiaiDau tournament) {
        this.connection = connection;
        this.currentTournament = tournament;
        this.overviewService = new OverviewService(connection);
        refreshData();
    }

    /**
     * Làm mới dữ liệu
     */
    public void refreshData() {
        if (overviewService == null || connection == null) {
            // Hiển thị thông tin mặc định
            lblTournamentName.setText("Tên giải: Chưa kết nối");
            lblTournamentStatus.setText("Trạng thái: Không có dữ liệu");
            lblTotalContents.setText("📋 Số nội dung: 0");
            lblTotalPlayers.setText("👤 Tổng VĐV: 0");
            lblTotalClubs.setText("🏢 Số CLB: 0");
            contentTableModel.setRowCount(0);
            clubTableModel.setRowCount(0);
            return;
        }

        try {
            // Lấy thống kê tổng quan
            OverviewStatsDTO stats = overviewService.getOverviewStats(currentTournament);

            // Cập nhật UI
            lblTournamentName.setText("Tên giải: " + stats.getTournamentName());
            lblTournamentStatus.setText("Trạng thái: " + stats.getTournamentStatus());
            lblTotalContents.setText("📋 Số nội dung: " + stats.getTotalContents());
            lblTotalPlayers.setText("👤 Tổng VĐV: " + stats.getTotalPlayersInTournament());
            lblTotalClubs.setText("🏢 Số CLB: " + stats.getTotalClubs());

            // Cập nhật thống kê chi tiết
            updateContentStats();
            updateClubStats();

        } catch (Exception e) {
            System.err.println("Lỗi làm mới dữ liệu tổng quan: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Lỗi làm mới dữ liệu: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateContentStats() {
        try {
            List<ContentStatsDTO> detailedStats = overviewService.getDetailedContentStats(currentTournament.getId());

            // Xóa dữ liệu cũ
            contentTableModel.setRowCount(0);

            if (detailedStats.isEmpty()) {
                contentTableModel.addRow(new Object[] { "Chưa có nội dung nào", 0, 0, 0, "" });
            } else {
                // Thêm dữ liệu vào table
                for (ContentStatsDTO stat : detailedStats) {
                    contentTableModel.addRow(new Object[] {
                            stat.getTenNoiDung(),
                            stat.getSoVDV(),
                            stat.getSoTranDuKien(),
                            stat.getSoTranDaThiDau(),
                            stat.getTrangThai()
                    });
                }
            }
        } catch (Exception e) {
            contentTableModel.setRowCount(0);
            contentTableModel.addRow(new Object[] { "Lỗi lấy dữ liệu nội dung", 0, 0, 0, "Lỗi" });
            System.err.println("Lỗi cập nhật thống kê nội dung: " + e.getMessage());
        }
    }

    private void updateClubStats() {
        try {
            Map<String, Integer> clubStats = overviewService.getClubStats();

            // Xóa dữ liệu cũ
            clubTableModel.setRowCount(0);

            if (clubStats.isEmpty()) {
                clubTableModel.addRow(new Object[] { "Chưa có câu lạc bộ nào", 0 });
            } else {
                // Thêm dữ liệu vào table
                for (Map.Entry<String, Integer> entry : clubStats.entrySet()) {
                    clubTableModel.addRow(new Object[] {
                            entry.getKey(),
                            entry.getValue()
                    });
                }
            }
        } catch (Exception e) {
            clubTableModel.setRowCount(0);
            clubTableModel.addRow(new Object[] { "Lỗi lấy dữ liệu câu lạc bộ", 0 });
            System.err.println("Lỗi cập nhật thống kê câu lạc bộ: " + e.getMessage());
        }
    }
}