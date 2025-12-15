package com.example.btms.ui.referee;

import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.example.btms.model.club.CauLacBo;
import com.example.btms.model.referee.TrongTai;
import com.example.btms.service.club.CauLacBoService;
import com.example.btms.service.referee.TrongTaiService;

/**
 * Panel quản lý Trọng tài
 */
public class TrongTaiManagementPanel extends JPanel {
    private final TrongTaiService trongTaiService;
    private final CauLacBoService clbService;
    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private JTable table;
    private DefaultTableModel model;
    private JButton btnAdd, btnEdit, btnDelete, btnRefresh;
    private JTextField txtSearch;
    private JLabel lblCount;
    private TableRowSorter<DefaultTableModel> sorter;

    public TrongTaiManagementPanel(TrongTaiService trongTaiService, CauLacBoService clbService) {
        this.trongTaiService = trongTaiService;
        this.clbService = clbService;
        init();
        layoutUi();
        reload();
    }

    private void init() {
        model = new DefaultTableModel(
                new Object[] { "Mã TT", "Họ tên", "Ngày sinh", "Giới tính", "SĐT", "Email", "CLB" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        btnAdd = new JButton("Thêm mới");
        btnEdit = new JButton("Sửa");
        btnDelete = new JButton("Xóa");
        btnRefresh = new JButton("Làm mới");

        txtSearch = new JTextField(15);
        lblCount = new JLabel("0/0");

        btnAdd.addActionListener(e -> onAdd());
        btnEdit.addActionListener(e -> onEdit());
        btnDelete.addActionListener(e -> onDelete());
        btnRefresh.addActionListener(e -> reload());

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }
        });
    }

    private void layoutUi() {
        setLayout(new BorderLayout(10, 10));
        JPanel top = new JPanel();
        top.add(new JLabel("Tìm:"));
        top.add(txtSearch);
        top.add(lblCount);
        top.add(btnAdd);
        top.add(btnEdit);
        top.add(btnDelete);
        top.add(btnRefresh);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void reload() {
        try {
            List<TrongTai> list = trongTaiService.getAllTrongTai();
            model.setRowCount(0);
            for (TrongTai tt : list) {
                String dob = (tt.getNgaySinh() != null) ? tt.getNgaySinh().format(DOB_FMT) : "";
                String gt = "";
                if (tt.getGioiTinh() != null) {
                    gt = tt.getGioiTinh() ? "Nam" : "Nữ";
                }
                String clb = "";
                if (tt.getIdClb() != null) {
                    try {
                        for (CauLacBo c : clbService.findAll()) {
                            if (c.getId() != null && c.getId().equals(tt.getIdClb())) {
                                clb = c.getTenClb();
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                model.addRow(new Object[] {
                        tt.getMaTrongTai(),
                        tt.getHoTen(),
                        dob,
                        gt,
                        tt.getSoDienThoai() != null ? tt.getSoDienThoai() : "",
                        tt.getEmail() != null ? tt.getEmail() : "",
                        clb
                });
            }
            updateFilter();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAdd() {
        TrongTaiDialog dialog = new TrongTaiDialog(null, "Thêm Trọng tài", null, trongTaiService, clbService);
        dialog.setVisible(true);
        reload();
        updateCountLabel();
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn trọng tài để sửa", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String maTrongTai = (String) model.getValueAt(modelRow, 0);
        try {
            TrongTai current = trongTaiService.getTrongTaiById(maTrongTai)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy trọng tài"));
            TrongTaiDialog dialog = new TrongTaiDialog(null, "Sửa Trọng tài", current, trongTaiService, clbService);
            dialog.setVisible(true);
            reload();
            updateCountLabel();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi lấy thông tin: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn trọng tài để xóa", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String maTrongTai = (String) model.getValueAt(modelRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa trọng tài này?", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        try {
            trongTaiService.deleteTrongTai(maTrongTai);
            JOptionPane.showMessageDialog(this, "Đã xóa trọng tài", "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
            reload();
            updateCountLabel();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Xóa thất bại: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateFilter() {
        String q = txtSearch.getText();
        if (q == null || q.trim().isEmpty()) {
            sorter.setRowFilter(null);
            updateCountLabel();
            return;
        }
        String pattern = java.util.regex.Pattern.quote(q.trim());
        RowFilter<DefaultTableModel, Integer> fMaTT = RowFilter.regexFilter("(?i)" + pattern, 0);
        RowFilter<DefaultTableModel, Integer> fHoTen = RowFilter.regexFilter("(?i)" + pattern, 1);
        RowFilter<DefaultTableModel, Integer> fSdt = RowFilter.regexFilter("(?i)" + pattern, 4);
        RowFilter<DefaultTableModel, Integer> fEmail = RowFilter.regexFilter("(?i)" + pattern, 5);
        RowFilter<DefaultTableModel, Integer> fClb = RowFilter.regexFilter("(?i)" + pattern, 6);
        sorter.setRowFilter(RowFilter.orFilter(java.util.List.of(fMaTT, fHoTen, fSdt, fEmail, fClb)));
        updateCountLabel();
    }

    private void updateCountLabel() {
        int visible = table.getRowCount();
        int total = model.getRowCount();
        lblCount.setText(visible + "/" + total + " trọng tài");
    }

    /**
     * Public refresh API for MainFrame and tree context menu.
     */
    public void refreshAll() {
        reload();
        updateCountLabel();
    }
}
