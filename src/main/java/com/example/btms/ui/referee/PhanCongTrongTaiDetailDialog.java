package com.example.btms.ui.referee;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.example.btms.model.referee.PhanCongTrongTai;
import com.example.btms.model.referee.TrongTai;
import com.example.btms.service.referee.PhanCongTrongTaiService;
import com.example.btms.service.referee.TrongTaiService;

/**
 * Dialog để xem chi tiết và chỉnh sửa phân công trọng tài
 * 
 * @author BTMS Team
 * @version 1.0
 */
public class PhanCongTrongTaiDetailDialog extends JDialog {
    private final PhanCongTrongTaiService phanCongService;
    private final TrongTaiService trongTaiService;
    private final PhanCongTrongTai original;
    private final boolean editMode;

    // Form components
    private final JTextField maPhanCongField = new JTextField(30);
    private final JComboBox<TrongTai> trongTaiCombo = new JComboBox<>();
    private final JTextField maTranDauField = new JTextField(30);
    private final JComboBox<String> vaiTroCombo = new JComboBox<>(new String[] {
            "Trọng tài chính", "Trọng tài biên", "Trọng tài giao cầu", "Trọng tài tổng"
    });
    private final JTextArea ghiChuArea = new JTextArea(4, 30);

    // Result
    private boolean saved = false;

    public PhanCongTrongTaiDetailDialog(Window parent, String title,
            PhanCongTrongTai assignment,
            PhanCongTrongTaiService phanCongService,
            TrongTaiService trongTaiService) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        this.phanCongService = phanCongService;
        this.trongTaiService = trongTaiService;
        this.original = assignment;
        this.editMode = assignment != null;

