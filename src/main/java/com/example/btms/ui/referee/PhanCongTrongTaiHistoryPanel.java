package com.example.btms.ui.referee;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.example.btms.model.referee.PhanCongTrongTai;
import com.example.btms.model.referee.TrongTai;
import com.example.btms.service.referee.PhanCongTrongTaiService;
import com.example.btms.service.referee.TrongTaiService;

/**
 * Panel để xem lịch sử phân công các trọng tài
 * 
 * @author BTMS Team
 * @version 1.0
 */
public class PhanCongTrongTaiHistoryPanel extends JPanel {
    private final PhanCongTrongTaiService phanCongService;
    private final TrongTaiService trongTaiService;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cboFilterType;
    private JComboBox<String> cboRoleFilter;
    private JButton btnRefresh, btnAdd, btnEdit, btnDelete, btnViewStats;
    private JLabel lblCount, lblTitle;
    private TableRowSorter<DefaultTableModel> sorter;

    // Column indices
    private static final int COL_MA_PHAN_CONG = 0;
    private static final int COL_MA_TRONG_TAI = 1;
    private static final int COL_TEN_TRONG_TAI = 2;
    private static final int COL_MA_TRAN_DAU = 3;
    private static final int COL_VAI_TRO = 4;
    private static final int COL_GHI_CHU = 5;

    public PhanCongTrongTaiHistoryPanel(PhanCongTrongTaiService phanCongService, TrongTaiService trongTaiService) {
        this.phanCongService = phanCongService;
        this.trongTaiService = trongTaiService;

        initComponents();
        layoutComponents();
        loadData();
        setupEventHandlers();
    }

