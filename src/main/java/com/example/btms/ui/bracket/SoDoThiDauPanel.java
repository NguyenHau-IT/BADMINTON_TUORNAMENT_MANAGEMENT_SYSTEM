package com.example.btms.ui.bracket;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.example.btms.config.Prefs;
import com.example.btms.model.bracket.SoDoCaNhan;
import com.example.btms.model.bracket.SoDoDoi;
import com.example.btms.model.category.NoiDung;
import com.example.btms.model.draw.BocThamCaNhan;
import com.example.btms.model.draw.BocThamDoi;
import com.example.btms.model.match.CourtSession;
import com.example.btms.model.result.KetQuaDoi;
import com.example.btms.repository.bracket.SoDoCaNhanRepository;
import com.example.btms.repository.bracket.SoDoDoiRepository;
import com.example.btms.repository.category.NoiDungRepository;
import com.example.btms.repository.cateoftuornament.ChiTietGiaiDauRepository;
import com.example.btms.repository.club.CauLacBoRepository;
import com.example.btms.repository.draw.BocThamCaNhanRepository;
import com.example.btms.repository.draw.BocThamDoiRepository;
import com.example.btms.repository.player.VanDongVienRepository;
import com.example.btms.repository.result.KetQuaCaNhanRepository;
import com.example.btms.repository.result.KetQuaDoiRepository;
import com.example.btms.service.bracket.SoDoCaNhanService;
import com.example.btms.service.bracket.SoDoDoiService;
import com.example.btms.service.category.NoiDungService;
import com.example.btms.service.cateoftuornament.ChiTietGiaiDauService;
import com.example.btms.service.club.CauLacBoService;
import com.example.btms.service.draw.BocThamCaNhanService;
import com.example.btms.service.draw.BocThamDoiService;
import com.example.btms.service.match.CourtManagerService;
import com.example.btms.service.player.VanDongVienService;
import com.example.btms.service.result.KetQuaCaNhanService;
import com.example.btms.service.result.KetQuaDoiService;
import com.example.btms.service.team.DoiService;
import com.example.btms.ui.control.BadmintonControlPanel;
import com.example.btms.ui.control.MultiCourtControlPanel;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Trang "Sơ đồ thi đấu" hiển thị bracket loại trực tiếp 16 -> 1 (5 cột)
 * theo các công thức được yêu cầu:
 * TOA_DO_X = 20 + (COL - 1) * 140
 * TOA_DO_Y = 40 + THU_TU * (32 * 2^(COL-1))
 * VI_TRI = THU_TU + 1 (số thứ tự hiển thị)
 *
 * Cột 1: 16 chỗ (vòng 1)
 * Cột 2: 8 chỗ (tứ kết)
 * Cột 3: 4 chỗ (bán kết)
 * Cột 4: 2 chỗ (chung kết)
 * Cột 5: 1 chỗ (vô địch)
 *
 * Nguồn tên đội lấy từ bảng BOC_THAM_DOI (thứ tự bắt đầu 0) nếu có; nếu thiếu
 * thì hiển thị "Slot n".
 */
public class SoDoThiDauPanel extends JPanel {
    // Timer tự động reload sơ đồ mỗi 10 giây
    private final javax.swing.Timer autoRefreshTimer;

    // Main tabs: "Sơ đồ" (bracket) and "Thi đấu" (embedded MultiCourtControlPanel)
    private final JTabbedPane mainTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    private final JPanel bracketTab = new JPanel(new BorderLayout(8, 8));
    private MultiCourtControlPanel embeddedMultiCourt;

    private final BracketCanvas canvas = new BracketCanvas();
    private final JButton btnSave = new JButton("Lưu");
    private final JButton btnSeedFromDraw = new JButton("Gán theo bốc thăm");
    private final JButton btnReloadSaved = new JButton("Tải sơ đồ đã lưu");
    private final JButton btnDeleteAll = new JButton("Xóa sơ đồ + kết quả + bốc thăm");
    // Chế độ hiển thị/sửa/thi đấu chuyển sang combobox
    private final JComboBox<String> cmbMode = new JComboBox<>(new String[] { "Xem", "Sửa", "Thi đấu" });
    private final JButton btnSaveResults = new JButton("Lưu kết quả");
    private final JButton btnExportBracketPdf = new JButton("Xuất sơ đồ PDF");
    private final JButton btnRefresh = new JButton("Làm mới");
    // Medals table (EAST)
    private final JTable medalTable = new JTable();
    private final DefaultTableModel medalModel = new DefaultTableModel(new Object[] { "Kết quả" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    // Track last saved medals to avoid redundant writes during auto-save
    private String lastSavedMedalKey = null;
    // Cached base font for PDF (Unicode)
    private transient com.lowagie.text.pdf.BaseFont pdfBaseFont;

    // Nội dung được chọn (không dùng combobox nữa)
    private final java.util.List<NoiDung> noiDungList = new java.util.ArrayList<>();
    private NoiDung selectedNoiDung = null;
    private final JLabel lblGiai = new JLabel();
    // Remember a pending selection when combo items haven't loaded yet
    private Integer pendingSelectNoiDungId = null;
    private final JLabel lblNoiDungValue = new JLabel(); // hiển thị tên nội dung khi dùng chế độ label
    // Track bracket number (1-based): khi load sơ đồ, sẽ load bracket number này

    // Services
    private final Prefs prefs = new Prefs();
    private final ChiTietGiaiDauService chiTietGiaiDauService;
    private final NoiDungService noiDungService;
    private final BocThamDoiService bocThamService;
    private final BocThamCaNhanService bocThamCaNhanService;
    private final SoDoCaNhanService soDoCaNhanService;
    private final SoDoDoiService soDoDoiService;
    private final DoiService doiService;
    private final CauLacBoService clbService;
    private final KetQuaDoiService ketQuaDoiService;
    private final KetQuaCaNhanService ketQuaCaNhanService;
    private final VanDongVienService vdvService;
    private final Connection conn; // lưu connection để mở panel thi đấu
    private NetworkInterface networkInterface; // lưu network interface để truyền cho embeddedMultiCourt

    // Flag để track khi render cho PDF export (để dùng font 16pt thay vì từ Prefs)
    private boolean renderingForPdf = false;

    // Seed mode controls moved to SettingsPanel; this panel now reads Prefs only.

    public SoDoThiDauPanel(Connection conn) { // giữ signature cũ để MainFrame không phải đổi nhiều
        Objects.requireNonNull(conn, "Connection null");
        this.conn = conn;
        this.noiDungService = new NoiDungService(new NoiDungRepository(conn));
        this.bocThamService = new BocThamDoiService((Connection) conn, new BocThamDoiRepository((Connection) conn));
        this.soDoDoiService = new SoDoDoiService(new SoDoDoiRepository((Connection) conn));
        this.doiService = new DoiService(conn);
        this.clbService = new CauLacBoService(new CauLacBoRepository((Connection) conn));
        this.ketQuaDoiService = new KetQuaDoiService(new KetQuaDoiRepository((Connection) conn));
        this.bocThamCaNhanService = new BocThamCaNhanService(new BocThamCaNhanRepository((Connection) conn));
        this.soDoCaNhanService = new SoDoCaNhanService(new SoDoCaNhanRepository((Connection) conn));
        this.ketQuaCaNhanService = new KetQuaCaNhanService(new KetQuaCaNhanRepository((Connection) conn));
        this.vdvService = new VanDongVienService(new VanDongVienRepository((Connection) conn));
        this.chiTietGiaiDauService = new ChiTietGiaiDauService(new ChiTietGiaiDauRepository(conn));
        setLayout(new BorderLayout());
        // Build "Sơ đồ" tab content
        bracketTab.add(buildTop(), BorderLayout.NORTH);
        // Put canvas inside a scroll pane and increase scroll sensitivity because
        // default wheel scrolling can feel slow on some systems.
        javax.swing.JScrollPane canvasScroll = new javax.swing.JScrollPane(canvas);
        // Allow user to control increment via prefs (default 24)
        int _unitInc = 24;
        try {
            _unitInc = Math.max(1, prefs.getInt("bracket.scroll.unitIncrement", 24));
        } catch (Throwable ignore) {
        }
        final int unitInc = _unitInc;
        canvasScroll.getVerticalScrollBar().setUnitIncrement(unitInc);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(unitInc);
        // Amplify wheel movement: some touchpads send tiny units; multiply them so
        // scrolling feels snappier.
        canvas.addMouseWheelListener(e -> {
            javax.swing.JScrollPane sp = (javax.swing.JScrollPane) javax.swing.SwingUtilities
                    .getAncestorOfClass(javax.swing.JScrollPane.class, canvas);
            if (sp == null)
                return;
            int units = e.getUnitsToScroll(); // typically -/+ units
            int multiplier = 1;
            try {
                multiplier = Math.max(1, prefs.getInt("bracket.scroll.multiplier", 3));
            } catch (Throwable ignore) {
            }
            int delta = units * unitInc * multiplier;
            javax.swing.JScrollBar bar = sp.getVerticalScrollBar();
            bar.setValue(bar.getValue() + delta);
            e.consume();
        });
        bracketTab.add(canvasScroll, BorderLayout.CENTER);
        bracketTab.add(buildRightPanel(), BorderLayout.EAST);
        bracketTab.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Add tabs to main container
        mainTabs.addTab("Sơ đồ", bracketTab);
        ensureEmbeddedMultiCourtTab();
        add(mainTabs, BorderLayout.CENTER);
        // Chỉ tải dữ liệu khi đã có giải được chọn trong Prefs
        updateGiaiLabel();
        if (prefs.getInt("selectedGiaiDauId", -1) > 0) {
            // Load ngay (đồng bộ) để tránh race khi caller gọi selectNoiDungById rồi
            // auto-save
            loadNoiDungOptions();
            // Không load sơ đồ tự động, chỉ load khi user chọn
        }
        installMatchModeInteraction();
        // Khôi phục chế độ đã chọn trước đó
        try {
            int savedMode = Math.max(0, Math.min(2, prefs.getInt("bracket.ui.mode", 0)));
            cmbMode.setSelectedIndex(savedMode);
        } catch (Throwable ignore) {
        }
        updateCanvasForMode();

        // Khởi động timer tự động reload mỗi 10 giây
        autoRefreshTimer = new javax.swing.Timer(5000, e -> reloadData());
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
    }

    /**
     * Ensure the embedded "Thi đấu" tab is present and wired with DB connection.
     */
    private void ensureEmbeddedMultiCourtTab() {
        if (embeddedMultiCourt == null) {
            embeddedMultiCourt = new MultiCourtControlPanel();
            try {
                embeddedMultiCourt.setConnection(this.conn);
                // Set network interface nếu có
                if (networkInterface != null) {
                    embeddedMultiCourt.setNetworkInterface(networkInterface);
                }
            } catch (Throwable ignore) {
            }
        }
        // Add the tab only once
        boolean added = false;
        for (int i = 0; i < mainTabs.getTabCount(); i++) {
            if (mainTabs.getComponentAt(i) == embeddedMultiCourt) {
                added = true;
                break;
            }
        }
        if (!added) {
            mainTabs.addTab("Thi đấu", embeddedMultiCourt);
        }
    }

    /**
     * Set network interface cho embedded MultiCourtControlPanel
     */
    public void setNetworkInterface(NetworkInterface nif) {
        this.networkInterface = nif;
        // Cập nhật embeddedMultiCourt nếu đã tạo
        if (embeddedMultiCourt != null) {
            try {
                embeddedMultiCourt.setNetworkInterface(nif);
            } catch (Throwable ignore) {
            }
        }
    }

    private boolean isMatchMode() {
        return cmbMode.getSelectedIndex() == 2; // 0: Xem, 1: Sửa, 2: Thi đấu
    }

    private void updateCanvasForMode() {
        int idx = cmbMode.getSelectedIndex();
        // Edit mode chỉ khi chọn "Sửa"
        canvas.setEditMode(idx == 1);
    }

    /**
     * Programmatically select a specific Nội dung by its id in the combo box.
     * If found, it will trigger the existing action listener to reload the bracket.
     */
    public void selectNoiDungById(Integer id) {
        if (id == null)
            return;
        for (NoiDung it : noiDungList) {
            if (it != null && it.getId() != null && it.getId().equals(id)) {
                selectedNoiDung = it;
                updateNoiDungLabelText();
                // Reset bracket number về 1 khi chọn content mới
                // Xóa canvas - không load sơ đồ tự động
                clearAllSlots();
                refreshMedalTable("", "", "", "");
                return;
            }
        }
        // Not found now; remember for when options are (re)loaded
        pendingSelectNoiDungId = id;
    }

    /**
     * Chọn sơ đồ cụ thể (bracketNumber: 1, 2, 3, ...)
     * Reload dữ liệu theo sơ đồ số đó
     */
    public void selectBracketNumber(Integer bracketNumber) {
        if (bracketNumber == null || bracketNumber < 1) {
            bracketNumber = 1;
        }
        // Lưu bracket number hiện tại (không lưu vào Prefs)
        loadBestAvailable();
    }

    /**
     * Bật/tắt chế độ chỉ-hiển-thị bằng label cho trường Nội dung.
     * Khi bật, ẩn combobox và hiện label với tên nội dung đang chọn.
     */
    public void setNoiDungLabelMode(boolean on) {
        lblNoiDungValue.setVisible(true);
        updateNoiDungLabelText();
        revalidate();
        repaint();
    }

    /**
     * Public method for parent containers to force data reload (e.g., when tab is
     * selected).
     */
    public void reloadData() {
        try {
            updateGiaiLabel();
            int idGiai = prefs.getInt("selectedGiaiDauId", -1);
            if (idGiai > 0) {
                // Thực sự reload dữ liệu sơ đồ từ database
                loadBestAvailable();
            }
        } catch (Exception ignore) {
        }
        // Đảm bảo luôn cập nhật lại canvas khi reload
        canvas.repaint();
    }

    /**
     * Tự động nạp sơ đồ từ dữ liệu bốc thăm và lưu ngay vào CSDL.
     * Dùng cho workflow tự động (chuột phải ở cây điều hướng).
     */
    public void autoSeedFromDrawAndSave() {
        try {
            // Nếu nội dung chưa sẵn sàng (vừa được set pending), cố gắng nạp lại ngay
            if (selectedNoiDung == null && pendingSelectNoiDungId != null) {
                loadNoiDungOptions();
            }
            loadFromBocTham();
            saveBracket();
        } catch (Throwable ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Không thể tự động tạo & lưu sơ đồ: " + ex.getMessage(),
                    "Lỗi",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateNoiDungLabelText() {
        NoiDung nd = selectedNoiDung;
        String name = (nd != null && nd.getTenNoiDung() != null) ? nd.getTenNoiDung().trim() : "";
        lblNoiDungValue.setText(name);
    }

    private JPanel buildTop() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        line.add(lblGiai);
        line.add(new JLabel(" | Nội dung:"));
        // Luôn dùng label để hiển thị tên nội dung; không hiển thị combobox
        lblNoiDungValue.setVisible(true);
        line.add(lblNoiDungValue);
        line.add(new JLabel(" | Chế độ:"));
        line.add(cmbMode);
        // Nút làm mới để tải lại dữ liệu hiện tại
        line.add(btnRefresh);
        line.add(btnExportBracketPdf);
        p.add(line, BorderLayout.CENTER);

        btnSave.addActionListener(e -> saveBracket());
        btnSaveResults.addActionListener(e -> saveMedalResults());
        btnReloadSaved.addActionListener(e -> loadSavedSoDo());
        btnSeedFromDraw.addActionListener(e -> loadFromBocTham());
        btnRefresh.addActionListener(e -> reloadData());
        cmbMode.addActionListener(e -> {
            updateCanvasForMode();
            try {
                prefs.putInt("bracket.ui.mode", cmbMode.getSelectedIndex());
            } catch (Throwable ignore) {
            }
        });
        btnExportBracketPdf.addActionListener(e -> exportBracketPdf());
        return p;
    }

    // Cài đặt double-click để mở bảng điều khiển trận đấu khi bật "Chế độ thi đấu"
    private void installMatchModeInteraction() {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isMatchMode())
                    return;
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 2)
                    return;
                BracketCanvas.Slot s = canvas.slotAt(e.getPoint());
                if (s == null)
                    return;
                // Luôn lấy VĐV nằm trên là VĐV 1 (A), dưới là VĐV 2 (B)
                int idxA = (s.thuTu % 2 == 0) ? s.thuTu : s.thuTu - 1;
                int idxB = idxA + 1;
                BracketCanvas.Slot slotA = null, slotB = null;
                for (BracketCanvas.Slot it : canvas.getSlots()) {
                    if (it.col == s.col && it.thuTu == idxA)
                        slotA = it;
                    if (it.col == s.col && it.thuTu == idxB)
                        slotB = it;
                }
                String a = (slotA != null && slotA.text != null) ? slotA.text.trim() : "";
                String b = (slotB != null && slotB.text != null) ? slotB.text.trim() : "";
                if (a.isBlank() || b.isBlank()) {
                    JOptionPane.showMessageDialog(SoDoThiDauPanel.this,
                            "Cặp này chưa đủ tên để mở trận.",
                            "Thi đấu",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                openMatchControlForPair(a, b);
            }
        });
    }