        initDialog();
    }

    private void initDialog() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Load data
        loadRefereeData();

        // Build form
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        // Populate form if editing
        if (editMode) {
            populateForm();
        }

        // Setup dialog
        pack();
        setLocationRelativeTo(getParent());

        // ESC to close
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void loadRefereeData() {
        try {
            List<TrongTai> referees = trongTaiService.getAllTrongTai();
            trongTaiCombo.removeAllItems();

            for (TrongTai referee : referees) {
                trongTaiCombo.addItem(referee);
            }

            // Custom renderer to show referee name
            trongTaiCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(
                        javax.swing.JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                    if (value instanceof TrongTai) {
                        TrongTai ref = (TrongTai) value;
                        setText(String.format("%s - %s", ref.getMaTrongTai(), ref.getHoTen()));
                    }
                    return this;
                }
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tải danh sách trọng tài: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Thông tin phân công"));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Mã phân công
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Mã phân công:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        maPhanCongField.setEditable(false);
        maPhanCongField.setToolTipText("Mã phân công (tự động tạo)");
        panel.add(maPhanCongField, gbc);
        row++;

        // Trọng tài
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Trọng tài:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        trongTaiCombo.setToolTipText("Chọn trọng tài cần phân công");
        panel.add(trongTaiCombo, gbc);
        row++;

        // Mã trận đấu
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Mã trận đấu:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        maTranDauField.setToolTipText("Nhập mã trận đấu (UUID v7)");
        panel.add(maTranDauField, gbc);
        row++;

        // Vai trò
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Vai trò:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        vaiTroCombo.setToolTipText("Chọn vai trò của trọng tài trong trận đấu");
        panel.add(vaiTroCombo, gbc);
        row++;

        // Ghi chú
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Ghi chú:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        ghiChuArea.setLineWrap(true);
        ghiChuArea.setWrapStyleWord(true);
        ghiChuArea.setToolTipText("Nhập ghi chú về phân công (có thể để trống)");
        JScrollPane scrollPane = new JScrollPane(ghiChuArea);
        panel.add(scrollPane, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton btnSave = new JButton(editMode ? "💾 Lưu thay đổi" : "✅ Tạo phân công");
        JButton btnCancel = new JButton("❌ Hủy");

        btnSave.addActionListener(e -> saveAssignment());
        btnCancel.addActionListener(e -> dispose());

        panel.add(btnSave);
        panel.add(btnCancel);

        // Set default button
        getRootPane().setDefaultButton(btnSave);

        return panel;
    }

    private void populateForm() {
        if (original == null)
            return;

        maPhanCongField.setText(original.getMaPhanCong());
        maTranDauField.setText(original.getMaTranDau());

        // Select referee
        String maTrongTai = original.getMaTrongTai();
        for (int i = 0; i < trongTaiCombo.getItemCount(); i++) {
            TrongTai ref = trongTaiCombo.getItemAt(i);
            if (ref.getMaTrongTai().equals(maTrongTai)) {
                trongTaiCombo.setSelectedIndex(i);
                break;
            }
        }

        // Set role (convert from English to Vietnamese for UI)
        String vaiTro = original.getVaiTro();
        if (vaiTro != null) {
            String vietnameseRole = convertRoleToVietnamese(vaiTro);
            vaiTroCombo.setSelectedItem(vietnameseRole);
        }

        // Set notes
        String ghiChu = original.getGhiChu();
        if (ghiChu != null) {
            ghiChuArea.setText(ghiChu);
        }
    }

    private void saveAssignment() {
        try {
            // Validate inputs
            if (!validateInputs()) {
                return;
            }

            // Create/update assignment
            PhanCongTrongTai assignment = buildAssignmentFromForm();

            if (editMode) {
                // Update existing (note: current service doesn't have update method)
                // For now, we'll just show info that this is read-only
                JOptionPane.showMessageDialog(this,
                        "Hiện tại chỉ hỗ trợ xem thông tin. Để chỉnh sửa, vui lòng xóa và tạo mới.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else {
                // Create new
                phanCongService.createAssignment(assignment);
                JOptionPane.showMessageDialog(this,
                        "Tạo phân công thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }

            saved = true;
            dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi lưu phân công: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInputs() {
        // Check referee selection
        if (trongTaiCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn trọng tài!",
                    "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            trongTaiCombo.requestFocus();
            return false;
        }

        // Check match ID
        String maTranDau = maTranDauField.getText().trim();
        if (maTranDau.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập mã trận đấu!",
                    "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            maTranDauField.requestFocus();
            return false;
        }

        // Check role
        if (vaiTroCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn vai trò!",
                    "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            vaiTroCombo.requestFocus();
            return false;
        }

        return true;
    }

    private PhanCongTrongTai buildAssignmentFromForm() {
        PhanCongTrongTai assignment = new PhanCongTrongTai();

        if (editMode && original != null) {
            assignment.setMaPhanCong(original.getMaPhanCong());
        }

        TrongTai selectedRef = (TrongTai) trongTaiCombo.getSelectedItem();
        assignment.setMaTrongTai(selectedRef.getMaTrongTai());
        assignment.setMaTranDau(maTranDauField.getText().trim());

        // Convert role from Vietnamese (UI) to English (database)
        String selectedRole = (String) vaiTroCombo.getSelectedItem();
        String englishRole = convertRoleToEnglish(selectedRole);
        assignment.setVaiTro(englishRole);

        assignment.setGhiChu(ghiChuArea.getText().trim());

        return assignment;
    }

    /**
     * Kiểm tra xem có lưu thành công không
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * Factory method để tạo dialog thêm mới
     */
    public static PhanCongTrongTaiDetailDialog createForNew(Window parent,
            PhanCongTrongTaiService phanCongService,
            TrongTaiService trongTaiService) {
        return new PhanCongTrongTaiDetailDialog(parent, "Tạo phân công mới",
                null, phanCongService, trongTaiService);
    }

    /**
     * Factory method để tạo dialog xem/sửa
     */
    public static PhanCongTrongTaiDetailDialog createForEdit(Window parent,
            PhanCongTrongTai assignment,
            PhanCongTrongTaiService phanCongService,
            TrongTaiService trongTaiService) {
        return new PhanCongTrongTaiDetailDialog(parent, "Chi tiết phân công",
                assignment, phanCongService, trongTaiService);
    }

    /**
     * Chuyển đổi vai trò từ tiếng Anh (database) sang tiếng Việt (UI)
     */
    private String convertRoleToVietnamese(String englishRole) {
        if (englishRole == null)
            return null;

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
        if (vietnameseRole == null)
            return null;

        return switch (vietnameseRole) {
            case "Trọng tài chính" -> "CHIEF";
            case "Trọng tài biên" -> "LINE";
            case "Trọng tài giao cầu" -> "SERVICE";
            case "Trọng tài tổng" -> "UMPIRE";
            default -> vietnameseRole; // Fallback
        };
    }
}