    private void initComponents() {
        // Khởi tạo table với column headers
        String[] columnNames = {
                "Mã phân công", "Mã trọng tài", "Tên trọng tài",
                "Mã trận đấu", "Vai trò", "Ghi chú"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);

        // Row sorter
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Components
        lblTitle = new JLabel("📋 Lịch sử phân công trọng tài");
        lblTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

        txtSearch = new JTextField(20);
        txtSearch.setToolTipText("Tìm kiếm theo mã trọng tài, tên, hoặc mã trận đấu");

        cboFilterType = new JComboBox<>(new String[] {
                "Tất cả", "Mã trọng tài", "Tên trọng tài", "Mã trận đấu"
        });

        cboRoleFilter = new JComboBox<>(new String[] {
                "Tất cả vai trò", "Trọng tài chính", "Trọng tài biên", "Trọng tài giao cầu", "Trọng tài tổng"
        });

        btnRefresh = new JButton("🔄 Làm mới");
        btnAdd = new JButton("➕ Thêm mới");
        btnEdit = new JButton("✏️ Xem chi tiết");
        btnDelete = new JButton("🗑️ Xóa");
        btnViewStats = new JButton("📊 Thống kê");

        lblCount = new JLabel("Tổng: 0 phân công");
        lblCount.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.add(lblTitle);

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controlPanel.add(new JLabel("Lọc theo:"));
        controlPanel.add(cboFilterType);
        controlPanel.add(new JLabel("Tìm kiếm:"));
        controlPanel.add(txtSearch);
        controlPanel.add(new JLabel("Vai trò:"));
        controlPanel.add(cboRoleFilter);
        controlPanel.add(btnRefresh);
        controlPanel.add(btnAdd);
        controlPanel.add(btnEdit);
        controlPanel.add(btnDelete);
        controlPanel.add(btnViewStats);
        controlPanel.add(lblCount);

        // Table panel
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Layout
        add(headerPanel, BorderLayout.NORTH);
        add(controlPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        // Column configuration
        configureColumns();
    }

    private void configureColumns() {
        // Set column widths
        table.getColumnModel().getColumn(COL_MA_PHAN_CONG).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_MA_TRONG_TAI).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_TEN_TRONG_TAI).setPreferredWidth(150);
        table.getColumnModel().getColumn(COL_MA_TRAN_DAU).setPreferredWidth(200);
        table.getColumnModel().getColumn(COL_VAI_TRO).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_GHI_CHU).setPreferredWidth(200);

        // Center align specific columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(COL_MA_TRONG_TAI).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(COL_VAI_TRO).setCellRenderer(centerRenderer);

        // Left align other columns
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        table.getColumnModel().getColumn(COL_TEN_TRONG_TAI).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(COL_MA_TRAN_DAU).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(COL_GHI_CHU).setCellRenderer(leftRenderer);
    }

    private void setupEventHandlers() {
        // Search functionality
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
        });

        // Filter type change
        cboFilterType.addActionListener(e -> filterTable());

        // Role filter change
        cboRoleFilter.addActionListener(e -> filterTable());

        // Refresh button
        btnRefresh.addActionListener(e -> {
            loadData();
            JOptionPane.showMessageDialog(this, "Đã làm mới dữ liệu!", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        // Add button
        btnAdd.addActionListener(e -> addNewAssignment());

        // Edit button
        btnEdit.addActionListener(e -> viewAssignmentDetails());

        // Delete button
        btnDelete.addActionListener(e -> deleteSelectedAssignment());

        // Stats button
        btnViewStats.addActionListener(e -> showStatistics());

        // Double click to view details
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewAssignmentDetails();
                }
            }
        });
    }

    private void loadData() {
        try {
            // Clear existing data
            tableModel.setRowCount(0);

            // Get all assignments
            List<PhanCongTrongTai> assignments = phanCongService.getAllAssignments();

            // Get referee names for display
            Map<String, String> refereeNames = loadRefereeNames();

            // Populate table
            for (PhanCongTrongTai assignment : assignments) {
                String refereeName = refereeNames.getOrDefault(assignment.getMaTrongTai(), "N/A");

                Object[] row = {
                        assignment.getMaPhanCong(),
                        assignment.getMaTrongTai(),
                        refereeName,
                        assignment.getMaTranDau(),
                        convertRoleToVietnamese(assignment.getVaiTro()),
                        assignment.getGhiChu() != null ? assignment.getGhiChu() : ""
                };

                tableModel.addRow(row);
            }

            updateCountLabel();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tải dữ liệu: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, String> loadRefereeNames() {
        try {
            List<TrongTai> referees = trongTaiService.getAllTrongTai();
            return referees.stream()
                    .collect(Collectors.toMap(
                            TrongTai::getMaTrongTai,
                            TrongTai::getHoTen,
                            (existing, replacement) -> existing // Keep existing if duplicate key
                    ));
        } catch (Exception e) {
            System.err.println("Error loading referee names: " + e.getMessage());
            return Map.of(); // Return empty map if error
        }
    }

    private void filterTable() {
        String searchText = txtSearch.getText().trim();
        String filterType = (String) cboFilterType.getSelectedItem();
        String roleFilter = (String) cboRoleFilter.getSelectedItem();

        RowFilter<DefaultTableModel, Object> filter = new RowFilter<DefaultTableModel, Object>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                // Role filter
                if (!"Tất cả vai trò".equals(roleFilter)) {
                    String role = entry.getStringValue(COL_VAI_TRO);
                    // Role in UI is Vietnamese, so compare directly
                    if (!roleFilter.equals(role)) {
                        return false;
                    }
                }

                // Text search
                if (searchText.isEmpty()) {
                    return true;
                }

                String lowerSearchText = searchText.toLowerCase();

                switch (filterType) {
                    case "Mã trọng tài":
                        return entry.getStringValue(COL_MA_TRONG_TAI).toLowerCase().contains(lowerSearchText);
                    case "Tên trọng tài":
                        return entry.getStringValue(COL_TEN_TRONG_TAI).toLowerCase().contains(lowerSearchText);
                    case "Mã trận đấu":
                        return entry.getStringValue(COL_MA_TRAN_DAU).toLowerCase().contains(lowerSearchText);
                    default: // "Tất cả"
                        return entry.getStringValue(COL_MA_TRONG_TAI).toLowerCase().contains(lowerSearchText) ||
                                entry.getStringValue(COL_TEN_TRONG_TAI).toLowerCase().contains(lowerSearchText) ||
                                entry.getStringValue(COL_MA_TRAN_DAU).toLowerCase().contains(lowerSearchText) ||
                                entry.getStringValue(COL_MA_PHAN_CONG).toLowerCase().contains(lowerSearchText);
                }
            }
        };

        sorter.setRowFilter(filter);
        updateCountLabel();
    }

    private void updateCountLabel() {
        int visibleRows = table.getRowCount();
        int totalRows = tableModel.getRowCount();

        if (visibleRows == totalRows) {
            lblCount.setText(String.format("Tổng: %d phân công", totalRows));
        } else {
            lblCount.setText(String.format("Hiển thị: %d/%d phân công", visibleRows, totalRows));
        }
    }

    private void viewAssignmentDetails() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một phân công để xem chi tiết!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        String maPhanCong = (String) tableModel.getValueAt(modelRow, COL_MA_PHAN_CONG);

        try {
            Optional<PhanCongTrongTai> assignmentOpt = phanCongService.getAssignmentById(maPhanCong);
            if (assignmentOpt.isPresent()) {
                PhanCongTrongTaiDetailDialog dialog = PhanCongTrongTaiDetailDialog.createForEdit(
                        javax.swing.SwingUtilities.getWindowAncestor(this),
                        assignmentOpt.get(), phanCongService, trongTaiService);
                dialog.setVisible(true);

                if (dialog.isSaved()) {
                    loadData(); // Refresh if changes were made
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy thông tin phân công!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi lấy thông tin phân công: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addNewAssignment() {
        PhanCongTrongTaiDetailDialog dialog = PhanCongTrongTaiDetailDialog.createForNew(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                phanCongService, trongTaiService);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            loadData(); // Refresh after adding
        }
    }

    private void deleteSelectedAssignment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một phân công để xóa!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        String maPhanCong = (String) tableModel.getValueAt(modelRow, COL_MA_PHAN_CONG);
        String tenTrongTai = (String) tableModel.getValueAt(modelRow, COL_TEN_TRONG_TAI);
        String maTranDau = (String) tableModel.getValueAt(modelRow, COL_MA_TRAN_DAU);

        int choice = JOptionPane.showConfirmDialog(this,
                String.format("Bạn có chắc muốn xóa phân công?\n\nTrọng tài: %s\nTrận đấu: %s",
                        tenTrongTai, maTranDau),
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                phanCongService.deleteAssignment(maPhanCong);
                JOptionPane.showMessageDialog(this,
                        "Xóa phân công thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadData(); // Refresh after deleting
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi xóa phân công: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showStatistics() {
        try {
            List<PhanCongTrongTai> allAssignments = phanCongService.getAllAssignments();

            // Calculate statistics
            Map<String, Long> roleStats = allAssignments.stream()
                    .collect(Collectors.groupingBy(
                            assignment -> assignment.getVaiTro() != null ? assignment.getVaiTro() : "Không xác định",
                            Collectors.counting()));

            Map<String, Long> refereeStats = allAssignments.stream()
                    .collect(Collectors.groupingBy(
                            PhanCongTrongTai::getMaTrongTai,
                            Collectors.counting()));

            StringBuilder stats = new StringBuilder();
            stats.append("📊 THỐNG KÊ PHÂN CÔNG TRỌNG TÀI\n\n");

            stats.append("🎯 Phân bố theo vai trò:\n");
            roleStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> stats.append(String.format("   %s: %d phân công\n",
                            entry.getKey(), entry.getValue())));

            stats.append(String.format("\n👨‍⚖️ Tổng số trọng tài được phân công: %d\n", refereeStats.size()));
            stats.append(String.format("📋 Tổng số phân công: %d\n", allAssignments.size()));

            if (!refereeStats.isEmpty()) {
                long maxAssignments = refereeStats.values().stream().mapToLong(Long::longValue).max().orElse(0);
                long minAssignments = refereeStats.values().stream().mapToLong(Long::longValue).min().orElse(0);
                double avgAssignments = refereeStats.values().stream().mapToLong(Long::longValue).average().orElse(0.0);

                stats.append(String.format("\n📈 Số phân công trung bình: %.1f\n", avgAssignments));
                stats.append(String.format("📈 Số phân công nhiều nhất: %d\n", maxAssignments));
                stats.append(String.format("📉 Số phân công ít nhất: %d\n", minAssignments));
            }

            JOptionPane.showMessageDialog(this, stats.toString(), "Thống kê phân công",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tạo thống kê: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Public method để refresh data từ bên ngoài
     */
    public void refreshData() {
        loadData();
    }

    /**
     * Public method để lọc theo trọng tài cụ thể
     */
    public void filterByReferee(String maTrongTai) {
        cboFilterType.setSelectedItem("Mã trọng tài");
        txtSearch.setText(maTrongTai);
    }

    /**
     * Public method để lọc theo trận đấu cụ thể
     */
    public void filterByMatch(String maTranDau) {
        cboFilterType.setSelectedItem("Mã trận đấu");
        txtSearch.setText(maTranDau);
    }

    /**
     * Public method để lọc theo vai trò
     */
    public void filterByRole(String vaiTro) {
        cboRoleFilter.setSelectedItem(vaiTro);
    }

    /**
     * Chuyển đổi vai trò từ tiếng Anh (database) sang tiếng Việt (UI)
     */
    private String convertRoleToVietnamese(String englishRole) {
        if (englishRole == null || englishRole.isEmpty())
            return "";

        return switch (englishRole.toUpperCase()) {
            case "CHIEF" -> "Trọng tài chính";
            case "LINE" -> "Trọng tài biên";
            case "SERVICE" -> "Trọng tài giao cầu";
            case "UMPIRE" -> "Trọng tài tổng";
            default -> englishRole; // Fallback
        };
    }

    /**
     * Chuyển đổi vai trò từ tiếng Việt (UI) sang tiếng Anh (database)
     */
    private String convertRoleToEnglish(String vietnameseRole) {
        if (vietnameseRole == null || vietnameseRole.isEmpty())
            return "";

        return switch (vietnameseRole) {
            case "Trọng tài chính" -> "CHIEF";
            case "Trọng tài biên" -> "LINE";
            case "Trọng tài giao cầu" -> "SERVICE";
            case "Trọng tài tổng" -> "UMPIRE";
            default -> vietnameseRole; // Fallback
        };
    }
}