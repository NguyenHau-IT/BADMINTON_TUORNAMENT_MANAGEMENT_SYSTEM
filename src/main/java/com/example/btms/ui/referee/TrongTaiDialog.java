package com.example.btms.ui.referee;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.example.btms.model.club.CauLacBo;
import com.example.btms.model.referee.TrongTai;
import com.example.btms.service.club.CauLacBoService;
import com.example.btms.service.referee.TrongTaiService;
import com.toedter.calendar.JDateChooser;

/**
 * Dialog thêm/sửa Trọng tài
 */
public class TrongTaiDialog extends JDialog {
    private final TrongTaiService trongTaiService;
    private final CauLacBoService clbService;
    private final TrongTai original; // null nếu thêm mới
    private final boolean editMode;

    private final JTextField maTrongTaiField = new JTextField(28);
    private final JTextField hoTenField = new JTextField(28);
    private final JDateChooser ngaySinhChooser = new JDateChooser();
    private final JComboBox<String> gioiTinhCombo = new JComboBox<>(new String[] { "Nam", "Nữ" });
    private final JTextField soDienThoaiField = new JTextField(28);
    private final JTextField emailField = new JTextField(28);
    private final JPasswordField matKhauField = new JPasswordField(28);
    private final JComboBox<Object> clbCombo = new JComboBox<>(); // chứa item là CLB hoặc "— Không —"
    private final JTextField ghiChuField = new JTextField(28);