    // Mở dialog chọn sân và mở trong cửa sổ Nhiều sân (MultiCourtControlPanel),
    // tự tạo/chọn tab sân và prefill tên/hạng mục
    private void openMatchControlForPair(String displayA, String displayB) {
        try {
            // 1) Chỉ cho chọn các sân đã được mở trong cửa sổ Thi đấu
            // (MultiCourtControlPanel)
            com.example.btms.ui.control.MultiCourtControlPanel mcExisting = findExistingMultiCourtPanel();
            if (mcExisting == null) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Chưa mở cửa sổ Thi đấu. Vui lòng mở cửa sổ Thi đấu và tạo sân trước.",
                        "Thi đấu", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            java.util.List<String> openedCourts = new java.util.ArrayList<>();
            // Yêu cầu cửa sổ Thi đấu tạo tab cho các sân đã tồn tại nếu chưa có (hydrate)
            try {
                java.lang.reflect.Method mEns = mcExisting.getClass().getDeclaredMethod("ensureTabsForExistingCourts");
                mEns.setAccessible(true);
                mEns.invoke(mcExisting);
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException
                    | InvocationTargetException ignore) {
            }
            try {
                java.lang.reflect.Field fTabs = mcExisting.getClass().getDeclaredField("courtTabs");
                fTabs.setAccessible(true);
                Object obj = fTabs.get(mcExisting);
                if (obj instanceof javax.swing.JTabbedPane tabs) {
                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        String title = tabs.getTitleAt(i);
                        if (title != null && !title.isBlank())
                            openedCourts.add(title);
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException
                    | SecurityException ignore) {
            }
            if (openedCourts.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Chưa có sân nào trong cửa sổ Thi đấu. Vui lòng thêm sân trước.",
                        "Thi đấu", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            String[] courts = openedCourts.toArray(String[]::new);
            String courtId = (String) javax.swing.JOptionPane.showInputDialog(
                    this,
                    "Chọn sân để mở bảng điều khiển",
                    "Chọn sân",
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    courts,
                    courts[0]);
            if (courtId == null || courtId.isBlank())
                return; // huỷ
            // 2) Trước khi mở, kiểm tra trạng thái sân và trạng thái trận đấu của sân
            try {
                CourtManagerService cmsCheck = CourtManagerService.getInstance();
                java.util.Map<String, com.example.btms.service.match.CourtManagerService.CourtStatus> stMap = cmsCheck
                        .getAllCourtStatus();
                com.example.btms.service.match.CourtManagerService.CourtStatus st = (stMap != null)
                        ? stMap.get(courtId)
                        : null;
                boolean busyPlaying = (st != null && st.isPlaying);
                boolean busyPaused = (st != null && st.isPaused && !st.isFinished);
                if (busyPlaying || busyPaused) {
                    String matchState = busyPlaying ? "Đang thi đấu" : "Tạm dừng";
                    int choice = javax.swing.JOptionPane.showConfirmDialog(this,
                            String.format(
                                    "%s đang có trận (%s).\nBạn muốn mở tab hiện có (không thay đổi dữ liệu trận)?",
                                    courtId, matchState),
                            "Sân đang bận", javax.swing.JOptionPane.YES_NO_OPTION,
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    if (choice == javax.swing.JOptionPane.YES_OPTION) {
                        // Không tạo mới, chỉ chọn tab sân đã mở và đưa cửa sổ ra trước
                        try {
                            selectCourtTab(mcExisting, courtId);
                        } catch (Exception ignore) {
                        }
                        try {
                            java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(mcExisting);
                            if (win != null) {
                                win.setVisible(true);
                                win.toFront();
                                win.requestFocus();
                            }
                        } catch (Exception ignore) {
                        }
                        return;
                    } else {
                        // Người dùng muốn chọn sân khác/huỷ
                        return;
                    }
                }
            } catch (HeadlessException ignore) {
            }

            // 3) Xác định nội dung và tiêu đề header
            // Xác định nội dung (đơn/đôi) và header
            NoiDung nd = selectedNoiDung;
            String header = (nd != null && nd.getTenNoiDung() != null) ? nd.getTenNoiDung().trim() : "";
            boolean isTeam = nd != null && Boolean.TRUE.equals(nd.getTeam());

            // 4) Dùng đúng cửa sổ Nhiều sân đang mở; không tự tạo mới
            MultiCourtControlPanel mcPanel = mcExisting;
            try {
                mcPanel.setConnection(this.conn);
            } catch (Throwable ignore) {
            }

            // Không tạo sân/tab mới ở đây. Chỉ tiếp tục nếu tab sân đã tồn tại trong cửa
            // sổ.
            CourtManagerService cms = CourtManagerService.getInstance();
            CourtSession session = cms.getCourt(courtId);
            boolean tabExists = false;
            try {
                java.lang.reflect.Field fTabs = mcPanel.getClass().getDeclaredField("courtTabs");
                fTabs.setAccessible(true);
                Object obj = fTabs.get(mcPanel);
                if (obj instanceof javax.swing.JTabbedPane tabs) {
                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        if (courtId.equals(tabs.getTitleAt(i))) {
                            tabExists = true;
                            break;
                        }
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException
                    | SecurityException ignore) {
            }
            if (!tabExists) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Sân này chưa được mở trong cửa sổ Thi đấu.",
                        "Thi đấu", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (session != null) {
                // Cập nhật header hiển thị nếu có thay đổi tiêu đề nội dung
                session.header = header;
            }

            // 5) Chọn tab của sân và bring-to-front cửa sổ
            try {
                selectCourtTab(mcPanel, courtId);
            } catch (Exception ignore) {
            }
            // If using embedded panel, bring its tab to front
            if (mcPanel == embeddedMultiCourt) {
                try {
                    mainTabs.setSelectedComponent(embeddedMultiCourt);
                } catch (Exception ignore) {
                }
            }
            try {
                java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(mcPanel);
                if (win != null) {
                    win.setVisible(true);
                    win.toFront();
                    win.requestFocus();
                }
            } catch (Exception ignore) {
            }

            // 6) Prefill BadmintonControlPanel bên trong tab
            final BadmintonControlPanel panel;
            if (session != null && session.controlPanel instanceof BadmintonControlPanel p) {
                panel = p;
            } else {
                // thử lấy từ tab đang chọn
                BadmintonControlPanel foundPanel = null;
                try {
                    java.lang.reflect.Field fTabs = mcPanel.getClass().getDeclaredField("courtTabs");
                    fTabs.setAccessible(true);
                    Object obj = fTabs.get(mcPanel);
                    if (obj instanceof javax.swing.JTabbedPane tabs) {
                        // Ưu tiên tab theo courtId
                        for (int i = 0; i < tabs.getTabCount(); i++) {
                            if (courtId.equals(tabs.getTitleAt(i))
                                    && tabs.getComponentAt(i) instanceof BadmintonControlPanel p2) {
                                foundPanel = p2;
                                tabs.setSelectedIndex(i);
                                break;
                            }
                        }
                        int idx = tabs.getSelectedIndex();
                        if (foundPanel == null && idx >= 0
                                && tabs.getComponentAt(idx) instanceof BadmintonControlPanel p2) {
                            foundPanel = p2;
                        }
                    }
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException
                        | SecurityException ignore) {
                }
                panel = foundPanel;
            }

            if (panel != null) {
                try {
                    panel.setConnection(this.conn);
                } catch (Throwable ignore) {
                }
                try {
                    java.lang.reflect.Method mReload = BadmintonControlPanel.class
                            .getDeclaredMethod("reloadListsFromDb");
                    mReload.setAccessible(true);
                    mReload.invoke(panel);
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException
                        | InvocationTargetException ignore) {
                }

                try {
                    java.lang.reflect.Field fDoubles = panel.getClass().getDeclaredField("doubles");
                    java.lang.reflect.Field fHeaderS = panel.getClass().getDeclaredField("cboHeaderSingles");
                    java.lang.reflect.Field fHeaderD = panel.getClass().getDeclaredField("cboHeaderDoubles");
                    java.lang.reflect.Field fNameA = panel.getClass().getDeclaredField("cboNameA");
                    java.lang.reflect.Field fNameB = panel.getClass().getDeclaredField("cboNameB");
                    java.lang.reflect.Field fTeamA = panel.getClass().getDeclaredField("cboTeamA");
                    java.lang.reflect.Field fTeamB = panel.getClass().getDeclaredField("cboTeamB");

                    fDoubles.setAccessible(true);
                    fHeaderS.setAccessible(true);
                    fHeaderD.setAccessible(true);
                    fNameA.setAccessible(true);
                    fNameB.setAccessible(true);
                    fTeamA.setAccessible(true);
                    fTeamB.setAccessible(true);

                    javax.swing.JCheckBox cbDoubles = (javax.swing.JCheckBox) fDoubles.get(panel);
                    javax.swing.JComboBox<?> cboHeaderSingles = (javax.swing.JComboBox<?>) fHeaderS.get(panel);
                    javax.swing.JComboBox<?> cboHeaderDoubles = (javax.swing.JComboBox<?>) fHeaderD.get(panel);
                    javax.swing.JComboBox<?> cboNameA = (javax.swing.JComboBox<?>) fNameA.get(panel);
                    javax.swing.JComboBox<?> cboNameB = (javax.swing.JComboBox<?>) fNameB.get(panel);
                    javax.swing.JComboBox<?> cboTeamA = (javax.swing.JComboBox<?>) fTeamA.get(panel);
                    javax.swing.JComboBox<?> cboTeamB = (javax.swing.JComboBox<?>) fTeamB.get(panel);

                    // Chuyển chế độ đơn/đôi đúng cách (gọi toggle để UI + dữ liệu cập nhật)
                    cbDoubles.setSelected(isTeam);
                    try {
                        java.lang.reflect.Method mToggle = panel.getClass().getDeclaredMethod("toggleSinglesOrDoubles");
                        mToggle.setAccessible(true);
                        mToggle.invoke(panel);
                    } catch (Exception ignore) {
                    }
                    if (!header.isBlank()) {
                        if (isTeam) {
                            selectByString(cboHeaderDoubles, header);
                            // Trigger action event để reload team data
                            java.awt.event.ActionEvent ae = new java.awt.event.ActionEvent(cboHeaderDoubles,
                                    java.awt.event.ActionEvent.ACTION_PERFORMED, "comboBoxChanged");
                            for (java.awt.event.ActionListener al : cboHeaderDoubles.getActionListeners()) {
                                try {
                                    al.actionPerformed(ae);
                                } catch (Exception ignore) {
                                }
                            }
                        } else {
                            selectByString(cboHeaderSingles, header);
                            // Trigger action event để reload player data
                            java.awt.event.ActionEvent ae = new java.awt.event.ActionEvent(cboHeaderSingles,
                                    java.awt.event.ActionEvent.ACTION_PERFORMED, "comboBoxChanged");
                            for (java.awt.event.ActionListener al : cboHeaderSingles.getActionListeners()) {
                                try {
                                    al.actionPerformed(ae);
                                } catch (Exception ignore) {
                                }
                            }
                        }
                    }

                    // Đợi một chút để data được load xong trước khi set tên
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        if (isTeam) {
                            String teamA = extractTeamNameFromDisplay(displayA);
                            String teamB = extractTeamNameFromDisplay(displayB);
                            selectTeamByName(cboTeamA, teamA);
                            selectTeamByName(cboTeamB, teamB);
                        } else {
                            String nameA = extractNameBeforeClub(displayA);
                            String nameB = extractNameBeforeClub(displayB);
                            selectByString(cboNameA, nameA);
                            selectByString(cboNameB, nameB);
                        }

                        // Trigger update để đảm bảo match được cập nhật với tên và header đúng
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                // Delay để đảm bảo UI đã sẵn sàng
                                Thread.sleep(100);

                                java.lang.reflect.Method updateMethod = null;
                                if (isTeam) {
                                    updateMethod = panel.getClass().getDeclaredMethod("updateFromTeams");
                                } else {
                                    updateMethod = panel.getClass().getDeclaredMethod("updateFromVdv");
                                }
                                if (updateMethod != null) {
                                    updateMethod.setAccessible(true);
                                    updateMethod.invoke(panel);
                                }

                                // Delay thêm để đảm bảo update method đã hoàn thành
                                Thread.sleep(100);

                                // Trực tiếp set header và names vào match và mini panel
                                // Lấy match object từ panel
                                java.lang.reflect.Field matchField = panel.getClass().getDeclaredField("match");
                                matchField.setAccessible(true);
                                Object matchObj = matchField.get(panel);

                                // Lấy mini panel
                                java.lang.reflect.Field miniField = panel.getClass().getDeclaredField("mini");
                                miniField.setAccessible(true);
                                Object miniObj = miniField.get(panel);

                                if (matchObj != null && miniObj != null) {
                                    // Set names vào match
                                    java.lang.reflect.Method setNamesMethod = matchObj.getClass()
                                            .getDeclaredMethod("setNames", String.class, String.class);
                                    setNamesMethod.setAccessible(true);

                                    String finalNameA = isTeam ? extractTeamNameFromDisplay(displayA)
                                            : extractNameBeforeClub(displayA);
                                    String finalNameB = isTeam ? extractTeamNameFromDisplay(displayB)
                                            : extractNameBeforeClub(displayB);

                                    setNamesMethod.invoke(matchObj, finalNameA, finalNameB);

                                    // Set header vào mini panel
                                    java.lang.reflect.Method setHeaderMethod = miniObj.getClass()
                                            .getDeclaredMethod("setHeader", String.class);
                                    setHeaderMethod.setAccessible(true);
                                    setHeaderMethod.invoke(miniObj, header);

                                    // Force update lại broadcast data
                                    try {
                                        java.lang.reflect.Method broadcastMethod = panel.getClass()
                                                .getDeclaredMethod("broadcastMatchState");
                                        if (broadcastMethod != null) {
                                            broadcastMethod.setAccessible(true);
                                            broadcastMethod.invoke(panel);
                                        }
                                    } catch (Exception ignore) {
                                        // Method not found - this is expected for some panel types
                                    }
                                }
                            } catch (Exception ex) {
                                System.err.println("Error setting match data: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        });
                    });

                    // Đặt courtId nếu panel hỗ trợ (thường đã được set bởi MultiCourtControlPanel)
                    try {
                        java.lang.reflect.Method setCourtIdMethod = panel.getClass().getDeclaredMethod("setCourtId",
                                String.class);
                        setCourtIdMethod.setAccessible(true);
                        setCourtIdMethod.invoke(panel, courtId);
                    } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException
                            | SecurityException | InvocationTargetException ignore) {
                    }
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException
                        | SecurityException ignore) {
                }
            }
        } catch (HeadlessException ex) {
            JOptionPane.showMessageDialog(this, "Không thể mở bảng điều khiển trận đấu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Duyệt toàn bộ cửa sổ để tìm một MultiCourtControlPanel đang hiện diện. */
    private MultiCourtControlPanel findExistingMultiCourtPanel() {
        // Prefer embedded panel inside this view if available
        if (embeddedMultiCourt != null) {
            return embeddedMultiCourt;
        }
        try {
            for (java.awt.Window w : java.awt.Window.getWindows()) {
                if (w == null)
                    continue;
                MultiCourtControlPanel p = findMultiCourtIn(w);
                if (p != null)
                    return p;
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private static MultiCourtControlPanel findMultiCourtIn(java.awt.Component c) {
        if (c instanceof MultiCourtControlPanel m)
            return m;
        if (c instanceof java.awt.Container ct) {
            for (java.awt.Component ch : ct.getComponents()) {
                MultiCourtControlPanel found = findMultiCourtIn(ch);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /** Chọn tab theo courtId trên MultiCourtControlPanel (qua reflection). */
    private static void selectCourtTab(MultiCourtControlPanel mc, String courtId) throws Exception {
        java.lang.reflect.Field fTabs = mc.getClass().getDeclaredField("courtTabs");
        fTabs.setAccessible(true);
        Object obj = fTabs.get(mc);
        if (!(obj instanceof javax.swing.JTabbedPane tabs))
            return;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            String title = tabs.getTitleAt(i);
            if (courtId.equals(title)) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
    }

    // ensureCourtControlTabExists: removed (unused)

    // Chọn phần tử theo text (so sánh equals) nếu có
    private static void selectByString(javax.swing.JComboBox<?> cb, String text) {
        if (cb == null || text == null)
            return;
        String needle = text.trim();
        for (int i = 0; i < cb.getItemCount(); i++) {
            Object it = cb.getItemAt(i);
            if (it != null) {
                String s = it.toString().trim();
                if (s.equalsIgnoreCase(needle)) {
                    cb.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    // Tìm và chọn đội theo tên đội (DangKiDoi.tenTeam)
    private static void selectTeamByName(javax.swing.JComboBox<?> cb, String teamName) {
        if (cb == null || teamName == null)
            return;
        String needle = teamName.trim();
        for (int i = 0; i < cb.getItemCount(); i++) {
            Object obj = cb.getItemAt(i);
            if (obj instanceof com.example.btms.model.team.DangKiDoi it) {
                String s = it.getTenTeam() != null ? it.getTenTeam().trim() : "";
                if (s.equalsIgnoreCase(needle)) {
                    cb.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private static String extractNameBeforeClub(String display) {
        if (display == null)
            return "";
        int sep = display.indexOf(" - ");
        return (sep >= 0) ? display.substring(0, sep).trim() : display.trim();
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Kết quả"));
        medalTable.setModel(medalModel);
        medalTable.setTableHeader(null); // ẩn header để đúng 1 cột đơn giản
        medalTable.setRowHeight(26);
        medalTable.setShowGrid(false);
        JScrollPane sp = new JScrollPane(medalTable);
        sp.setPreferredSize(new Dimension(240, 140));
        p.add(sp, BorderLayout.CENTER);
        // Khởi tạo 4 dòng rỗng
        refreshMedalTable("", "", "", "");
        return p;
    }

    // ===== Export bracket to PDF =====
    private void exportBracketPdf() {
        try {
            /*
             * ===============================
             * 1. Render canvas -> ảnh DPI cao
             * ===============================
             */
            Dimension pref = canvas.getPreferredSize();
            if (pref != null)
                canvas.setSize(pref);

            int w = Math.max(1, canvas.getWidth());
            int h = Math.max(1, canvas.getHeight());

            int dpiScale = 2; // 2 = ~200 DPI, in rất ổn (3 nếu muốn cực nét)
            BufferedImage img = new BufferedImage(
                    w * dpiScale,
                    h * dpiScale,
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g2 = img.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, img.getWidth(), img.getHeight());
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.scale(dpiScale, dpiScale);

            renderingForPdf = true;
            canvas.printAll(g2);
            renderingForPdf = false;
            g2.dispose();

            /*
             * ===============================
             * 2. Chọn file lưu
             * ===============================
             */
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Lưu sơ đồ thi đấu (PDF)");
            chooser.setSelectedFile(new File(suggestBracketPdfFileName()));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
                return;

            File file = chooser.getSelectedFile();
            String path = file.getAbsolutePath().toLowerCase().endsWith(".pdf")
                    ? file.getAbsolutePath()
                    : file.getAbsolutePath() + ".pdf";

            /*
             * ===============================
             * 3. PDF A4 ngang – margin đều
             * ===============================
             */
            int margin = 18;
            Document doc = new Document(
                    PageSize.A4.rotate(),
                    margin, margin, margin, margin);

            try (FileOutputStream fos = new FileOutputStream(path)) {
                PdfWriter writer = PdfWriter.getInstance(doc, fos);

                /*
                 * ===============================
                 * 4. Header
                 * ===============================
                 */
                String tournament = new Prefs().get("selectedGiaiDauName", null);
                if (tournament == null || tournament.isBlank()) {
                    String giaiLbl = lblGiai.getText();
                    tournament = (giaiLbl != null && !giaiLbl.isBlank())
                            ? giaiLbl.replaceFirst("^Giải: ", "").trim()
                            : "Giải đấu";
                }
                pdfFont(12f, com.lowagie.text.Font.NORMAL);
                writer.setPageEvent(new ReportPageEvent(ensureBaseFont(), tournament));

                doc.open();

                /*
                 * ===============================
                 * 5. Tiêu đề
                 * ===============================
                 */
                String ndName = lblNoiDungValue.getText();
                String titleStr = (ndName != null && !ndName.isBlank())
                        ? ndName
                        : "SƠ ĐỒ THI ĐẤU";

                com.lowagie.text.Font titleFont = pdfFont(10f, com.lowagie.text.Font.BOLD);

                com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(titleStr, titleFont);
                title.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                title.setSpacingAfter(6f);
                doc.add(title);

                /*
                 * ===============================
                 * 6. Trim viền trắng
                 * ===============================
                 */
                BufferedImage trimmed = trimWhiteBordersWithPadding(img, 0, 0);
                System.out.println("Trimmed bracket image: " +
                        trimmed.getWidth() + " x " + trimmed.getHeight());
                /*
                 * ===============================
                 * 7. SCALE – TO HẾT CỠ CÓ THỂ
                 * ===============================
                 */
                float pageW = doc.getPageSize().getWidth();
                float pageH = doc.getPageSize().getHeight();

                float maxW = pageW
                        - doc.leftMargin()
                        - doc.rightMargin();

                float maxH = pageH
                        - doc.topMargin()
                        - doc.bottomMargin()
                        - 10f; // chừa cho title

                float scaleW = maxW / trimmed.getWidth();
                float scaleH = maxH / trimmed.getHeight();

                // ưu tiên full ngang vùng sơ đồ
                float scale = scaleW;

                // nếu cao quá thì giảm
                if (trimmed.getHeight() * scale > maxH) {
                    scale = scaleH * 0.98f;
                }

                if (scale <= 0f)
                    scale = 1f;

                /*
                 * ===============================
                 * 8. Nhúng ảnh – SÁT TRÁI + SÁT DƯỚI
                 * ===============================
                 */
                Image bracketImg = Image.getInstance(trimmed, null);
                bracketImg.scalePercent(scale * 100f);

                float bracketX = doc.leftMargin();
                float bracketY = doc.bottomMargin();

                bracketImg.setAbsolutePosition(bracketX, bracketY);

                PdfContentByte cb = writer.getDirectContent();

                Image logo = Image.getInstance(tryLoadReportLogoAwt(), null);
                logo.scaleToFit(250f, 250f);

                float logoX = pageW - doc.rightMargin() - logo.getScaledWidth();
                float logoY = pageH - doc.topMargin() - logo.getScaledHeight();

                logo.setAbsolutePosition(logoX, logoY);
                Image sponsor = Image.getInstance(tryLoadSponsorLogoAwt(), null);
                sponsor.scaleToFit(100f, 100f);

                float sponsorX = pageW - doc.rightMargin() - sponsor.getScaledWidth();
                float sponsorY = doc.bottomMargin();

                sponsor.setAbsolutePosition(sponsorX, sponsorY);
                cb.addImage(logo);
                cb.addImage(sponsor);
                cb.addImage(bracketImg);
                doc.close();

            }

            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Đã xuất PDF:\n" + path,
                    "Xuất sơ đồ thi đấu",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Lỗi xuất sơ đồ PDF: " + ex.getMessage(),
                    "Lỗi",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Export all nội dung brackets into a single multi-page PDF. */
    public boolean exportAllBracketsToSinglePdf(File file) {
        if (file == null)
            return false;

        try {
            if (noiDungList == null || noiDungList.isEmpty())
                return false;

            /*
             * ===============================
             * 1. Build target list
             * ===============================
             */
            List<NoiDung> targets = new ArrayList<>();
            int idGiai = prefs.getInt("selectedGiaiDauId", -1);

            for (NoiDung nd : noiDungList) {
                if (nd == null || nd.getId() == null)
                    continue;

                boolean include = false;
                try {
                    int soDo = chiTietGiaiDauService.findSoDo(idGiai, nd.getId());
                    if (Boolean.TRUE.equals(nd.getTeam())) {
                        var ds = bocThamService.list(idGiai, nd.getId(), soDo);
                        include = (ds != null && !ds.isEmpty());
                        if (!include) {
                            var sodo = soDoDoiService.listAll(idGiai, nd.getId());
                            include = (sodo != null && !sodo.isEmpty());
                        }
                    } else {
                        var ds = bocThamCaNhanService.list(idGiai, nd.getId(), soDo);
                        include = (ds != null && !ds.isEmpty());
                        if (!include) {
                            var sodo = soDoCaNhanService.listAll(idGiai, nd.getId());
                            include = (sodo != null && !sodo.isEmpty());
                        }
                    }
                } catch (RuntimeException ignore) {
                }

                if (include)
                    targets.add(nd);
            }

            if (targets.isEmpty())
                return false;

            /*
             * ===============================
             * 2. Tournament name & path
             * ===============================
             */
            String tournament = new Prefs().get("selectedGiaiDauName", null);
            if (tournament == null || tournament.isBlank()) {
                String giaiLbl = lblGiai.getText();
                tournament = (giaiLbl != null && !giaiLbl.isBlank())
                        ? giaiLbl.replaceFirst("^Giải: ", "").trim()
                        : "Giải đấu";
            }

            String path = file.getAbsolutePath().toLowerCase().endsWith(".pdf")
                    ? file.getAbsolutePath()
                    : file.getAbsolutePath() + ".pdf";

            /*
             * ===============================
             * 3. PDF A4 ngang – margin đều
             * ===============================
             */
            int margin = 18;
            com.lowagie.text.Document doc = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4.rotate(),
                    margin, margin, margin, margin);

            try (FileOutputStream fos = new FileOutputStream(path)) {
                com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(doc, fos);

                pdfFont(12f, com.lowagie.text.Font.NORMAL);
                writer.setPageEvent(
                        new ReportPageEvent(ensureBaseFont(), tournament));

                doc.open();

                NoiDung old = selectedNoiDung;

                /*
                 * ===============================
                 * 4. Loop từng nội dung → mỗi trang
                 * ===============================
                 */
                for (int i = 0; i < targets.size(); i++) {
                    NoiDung nd = targets.get(i);
                    if (nd == null || nd.getId() == null)
                        continue;

                    // Select & load
                    selectedNoiDung = nd;
                    updateNoiDungLabelText();
                    loadBestAvailable();

                    /*
                     * ===== Title =====
                     */
                    String ndName = nd.getTenNoiDung();
                    String titleStr = (ndName != null && !ndName.isBlank())
                            ? ndName.trim()
                            : "SƠ ĐỒ THI ĐẤU";

                    com.lowagie.text.Font titleFont = pdfFont(10f, com.lowagie.text.Font.BOLD);

                    com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(titleStr, titleFont);
                    title.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                    title.setSpacingAfter(6f);
                    doc.add(title);

                    /*
                     * ===============================
                     * 5. Render canvas → ảnh DPI cao
                     * ===============================
                     */
                    java.awt.Dimension pref = canvas.getPreferredSize();
                    if (pref != null)
                        canvas.setSize(pref);

                    int w = Math.max(1, canvas.getWidth());
                    int h = Math.max(1, canvas.getHeight());

                    int dpiScale = 2;
                    BufferedImage img = new BufferedImage(
                            w * dpiScale,
                            h * dpiScale,
                            BufferedImage.TYPE_INT_RGB);

                    Graphics2D g2 = img.createGraphics();
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, img.getWidth(), img.getHeight());
                    g2.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.scale(dpiScale, dpiScale);

                    renderingForPdf = true;
                    canvas.printAll(g2);
                    renderingForPdf = false;
                    g2.dispose();

                    /*
                     * ===============================
                     * 6. Trim viền trắng
                     * ===============================
                     */
                    BufferedImage trimmed = trimWhiteBordersWithPadding(img, 0, 0);

                    /*
                     * ===============================
                     * 7. Scale – tối đa có thể
                     * ===============================
                     */
                    float logoAreaWidth = 160f;
                    float pageW = doc.getPageSize().getWidth();
                    float pageH = doc.getPageSize().getHeight();

                    float maxW = pageW
                            - doc.leftMargin()
                            - doc.rightMargin()
                            - logoAreaWidth;

                    float maxH = pageH
                            - doc.topMargin()
                            - doc.bottomMargin()
                            - 10f;

                    float scaleW = maxW / trimmed.getWidth();
                    float scaleH = maxH / trimmed.getHeight();

                    float scale = scaleW;
                    if (trimmed.getHeight() * scale > maxH) {
                        scale = scaleH * 0.98f;
                    }
                    if (scale <= 0f)
                        scale = 1f;

                    /*
                     * ===============================
                     * 8. Add bracket image
                     * ===============================
                     */
                    com.lowagie.text.Image bracketImg = com.lowagie.text.Image.getInstance(trimmed, null);
                    bracketImg.scalePercent(scale * 100f);
                    bracketImg.setAbsolutePosition(
                            doc.leftMargin(),
                            doc.bottomMargin());

                    PdfContentByte cb = writer.getDirectContent();
                    cb.addImage(bracketImg);

                    /*
                     * ===============================
                     * 9. Logo & Sponsor (PDF layer)
                     * ===============================
                     */
                    BufferedImage awtLogo = tryLoadReportLogoAwt();
                    if (awtLogo != null) {
                        com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(awtLogo, null);
                        logo.scaleToFit(300f, 300f);

                        float logoX = pageW - doc.rightMargin() - logo.getScaledWidth();
                        float logoY = pageH - doc.topMargin() - logo.getScaledHeight();

                        logo.setAbsolutePosition(logoX, logoY);
                        cb.addImage(logo);
                    }

                    BufferedImage sponsorAwt = tryLoadSponsorLogoAwt();
                    if (sponsorAwt != null) {
                        com.lowagie.text.Image sponsor = com.lowagie.text.Image.getInstance(sponsorAwt, null);
                        sponsor.scaleToFit(130f, 130f);

                        float sponsorX = pageW - doc.rightMargin() - sponsor.getScaledWidth();
                        float sponsorY = doc.bottomMargin();

                        sponsor.setAbsolutePosition(sponsorX, sponsorY);
                        cb.addImage(sponsor);
                    }

                    if (i < targets.size() - 1)
                        doc.newPage();
                }

                // Restore selection
                selectedNoiDung = old;
                updateNoiDungLabelText();

                doc.close();
            }

            return true;

        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Export mỗi nội dung thành một file PDF trong thư mục chỉ định. Trả về số file
     * đã tạo.
     */
    public int exportEachBracketToDirectory(File dir) {
        if (dir == null)
            return 0;
        if (!dir.exists())
            dir.mkdirs();
        if (!dir.isDirectory())
            return 0;

        int count = 0;

        /*
         * ===============================
         * Tournament name
         * ===============================
         */
        String tournament = new Prefs().get("selectedGiaiDauName", null);
        if (tournament == null || tournament.isBlank()) {
            String giaiLbl = lblGiai.getText();
            tournament = (giaiLbl != null && !giaiLbl.isBlank())
                    ? giaiLbl.replaceFirst("^Giải: ", "").trim()
                    : "Giải đấu";
        }

        int idGiai = prefs.getInt("selectedGiaiDauId", -1);
        NoiDung old = selectedNoiDung;

        for (NoiDung nd : noiDungList) {
            if (nd == null || nd.getId() == null)
                continue;

            /*
             * ===============================
             * Skip nội dung không có sơ đồ
             * ===============================
             */
            boolean include = false;
            try {
                int soDo = chiTietGiaiDauService.findSoDo(idGiai, nd.getId());
                if (Boolean.TRUE.equals(nd.getTeam())) {
                    var ds = bocThamService.list(idGiai, nd.getId(), soDo);
                    include = (ds != null && !ds.isEmpty());
                    if (!include) {
                        var sodo = soDoDoiService.listAll(idGiai, nd.getId());
                        include = (sodo != null && !sodo.isEmpty());
                    }
                } else {
                    var ds = bocThamCaNhanService.list(idGiai, nd.getId(), soDo);
                    include = (ds != null && !ds.isEmpty());
                    if (!include) {
                        var sodo = soDoCaNhanService.listAll(idGiai, nd.getId());
                        include = (sodo != null && !sodo.isEmpty());
                    }
                }
            } catch (RuntimeException ignore) {
            }

            if (!include)
                continue;

            try {
                /*
                 * ===============================
                 * Select & load content
                 * ===============================
                 */
                selectedNoiDung = nd;
                updateNoiDungLabelText();
                loadBestAvailable();

                File outFile = new File(dir, suggestBracketPdfFileName());

                /*
                 * ===============================
                 * PDF A4 ngang – margin đều
                 * ===============================
                 */
                int margin = 18;
                com.lowagie.text.Document doc = new com.lowagie.text.Document(
                        com.lowagie.text.PageSize.A4.rotate(),
                        margin, margin, margin, margin);

                try (FileOutputStream fos = new FileOutputStream(outFile)) {

                    com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(doc, fos);

                    pdfFont(12f, com.lowagie.text.Font.NORMAL);
                    writer.setPageEvent(
                            new ReportPageEvent(ensureBaseFont(), tournament));

                    doc.open();

                    /*
                     * ===============================
                     * Title
                     * ===============================
                     */
                    String ndName = nd.getTenNoiDung();
                    String titleStr = (ndName != null && !ndName.isBlank())
                            ? ndName.trim()
                            : "SƠ ĐỒ THI ĐẤU";

                    com.lowagie.text.Font titleFont = pdfFont(10f, com.lowagie.text.Font.BOLD);

                    com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(titleStr, titleFont);
                    title.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                    title.setSpacingAfter(6f);
                    doc.add(title);

                    /*
                     * ===============================
                     * Render canvas → ảnh DPI cao
                     * ===============================
                     */
                    java.awt.Dimension pref = canvas.getPreferredSize();
                    if (pref != null)
                        canvas.setSize(pref);

                    int w = Math.max(1, canvas.getWidth());
                    int h = Math.max(1, canvas.getHeight());

                    int dpiScale = 2;
                    BufferedImage img = new BufferedImage(
                            w * dpiScale,
                            h * dpiScale,
                            BufferedImage.TYPE_INT_RGB);

                    Graphics2D g2 = img.createGraphics();
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, img.getWidth(), img.getHeight());
                    g2.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.scale(dpiScale, dpiScale);

                    renderingForPdf = true;
                    canvas.printAll(g2);
                    renderingForPdf = false;
                    g2.dispose();

                    /*
                     * ===============================
                     * Trim viền trắng
                     * ===============================
                     */
                    BufferedImage trimmed = trimWhiteBordersWithPadding(img, 0, 0);

                    /*
                     * ===============================
                     * Scale – tối đa có thể
                     * ===============================
                     */
                    float logoAreaWidth = 160f;
                    float pageW = doc.getPageSize().getWidth();
                    float pageH = doc.getPageSize().getHeight();

                    float maxW = pageW
                            - doc.leftMargin()
                            - doc.rightMargin()
                            - logoAreaWidth;

                    float maxH = pageH
                            - doc.topMargin()
                            - doc.bottomMargin()
                            - 10f;

                    float scaleW = maxW / trimmed.getWidth();
                    float scaleH = maxH / trimmed.getHeight();

                    float scale = scaleW;
                    if (trimmed.getHeight() * scale > maxH) {
                        scale = scaleH * 0.98f;
                    }
                    if (scale <= 0f)
                        scale = 1f;

                    /*
                     * ===============================
                     * Add bracket image
                     * ===============================
                     */
                    com.lowagie.text.Image bracketImg = com.lowagie.text.Image.getInstance(trimmed, null);
                    bracketImg.scalePercent(scale * 100f);
                    bracketImg.setAbsolutePosition(
                            doc.leftMargin(),
                            doc.bottomMargin());

                    PdfContentByte cb = writer.getDirectContent();
                    cb.addImage(bracketImg);

                    /*
                     * ===============================
                     * Logo & Sponsor (PDF layer)
                     * ===============================
                     */
                    BufferedImage awtLogo = tryLoadReportLogoAwt();
                    if (awtLogo != null) {
                        com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(awtLogo, null);
                        logo.scaleToFit(300f, 300f);

                        float logoX = pageW - doc.rightMargin() - logo.getScaledWidth();
                        float logoY = pageH - doc.topMargin() - logo.getScaledHeight();

                        logo.setAbsolutePosition(logoX, logoY);
                        cb.addImage(logo);
                    }

                    BufferedImage sponsorAwt = tryLoadSponsorLogoAwt();
                    if (sponsorAwt != null) {
                        com.lowagie.text.Image sponsor = com.lowagie.text.Image.getInstance(sponsorAwt, null);
                        sponsor.scaleToFit(130f, 130f);

                        float sponsorX = pageW - doc.rightMargin() - sponsor.getScaledWidth();
                        float sponsorY = doc.bottomMargin();

                        sponsor.setAbsolutePosition(sponsorX, sponsorY);
                        cb.addImage(sponsor);
                    }

                    doc.close();
                    count++;
                }
            } catch (Exception ignore) {
            }
        }

        /*
         * ===============================
         * Restore previous selection
         * ===============================
         */
        selectedNoiDung = old;
        updateNoiDungLabelText();
        return count;
    }

    private String suggestBracketPdfFileName() {
        String ten = new Prefs().get("selectedGiaiDauName", "giai-dau");
        String ndLabel = lblNoiDungValue.getText();
        if (ndLabel == null || ndLabel.isBlank())
            ndLabel = "nội dung";
        // Giữ dấu, chỉ xóa ký tự không hợp lệ trong tên file
        String base = ten + "_" + ndLabel + ".pdf";
        base = base.replaceAll("[<>:\"/\\|?*]", "_");
        return base;
    }

    // Filename normalizer (local copy): remove diacritics, keep [a-zA-Z0-9-_],
    // collapse dashes
    private static String normalizeFileName(String s) {
        if (s == null)
            return "file";
        String x = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-zA-Z0-9-_]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase();
        if (x.isBlank())
            return "file";
        return x;
    }

    private static BufferedImage trimWhiteBordersWithPadding(
            BufferedImage src, int padding, int extraRightPad) {
        if (src == null)
            return null;
        int w = src.getWidth();
        int h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        final int TH = 245; // threshold to treat as white/near-white
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                boolean nearWhite = (r >= TH && g >= TH && b >= TH);
                if (!nearWhite) {
                    if (x < minX)
                        minX = x;
                    if (y < minY)
                        minY = y;
                    if (x > maxX)
                        maxX = x;
                    if (y > maxY)
                        maxY = y;
                }
            }
        }
        if (maxX < 0 || maxY < 0) {
            // All white; return as is
            return src;
        }
        int pad = Math.max(0, padding);
        int x0 = Math.max(0, minX - pad);
        int y0 = Math.max(0, minY - pad);
        // Keep full width to right edge + extra padding (extraRightPad)
        int x1 = Math.min(w - 1 + extraRightPad, w - 1); // Don't exceed original width
        int y1 = Math.min(h - 1, maxY + pad);
        int nw = Math.max(1, x1 - x0 + 1);
        int nh = Math.max(1, y1 - y0 + 1);

        // Extract subimage từ src, sau đó thêm white padding bên phải
        BufferedImage sub = src.getSubimage(x0, y0, Math.min(nw, w - x0), nh);

        // Tạo output image với width lớn hơn để có chỗ cho logo bên phải
        int outputW = nw + extraRightPad;
        BufferedImage out = new BufferedImage(outputW, nh,
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = out.createGraphics();
        // Fill with white
        g2.setColor(java.awt.Color.WHITE);
        g2.fillRect(0, 0, outputW, nh);
        // Draw bracket image on the left
        g2.drawImage(sub, 0, 0, null);
        g2.dispose();
        return out;
    }

    // Load report logo as AWT image for overlay onto the bracket bitmap
    private java.awt.image.BufferedImage tryLoadReportLogoAwt() {
        try {
            String logoPath = new Prefs().get("report.logo.path", "");
            if (logoPath == null || logoPath.isBlank())
                return null;
            File f = new File(logoPath);
            if (!f.exists())
                return null;
            return javax.imageio.ImageIO.read(f);
        } catch (IOException ignore) {
            return null;
        }
    }

    // Load sponsor logo as AWT image for overlay onto the bracket bitmap
    private java.awt.image.BufferedImage tryLoadSponsorLogoAwt() {
        try {
            String logoPath = new Prefs().get("report.sponsor.logo.path", "");
            if (logoPath == null || logoPath.isBlank())
                return null;
            File f = new File(logoPath);
            if (!f.exists())
                return null;
            return javax.imageio.ImageIO.read(f);
        } catch (IOException ignore) {
            return null;
        }
    }

    // Same as overlayLogoTopRight but allows separate right and top padding.
    private static void overlayLogoTopRightWithPaddings(java.awt.image.BufferedImage base,
            java.awt.image.BufferedImage logo, int targetHeightPx, int paddingRightPx, int paddingTopPx) {
        if (base == null || logo == null || targetHeightPx <= 0)
            return;
        int srcW = logo.getWidth();
        int srcH = logo.getHeight();
        if (srcW <= 0 || srcH <= 0)
            return;
        double k = targetHeightPx / (double) srcH;
        int newH = Math.max(1, targetHeightPx);
        int newW = Math.max(1, (int) Math.round(srcW * k));
        java.awt.Image scaled = logo.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
        int x = Math.max(0, base.getWidth() - newW - Math.max(0, paddingRightPx));
        int y = Math.max(0, Math.max(0, paddingTopPx));
        java.awt.Graphics2D g = base.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(scaled, x, y, null);
        } finally {
            g.dispose();
        }
    }

    // Draw a scaled logo at bottom-right of the given image, keeping aspect ratio
    private static void overlayLogoBottomRight(java.awt.image.BufferedImage base,
            java.awt.image.BufferedImage logo, int targetHeightPx, int paddingPx) {
        if (base == null || logo == null || targetHeightPx <= 0)
            return;
        int srcW = logo.getWidth();
        int srcH = logo.getHeight();
        if (srcW <= 0 || srcH <= 0)
            return;
        double k = targetHeightPx / (double) srcH;
        int newH = Math.max(1, targetHeightPx);
        int newW = Math.max(1, (int) Math.round(srcW * k));
        java.awt.Image scaled = logo.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
        int x = Math.max(0, base.getWidth() - newW - Math.max(0, paddingPx));
        int y = Math.max(0, base.getHeight() - newH - Math.max(0, paddingPx));
        java.awt.Graphics2D g = base.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(scaled, x, y, null);
        } finally {
            g.dispose();
        }
    }

    // Read desired overlay logo height in points (on paper). Default larger: 14pt.
    // Pref key: report.logo.overlay.pt
    private float getOverlayLogoPt() {
        try {
            String s = prefs.get("report.logo.overlay.pt", null);
            if (s != null) {
                double d = Double.parseDouble(s.trim());
                float val = (float) d;
                if (Float.isFinite(val) && val > 0f && val < 72f)
                    return val; // clamp
            }
        } catch (NumberFormatException ignore) {
        }
        return 9f;
    }

    // Read overlay padding in points. Default 4pt.
    // Pref key: report.logo.overlay.padding.pt
    private float getOverlayPaddingPt() {
        try {
            String s = prefs.get("report.logo.overlay.padding.pt", null);
            if (s != null) {
                double d = Double.parseDouble(s.trim());
                float val = (float) d;
                if (Float.isFinite(val) && val >= 0f && val < 48f)
                    return val;
            }
        } catch (NumberFormatException ignore) {
        }
        return 0f;
    }

    // Vertical offset for the bracket image in PDF (spacingBefore), in points.
    // Negative value moves the image up. Default -10 (approx 10px up on paper).
    // Pref key: bracket.image.offset.before.pt
    private float getBracketImageOffsetBeforePt() {
        try {
            String s = prefs.get("bracket.image.offset.before.pt", null);
            if (s != null) {
                double d = Double.parseDouble(s.trim());
                float val = (float) d;
                if (Float.isFinite(val) && val >= -40f && val <= 40f)
                    return val;
            }
        } catch (NumberFormatException ignore) {
        }
        return -15f;
    }

    // Reduce bracket image height by N pixels (on the rendered bitmap) by
    // decreasing scale.
    // Pref key: bracket.image.reduce.height.px, default 10px.
    private int getReduceBracketHeightPx() {
        try {
            // Cho sơ đồ 16/32 vừa vặn khung A4: không reduce (0px)
            // Chỉ reduce 64-seat (30px) để tránh tràn ra khỏi trang
            int maxSlots = canvas.spots != null && canvas.spots.length > 0 ? canvas.spots[0] : 64;
            if (maxSlots <= 32) {
                // Sơ đồ nhỏ (16/32): không reduce, để tối đa hóa kích thước
                return 0;
            }
            // Sơ đồ lớn (64): reduce 30px để tránh tràn
            int val = prefs.getInt("bracket.image.reduce.height.px", 30);
            if (val < 0)
                return 0;
            if (val > 100)
                return 100; // clamp
            return val;
        } catch (Throwable ignore) {
            return 0; // safe default
        }
    }

    // Multiplier to enlarge overlay logo aggressively. Default 10x per request.
    // Pref key: report.logo.overlay.multiplier (float)
    private float getOverlayMultiplier() {
        try {
            String s = prefs.get("report.logo.overlay.multiplier", null);
            if (s != null) {
                double d = Double.parseDouble(s.trim());
                float val = (float) d;
                if (Float.isFinite(val) && val > 0.1f && val <= 20f)
                    return val;
            }
        } catch (NumberFormatException ignore) {
        }
        return 10f; // default 10x
    }

    // Additional adjust factor applied to overlay size to fine-tune "a bit
    // smaller".
    // Pref key: report.logo.overlay.adjust.factor (float), default 0.9f
    private float getOverlayAdjustFactor() {
        try {
            String s = prefs.get("report.logo.overlay.adjust.factor", null);
            if (s != null) {
                double d = Double.parseDouble(s.trim());
                float val = (float) d;
                if (Float.isFinite(val) && val > 0.2f && val <= 2.0f)
                    return val;
            }
        } catch (NumberFormatException ignore) {
        }
        return 0.9f;
    }

    private com.lowagie.text.Font pdfFont(float size, int style) {
        try {
            if (pdfBaseFont == null) {
                // Attempt to load a Unicode-capable Windows font (Arial/Tahoma/Segoe UI)
                String[] candidates = new String[] {
                        "C:/Windows/Fonts/arial.ttf",
                        "C:/Windows/Fonts/tahoma.ttf",
                        "C:/Windows/Fonts/segoeui.ttf"
                };
                File found = null;
                for (String p : candidates) {
                    File f = new File(p);
                    if (f.exists()) {
                        found = f;
                        break;
                    }
                }
                if (found != null) {
                    pdfBaseFont = com.lowagie.text.pdf.BaseFont.createFont(found.getAbsolutePath(),
                            com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED);
                } else {
                    // Fallback to built-in Helvetica (may not render all diacritics perfectly)
                    return new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, size, style);
                }
            }
            return new com.lowagie.text.Font(pdfBaseFont, size, style);
        } catch (IOException | RuntimeException ignore) {
            return new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, size, style);
        }
    }

    // Ensure base font for page event (fall back to default)
    private com.lowagie.text.pdf.BaseFont ensureBaseFont() {
        try {
            if (pdfBaseFont != null)
                return pdfBaseFont;
            return com.lowagie.text.pdf.BaseFont.createFont();
        } catch (com.lowagie.text.DocumentException e) {
            System.err.println("ensureBaseFont DocumentException: " + e.getMessage());
            return null;
        } catch (java.io.IOException e) {
            System.err.println("ensureBaseFont IOException: " + e.getMessage());
            return null;
        }
    }

    private static final class ReportPageEvent extends com.lowagie.text.pdf.PdfPageEventHelper {
        private final com.lowagie.text.pdf.BaseFont baseFont;
        private final String tournamentName;

        ReportPageEvent(BaseFont baseFont, String tournamentName) {
            this.baseFont = baseFont;
            this.tournamentName = tournamentName;
        }

        @Override
        public void onEndPage(com.lowagie.text.pdf.PdfWriter writer, com.lowagie.text.Document document) {
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
            float left = document.left();
            float top = document.top();

            if (tournamentName != null && !tournamentName.isBlank()) {
                cb.beginText();
                try {
                    com.lowagie.text.pdf.BaseFont bf = (baseFont != null)
                            ? baseFont
                            : com.lowagie.text.pdf.BaseFont.createFont();
                    cb.setFontAndSize(bf, 8f); // nhỏ hơn trước
                } catch (com.lowagie.text.DocumentException e) {
                    System.err.println("header BaseFont DocumentException: " + e.getMessage());
                } catch (java.io.IOException e) {
                    System.err.println("header BaseFont IOException: " + e.getMessage());
                }
                cb.showTextAligned(com.lowagie.text.Element.ALIGN_LEFT, tournamentName, left,
                        top, 0);
                cb.endText();
            }
        }
    }

    private void deleteBracketAndResults(int soDo) {
        int idGiai = prefs.getInt("selectedGiaiDauId", -1);
        NoiDung nd = selectedNoiDung;
        if (idGiai <= 0 || nd == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn giải hoặc nội dung", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xoá ?",
                "Xác nhận xoá", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean isTeam = Boolean.TRUE.equals(nd.getTeam());
        int idNoiDung = nd.getId();

        try {
            // 1) Xoá toàn bộ bản ghi sơ đồ theo loại nội dung
            if (isTeam) {
                List<SoDoDoi> olds = soDoDoiService.listAll(idGiai, idNoiDung);
                for (SoDoDoi r : olds) {
                    try {
                        soDoDoiService.delete(idGiai, idNoiDung, r.getViTri());
                    } catch (Exception ignore) {
                    }
                }
            } else {
                List<SoDoCaNhan> olds = soDoCaNhanService.listAll(idGiai, idNoiDung);
                for (SoDoCaNhan r : olds) {
                    try {
                        soDoCaNhanService.delete(idGiai, idNoiDung, r.getViTri());
                    } catch (Exception ignore) {
                    }
                }
            }

            // 2) Xoá kết quả huy chương (ranks 1,2,3)
            for (int rank : new int[] { 1, 2, 3 }) {
                try {
                    if (isTeam) {
                        ketQuaDoiService.delete(idGiai, idNoiDung, rank);
                    } else {
                        ketQuaCaNhanService.delete(idGiai, idNoiDung, rank);
                    }
                } catch (Exception ignore) {
                }
            }

            // 3) Xoá danh sách bốc thăm (draws)
            try {
                if (isTeam) {
                    var drawList = bocThamService.list(idGiai, idNoiDung, soDo);
                    for (var r : drawList) {
                        try {
                            bocThamService.delete(idGiai, idNoiDung, r.getThuTu());
                        } catch (Exception ignore) {
                        }
                    }
                } else {
                    var drawList = bocThamCaNhanService.list(idGiai, idNoiDung, soDo);
                    for (var r : drawList) {
                        try {
                            bocThamCaNhanService.delete(idGiai, idNoiDung, r.getIdVdv());
                        } catch (Exception ignore) {
                        }
                    }
                }
            } catch (Exception ignore) {
            }

            // 4) Dọn UI: xoá text ô, reset bảng huy chương và cache
            clearAllSlots();
            refreshMedalTable("", "", "", "");
            lastSavedMedalKey = null;

            JOptionPane.showMessageDialog(this, "Đã xoá sơ đồ, kết quả và bốc thăm (nếu có)", "Hoàn tất",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi xoá: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveBracket() {
        int idGiai = prefs.getInt("selectedGiaiDauId", -1);
        NoiDung nd = selectedNoiDung;
        if (idGiai <= 0 || nd == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn giải hoặc nội dung", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idNoiDung = nd.getId();
        try {
            boolean isTeam = Boolean.TRUE.equals(nd.getTeam());
            if (isTeam) {
                // Xóa toàn bộ sơ đồ cũ (đội)
                List<SoDoDoi> olds = soDoDoiService.listAll(idGiai, idNoiDung);
                for (SoDoDoi r : olds) {
                    try {
                        soDoDoiService.delete(idGiai, idNoiDung, r.getViTri());
                    } catch (Exception ignore) {
                    }
                }
                // Lưu các ô đang hiển thị (đội)
                LocalDateTime now = LocalDateTime.now();
                for (BracketCanvas.Slot s : canvas.getSlots()) {
                    if (s.text != null && !s.text.isBlank()) {
                        // Hiển thị đang ở dạng "TEN_TEAM - TEN_CLB" => tách lấy TEN_TEAM để tra ID_CLB
                        String teamName = extractTeamNameFromDisplay(s.text.trim());
                        Integer idClb = null;
                        try {
                            int found = doiService.getIdClbByTeamName(teamName, idNoiDung, idGiai);
                            if (found > 0)
                                idClb = found;
                        } catch (RuntimeException ignored) {
                        }
                        Integer soDo = s.col;
                        try {
                            if (idClb != null)
                                soDo = bocThamService.getSoDo(idGiai, idNoiDung, idClb);
                        } catch (RuntimeException ignored) {
                        }
                        // Lưu chỉ tên đội (không bao gồm tên CLB sau dấu -)
                        soDoDoiService.create(idGiai, idNoiDung, idClb, teamName, s.x, s.y, s.order, soDo, now,
                                null, null);
                    }
                }
            } else {
                // Xóa toàn bộ sơ đồ cũ (cá nhân)
                List<SoDoCaNhan> olds = soDoCaNhanService.listAll(idGiai, idNoiDung);
                for (SoDoCaNhan r : olds) {
                    try {
                        soDoCaNhanService.delete(idGiai, idNoiDung, r.getViTri());
                    } catch (Exception ignore) {
                    }
                }
                // Map name -> idVdv từ đăng kí cá nhân
                java.util.Map<String, Integer> nameToId = vdvService.loadSinglesNames(idNoiDung, idGiai);
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                for (BracketCanvas.Slot s : canvas.getSlots()) {
                    if (s.text != null && !s.text.isBlank()) {
                        Integer idVdv = resolveVdvIdFromDisplay(nameToId, s.text.trim());
                        if (idVdv == null)
                            continue; // bỏ qua nếu không xác định được
                        Integer soDo = s.col;
                        try {
                            var bt = bocThamCaNhanService.getOne(idGiai, idNoiDung, idVdv);
                            if (bt != null && bt.getSoDo() != null)
                                soDo = bt.getSoDo();
                        } catch (RuntimeException ignore) {
                        }
                        soDoCaNhanService.create(idGiai, idNoiDung, idVdv, s.x, s.y, s.order, soDo, now, null, null);
                    }
                }
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi lưu sơ đồ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ===================== DB wiring ===================== */
    private void loadNoiDungOptions() {
        try {
            Integer idGiai = prefs.getInt("selectedGiaiDauId", -1);
            List<NoiDung> all = noiDungService.getNoiDungByTuornament(idGiai);
            noiDungList.clear();
            if (all != null)
                noiDungList.addAll(all);
            if (!noiDungList.isEmpty())
                selectedNoiDung = noiDungList.get(0);
            if (pendingSelectNoiDungId != null) {
                for (NoiDung it : noiDungList) {
                    if (it != null && it.getId() != null && it.getId().equals(pendingSelectNoiDungId)) {
                        selectedNoiDung = it;
                        break;
                    }
                }
                pendingSelectNoiDungId = null;
            }
            updateNoiDungLabelText();
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải nội dung: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateGiaiLabel() {
        int idGiai = prefs.getInt("selectedGiaiDauId", -1);
        String ten = prefs.get("selectedGiaiDauName", null);
        if (idGiai > 0) {
            lblGiai.setText("Giải: " + (ten != null && !ten.isBlank() ? ten : ("ID=" + idGiai)));
        } else {
            lblGiai.setText("Giải: (chưa chọn)");
        }
    }

    private void loadFromBocTham() {
        int idGiai = prefs.getInt("selectedGiaiDauId", -1);
        NoiDung nd = selectedNoiDung;
        int soDo = chiTietGiaiDauService.findSoDo(idGiai, nd.getId());
        if (idGiai <= 0 || nd == null) {
            return;
        }

        boolean isTeam = Boolean.TRUE.equals(nd.getTeam());
        if (isTeam) {
            List<BocThamDoi> list;
            try {
                list = bocThamService.list(idGiai, nd.getId(), soDo);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tải sơ đồ từ DB: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Tính toán số lượng sơ đồ dựa trên tổng số VĐV và kích thước sơ đồ
            int totalTeams = list.size();

            // Xác định kích thước tối thích hợp cho sơ đồ dựa trên tổng số đội
            int bracketSize;
            int maxPerBracket;
            if (totalTeams > 32) {
                bracketSize = 64;
                maxPerBracket = 64;
                canvas.setBracketSize(64);
            } else if (totalTeams > 16) {
                bracketSize = 32;
                maxPerBracket = 32;
                canvas.setBracketSize(32);
            } else {
                bracketSize = 16;
                maxPerBracket = 16;
                canvas.setBracketSize(16);
            }

            // Chỉ lấy dữ liệu cho sơ đồ hiện tại
            List<BocThamDoi> bracketData = new ArrayList<>();
            bracketData = list;

            // Decide seeding column and block size based on number of participants
            int N = bracketData.size();
            int M; // block size = full bracket slots
            int seedCol; // 1..columns

            // M = bracketSize (full bracket)
            M = bracketSize;

            // Xác định seedCol dựa trên số lượng VĐV
            if (N >= bracketSize / 2) {
                seedCol = 1;
            } else if (N >= bracketSize / 4) {
                seedCol = 2;
            } else if (N >= bracketSize / 8) {
                seedCol = 3;
            } else {
                seedCol = Math.max(1, Math.min(4, canvas.getColumns() - 1));
            }

            // Tính kích thước column thực tế dựa trên seedCol
            // Col 1: bracketSize, Col 2: bracketSize/2, Col 3: bracketSize/4, ...
            int columnSize = bracketSize / (int) Math.pow(2, seedCol - 1);
            int useN = Math.min(N, columnSize);
            List<Integer> pos = computeSeedPositionsWithMode(useN, columnSize);
            int[] slotToEntry = new int[columnSize];
            Arrays.fill(slotToEntry, -1);
            for (int i = 0; i < useN; i++) {
                int posIdx = pos.get(i);
                if (posIdx >= 0 && posIdx < columnSize) {
                    slotToEntry[posIdx] = i;
                }
            }
            if (prefs.getBool("bracket.seed.avoidSameClub", true)) {
                adjustAssignmentsToAvoidSameClubTeams(slotToEntry, bracketData, nd.getId(), idGiai);
                ensureBalanceConstraint(slotToEntry);
            }
            List<String> namesByT = new ArrayList<>();
            for (int t = 0; t < M; t++)
                namesByT.add(null);
            for (int t = 0; t < columnSize; t++) {
                int i = slotToEntry[t];
                if (i < 0 || i >= N)
                    continue;
                BocThamDoi row = bracketData.get(i);
                String team = row.getTenTeam() != null ? row.getTenTeam().trim() : "";
                String club = "";
                try {
                    Integer idClb = row.getIdClb();
                    if (idClb == null && !team.isBlank()) {
                        idClb = doiService.getIdClbByTeamName(team, nd.getId(), idGiai);
                    }
                    if (idClb != null) {
                        var c = clbService.findOne(idClb);
                        if (c != null && c.getTenClb() != null)
                            club = c.getTenClb().trim();
                    }
                } catch (RuntimeException ignore) {
                }
                String display = club.isBlank() ? team : (team + " - " + club);
                namesByT.set(t, display);
            }
            canvas.clearTextOverrides();
            if (M >= 2) {
                int pairCount = M / 2;
                for (int p = 0; p < pairCount; p++) {
                    String a = namesByT.get(2 * p);
                    String b = namesByT.get(2 * p + 1);
                    boolean hasA = a != null && !a.isBlank();
                    boolean hasB = b != null && !b.isBlank();
                    if (hasA ^ hasB) {
                        String winner = hasA ? a : b;
                        namesByT.set(2 * p, null);
                        namesByT.set(2 * p + 1, null);
                        if (seedCol < canvas.getColumns())
                            canvas.setTextOverride(seedCol + 1, p, winner);
                    }
                }
            }
            // Clear any previous score labels when reseeding from draw
            canvas.clearScoreOverrides();
            canvas.setParticipantsForColumn(namesByT, seedCol);
            canvas.repaint();
            updateMedalsFromCanvas();
        } else {
            List<BocThamCaNhan> list;
            try {
                list = bocThamCaNhanService.list(idGiai, nd.getId(), soDo);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tải bốc thăm cá nhân: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Tính toán số lượng sơ đồ dựa trên tổng số VĐV cá nhân và kích thước sơ đồ
            int totalSingles = list.size();

            // Xác định kích thước tối thích hợp cho sơ đồ dựa trên tổng số VĐV cá nhân
            int bracketSize;
            int maxPerBracket;
            if (totalSingles > 32) {
                bracketSize = 64;
                maxPerBracket = 64;
                canvas.setBracketSize(64);
            } else if (totalSingles > 16) {
                bracketSize = 32;
                maxPerBracket = 32;
                canvas.setBracketSize(32);
            } else {
                bracketSize = 16;
                maxPerBracket = 16;
                canvas.setBracketSize(16);
            }
            // Chỉ lấy dữ liệu cho sơ đồ hiện tại
            List<BocThamCaNhan> bracketData = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                if (i >= 0 && i < list.size()) {
                    bracketData.add(list.get(i));
                }
            }
            int N = bracketData.size();
            int M;
            int seedCol;

            // M = bracketSize (full bracket)
            M = bracketSize;

            // Xác định seedCol dựa trên số lượng VĐV cá nhân
            if (N >= bracketSize / 2) {
                seedCol = 1;
            } else if (N >= bracketSize / 4) {
                seedCol = 2;
            } else if (N >= bracketSize / 8) {
                seedCol = 3;
            } else {
                seedCol = Math.max(1, Math.min(4, canvas.getColumns() - 1));
            }

            // Tính kích thước column thực tế dựa trên seedCol
            // Col 1: bracketSize, Col 2: bracketSize/2, Col 3: bracketSize/4, ...
            int columnSize = bracketSize / (int) Math.pow(2, seedCol - 1);
            int useN = Math.min(N, columnSize);
            List<Integer> pos = computeSeedPositionsWithMode(useN, columnSize);
            int[] slotToEntry = new int[columnSize];
            java.util.Arrays.fill(slotToEntry, -1);
            for (int i = 0; i < useN; i++) {
                int posIdx = pos.get(i);
                if (posIdx >= 0 && posIdx < columnSize) {
                    slotToEntry[posIdx] = i;
                }
            }
            if (prefs.getBool("bracket.seed.avoidSameClub", true)) {
                adjustAssignmentsToAvoidSameClubSingles(slotToEntry, bracketData);
                // Ensure balance constraint is maintained after anti-CLB adjustments
                ensureBalanceConstraint(slotToEntry);
            }
            List<String> namesByT = new ArrayList<>();
            for (int t = 0; t < M; t++)
                namesByT.add(null);
            for (int t = 0; t < columnSize; t++) {
                int i = slotToEntry[t];
                if (i < 0 || i >= N)
                    continue;
                var row = bracketData.get(i);
                String display;
                try {
                    var vdv = vdvService.findOne(row.getIdVdv());
                    String name = vdv.getHoTen() != null ? vdv.getHoTen().trim() : ("VDV#" + row.getIdVdv());
                    String club = vdvService.getClubNameById(row.getIdVdv());
                    display = (club != null && !club.isBlank()) ? (name + " - " + club.trim()) : name;
                } catch (RuntimeException ignore) {
                    display = "VDV#" + row.getIdVdv();
                }
                namesByT.set(t, display);
            }
            canvas.clearTextOverrides();
            if (M >= 2) {
                int pairCount = M / 2;
                for (int p = 0; p < pairCount; p++) {
                    String a = namesByT.get(2 * p);
                    String b = namesByT.get(2 * p + 1);
                    boolean hasA = a != null && !a.isBlank();
                    boolean hasB = b != null && !b.isBlank();
                    if (hasA ^ hasB) {
                        String winner = hasA ? a : b;
                        namesByT.set(2 * p, null);
                        namesByT.set(2 * p + 1, null);
                        if (seedCol < canvas.getColumns())
                            canvas.setTextOverride(seedCol + 1, p, winner);
                    }
                }
            }
            canvas.clearScoreOverrides();
            canvas.setParticipantsForColumn(namesByT, seedCol);
            canvas.repaint();
            updateMedalsFromCanvas();
        }
    }

    private boolean loadSavedSoDo() {
        int idGiai = prefs.getInt("selectedGiaiDauId", -1);
        NoiDung nd = selectedNoiDung;
        if (idGiai <= 0 || nd == null)
            return false;
        boolean isTeam = Boolean.TRUE.equals(nd.getTeam());
        if (isTeam) {
            List<SoDoDoi> list;
            try {
                list = soDoDoiService.listAll(idGiai, nd.getId());
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tải sơ đồ đã lưu: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (list == null || list.isEmpty())
                return false;

            // Xác định kích thước sơ đồ dựa trên maxViTri (vị trí lớn nhất) từ dữ liệu đã
            // lưu
            int maxViTri = 0;
            for (SoDoDoi r : list) {
                if (r.getViTri() != null && r.getViTri() > maxViTri) {
                    maxViTri = r.getViTri();
                }
            }

            int bracketSize;
            if (maxViTri >= 63) {
                bracketSize = 64;
            } else if (maxViTri >= 31) {
                bracketSize = 32;
            } else {
                bracketSize = 16;
            }
            canvas.setBracketSize(bracketSize);

            // Không phân chia sơ đồ nữa - tải hết tất cả dữ liệu
            List<SoDoDoi> bracketData = new ArrayList<>(list);

            if (bracketData.isEmpty())
                return false;

            // Detect saved bracket size by max order; 64-seed tree has 127 slots, 32-seed
            // has 63, 16-seed has 31
            int maxOrder = 0;
            for (SoDoDoi r : bracketData) {
                if (r.getViTri() != null && r.getViTri() > maxOrder)
                    maxOrder = r.getViTri();
            }
            // Cập nhật canvas size nếu cần dựa trên maxOrder
            int detectedSize = maxOrder > 63 ? 64 : (maxOrder > 31 ? 32 : 16);
            if (detectedSize != bracketSize) {
                canvas.setBracketSize(detectedSize);
                bracketSize = detectedSize;
            }
            List<String> blanks = new ArrayList<>();
            // Tính seedSpots dựa trên bracketSize: 16-seed = 16, 32-seed = 32, 64-seed = 64
            int seedSpots = bracketSize;
            for (int i = 0; i < seedSpots; i++)
                blanks.add("");
            canvas.clearTextOverrides();
            canvas.clearScoreOverrides();
            canvas.setParticipantsForColumn(blanks, 1);
            for (SoDoDoi r : bracketData) {
                BracketCanvas.Slot slot = canvas.findByOrder(r.getViTri());
                if (slot != null) {
                    // Create display text with club name for UI, but keep team name in DB
                    String team = r.getTenTeam() != null ? r.getTenTeam().trim() : "";
                    String display = team;
                    if (!team.isBlank()) {
                        try {
                            Integer idClb = doiService.getIdClbByTeamName(team, nd.getId(), idGiai);
                            if (idClb != null) {
                                var club = clbService.findOne(idClb);
                                if (club != null && club.getTenClb() != null && !club.getTenClb().trim().isEmpty()) {
                                    display = team + " - " + club.getTenClb().trim();
                                }
                            }
                        } catch (RuntimeException ignore) {
                        }
                    }
                    canvas.setTextOverride(slot.col, slot.thuTu, display);
                    try {
                        Integer diem = r.getDiem();
                        if (diem != null) {
                            canvas.setScoreOverride(slot.col, slot.thuTu, String.valueOf(diem));
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
            try {
                canvas.refreshAfterOverrides();
                canvas.repaint();
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                System.err.println("ERROR in loadSavedSoDo (team) refreshAfterOverrides: " + aioobe.getMessage());
                aioobe.printStackTrace(System.err);
                JOptionPane.showMessageDialog(this, "Lỗi load sơ đồ đội: " + aioobe.toString(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            updateMedalsFromCanvas();
            return true;
        } else {
            List<SoDoCaNhan> list;
            try {
                list = soDoCaNhanService.listAll(idGiai, nd.getId());
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tải sơ đồ (cá nhân) đã lưu: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (list == null || list.isEmpty())
                return false;

            // Xác định kích thước sơ đồ dựa trên maxViTri (vị trí lớn nhất) từ dữ liệu đã
            // lưu
            int maxViTri = 0;
            for (SoDoCaNhan r : list) {
                if (r.getViTri() != null && r.getViTri() > maxViTri) {
                    maxViTri = r.getViTri();
                }
            }

            int bracketSize;
            if (maxViTri >= 63) {
                bracketSize = 64;
            } else if (maxViTri >= 31) {
                bracketSize = 32;
            } else {
                bracketSize = 16;
            }
            canvas.setBracketSize(bracketSize);

            // Không phân chia sơ đồ nữa - tải hết tất cả dữ liệu
            List<SoDoCaNhan> bracketData = new ArrayList<>(list);

            List<String> blanks = new ArrayList<>();
            // Tính seedSpots dựa trên bracketSize: 16-seed = 16, 32-seed = 32, 64-seed = 64
            int seedSpots = bracketSize;
            for (int i = 0; i < seedSpots; i++)
                blanks.add("");
            canvas.clearTextOverrides();
            canvas.clearScoreOverrides();
            canvas.setParticipantsForColumn(blanks, 1);
            for (SoDoCaNhan r : bracketData) {
                BracketCanvas.Slot slot = canvas.findByOrder(r.getViTri());
                if (slot != null) {
                    String display;
                    try {
                        var vdv = vdvService.findOne(r.getIdVdv());
                        String name = vdv.getHoTen() != null ? vdv.getHoTen().trim() : ("VDV#" + r.getIdVdv());
                        String club = vdvService.getClubNameById(r.getIdVdv());
                        display = (club != null && !club.isBlank()) ? (name + " - " + club.trim()) : name;
                    } catch (RuntimeException ignore) {
                        display = "VDV#" + r.getIdVdv();
                    }
                    canvas.setTextOverride(slot.col, slot.thuTu, display);
                    try {
                        Integer diem = r.getDiem();
                        if (diem != null) {
                            canvas.setScoreOverride(slot.col, slot.thuTu, String.valueOf(diem));
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
            try {
                canvas.refreshAfterOverrides();
                canvas.repaint();
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                System.err.println("ERROR in loadSavedSoDo (singles) refreshAfterOverrides: " + aioobe.getMessage());
                aioobe.printStackTrace(System.err);
                JOptionPane.showMessageDialog(this, "Lỗi load sơ đồ cá nhân: " + aioobe.toString(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            updateMedalsFromCanvas();
            return true;
        }
    }

    private void loadBestAvailable() {
        boolean have = loadSavedSoDo();
        if (!have) {
            loadFromBocTham();
        }
        updateMedalsFromCanvas();
        canvas.repaint();
    }

    private void clearAllSlots() {
        // Giữ cấu trúc, xóa text
        List<String> blanks = new ArrayList<>();
        int seedSpots = canvas.getSeedSpotsCount();
        for (int i = 0; i < seedSpots; i++)
            blanks.add("");
        canvas.setParticipantsForColumn(blanks, 1);
        canvas.clearTextOverrides();
        canvas.repaint();
        updateMedalsFromCanvas();
    }

    // Top-heavy fill: always put ceil(n/2) to top half first, then bottom half
    private static List<Integer> computeTopHeavyPositionsWithinBlock(int N, int M) {
        List<Integer> out = new ArrayList<>(N);
        fillTopHeavy(0, M, N, out);
        return out;
    }

    // Compute seeding positions based on selected mode; enforce that for any N,
    // the top half has >= bottom half and differs by at most 1 entrant.
    private List<Integer> computeSeedPositionsWithMode(int N, int M) {
        int mode = Math.max(1, Math.min(7, prefs.getInt("bracket.seed.mode", 2)));
        List<Integer> base;
        switch (M) {
            case 8 -> {
                // 0-based slot order per entrant index i = 0..N-1
                int[][] modes = new int[][] {
                        { 0, 4, 2, 6, 1, 5, 3, 7 }, // Mode 1: [1,5,3,7,2,6,4,8]
                        { 7, 3, 5, 1, 6, 2, 4, 0 }, // Mode 2: [8,4,6,2,7,3,5,1]
                        { 0, 7, 4, 3, 2, 5, 6, 1 }, // Mode 3: [1,8,5,4,3,6,7,2]
                        { 0, 1, 2, 3, 4, 5, 6, 7 }, // Mode 4: natural
                        { 7, 6, 5, 4, 3, 2, 1, 0 }, // Mode 5: reverse
                        { 7, 2, 5, 1, 6, 3, 4, 0 }, // Mode 6: variant
                        { 0, 7, 3, 4, 1, 6, 2, 5 } // Mode 7: variant
                };
                int[] arr = modes[mode - 1];
                base = balancedFromFullMapping(arr, M, N);
            }
            case 4 -> {
                int[][] modes = new int[][] {
                        { 0, 2, 1, 3 }, // 1,3,2,4
                        { 3, 1, 2, 0 },
                        { 0, 3, 1, 2 },
                        { 0, 1, 2, 3 },
                        { 3, 2, 1, 0 },
                        { 2, 0, 3, 1 },
                        { 1, 3, 0, 2 }
                };
                int[] arr = modes[mode - 1];
                base = balancedFromFullMapping(arr, M, N);
            }
            case 2 -> {
                int[][] modes = new int[][] {
                        { 0, 1 }, { 1, 0 }, { 0, 1 }, { 0, 1 }, { 1, 0 }, { 0, 1 }, { 1, 0 }
                };
                int[] arr = modes[mode - 1];
                base = balancedFromFullMapping(arr, M, N);
            }
            case 16 -> // For 16, fallback to top-heavy which is generally balanced; anti-CLB step will
                // adjust pairs
                base = computeTopHeavyPositionsWithinBlock(N, M);
            default -> base = computeTopHeavyPositionsWithinBlock(N, M);
        }
        return base;
    }

    // Build a balanced order from a full mapping over M slots so that for any
    // prefix
    // of length k (1..N), top half gets ceil(k/2) and bottom half gets floor(k/2),
    // preserving relative order within each half per the given mapping.
    private static List<Integer> balancedFromFullMapping(int[] fullMapping, int M, int N) {
        List<Integer> top = new ArrayList<>(M / 2);
        List<Integer> bot = new ArrayList<>(M / 2);
        int half = M / 2;
        for (int pos : fullMapping) {
            if (pos < half)
                top.add(pos);
            else
                bot.add(pos);
        }
        List<Integer> out = new ArrayList<>(Math.min(N, M));
        int ti = 0, bi = 0;
        for (int k = 0; k < Math.min(N, M); k++) {
            boolean pickTop = (k % 2 == 0);
            if (pickTop) {
                if (ti < top.size())
                    out.add(top.get(ti++));
                else if (bi < bot.size())
                    out.add(bot.get(bi++));
            } else {
                if (bi < bot.size())
                    out.add(bot.get(bi++));
                else if (ti < top.size())
                    out.add(top.get(ti++));
            }
        }
        return out;
    }

    // ===== Anti-CLB adjustment (greedy swap) =====
    private void adjustAssignmentsToAvoidSameClubTeams(int[] slotToEntry, List<BocThamDoi> list, int idNoiDung,
            int idGiai) {
        if (slotToEntry == null || list == null || list.isEmpty()) {
            return;
        }

        // Precompute club id per entry index
        int[] clubs = new int[list.size()];
        Arrays.fill(clubs, 0);
        for (int i = 0; i < list.size(); i++) {
            try {
                BocThamDoi row = list.get(i);
                if (row == null)
                    continue;

                Integer idClb = row.getIdClb();
                if ((idClb == null || idClb <= 0) && row.getTenTeam() != null && !row.getTenTeam().isBlank()) {
                    int found = doiService.getIdClbByTeamName(row.getTenTeam().trim(), idNoiDung, idGiai);
                    if (found > 0)
                        idClb = found;
                }
                clubs[i] = (idClb != null) ? idClb : 0;
            } catch (RuntimeException ignore) {
                clubs[i] = 0;
            }
        }
        greedyAvoidSameClub(slotToEntry, clubs);
    }

    private void adjustAssignmentsToAvoidSameClubSingles(int[] slotToEntry,
            List<BocThamCaNhan> list) {
        if (slotToEntry == null || list == null || list.isEmpty()) {
            return;
        }

        int[] clubs = new int[list.size()];
        Arrays.fill(clubs, 0);
        for (int i = 0; i < list.size(); i++) {
            try {
                var row = list.get(i);
                if (row == null)
                    continue;

                var vdv = vdvService.findOne(row.getIdVdv());
                Integer clubId = (vdv != null) ? vdv.getIdClb() : null;
                clubs[i] = (clubId != null) ? clubId : 0;
            } catch (RuntimeException ignore) {
                clubs[i] = 0;
            }
        }
        greedyAvoidSameClub(slotToEntry, clubs);
    }

    private void greedyAvoidSameClub(int[] slotToEntry, int[] clubsByEntry) {
        int M = slotToEntry.length;
        if (M < 2 || clubsByEntry.length == 0)
            return;

        // Evaluate current clashes
        IntSupplier clashCount = () -> {
            int cnt = 0;
            for (int p = 0; p < M / 2; p++) {
                int aSlot = 2 * p, bSlot = 2 * p + 1;
                int a = slotToEntry[aSlot], b = slotToEntry[bSlot];
                if (a >= 0 && b >= 0 && a < clubsByEntry.length && b < clubsByEntry.length
                        && clubsByEntry[a] != 0 && clubsByEntry[a] == clubsByEntry[b]) {
                    cnt++;
                }
            }
            return cnt;
        };

        int attempts = 0;
        int initialClashes = clashCount.getAsInt();
        if (initialClashes == 0)
            return; // No clashes to resolve

        while (clashCount.getAsInt() > 0 && attempts < 64) { // small cap to avoid infinite loops
            boolean improved = false;
            int currentClashes = clashCount.getAsInt();

            for (int p = 0; p < M / 2; p++) {
                int aSlot = 2 * p, bSlot = 2 * p + 1;
                int a = slotToEntry[aSlot], b = slotToEntry[bSlot];
                if (!(a >= 0 && b >= 0))
                    continue;
                if (a >= clubsByEntry.length || b >= clubsByEntry.length)
                    continue;
                int cl = clubsByEntry[a];
                int cr = clubsByEntry[b];
                if (cl == 0 || cr == 0 || cl != cr)
                    continue; // no clash
                int before = clashCount.getAsInt();

                // Try swap b with any other slot s to reduce clashes
                // BUT maintain balance: check all levels before allowing swap
                for (int s = 0; s < M; s++) {
                    if (s == aSlot || s == bSlot)
                        continue;

                    // Try the swap temporarily
                    swap(slotToEntry, s, bSlot);

                    // Check if this swap violates balance constraint at any level
                    boolean violatesBalance = wouldViolateBalanceConstraint(slotToEntry, 0, M);

                    if (!violatesBalance) {
                        // Swap is safe - check if it reduces clashes
                        int after = clashCount.getAsInt();
                        if (after < before) {
                            improved = true;
                            break; // Keep the swap
                        }
                    }

                    // Revert the swap (either it violates balance or doesn't improve clashes)
                    swap(slotToEntry, s, bSlot);
                }
                if (improved)
                    break;
            }

            // If no improvement possible, break to avoid infinite loop
            if (!improved || clashCount.getAsInt() >= currentClashes)
                break;
            attempts++;
        }
    }

    private void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    private boolean wouldViolateBalanceConstraint(int[] slotToEntry, int start, int size) {
        if (slotToEntry == null || size <= 1 || start < 0 || start >= slotToEntry.length)
            return false;

        int halfSize = size / 2;
        int leftStart = start;
        int rightStart = start + halfSize;

        // Ensure bounds are valid
        if (rightStart + halfSize > slotToEntry.length)
            return false;

        // Count entries in left and right halves
        int leftCount = 0, rightCount = 0;
        for (int i = leftStart; i < leftStart + halfSize; i++) {
            if (i < slotToEntry.length && slotToEntry[i] >= 0)
                leftCount++;
        }
        for (int i = rightStart; i < rightStart + halfSize; i++) {
            if (i < slotToEntry.length && slotToEntry[i] >= 0)
                rightCount++;
        }

        // Check balance constraint: left should be >= right
        if (leftCount < rightCount) {
            return true; // Violates constraint
        }

        // Recursively check sub-brackets
        return wouldViolateBalanceConstraint(slotToEntry, leftStart, halfSize) ||
                wouldViolateBalanceConstraint(slotToEntry, rightStart, halfSize);
    }

    // Verify and fix balance constraint after anti-CLB adjustments
    private void ensureBalanceConstraint(int[] slotToEntry) {
        if (slotToEntry == null)
            return;

        int M = slotToEntry.length;
        if (M < 2)
            return;

        // Apply balance constraint recursively at all levels
        try {
            ensureBalanceConstraintRecursive(slotToEntry, 0, M);
        } catch (Exception e) {
            // Fallback to simple top-level balance if recursive fails
            System.err.println("Recursive balance failed, using simple balance: " + e.getMessage());
            ensureSimpleBalance(slotToEntry);
        }
    }

    private void ensureSimpleBalance(int[] slotToEntry) {
        int M = slotToEntry.length;
        int halfSize = M / 2;
        int topCount = 0, bottomCount = 0;

        for (int i = 0; i < M; i++) {
            if (slotToEntry[i] >= 0) {
                if (i < halfSize)
                    topCount++;
                else
                    bottomCount++;
            }
        }

        while (topCount < bottomCount) {
            int bottomSlot = -1;
            for (int i = halfSize; i < M; i++) {
                if (slotToEntry[i] >= 0) {
                    bottomSlot = i;
                    break;
                }
            }
            if (bottomSlot == -1)
                break;

            int topSlot = -1;
            for (int i = 0; i < halfSize; i++) {
                if (slotToEntry[i] < 0) {
                    topSlot = i;
                    break;
                }
            }
            if (topSlot == -1)
                break;

            slotToEntry[topSlot] = slotToEntry[bottomSlot];
            slotToEntry[bottomSlot] = -1;
            topCount++;
            bottomCount--;
        }
    }

    private void ensureBalanceConstraintRecursive(int[] slotToEntry, int start, int size) {
        if (size <= 1)
            return;

        int halfSize = size / 2;
        int leftStart = start;
        int rightStart = start + halfSize;

        // Count entries in left and right halves
        int leftCount = 0, rightCount = 0;
        for (int i = leftStart; i < leftStart + halfSize; i++) {
            if (slotToEntry[i] >= 0)
                leftCount++;
        }
        for (int i = rightStart; i < rightStart + halfSize; i++) {
            if (slotToEntry[i] >= 0)
                rightCount++;
        }

        // Fix if left < right (violates constraint: left should be >= right)
        while (leftCount < rightCount) {
            // Find first non-empty slot in right half
            int rightSlot = -1;
            for (int i = rightStart; i < rightStart + halfSize; i++) {
                if (slotToEntry[i] >= 0) {
                    rightSlot = i;
                    break;
                }
            }
            if (rightSlot == -1)
                break; // No entries to move

            // Find first empty slot in left half
            int leftSlot = -1;
            for (int i = leftStart; i < leftStart + halfSize; i++) {
                if (slotToEntry[i] < 0) {
                    leftSlot = i;
                    break;
                }
            }
            if (leftSlot == -1)
                break; // No empty slots in left

            // Move entry from right to left
            slotToEntry[leftSlot] = slotToEntry[rightSlot];
            slotToEntry[rightSlot] = -1;
            leftCount++;
            rightCount--;
        }

        // Recursively apply to sub-brackets
        ensureBalanceConstraintRecursive(slotToEntry, leftStart, halfSize);
        ensureBalanceConstraintRecursive(slotToEntry, rightStart, halfSize);
    }

    private static void fillTopHeavy(int start, int block, int n, List<Integer> out) {
        if (n <= 0)
            return;
        if (block == 1) {
            out.add(start);
            return;
        }
        int half = block / 2;
        int nTop = (n + 1) / 2; // ceil
        int nBot = n - nTop; // floor
        fillTopHeavy(start, half, nTop, out);
        fillTopHeavy(start + half, half, nBot, out);
    }

    // ===== Medal logic =====
    private void updateMedalsFromCanvas() {
        // Dynamic columns: finals at last column, finalists at last-1, semis inputs at
        // last-2
        int finalsCol = Math.max(1, canvas.getColumns());
        int finalistsCol = Math.max(1, finalsCol - 1);
        int semisInputCol = Math.max(1, finalsCol - 2);

        String gold = getTextAt(finalsCol, 0);
        String finalistA = getTextAt(finalistsCol, 0);
        String finalistB = getTextAt(finalistsCol, 1);
        String silver = null;
        if (gold != null && !gold.isBlank()) {
            if (finalistA != null && finalistA.equals(gold))
                silver = finalistB;
            else if (finalistB != null && finalistB.equals(gold))
                silver = finalistA;
        }
        // Bronzes are losers of the two semifinals: each semifinal pair is in
        // semisInputCol
        String semiA1 = getTextAt(semisInputCol, 0);
        String semiA2 = getTextAt(semisInputCol, 1);
        String semiB1 = getTextAt(semisInputCol, 2);
        String semiB2 = getTextAt(semisInputCol, 3);
        String bronze1 = null;
        String bronze2 = null;
        if (finalistA != null && !finalistA.isBlank()) {
            if (finalistA.equals(semiA1))
                bronze1 = semiA2;
            else if (finalistA.equals(semiA2))
                bronze1 = semiA1;
        }
        if (finalistB != null && !finalistB.isBlank()) {
            if (finalistB.equals(semiB1))
                bronze2 = semiB2;
            else if (finalistB.equals(semiB2))
                bronze2 = semiB1;
        }
        String g = safe(gold).trim();
        String s = safe(silver).trim();
        String b1 = safe(bronze1).trim();
        String b2 = safe(bronze2).trim();
        refreshMedalTable(g, s, b1, b2);

        // Auto-save medals when a champion exists and the snapshot changed
        String snapshotKey = String.join("|", g, s, b1, b2);
        if (!g.isBlank() && !snapshotKey.equals(lastSavedMedalKey)) {
            lastSavedMedalKey = snapshotKey;
            persistMedals(true); // silent
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String getTextAt(int col, int thuTu) {
        // Truy vấn slot hiện tại từ canvas
        for (var s : canvas.getSlots()) {
            if (s.col == col && s.thuTu == thuTu)
                return s.text;
        }
        return null;
    }

    private void refreshMedalTable(String gold, String silver, String bronze1, String bronze2) {
        medalModel.setRowCount(0);
        medalModel.addRow(new Object[] { "Vàng: " + gold });
        medalModel.addRow(new Object[] { "Bạc: " + silver });
        medalModel.addRow(new Object[] { "Đồng: " + bronze1 });
        medalModel.addRow(new Object[] { "Đồng: " + bronze2 });
    }

    // ===== Persist medals =====
    private void saveMedalResults() {
        persistMedals(false);
    }

    // Core implementation for persisting medals. When silent=true, suppress
    // dialogs.
    private void persistMedals(boolean silent) {
        int idGiai = prefs.getInt("selectedGiaiDauId", -1);
        NoiDung nd = selectedNoiDung;
        if (idGiai <= 0 || nd == null) {
            if (!silent) {
                JOptionPane.showMessageDialog(this, "Chưa chọn giải hoặc nội dung", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        int idNoiDung = nd.getId();
        try {
            String vangRaw = extractNameFromMedalRow(0);
            String bacRaw = extractNameFromMedalRow(1);
            String dong1Raw = extractNameFromMedalRow(2);
            String dong2Raw = extractNameFromMedalRow(3);
            MedalSet medals = sanitizeMedals(vangRaw, bacRaw, dong1Raw, dong2Raw);
            String vang = medals.gold;
            String bac = medals.silver;
            String dong1 = medals.bronze1;
            String dong2 = medals.bronze2;
            boolean isTeam = Boolean.TRUE.equals(nd.getTeam());
            if (isTeam) {
                List<KetQuaDoi> items = new ArrayList<>();
                if (!isBlank(vang))
                    items.add(buildKetQua(idGiai, idNoiDung, vang, 1));
                if (!isBlank(bac))
                    items.add(buildKetQua(idGiai, idNoiDung, bac, 2));
                if (!isBlank(dong1))
                    items.add(buildKetQua(idGiai, idNoiDung, dong1, 3));
                if (!isBlank(dong2))
                    items.add(buildKetQua(idGiai, idNoiDung, dong2, 3));
                ketQuaDoiService.replaceMedals(idGiai, idNoiDung, items);
            } else {
                // Singles: replace medals in bulk to allow two bronzes
                java.util.Map<String, Integer> map = vdvService.loadSinglesNames(idNoiDung, idGiai);
                List<com.example.btms.model.result.KetQuaCaNhan> items = new ArrayList<>();
                if (!isBlank(vang)) {
                    Integer idVang = resolveVdvIdFromDisplay(map, vang);
                    if (idVang != null)
                        items.add(new com.example.btms.model.result.KetQuaCaNhan(idGiai, idNoiDung, idVang, 1));
                }
                if (!isBlank(bac)) {
                    Integer idBac = resolveVdvIdFromDisplay(map, bac);
                    if (idBac != null)
                        items.add(new com.example.btms.model.result.KetQuaCaNhan(idGiai, idNoiDung, idBac, 2));
                }
                if (!isBlank(dong1)) {
                    Integer idD1 = resolveVdvIdFromDisplay(map, dong1);
                    if (idD1 != null)
                        items.add(new com.example.btms.model.result.KetQuaCaNhan(idGiai, idNoiDung, idD1, 3));
                }
                if (!isBlank(dong2)) {
                    Integer idD2 = resolveVdvIdFromDisplay(map, dong2);
                    if (idD2 != null)
                        items.add(new com.example.btms.model.result.KetQuaCaNhan(idGiai, idNoiDung, idD2, 3));
                }
                ketQuaCaNhanService.replaceMedals(idGiai, idNoiDung, items);
            }
            if (!silent) {
                JOptionPane.showMessageDialog(this, "Đã lưu kết quả huy chương", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (RuntimeException ex) {
            if (!silent) {
                JOptionPane.showMessageDialog(this, "Lỗi lưu kết quả: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                lastSavedMedalKey = null;
            }
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String extractNameFromMedalRow(int row) {
        if (row < 0 || row >= medalModel.getRowCount())
            return "";
        Object v = medalModel.getValueAt(row, 0);
        if (v == null)
            return "";
        String s = v.toString();
        int idx = s.indexOf(":");
        if (idx >= 0 && idx + 1 < s.length())
            return s.substring(idx + 1).trim();
        return s.trim();
    }

    // Enforce: max 1 gold, 1 silver, 2 bronzes for one nội dung; also remove blanks
    // and duplicates across ranks.
    private static class MedalSet {
        final String gold;
        final String silver;
        final String bronze1;
        final String bronze2;

        MedalSet(String g, String s, String b1, String b2) {
            this.gold = g;
            this.silver = s;
            this.bronze1 = b1;
            this.bronze2 = b2;
        }
    }

    private MedalSet sanitizeMedals(String rawGold, String rawSilver, String rawBronze1, String rawBronze2) {
        String g = safe(rawGold).trim();
        String s = safe(rawSilver).trim();
        String b1 = safe(rawBronze1).trim();
        String b2 = safe(rawBronze2).trim();
        // Remove blanks
        if (g.isBlank())
            g = "";
        if (s.isBlank())
            s = "";
        if (b1.isBlank())
            b1 = "";
        if (b2.isBlank())
            b2 = "";
        // Deduplicate across different ranks: priority order gold > silver > bronze1 >
        // bronze2
        if (!g.isBlank()) {
            if (s.equals(g))
                s = "";
            if (b1.equals(g))
                b1 = "";
            if (b2.equals(g))
                b2 = "";
        }
        if (!s.isBlank()) {
            if (b1.equals(s))
                b1 = "";
            if (b2.equals(s))
                b2 = "";
        }
        // Ensure at most two distinct bronzes
        if (b1.isBlank() && !b2.isBlank()) {
            // normalize to fill bronze1 first
            b1 = b2;
            b2 = "";
        } else if (!b1.isBlank() && !b2.isBlank() && b1.equals(b2)) {
            // drop duplicate
            b2 = "";
        }
        // Update the UI table to reflect sanitized values
        refreshMedalTable(g, s, b1, b2);
        return new MedalSet(g, s, b1, b2);
    }

    private KetQuaDoi buildKetQua(int idGiai, int idNoiDung, String teamDisplay, int thuHang) {
        String teamName = extractTeamNameFromDisplay(teamDisplay);
        Integer idClb = null;
        try {
            idClb = doiService.getIdClbByTeamName(teamName, idNoiDung, idGiai);
        } catch (RuntimeException ignore) {
        }
        if (idClb == null || idClb <= 0) {
            throw new IllegalStateException("Không xác định được CLB cho đội: " + teamDisplay);
        }
        // Chỉ lưu tên đội vào database, không bao gồm tên CLB
        return new KetQuaDoi(idGiai, idNoiDung, idClb, teamName, thuHang);
    }

    private String extractTeamNameFromDisplay(String display) {
        if (display == null)
            return "";
        String teamName = display.trim();
        int sep = teamName.indexOf(" - ");
        if (sep >= 0)
            teamName = teamName.substring(0, sep).trim();
        return teamName;
    }

    private Integer resolveVdvIdFromDisplay(java.util.Map<String, Integer> nameToId, String display) {
        if (display == null)
            return null;
        String name = display;
        int sep = display.indexOf(" - ");
        if (sep >= 0)
            name = display.substring(0, sep).trim();
        return nameToId.get(name);
    }

    /* ===================== Canvas ===================== */
    private class BracketCanvas extends JPanel {
        private int columns = 5;
        private int[] spots = { 16, 8, 4, 2, 1 };
        private static final int CELL_WIDTH = 150;
        private static final int CELL_HEIGHT = 36;
        private static final int INNER_UP_OFFSET = 10;
        private static final int BASE_INNER_RIGHT_OFFSET = 25;
        private static final int START_Y = 10;

        private List<String> participants = new ArrayList<>();
        private int seedColumn = 1;
        private final java.util.Map<Integer, String> textOverrides = new java.util.HashMap<>();
        private final java.util.Map<Integer, String> scoreOverrides = new java.util.HashMap<>();
        private boolean editMode = false;

        // Cache preferred size to avoid recalculating repeatedly
        private Dimension cachedPreferredSize = null;

        private static class Slot {
            int col;
            int thuTu;
            int x;
            int y;
            String text;
            int order;
        }

        private final List<Slot> slots = new ArrayList<>();

        private int cellWidthForCol(int col) {
            if (columns >= 7) {
                return renderingForPdf ? 170 : 150;
            }

            if (columns >= 6) {
                return renderingForPdf ? 170 : 150;
            }

            return renderingForPdf ? 170 : 140;
        }

        private int cellHeightForCol(int col) {
            return CELL_HEIGHT;
        }

        BracketCanvas() {
            setOpaque(true);
            setBackground(Color.WHITE);
            setFont(getFont().deriveFont(Font.PLAIN, 10f)); // giảm font từ 12f xuống 10f
            rebuildSlots();
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!editMode)
                        return;
                    if (e.getClickCount() >= 2 && e.getButton() == MouseEvent.BUTTON1) {
                        Slot s = getSlotAt(e.getPoint());
                        if (s != null) {
                            String newText = JOptionPane.showInputDialog(BracketCanvas.this, "Sửa tên đội:", s.text);
                            if (newText != null) {
                                setTextOverride(s.col, s.thuTu, newText.trim());
                                // cập nhật ngay cả trong cache slots
                                s.text = newText.trim();
                                repaint();
                                SoDoThiDauPanel.this.updateMedalsFromCanvas();
                            }
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    maybeShowContextMenu(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    maybeShowContextMenu(e);
                }

                private void maybeShowContextMenu(MouseEvent e) {
                    if (!editMode)
                        return;
                    if (!e.isPopupTrigger())
                        return;
                    Slot s = getSlotAt(e.getPoint());
                    if (s == null)
                        return;
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem mAdvance = new JMenuItem("Vẽ bản ghi");
                    JMenuItem mBack = new JMenuItem("Xoá bản ghi");
                    JMenuItem mEditScore = new JMenuItem("Sửa điểm...");
                    JMenuItem mClearScore = new JMenuItem("Xoá điểm");
                    mAdvance.setEnabled(s.col < columns && s.text != null && !s.text.isBlank());
                    mBack.setEnabled(s.col > 1 && s.text != null && !s.text.isBlank());
                    mEditScore.addActionListener(ev -> {
                        String current = getScoreOverride(s.col, s.thuTu);
                        String inp = JOptionPane.showInputDialog(BracketCanvas.this,
                                "Nhập điểm (ví dụ: 21-18 | 21-19)", current != null ? current : "");
                        if (inp != null) {
                            setScoreOverride(s.col, s.thuTu, inp);
                            repaint();
                        }
                    });
                    mClearScore.addActionListener(ev -> {
                        setScoreOverride(s.col, s.thuTu, null);
                        repaint();
                    });
                    mAdvance.addActionListener(ev -> {
                        Slot parent = parentOf(s);
                        if (parent != null) {
                            setTextOverride(parent.col, parent.thuTu, s.text);
                            parent.text = s.text;
                            repaint();
                            SoDoThiDauPanel.this.updateMedalsFromCanvas();
                            // lưu vào db theo trạng thái hiện tại của panel
                            int idGiai = SoDoThiDauPanel.this.prefs.getInt("selectedGiaiDauId", -1);
                            NoiDung ndSel = SoDoThiDauPanel.this.selectedNoiDung;
                            if (idGiai > 0 && ndSel != null) {
                                int idNoiDung = ndSel.getId();
                                if (Boolean.TRUE.equals(ndSel.getTeam())) {
                                    Integer idClb = null;
                                    String teamName = "";
                                    try {
                                        teamName = SoDoThiDauPanel.this
                                                .extractTeamNameFromDisplay(s.text.trim());
                                        int found = SoDoThiDauPanel.this.doiService.getIdClbByTeamName(teamName,
                                                idNoiDung, idGiai);
                                        if (found > 0)
                                            idClb = found;
                                    } catch (RuntimeException ignored) {
                                    }
                                    Integer soDo = parent.col;
                                    try {
                                        if (idClb != null)
                                            soDo = SoDoThiDauPanel.this.bocThamService.getSoDo(idGiai, idNoiDung,
                                                    idClb);
                                    } catch (RuntimeException ignored) {
                                    }
                                    try {
                                        // Upsert: update if parent slot already exists, else create
                                        var existing = SoDoThiDauPanel.this.soDoDoiService.getOne(idGiai, idNoiDung,
                                                parent.order);
                                        if (existing != null) {
                                            SoDoThiDauPanel.this.soDoDoiService.update(
                                                    idGiai,
                                                    idNoiDung,
                                                    parent.order,
                                                    idClb,
                                                    teamName, // Chỉ lưu tên đội, không bao gồm tên CLB
                                                    parent.x,
                                                    parent.y,
                                                    soDo,
                                                    java.time.LocalDateTime.now(),
                                                    null,
                                                    null);
                                        } else {
                                            SoDoThiDauPanel.this.soDoDoiService.create(
                                                    idGiai,
                                                    idNoiDung,
                                                    idClb,
                                                    teamName, // Chỉ lưu tên đội, không bao gồm tên CLB
                                                    parent.x,
                                                    parent.y,
                                                    parent.order,
                                                    soDo,
                                                    java.time.LocalDateTime.now(),
                                                    null,
                                                    null);
                                        }
                                    } catch (RuntimeException exUp) {
                                        // Fallback: try create if update path failed due to not found
                                        try {
                                            SoDoThiDauPanel.this.soDoDoiService.create(
                                                    idGiai,
                                                    idNoiDung,
                                                    idClb,
                                                    teamName, // Chỉ lưu tên đội, không bao gồm tên CLB
                                                    parent.x,
                                                    parent.y,
                                                    parent.order,
                                                    soDo,
                                                    java.time.LocalDateTime.now(),
                                                    null,
                                                    null);
                                        } catch (RuntimeException ignore2) {
                                        }
                                    }
                                } else {
                                    java.util.Map<String, Integer> map = SoDoThiDauPanel.this.vdvService
                                            .loadSinglesNames(idNoiDung, idGiai);
                                    Integer idVdv = SoDoThiDauPanel.this.resolveVdvIdFromDisplay(map, s.text.trim());
                                    if (idVdv != null) {
                                        Integer soDo = parent.col;
                                        try {
                                            var bt = SoDoThiDauPanel.this.bocThamCaNhanService.getOne(idGiai, idNoiDung,
                                                    idVdv);
                                            if (bt != null && bt.getSoDo() != null)
                                                soDo = bt.getSoDo();
                                        } catch (RuntimeException ignored) {
                                        }
                                        try {
                                            // Upsert: update if parent slot exists, else create
                                            var existing = SoDoThiDauPanel.this.soDoCaNhanService.getOne(idGiai,
                                                    idNoiDung, parent.order);
                                            if (existing != null) {
                                                SoDoThiDauPanel.this.soDoCaNhanService.update(
                                                        idGiai,
                                                        idNoiDung,
                                                        parent.order,
                                                        idVdv,
                                                        parent.x,
                                                        parent.y,
                                                        soDo,
                                                        java.time.LocalDateTime.now(),
                                                        null,
                                                        null);
                                            } else {
                                                SoDoThiDauPanel.this.soDoCaNhanService.create(
                                                        idGiai,
                                                        idNoiDung,
                                                        idVdv,
                                                        parent.x,
                                                        parent.y,
                                                        parent.order,
                                                        soDo,
                                                        java.time.LocalDateTime.now(),
                                                        null,
                                                        null);
                                            }
                                        } catch (RuntimeException exUp) {
                                            try {
                                                SoDoThiDauPanel.this.soDoCaNhanService.create(
                                                        idGiai,
                                                        idNoiDung,
                                                        idVdv,
                                                        parent.x,
                                                        parent.y,
                                                        parent.order,
                                                        soDo,
                                                        java.time.LocalDateTime.now(),
                                                        null,
                                                        null);
                                            } catch (RuntimeException ignore2) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
                    mBack.addActionListener(ev -> {
                        // Xoá bản ghi: chỉ xoá dữ liệu tại ô hiện tại, KHÔNG đẩy về nhánh trước
                        if (s.text == null || s.text.isBlank()) {
                            return;
                        }
                        // Kiểm tra: nếu VĐV/Đội chỉ có 1 bản ghi trong sơ đồ thì KHÔNG xoá
                        int idGiai = SoDoThiDauPanel.this.prefs.getInt("selectedGiaiDauId", -1);
                        NoiDung ndSel = SoDoThiDauPanel.this.selectedNoiDung;
                        if (idGiai > 0 && ndSel != null) {
                            try {
                                if (Boolean.TRUE.equals(ndSel.getTeam())) {
                                    String display = s.text.trim();
                                    // Ưu tiên so theo ID_CLB nếu xác định được từ tên đội
                                    Integer idClb = null;
                                    try {
                                        String teamName = SoDoThiDauPanel.this.extractTeamNameFromDisplay(display);
                                        int found = SoDoThiDauPanel.this.doiService.getIdClbByTeamName(teamName,
                                                ndSel.getId(), idGiai);
                                        if (found > 0)
                                            idClb = found;
                                    } catch (RuntimeException ignored) {
                                    }
                                    List<SoDoDoi> all = SoDoThiDauPanel.this.soDoDoiService.listAll(idGiai,
                                            ndSel.getId());
                                    long cnt = 0;
                                    Integer minX = null;
                                    if (idClb != null && idClb > 0) {
                                        for (SoDoDoi r : all) {
                                            if (r.getIdClb() != null && r.getIdClb().intValue() == idClb.intValue()) {
                                                cnt++;
                                                if (r.getToaDoX() != null) {
                                                    if (minX == null || r.getToaDoX() < minX)
                                                        minX = r.getToaDoX();
                                                }
                                            }
                                        }
                                    } else {
                                        for (SoDoDoi r : all) {
                                            String tt = r.getTenTeam();
                                            if (tt != null && tt.trim().equals(display)) {
                                                cnt++;
                                                if (r.getToaDoX() != null) {
                                                    if (minX == null || r.getToaDoX() < minX)
                                                        minX = r.getToaDoX();
                                                }
                                            }
                                        }
                                    }
                                    if (cnt <= 1) {
                                        JOptionPane.showMessageDialog(BracketCanvas.this,
                                                "Không xoá để tránh mất dữ liệu.",
                                                "Không thể xoá", JOptionPane.WARNING_MESSAGE);
                                        return;
                                    }
                                    // Bảo vệ thêm: nếu ô hiện tại là lần xuất hiện bên trái nhất (toạ độ X nhỏ
                                    // nhất) thì không xoá
                                    if (minX != null && s.x <= minX) {
                                        JOptionPane.showMessageDialog(BracketCanvas.this,
                                                "Không thể xoá.",
                                                "Không thể xoá", JOptionPane.WARNING_MESSAGE);
                                        return;
                                    }
                                } else {
                                    String display = s.text.trim();
                                    java.util.Map<String, Integer> map = SoDoThiDauPanel.this.vdvService
                                            .loadSinglesNames(ndSel.getId(), idGiai);
                                    Integer idVdv = SoDoThiDauPanel.this.resolveVdvIdFromDisplay(map, display);
                                    if (idVdv == null) {
                                        JOptionPane.showMessageDialog(BracketCanvas.this,
                                                "Không xác định được VĐV từ ô đang chọn; không thể xoá an toàn.",
                                                "Không thể xoá", JOptionPane.WARNING_MESSAGE);
                                        return;
                                    }
                                    List<SoDoCaNhan> all = SoDoThiDauPanel.this.soDoCaNhanService.listAll(idGiai,
                                            ndSel.getId());
                                    long cnt = 0;
                                    Integer minX = null;
                                    for (SoDoCaNhan r : all) {
                                        if (r.getIdVdv() != null && r.getIdVdv().intValue() == idVdv.intValue()) {
                                            cnt++;
                                            if (r.getToaDoX() != null) {
                                                if (minX == null || r.getToaDoX() < minX)
                                                    minX = r.getToaDoX();
                                            }
                                        }
                                    }
                                    if (cnt <= 1) {
                                        JOptionPane.showMessageDialog(BracketCanvas.this,
                                                "Không xoá để tránh mất dữ liệu.",
                                                "Không thể xoá", JOptionPane.WARNING_MESSAGE);
                                        return;
                                    }
                                    if (minX != null && s.x <= minX) {
                                        JOptionPane.showMessageDialog(BracketCanvas.this,
                                                "Không thể xoá.",
                                                "Không thể xoá", JOptionPane.WARNING_MESSAGE);
                                        return;
                                    }
                                }
                            } catch (RuntimeException ex) {
                                // Nếu lỗi khi kiểm tra, ưu tiên an toàn: không xoá
                                JOptionPane.showMessageDialog(BracketCanvas.this,
                                        "Không thể kiểm tra số bản ghi: " + ex.getMessage(),
                                        "Không thể xoá", JOptionPane.WARNING_MESSAGE);
                                return;
                            }
                        }

                        // Sau khi qua kiểm tra, tiến hành xoá DB tại ô hiện tại (theo VI_TRI)
                        if (idGiai > 0 && ndSel != null) {
                            try {
                                if (Boolean.TRUE.equals(ndSel.getTeam())) {
                                    SoDoThiDauPanel.this.soDoDoiService.delete(idGiai, ndSel.getId(), s.order);
                                } else {
                                    SoDoThiDauPanel.this.soDoCaNhanService.delete(idGiai, ndSel.getId(), s.order);
                                }
                            } catch (RuntimeException ignore) {
                            }
                        }

                        // Xoá text ở ô hiện tại (UI)
                        setTextOverride(s.col, s.thuTu, "");
                        s.text = "";
                        repaint();
                        SoDoThiDauPanel.this.updateMedalsFromCanvas();
                    });
                    menu.add(mAdvance);
                    menu.add(mBack);
                    menu.addSeparator();
                    menu.add(mEditScore);
                    menu.add(mClearScore);
                    menu.show(BracketCanvas.this, e.getX(), e.getY());
                }
            };
            addMouseListener(ma);
        }

        void setEditMode(boolean on) {
            this.editMode = on;
            setCursor(on ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                    : java.awt.Cursor.getDefaultCursor());
        }

        void setBracketSize(int size) {
            if (size >= 64) {
                this.columns = 7;
                this.spots = new int[] { 64, 32, 16, 8, 4, 2, 1 };
            } else if (size >= 32) {
                this.columns = 6;
                this.spots = new int[] { 32, 16, 8, 4, 2, 1 };
            } else {
                this.columns = 5;
                this.spots = new int[] { 16, 8, 4, 2, 1 };
            }
            // Ensure seed column is in range
            if (seedColumn < 1 || seedColumn > this.columns)
                seedColumn = 1;
            rebuildSlots();
        }

        int getSeedSpotsCount() {
            return (spots != null && spots.length > 0) ? spots[0] : 16;
        }

        int getColumns() {
            return columns;
        }

        void setParticipantsForColumn(List<String> names, int seedCol) {
            this.participants = names != null ? names : new ArrayList<>();
            if (seedCol < 1 || seedCol > columns)
                seedCol = 1;
            this.seedColumn = seedCol;
            rebuildSlots();
        }

        void clearTextOverrides() {
            textOverrides.clear();
            cachedPreferredSize = null; // Invalidate cache
        }

        void setTextOverride(int col, int thuTu, String text) {
            int key = (col << 16) | (thuTu & 0xFFFF);
            if (text == null || text.isBlank()) {
                textOverrides.remove(key);
            } else {
                textOverrides.put(key, text);
            }
            cachedPreferredSize = null; // Invalidate cache when text changes
        }

        void clearScoreOverrides() {
            scoreOverrides.clear();
            cachedPreferredSize = null; // Invalidate cache
        }

        void setScoreOverride(int col, int thuTu, String score) {
            int key = (col << 16) | (thuTu & 0xFFFF);
            if (score == null || score.isBlank()) {
                scoreOverrides.remove(key);
            } else {
                scoreOverrides.put(key, score.trim());
            }
        }

        String getScoreOverride(int col, int thuTu) {
            return scoreOverrides.get((col << 16) | (thuTu & 0xFFFF));
        }

        // getSlots() is already defined above; avoid duplication

        void refreshAfterOverrides() {
            // Rebuild slots so newly set overrides are reflected in cached Slot.text
            rebuildSlots();
        }

        Slot findByOrder(int order) {
            for (Slot s : slots) {
                if (s.order == order)
                    return s;
            }
            return null;
        }

        private Slot getSlotAt(Point p) {
            for (Slot s : slots) {
                int w = cellWidthForCol(s.col), h = cellHeightForCol(s.col);
                if (p.x >= s.x && p.x <= s.x + w && p.y >= s.y && p.y <= s.y + h)
                    return s;
            }
            return null;
        }

        // Expose tiny helpers for outer interactions
        Slot slotAt(Point p) {
            return getSlotAt(p);
        }

        List<Slot> getSlots() {
            return slots;
        }

        private void rebuildSlots() {
            slots.clear();
            cachedPreferredSize = null;
            int orderCounter = 1;

            /*
             * ==============================
             * 1. TÍNH BASE STEP (CHIỀU DỌC)
             * ==============================
             */
            int seedCount = participants.size();
            int maxParticipants = spots[0]; // 16 / 32 / 64

            int targetHeight = (maxParticipants <= 32) ? 950 : 800;
            int usableHeight = Math.max(300, targetHeight - START_Y - 40);

            int baseStep;
            if (seedCount > 0) {
                baseStep = Math.max(20, usableHeight / seedCount);
            } else {
                baseStep = Math.max(16, usableHeight / maxParticipants);
            }

            int maxBaseStep = (maxParticipants <= 32) ? 50 : 40;
            baseStep = Math.min(maxBaseStep, Math.max(16, baseStep));

            /*
             * ==============================
             * 2. BUILD SLOT – TÍNH X CỘNG DỒN (QUAN TRỌNG)
             * ==============================
             */
            int xCursor = 20; // lề trái

            for (int col = 1; col <= columns; col++) {

                int spotCount = spots[col - 1];
                int verticalStep = (int) (baseStep * Math.pow(2, col - 1));
                int colWidth = cellWidthForCol(col);

                for (int t = 0; t < spotCount; t++) {

                    int baseY = START_Y + t * verticalStep;
                    int y;

                    if (col == 1) {
                        y = baseY;
                    } else {
                        int offset = 0;
                        if (columns == 5) { // 16-slot
                            offset = -15;
                        } else if (columns == 6) { // 32-slot
                            offset = -3;
                        }
                        y = baseY + verticalStep / 2 - INNER_UP_OFFSET + offset;
                        if (y < 0)
                            y = 0;
                    }

                    Slot s = new Slot();
                    s.col = col;
                    s.thuTu = t;
                    s.x = xCursor; // 🔥 FIX QUAN TRỌNG: KHÔNG NHÂN CỘT
                    s.y = y;
                    s.order = orderCounter++;

                    if (s.col == seedColumn) {
                        s.text = (t < participants.size() && participants.get(t) != null)
                                ? participants.get(t)
                                : "";
                    } else {
                        s.text = "";
                    }

                    String ovr = textOverrides.get((s.col << 16) | (s.thuTu & 0xFFFF));
                    if (ovr != null) {
                        s.text = ovr;
                    }

                    slots.add(s);
                }

                // 🔥 DỊCH QUA PHẢI CHO CỘT TIẾP THEO (CỘNG DỒN WIDTH)
                xCursor += colWidth + BASE_INNER_RIGHT_OFFSET;
            }

            /*
             * ==============================
             * 3. PREFERRED SIZE (PDF + SCROLL)
             * ==============================
             */
            int fontSize = getBracketNameFontSize();
            Font f = getFont().deriveFont(Font.BOLD, (float) fontSize);
            FontMetrics fm = getFontMetrics(f);

            int maxRight = xCursor + 40;

            for (Slot s : slots) {
                if (s.text != null && !s.text.isBlank()) {
                    int textW = fm.stringWidth(s.text);
                    int right = s.x + 4 + textW;
                    if (right > maxRight)
                        maxRight = right;
                }
            }

            int maxY = START_Y + spots[0] * baseStep + 50;

            setPreferredSize(new Dimension(maxRight, maxY));
            revalidate();
        }

        private Slot parentOf(Slot s) {
            if (s == null)
                return null;
            if (s.col >= columns)
                return null;
            return find(s.col + 1, s.thuTu / 2);
        }

        // childrenOf not used; parentOf is sufficient for our flows

        @Override
        public Dimension getPreferredSize() {

            // 1. Dùng cache nếu có
            if (cachedPreferredSize != null) {
                return cachedPreferredSize;
            }

            // 2. Fallback khi chưa có slot
            if (slots == null || slots.isEmpty()) {
                cachedPreferredSize = new Dimension(1000, 600);
                return cachedPreferredSize;
            }

            /*
             * ============================
             * 3. TÍNH WIDTH (X)
             * ============================
             */

            // Base width theo số cột + kích thước ô
            int baseWidth = 20
                    + (columns - 1) * 165
                    + cellWidthForCol(columns)
                    + 40;

            // Xét text dài nhất để tránh bị cắt
            int fontSize = getBracketNameFontSize();
            Font textFont = getFont().deriveFont(Font.BOLD, (float) fontSize);
            FontMetrics fm = getFontMetrics(textFont);

            int maxTextRight = baseWidth;

            for (Slot s : slots) {
                if (s.text != null && !s.text.isBlank()) {
                    int textRight = s.x + 4 + fm.stringWidth(s.text);
                    maxTextRight = Math.max(maxTextRight, textRight);
                }
            }

            int maxX = Math.max(baseWidth, maxTextRight + 10);

            /*
             * ============================
             * 4. TÍNH HEIGHT (Y)
             * ============================
             */

            int maxY = START_Y;

            for (Slot s : slots) {
                int slotBottom = s.y
                        + cellHeightForCol(s.col)
                        + 50; // margin an toàn phía dưới

                maxY = Math.max(maxY, slotBottom);
            }

            /*
             * ============================
             * 5. GIỚI HẠN TỐI THIỂU (A4)
             * ============================
             */

            maxX = Math.max(maxX, 1000);
            maxY = Math.max(maxY, 600);

            /*
             * ============================
             * 6. CACHE & RETURN
             * ============================
             */

            cachedPreferredSize = new Dimension(maxX, maxY);
            return cachedPreferredSize;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            /*
             * ==============================
             * 1. VẼ SLOT (Ô)
             * ==============================
             */
            for (Slot s : slots) {

                int col = s.col;
                int w = cellWidthForCol(col);
                int h = cellHeightForCol(col);

                int visualPercent = getVisualHeightPercent();
                int visualH = Math.max(12, h * visualPercent / 100);
                int yVis = s.y + (h - visualH) / 2;

                // Gradient fill (xám mờ trái → trong suốt phải)
                Paint oldPaint = g2.getPaint();
                GradientPaint gp = new GradientPaint(
                        s.x, yVis, new Color(0x55, 0x55, 0x55, 48),
                        s.x + w, yVis, new Color(0x55, 0x55, 0x55, 0));
                g2.setPaint(gp);
                g2.fillRoundRect(s.x, yVis, w, visualH, 8, 8);
                g2.setPaint(oldPaint);

                // Viền (trên – dưới – trái)
                g2.setColor(new Color(120, 120, 120));
                g2.drawLine(s.x, yVis, s.x + w, yVis); // top
                g2.drawLine(s.x, yVis + visualH, s.x + w, yVis + visualH); // bottom
                g2.drawLine(s.x, yVis, s.x, yVis + visualH); // left
            }

            /*
             * ==============================
             * 2. VẼ CONNECTOR
             * ==============================
             */
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(170, 170, 170));

            for (int col = 1; col < columns; col++) {

                int spotCount = spots[col - 1];

                for (int t = 0; t < spotCount; t += 2) {

                    Slot a = find(col, t);
                    Slot b = find(col, t + 1);
                    Slot p = find(col + 1, t / 2);

                    if (a == null || b == null || p == null)
                        continue;

                    int childRightX = a.x + cellWidthForCol(a.col);
                    int ay = a.y + cellHeightForCol(a.col) / 2;
                    int by = b.y + cellHeightForCol(b.col) / 2;
                    int py = p.y + cellHeightForCol(p.col) / 2;

                    int midY = (ay + by) / 2;
                    int trim = 15;

                    int vTop = Math.min(ay, by) + trim;
                    int vBot = Math.max(ay, by) - trim;

                    if (vBot < vTop) {
                        vTop = vBot = midY;
                    }

                    // Spine
                    g2.drawLine(childRightX, vTop, childRightX, vBot);

                    // Horizontal to parent
                    g2.drawLine(childRightX, midY, p.x, midY);

                    // Vertical up/down at parent
                    if (py != midY) {
                        g2.drawLine(p.x, Math.min(py, midY), p.x, Math.max(py, midY));
                    }
                }
            }

            /*
             * ==============================
             * 3. VẼ TEXT + SCORE
             * ==============================
             */
            g2.setColor(Color.BLACK);

            for (Slot s : slots) {

                if (s.text == null || s.text.isBlank())
                    continue;

                drawSlotText(g2, s);
                drawSlotScore(g2, s);
            }

            g2.dispose();
        }

        private int getVisualHeightPercent() {
            try {
                return Math.max(30, Math.min(100,
                        prefs.getInt("bracket.slot.visualHeightPercent", 60)));
            } catch (RuntimeException e) {
                return 60;
            }
        }

        private void drawSlotText(Graphics2D g2, Slot s) {

            int h = cellHeightForCol(s.col);
            int visH = Math.max(12, h * getVisualHeightPercent() / 100);
            int yVis = s.y + (h - visH) / 2;

            Font font = getFont().deriveFont(Font.BOLD, getBracketNameFontSize());
            g2.setFont(font);

            FontMetrics fm = g2.getFontMetrics();
            int textY = yVis + (visH - fm.getHeight()) / 2 + fm.getAscent() - 2;

            g2.drawString(s.text, s.x + 4, textY);
        }

        private void drawSlotScore(Graphics2D g2, Slot s) {

            String score = getScoreOverride(s.col, s.thuTu);
            if (score == null || score.isBlank())
                return;

            int w = cellWidthForCol(s.col);
            int h = cellHeightForCol(s.col);

            Font font = getFont().deriveFont(Font.BOLD, 12f);
            g2.setFont(font);

            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(score);

            int rectW = textW + 10;
            int rectX = s.x + w - rectW - 6;
            int rectY = s.y + h - fm.getHeight() - 4;

            g2.setColor(Color.WHITE);
            g2.fillRect(rectX, rectY, rectW, fm.getHeight());

            g2.setColor(Color.DARK_GRAY);
            g2.drawString(score, rectX + 5, rectY + fm.getAscent());
        }

        private Slot find(int col, int thuTu) {
            for (Slot s : slots) {
                if (s.col == col && s.thuTu == thuTu)
                    return s;
            }
            return null;
        }

        private int getBracketNameFontSize() {
            if (renderingForPdf) {
                if (columns >= 7)
                    return 23; // sơ đồ 64
                if (columns >= 6)
                    return 22; // sơ đồ 32
                return 16; // nhỏ hơn
            }

            try {
                int v = prefs.getInt("bracket.nameFontSize", 12);
                return Math.max(8, Math.min(20, v));
            } catch (RuntimeException e) {
                return 12;
            }
        }

    }
}
