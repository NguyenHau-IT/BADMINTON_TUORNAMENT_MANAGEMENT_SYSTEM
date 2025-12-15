package com.example.btms.ui.device;

import com.example.btms.model.device.DeviceSession;
import com.example.btms.service.device.DeviceSessionService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel giám sát các thiết bị đang truy cập web bấm điểm
 */
public class DeviceMonitorPanel extends JPanel {

    private final DeviceSessionService deviceSessionService;
    private JTable deviceTable;
    private DefaultTableModel tableModel;
    private Timer refreshTimer;
    private JLabel lblTotalDevices;
    private JLabel lblBlockedDevices;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    public DeviceMonitorPanel(DeviceSessionService deviceSessionService) {
        this.deviceSessionService = deviceSessionService;
        initComponents();
        startAutoRefresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel tiêu đề và thống kê
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Bảng hiển thị danh sách thiết bị
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // Panel các nút chức năng
        JPanel actionPanel = createActionPanel();
        add(actionPanel, BorderLayout.SOUTH);

        // Load dữ liệu ban đầu
        refreshDeviceList();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Tiêu đề
        JLabel titleLabel = new JLabel("Giám Sát Thiết Bị Truy Cập Web Bấm Điểm");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.WEST);

        // Thống kê
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        lblTotalDevices = new JLabel("Tổng: 0");
        lblTotalDevices.setFont(new Font("Arial", Font.PLAIN, 14));
        lblBlockedDevices = new JLabel("Bị chặn: 0");
        lblBlockedDevices.setFont(new Font("Arial", Font.PLAIN, 14));
        lblBlockedDevices.setForeground(Color.RED);

        statsPanel.add(lblTotalDevices);
        statsPanel.add(Box.createHorizontalStrut(20));
        statsPanel.add(lblBlockedDevices);

        panel.add(statsPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Tạo table model
        String[] columns = {
                "Session ID", "Mã Trọng Tài", "Tên Trọng Tài",
                "Thiết Bị", "Device ID", "IP Address", "Thời Gian Login",
                "Hoạt Động Cuối", "Xác thực", "Trạng thái", "Hoạt động"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        deviceTable = new JTable(tableModel);
        deviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceTable.setRowHeight(70); // Tăng chiều cao để chứa buttons không bị che
        deviceTable.getTableHeader().setReorderingAllowed(false);

        // Đặt độ rộng cột
        deviceTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Session ID
        deviceTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Mã TT
        deviceTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Tên TT
        deviceTable.getColumnModel().getColumn(3).setPreferredWidth(130); // Thiết bị
        deviceTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Device ID
        deviceTable.getColumnModel().getColumn(5).setPreferredWidth(120); // IP
        deviceTable.getColumnModel().getColumn(6).setPreferredWidth(140); // Login time
        deviceTable.getColumnModel().getColumn(7).setPreferredWidth(140); // Last activity
        deviceTable.getColumnModel().getColumn(8).setPreferredWidth(200); // Xác thực (với buttons)
        deviceTable.getColumnModel().getColumn(9).setPreferredWidth(100); // Trạng thái
        deviceTable.getColumnModel().getColumn(10).setPreferredWidth(180); // Hoạt động (với buttons)

        // Custom renderer cho cột xác thực (cột 8) - với buttons
        deviceTable.getColumnModel().getColumn(8).setCellRenderer(new javax.swing.table.TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(true);

                if (isSelected) {
                    panel.setBackground(table.getSelectionBackground());
                } else {
                    panel.setBackground(table.getBackground());
                }

                String verified = (String) value;
                JLabel statusLabel = new JLabel(verified, SwingConstants.CENTER);
                statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
                statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                buttonPanel.setOpaque(false);

                if ("Đã duyệt".equals(verified)) {
                    statusLabel.setForeground(new Color(0, 128, 0));
                    JButton btnUnverify = new JButton("✗ Từ chối");
                    btnUnverify.setToolTipText("Click để từ chối duyệt thiết bị này");
                    btnUnverify.setPreferredSize(new Dimension(85, 20));
                    btnUnverify.setForeground(Color.ORANGE.darker());
                    buttonPanel.add(btnUnverify);
                } else {
                    statusLabel.setForeground(Color.ORANGE);
                    JButton btnVerify = new JButton("✓ Duyệt");
                    btnVerify.setToolTipText("Click để duyệt cho phép thiết bị này truy cập");
                    btnVerify.setPreferredSize(new Dimension(75, 20));
                    btnVerify.setForeground(new Color(0, 128, 0));
                    btnVerify.setFont(btnVerify.getFont().deriveFont(Font.BOLD));
                    buttonPanel.add(btnVerify);
                }

                panel.add(statusLabel, BorderLayout.NORTH);
                panel.add(buttonPanel, BorderLayout.SOUTH);

                return panel;
            }
        });