    public TrongTaiDialog(Window parent, String title, TrongTai trongTai,
            TrongTaiService trongTaiService,
            CauLacBoService clbService) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        this.trongTaiService = trongTaiService;
        this.clbService = clbService;
        this.original = trongTai;
        this.editMode = trongTai != null;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        // Fill CLB combo
        clbCombo.addItem("— Không —");
        try {
            List<CauLacBo> clubs = this.clbService.findAll();
            for (CauLacBo c : clubs) {
                if (c != null) {
                    clbCombo.addItem(c);
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("Không thể tải danh sách CLB: " + ex.getMessage());
        }
        // Hiển thị tên CLB trong dropdown
        clbCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                java.awt.Component comp = super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof CauLacBo c) {
                    setText(c.getTenClb() != null ? c.getTenClb() : "(Không tên)");
                }
                return comp;
            }
        });

        // Form panel với GridBagLayout
        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Thông tin Trọng tài"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Mã trọng tài
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Mã TT:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        maTrongTaiField.setToolTipText("Nhập mã trọng tài (tối đa 10 ký tự)");
        if (editMode) {
            maTrongTaiField.setEditable(false);
        }
        form.add(maTrongTaiField, gc);
        row++;

        // Họ tên
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Họ tên:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        hoTenField.setToolTipText("Nhập họ tên đầy đủ của trọng tài");

        // Thêm listener để tự động sinh mã khi nhập họ tên (chỉ khi thêm mới)
        if (!editMode) {
            hoTenField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    updateMaTrongTai();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    updateMaTrongTai();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    updateMaTrongTai();
                }

                private void updateMaTrongTai() {
                    String hoTen = hoTenField.getText().trim();
                    if (!hoTen.isEmpty()) {
                        String maAuto = generateMaTrongTai(hoTen);
                        maTrongTaiField.setText(maAuto);
                        // Mật khẩu giống mã trọng tài
                        matKhauField.setText(maAuto);
                        // Cho phép chỉnh sửa sau khi đã có họ tên
                        maTrongTaiField.setEditable(true);
                    } else {
                        maTrongTaiField.setText("");
                        matKhauField.setText("");
                        maTrongTaiField.setEditable(false);
                    }
                }
            });
            // Ban đầu không cho nhập mã
            maTrongTaiField.setEditable(false);
            maTrongTaiField.setToolTipText("Mã sẽ tự động sinh sau khi nhập họ tên");
        }

        form.add(hoTenField, gc);
        row++;

        // Ngày sinh
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Ngày sinh:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        ngaySinhChooser.setDateFormatString("dd/MM/yyyy");
        ngaySinhChooser.setToolTipText("Định dạng: dd/MM/yyyy (có thể để trống)");

        // Thêm listener để cập nhật mã khi thay đổi ngày sinh (chỉ khi thêm mới)
        if (!editMode) {
            ngaySinhChooser.addPropertyChangeListener("date", evt -> {
                String hoTen = hoTenField.getText().trim();
                if (!hoTen.isEmpty()) {
                    String maAuto = generateMaTrongTai(hoTen);
                    maTrongTaiField.setText(maAuto);
                    // Mật khẩu giống mã trọng tài
                    matKhauField.setText(maAuto);
                }
            });
        }

        form.add(ngaySinhChooser, gc);
        row++;

        // Giới tính
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Giới tính:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gioiTinhCombo.setToolTipText("Chọn giới tính của trọng tài");
        form.add(gioiTinhCombo, gc);
        row++;

        // Số điện thoại
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Số điện thoại:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        soDienThoaiField.setToolTipText("Nhập số điện thoại (có thể để trống)");
        form.add(soDienThoaiField, gc);
        row++;

        // Email
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Email:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        emailField.setToolTipText("Nhập email (có thể để trống)");
        form.add(emailField, gc);
        row++;

        // Mật khẩu
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Mật khẩu:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        if (!editMode) {
            matKhauField.setEditable(false);
            matKhauField.setToolTipText("Mật khẩu tự động giống mã trọng tài");
        } else {
            matKhauField.setToolTipText("Để trống nếu không muốn đổi mật khẩu");
        }
        form.add(matKhauField, gc);
        row++;

        // Câu lạc bộ
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Câu lạc bộ:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        clbCombo.setToolTipText("Chọn câu lạc bộ (có thể để 'Không')");
        form.add(clbCombo, gc);
        row++;

        // Ghi chú
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Ghi chú:"), gc);
        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        ghiChuField.setToolTipText("Nhập ghi chú (có thể để trống)");
        form.add(ghiChuField, gc);

        formWrap.add(form, BorderLayout.CENTER);
        add(formWrap, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnSave = new JButton("Lưu");
        JButton btnCancel = new JButton("Hủy");
        buttons.add(btnSave);
        buttons.add(btnCancel);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());

        if (editMode) {
            maTrongTaiField.setText(original.getMaTrongTai());
            hoTenField.setText(original.getHoTen());
            if (original.getNgaySinh() != null) {
                Date utilDate = Date.from(original.getNgaySinh().atStartOfDay(ZoneId.systemDefault()).toInstant());
                ngaySinhChooser.setDate(utilDate);
            }
            // set gender selection
            Boolean gt = original.getGioiTinh();
            if (gt != null) {
                gioiTinhCombo.setSelectedIndex(gt ? 0 : 1);
            }
            if (original.getSoDienThoai() != null) {
                soDienThoaiField.setText(original.getSoDienThoai());
            }
            if (original.getEmail() != null) {
                emailField.setText(original.getEmail());
            }
            // Không hiển thị mật khẩu cũ
            if (original.getGhiChu() != null) {
                ghiChuField.setText(original.getGhiChu());
            }
            // set club selection
            Integer clbId = original.getIdClb();
            if (clbId != null) {
                for (int i = 0; i < clbCombo.getItemCount(); i++) {
                    Object it = clbCombo.getItemAt(i);
                    if (it instanceof CauLacBo c && c.getId() != null && c.getId().equals(clbId)) {
                        clbCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } else {
                clbCombo.setSelectedIndex(0);
            }
        } else {
            // Chế độ thêm mới: điền sẵn mật khẩu mặc định
            matKhauField.setText("btms");
        }

        // Phím tắt và default button
        getRootPane().setDefaultButton(btnSave);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(parent);
    }

    private void onSave() {
        String maTrongTai = maTrongTaiField.getText() != null ? maTrongTaiField.getText().trim() : "";
        String hoTen = hoTenField.getText() != null ? hoTenField.getText().trim() : "";

        if (!editMode && maTrongTai.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã trọng tài.", "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
            maTrongTaiField.requestFocus();
            return;
        }

        if (hoTen.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập họ tên.", "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
            hoTenField.requestFocus();
            return;
        }

        Date utilDate = ngaySinhChooser.getDate();
        LocalDate ngaySinh = (utilDate != null)
                ? utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;

        Boolean gioiTinh = gioiTinhCombo.getSelectedIndex() == 0; // 0 = Nam (true), 1 = Nữ (false)

        String soDienThoai = soDienThoaiField.getText() != null ? soDienThoaiField.getText().trim() : null;
        if (soDienThoai != null && soDienThoai.isEmpty()) {
            soDienThoai = null;
        }

        String email = emailField.getText() != null ? emailField.getText().trim() : null;
        if (email != null && email.isEmpty()) {
            email = null;
        }

        String matKhau = new String(matKhauField.getPassword()).trim();
        if (editMode && matKhau.isEmpty()) {
            matKhau = null; // Không đổi mật khẩu
        } else if (!editMode && matKhau.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu.", "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
            matKhauField.requestFocus();
            return;
        }

        Integer idClb = null;
        Object clbSel = clbCombo.getSelectedItem();
        if (clbSel instanceof CauLacBo c) {
            idClb = c.getId();
        }

        String ghiChu = ghiChuField.getText() != null ? ghiChuField.getText().trim() : null;
        if (ghiChu != null && ghiChu.isEmpty()) {
            ghiChu = null;
        }

        try {
            if (editMode) {
                TrongTai updated = new TrongTai();
                updated.setHoTen(hoTen);
                updated.setNgaySinh(ngaySinh);
                updated.setGioiTinh(gioiTinh);
                updated.setSoDienThoai(soDienThoai);
                updated.setEmail(email);
                if (matKhau != null) {
                    updated.setMatKhau(matKhau);
                }
                updated.setIdClb(idClb);
                updated.setGhiChu(ghiChu);

                trongTaiService.updateTrongTai(original.getMaTrongTai(), updated);
                JOptionPane.showMessageDialog(this, "Cập nhật trọng tài thành công.", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                TrongTai newTrongTai = new TrongTai();
                newTrongTai.setMaTrongTai(maTrongTai);
                newTrongTai.setHoTen(hoTen);
                newTrongTai.setNgaySinh(ngaySinh);
                newTrongTai.setGioiTinh(gioiTinh);
                newTrongTai.setSoDienThoai(soDienThoai);
                newTrongTai.setEmail(email);
                newTrongTai.setMatKhau(matKhau);
                newTrongTai.setIdClb(idClb);
                newTrongTai.setGhiChu(ghiChu);

                trongTaiService.createTrongTai(newTrongTai);
                JOptionPane.showMessageDialog(this, "Thêm trọng tài thành công.", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            dispose();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Prefill CLB theo id (chỉ áp dụng khi thêm mới). Nếu id=null thì chọn "— Không
     * —".
     */
    public void preselectClub(Integer clbId) {
        if (editMode)
            return;
        if (clbId == null) {
            clbCombo.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < clbCombo.getItemCount(); i++) {
            Object it = clbCombo.getItemAt(i);
            if (it instanceof CauLacBo c && c.getId() != null && c.getId().equals(clbId)) {
                clbCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Prefill giới tính khi thêm mới.
     */
    public void preselectGender(Boolean gioiTinh) {
        if (editMode || gioiTinh == null)
            return;
        gioiTinhCombo.setSelectedIndex(gioiTinh ? 0 : 1);
    }

    /**
     * Tự động sinh mã trọng tài từ họ tên
     * Format: BTMS- + chữ cái đầu của các từ + ngày tháng và 2 số cuối năm sinh
     * (DDMMYY)
     */
    private String generateMaTrongTai(String hoTen) {
        if (hoTen == null || hoTen.trim().isEmpty()) {
            return "BTMS-";
        }

        StringBuilder ma = new StringBuilder("BTMS-");

        // Tách các từ trong họ tên
        String[] words = hoTen.trim().split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                // Lấy ký tự đầu tiên của mỗi từ, chuyển thành chữ hoa
                char firstChar = word.charAt(0);
                // Xử lý ký tự Unicode tiếng Việt
                String normalized = java.text.Normalizer.normalize(String.valueOf(firstChar),
                        java.text.Normalizer.Form.NFD);
                normalized = normalized.replaceAll("\\p{M}", ""); // Loại bỏ dấu
                if (!normalized.isEmpty()) {
                    ma.append(normalized.toUpperCase().charAt(0));
                }
            }
        }

        // Thêm ngày tháng năm sinh (DDMMYY)
        Date ngaySinhDate = ngaySinhChooser.getDate();
        if (ngaySinhDate != null) {
            LocalDate ngaySinh = ngaySinhDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int ngay = ngaySinh.getDayOfMonth();
            int thang = ngaySinh.getMonthValue();
            int nam = ngaySinh.getYear() % 100; // 2 số cuối của năm

            ma.append(String.format("%02d%02d%02d", ngay, thang, nam));
        } else {
            // Nếu chưa có ngày sinh, để trống hoặc dùng 000000
            ma.append("000000");
        }

        return ma.toString();
    }
}