        // Custom renderer cho cột hoạt động (cột 10) - với buttons
        deviceTable.getColumnModel().getColumn(10).setCellRenderer(new javax.swing.table.TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 5));
                panel.setOpaque(true);

                if (isSelected) {
                    panel.setBackground(table.getSelectionBackground());
                } else {
                    panel.setBackground(table.getBackground());
                }

                String status = (String) table.getValueAt(row, 9); // Lấy trạng thái từ cột 9

                JButton btnBlock = new JButton(status.equals("Bị chặn") ? "Bỏ chặn" : "Chặn");
                btnBlock.setToolTipText(
                        status.equals("Bị chặn") ? "Click để bỏ chặn thiết bị này" : "Click để chặn thiết bị này");
                btnBlock.setPreferredSize(new Dimension(75, 25));
                btnBlock.setForeground(status.equals("Bị chặn") ? new Color(0, 128, 0) : Color.RED);

                JButton btnKick = new JButton("Đá khỏi");
                btnKick.setToolTipText("Click để đá thiết bị này ra khỏi hệ thống (xóa session)");
                btnKick.setPreferredSize(new Dimension(75, 25));
                btnKick.setForeground(Color.ORANGE.darker());

                panel.add(btnBlock);
                panel.add(btnKick);

                return panel;
            }
        });

        // Custom renderer cho cột trạng thái (cột 9)
        deviceTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String status = (String) value;
                if ("Bị chặn".equals(status)) {
                    c.setForeground(Color.RED);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(new Color(0, 128, 0));
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        // Add mouse listener để handle button clicks
        deviceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = deviceTable.rowAtPoint(e.getPoint());
                int col = deviceTable.columnAtPoint(e.getPoint());

                if (row >= 0) {
                    deviceTable.setRowSelectionInterval(row, row);

                    if (col == 8) { // Cột xác thực
                        handleVerificationClick(row, e.getX(), e.getY());
                    } else if (col == 10) { // Cột hoạt động
                        handleActionClick(row, e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(deviceTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void handleVerificationClick(int row, int mouseX, int mouseY) {
        List<DeviceSession> sessions = deviceSessionService.getAllSessions();
        if (row >= sessions.size())
            return;

        DeviceSession session = sessions.get(row);

        if (session.isVerified()) {
            // Click nút "Từ chối"
            unverifySelectedDevice();
        } else {
            // Click nút "Duyệt"
            verifySelectedDevice();
        }
    }

    private void handleActionClick(int row, int mouseX, int mouseY) {
        List<DeviceSession> sessions = deviceSessionService.getAllSessions();
        if (row >= sessions.size())
            return;

        DeviceSession session = sessions.get(row);

        // Ước lượng vị trí button dựa trên mouseX
        // Button "Chặn/Bỏ chặn" ở bên trái, "Đá khỏi" ở bên phải
        java.awt.Rectangle cellRect = deviceTable.getCellRect(row, 10, false);
        int relativeX = mouseX - cellRect.x;

        if (relativeX < cellRect.width / 2) {
            // Click nút Chặn/Bỏ chặn
            if (session.isBlocked()) {
                unblockSelectedDevice();
            } else {
                blockSelectedDevice();
            }
        } else {
            // Click nút Đá khỏi
            kickSelectedDevice();
        }
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton btnRefresh = new JButton("🔄 Làm mới");
        btnRefresh.addActionListener(e -> refreshDeviceList());

        JButton btnClearAll = new JButton("🗑️ Xóa tất cả sessions");
        btnClearAll.setForeground(Color.RED.darker());
        btnClearAll.addActionListener(e -> clearAllSessions());

        panel.add(btnRefresh);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(btnClearAll);

        return panel;
    }

    /**
     * Refresh danh sách thiết bị từ service
     */
    private void refreshDeviceList() {
        // Lưu lại hàng đang chọn trước khi refresh
        int selectedRow = deviceTable.getSelectedRow();
        String selectedSessionId = null;
        if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
            selectedSessionId = (String) tableModel.getValueAt(selectedRow, 0); // Session ID column
        }

        tableModel.setRowCount(0);
        List<DeviceSession> sessions = deviceSessionService.getAllSessions();

        int blockedCount = 0;
        int rowToSelect = -1;
        int currentRow = 0;

        for (DeviceSession session : sessions) {
            String status = session.isBlocked() ? "Bị chặn" : "Hoạt động";
            if (session.isBlocked()) {
                blockedCount++;
            }

            String verifiedStatus = session.isVerified() ? "Đã duyệt" : "Chờ duyệt";

            String loginTime = session.getLoginTime() != null ? session.getLoginTime().format(TIME_FORMATTER) : "-";
            String lastActivity = session.getLastActivity() != null ? session.getLastActivity().format(TIME_FORMATTER)
                    : "-";

            // Truncate session ID để dễ nhìn
            String shortSessionId = session.getSessionId().length() > 12
                    ? session.getSessionId().substring(0, 12) + "..."
                    : session.getSessionId();

            // Lấy device ID hoặc hiển thị N/A
            String deviceId = session.getDeviceId() != null && !session.getDeviceId().isEmpty()
                    ? session.getDeviceId()
                    : "N/A";

            // Sử dụng deviceModel nếu có, nếu không dùng deviceName
            String deviceDisplay = session.getDeviceModel() != null && !session.getDeviceModel().isEmpty()
                    ? session.getDeviceModel()
                    : session.getDeviceName();

            tableModel.addRow(new Object[] {
                    shortSessionId,
                    session.getMaTrongTai(),
                    session.getTenTrongTai(),
                    deviceDisplay,
                    deviceId,
                    session.getIpAddress(),
                    loginTime,
                    lastActivity,
                    verifiedStatus,
                    status,
                    "" // Cột hoạt động (sẽ render buttons)
            });

            // Kiểm tra xem hàng này có phải là hàng đã chọn trước đó không
            if (selectedSessionId != null && shortSessionId.equals(selectedSessionId)) {
                rowToSelect = currentRow;
            }
            currentRow++;
        }

        // Cập nhật thống kê
        lblTotalDevices.setText("Tổng: " + sessions.size());
        lblBlockedDevices.setText("Bị chặn: " + blockedCount);

        // Khôi phục lại selection sau khi refresh
        if (rowToSelect >= 0 && rowToSelect < tableModel.getRowCount()) {
            deviceTable.setRowSelectionInterval(rowToSelect, rowToSelect);
            // Scroll đến hàng được chọn
            deviceTable.scrollRectToVisible(deviceTable.getCellRect(rowToSelect, 0, true));
        }
    }

    /**
     * Duyệt thiết bị được chọn
     */
    private void verifySelectedDevice() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn thiết bị cần duyệt!",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<DeviceSession> sessions = deviceSessionService.getAllSessions();
        if (selectedRow >= sessions.size()) {
            JOptionPane.showMessageDialog(this,
                    "Thiết bị không còn tồn tại!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            refreshDeviceList();
            return;
        }

        DeviceSession session = sessions.get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận duyệt thiết bị:\n" +
                        "Trọng tài: " + session.getTenTrongTai() + "\n" +
                        "Thiết bị: "
                        + (session.getDeviceModel() != null ? session.getDeviceModel() : session.getDeviceName()),
                "Xác nhận",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            deviceSessionService.verifySession(session.getSessionId());
            refreshDeviceList();
            JOptionPane.showMessageDialog(this,
                    "Đã duyệt thiết bị thành công!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Từ chối duyệt thiết bị được chọn
     */
    private void unverifySelectedDevice() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn thiết bị cần từ chối duyệt!",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<DeviceSession> sessions = deviceSessionService.getAllSessions();
        if (selectedRow >= sessions.size()) {
            JOptionPane.showMessageDialog(this,
                    "Thiết bị không còn tồn tại!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            refreshDeviceList();
            return;
        }

        DeviceSession session = sessions.get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận từ chối duyệt thiết bị:\n" +
                        "Trọng tài: " + session.getTenTrongTai() + "\n" +
                        "Thiết bị: "
                        + (session.getDeviceModel() != null ? session.getDeviceModel() : session.getDeviceName()),
                "Xác nhận",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            deviceSessionService.unverifySession(session.getSessionId());
            refreshDeviceList();
            JOptionPane.showMessageDialog(this,
                    "Đã từ chối duyệt thiết bị!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Chặn thiết bị được chọn
     */
    private void blockSelectedDevice() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thiết bị cần chặn!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<DeviceSession> sessions = deviceSessionService.getAllSessions();
        if (selectedRow >= sessions.size()) {
            return;
        }

        DeviceSession session = sessions.get(selectedRow);

        if (session.isBlocked()) {
            JOptionPane.showMessageDialog(this, "Thiết bị này đã bị chặn!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn chặn thiết bị:\n" +
                        session.getTenTrongTai() + " - " + session.getDeviceName() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            deviceSessionService.blockSession(session.getSessionId());
            refreshDeviceList();
            JOptionPane.showMessageDialog(this,
                    "Đã chặn thiết bị. Trọng tài sẽ không thể truy cập web bấm điểm nữa.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Bỏ chặn thiết bị được chọn
     */
    private void unblockSelectedDevice() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thiết bị cần bỏ chặn!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<DeviceSession> sessions = deviceSessionService.getAllSessions();
        if (selectedRow >= sessions.size()) {
            return;
        }

        DeviceSession session = sessions.get(selectedRow);

        if (!session.isBlocked()) {
            JOptionPane.showMessageDialog(this, "Thiết bị này chưa bị chặn!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        deviceSessionService.unblockSession(session.getSessionId());
        refreshDeviceList();
        JOptionPane.showMessageDialog(this, "Đã bỏ chặn thiết bị thành công!",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Đá thiết bị khỏi hệ thống (xóa session)
     */
    private void kickSelectedDevice() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thiết bị cần đá khỏi hệ thống!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<DeviceSession> sessions = deviceSessionService.getAllSessions();
        if (selectedRow >= sessions.size()) {
            return;
        }

        DeviceSession session = sessions.get(selectedRow);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đá thiết bị này khỏi hệ thống?\n" +
                        session.getTenTrongTai() + " - " + session.getDeviceName(),
                "Xác nhận", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            deviceSessionService.removeSession(session.getSessionId());
            refreshDeviceList();
            JOptionPane.showMessageDialog(this,
                    "Đã đá thiết bị khỏi hệ thống. Trọng tài cần đăng nhập lại.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Xóa tất cả sessions
     */
    private void clearAllSessions() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa TẤT CẢ sessions?\nTất cả trọng tài sẽ phải đăng nhập lại!",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            deviceSessionService.clearAllSessions();
            refreshDeviceList();
            JOptionPane.showMessageDialog(this, "Đã xóa tất cả sessions!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Bắt đầu tự động làm mới mỗi 5 giây
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer(5000, e -> refreshDeviceList());
        refreshTimer.start();
    }

    /**
     * Dừng tự động làm mới
     */
    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}
