package com.example.btms.ui.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import com.example.btms.config.Prefs;
import com.example.btms.model.bracket.SoDoCaNhan;
import com.example.btms.model.bracket.SoDoDoi;
import com.example.btms.model.court.Court;
import com.example.btms.model.match.BadmintonMatch;
import com.example.btms.model.match.ChiTietVan;
import com.example.btms.model.player.VanDongVien;
import com.example.btms.model.team.DangKiDoi;
import com.example.btms.repository.bracket.SoDoCaNhanRepository;
import com.example.btms.repository.bracket.SoDoDoiRepository;
import com.example.btms.repository.category.NoiDungRepository;
import com.example.btms.repository.club.CauLacBoRepository;
import com.example.btms.repository.match.ChiTietTranDauRepository;
import com.example.btms.repository.match.ChiTietVanRepository;
import com.example.btms.repository.player.VanDongVienRepository;
import com.example.btms.service.bracket.SoDoCaNhanService;
import com.example.btms.service.bracket.SoDoDoiService;
import com.example.btms.service.category.NoiDungService;
import com.example.btms.service.club.CauLacBoService;
import com.example.btms.service.match.ChiTietTranDauService;
import com.example.btms.service.match.ChiTietVanService;
import com.example.btms.service.player.VanDongVienService;
import com.example.btms.service.scoreboard.ScoreboardRemote;
import com.example.btms.service.scoreboard.ScoreboardService;
import com.example.btms.service.team.DoiService;
import com.example.btms.ui.scoreboard.MiniScorePanel;
import com.example.btms.util.log.Log;
import com.example.btms.util.net.NetworkUtil;
import com.example.btms.util.qr.QRCodeUtil;
import com.example.btms.util.sound.SoundPlayer;
import com.example.btms.util.swing.SelectionGuard;
import com.example.btms.util.ui.ButtonFactory;
import com.example.btms.web.controller.scoreBoard.ScoreboardPinController;
import com.google.zxing.WriterException;

public class BadmintonControlPanel extends JPanel implements PropertyChangeListener {

    /* ===== Services & model ===== */
    private BadmintonMatch match;
    private final ScoreboardService scoreboardSvc = new ScoreboardService();
    private Connection conn;
    private NetworkInterface selectedIf;
    private Court court; // Court object để lấy thông tin sân
    private String courtId = ""; // ID của sân để hiển thị trên monitor
    private int courtNumber = 1; // Số sân, mặc định là 1 nếu không lấy được từ courtId
    private NoiDungService noiDungService;
    private VanDongVienService vdvService;
    private SoDoCaNhanService soDoCaNhanService;
    private SoDoDoiService soDoDoiService;
    private ChiTietTranDauService chiTietTranDauService;
    private ChiTietVanService chiTietVanService;
    private CauLacBoService clbService;

    /* ===== Widgets: Config ===== */
    private final JComboBox<String> cboHeaderSingles = new JComboBox<>();
    private final JComboBox<String> cboHeaderDoubles = new JComboBox<>();
    private final JComboBox<String> cboNameA = new JComboBox<>();
    private final JComboBox<String> cboNameB = new JComboBox<>();
    private final JComboBox<DangKiDoi> cboTeamA = new JComboBox<>();
    private final JComboBox<DangKiDoi> cboTeamB = new JComboBox<>();
    private final JComboBox<String> bestOf = new JComboBox<>(new String[] { "Bo 1", "Bo 3" });
    private final JCheckBox doubles = new JCheckBox("Đánh đôi");
    private final JComboBox<String> initialServer = new JComboBox<>(
            new String[] { "Đội A giao cầu", "Đội B giao cầu" });

    /* ===== Widgets: Controls ===== */
    private final JComboBox<String> cboDisplayKind = new JComboBox<>(
            new String[] { "Dọc (vertical)", "Ngang (horizontal)" });
    private final JComboBox<String> cboScreen = new JComboBox<>();
    private JButton btnStart, btnFinish, btnReset, btnOpenDisplay, btnOpenDisplayH, btnCloseDisplay, btnReloadLists;
    private JButton pauseResume;
    private String currentMatchId = null;

    /* ===== Score buttons ===== */
    private JButton aPlus, bPlus, aMinus, bMinus, undo, nextGame, swapEnds, toggleServe;

    /* ===== Status labels ===== */
    private final JLabel lblGame = new JLabel("Ván 1", SwingConstants.LEFT);
    private final JLabel lblGamesWon = new JLabel("Ván: 0 - 0", SwingConstants.LEFT);
    private final JLabel lblServer = new JLabel("Giao cầu: A (R)", SwingConstants.LEFT);
    private final JLabel lblStatus = new JLabel("Sẵn sàng", SwingConstants.LEFT);
    private final JLabel lblWinner = new JLabel("-", SwingConstants.LEFT);

    /* ===== Remote control (URL + QR) ===== */
    private final JLabel lblRemoteUrl = new JLabel("-");
    private final JLabel lblRemoteQr = new JLabel();
    private boolean remoteUrlVisible = false;
    private boolean qrCodeVisible = false; // Mặc định hiển thị QR code
    private String currentRemoteUrl = null;
    private JButton btnToggleLinkVisible;
    private JButton btnToggleQrVisible;

    /* ===== Live preview ===== */
    private MiniScorePanel mini;
    private JPanel miniContainer; // Container chứa mini panel

    /* ===== Labels để ẩn/hiện ===== */
    private JLabel labHeaderSingles, labHeaderDoubles, labA1, labB1, labTeamA, labTeamB;

    /* ===== Data maps ===== */
    private final Map<String, Integer> headerKnrSingles = new LinkedHashMap<>();
    private final Map<String, Integer> headerKnrDoubles = new LinkedHashMap<>();
    private final Map<String, Integer> singlesNameToId = new HashMap<>();

    private boolean hasStarted = false;
    private boolean finishScheduled = false;
    private boolean screenshotTaken = false; // Flag để ngăn chụp ảnh nhiều lần cho cùng một match kết thúc
    private long lastScreenshotTime = 0; // Timestamp của lần chụp cuối cùng
    private javax.swing.Timer finishTimer = null;
    private volatile boolean restartSetPending = false;

    /* ===== Split panes & prefs ===== */
    private final JSplitPane mainSplit; // Left | CenterRight
    private final JSplitPane centerRightSplit; // Center | Right
    private final JSplitPane leftVert; // (Config | Controls)
    private final JSplitPane midVert; // (Live | Score+QR)
    private final JSplitPane rightVert; // (Status | Log)
    private final Prefs prefs = new Prefs();

    /* ===== UI const ===== */
    private static final String PH_HEADER_S = "— Chọn nội dung đơn —";
    private static final String PH_HEADER_D = "— Chọn nội dung đôi —";
    private static final String PH_PLAYER = "— Chọn VĐV —";
    private static final String PH_TEAM = "— Chọn đội —";

    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_VALUE = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_BTN = new Font("SansSerif", Font.BOLD, 14);

    private static final Color COL_PRIMARY = new Color(30, 136, 229);
    private static final Color COL_SUCCESS = new Color(46, 204, 113);
    private static final Color COL_DANGER = new Color(231, 76, 60);
    private static final Color COL_WARNING = new Color(241, 196, 15);
    private static final Color COL_NEUTRAL = new Color(120, 120, 120);

    private static final Dimension BTN_CTRL = new Dimension(200, 30);
    private static final Dimension BTN_SCORE = new Dimension(120, 46);
    private static final Dimension BTN_UTILITY = new Dimension(140, 46);

    /* ===== Utils ===== */
    private final Log logger = new Log();
    private final SelectionGuard guard = new SelectionGuard();

    // Client name cho broadcast - có thể set từ MainFrame
    private String customClientName = null;

    public BadmintonMatch getMatch() {
        return this.match;
    }

    private void initializeMatch() {
        if (courtPinCode != null && !courtPinCode.equals("0000")) {
            BadmintonMatch pinMatch = ScoreboardPinController
                    .getMatchByPin(courtPinCode);
            if (pinMatch != null) {
                this.match = pinMatch;
                return;
            }
        }
        // Fallback to shared match
        this.match = ScoreboardRemote.get().match();
    }

    private void switchToMatchByPin() {
        BadmintonMatch oldMatch = this.match;

        // Remove listeners from old match
        if (oldMatch != null) {
            oldMatch.removePropertyChangeListener(this);
            if (mini != null) {
                oldMatch.removePropertyChangeListener(mini);
            }
        }

        BadmintonMatch pinMatch = ScoreboardPinController
                .getMatchByPin(courtPinCode);
        if (pinMatch != null) {
            this.match = pinMatch;
            logger.logTs("Switched to PIN match for PIN: %s", courtPinCode);
        } else {
            this.match = ScoreboardRemote.get().match();
            logger.logTs("Fallback to shared match for PIN: %s", courtPinCode);
        }

        match.addPropertyChangeListener(this);

        recreateMiniPanel();

        SwingUtilities.invokeLater(() -> {
            if (mini != null) {
                // Force refresh mini panel with new match data
                mini.repaint();
                mini.revalidate();
            }
        });
    }

    private void recreateMiniPanel() {
        if (match != null && miniContainer != null) {
            SwingUtilities.invokeLater(() -> {
                // Remove old mini panel if exists
                if (mini != null) {
                    miniContainer.remove(mini);
                }

                // Create new mini panel with new match
                mini = new MiniScorePanel(match);
                mini.setBorder(new EmptyBorder(6, 6, 6, 6));

                // Add new mini panel to container
                miniContainer.add(mini);

                // Add listener to match for mini panel updates
                match.addPropertyChangeListener(mini);

                // Refresh container
                miniContainer.revalidate();
                miniContainer.repaint();

                logger.logTs("Recreated mini panel with new match for PIN: %s", courtPinCode);
            });
        }
    }

    public BadmintonControlPanel() {
        super(new BorderLayout());
        initializeMatch();
        SwingUtilities.invokeLater(() -> {
            if (match != null) {
                match.addPropertyChangeListener(this);
            }
        });
        /* ===== Column LEFT: Cấu hình + Điều khiển ===== */
        JPanel config = buildConfigCard();
        JPanel controls = buildControlsCard();
        leftVert = vSplit(config, controls, 0.30);
        JPanel leftCol = wrapWithSize(leftVert, new Dimension(300, 420), new Dimension(560, 620));

        /* ===== Column CENTER: Live + Score/QR ===== */
        JPanel live = buildLiveCard();

        // Khởi tạo MiniScorePanel sau khi buildLiveCard() đã tạo miniContainer
        mini = new MiniScorePanel(match);
        mini.setBorder(new EmptyBorder(6, 6, 6, 6));
        miniContainer.add(mini);

        JPanel scoreQ = buildScoreAndQrCard();
        midVert = vSplit(live, scoreQ, 0.40);
        JPanel midCol = wrapWithSize(midVert, new Dimension(520, 420), new Dimension(640, 620));

        /* ===== Column RIGHT: Trạng thái + Log ===== */
        JPanel status = buildStatusCard();
        JPanel logs = buildLogCard();
        rightVert = vSplit(status, logs, 0.20);
        JPanel rightCol = wrapWithSize(rightVert, new Dimension(300, 420), new Dimension(460, 620));

        /* ===== HORIZONTAL: Center | Right ===== */
        centerRightSplit = hSplit(midCol, rightCol, 0.80); // ~70% cho cột giữa

        /* ===== HORIZONTAL: Left | (Center|Right) ===== */
        mainSplit = hSplit(leftCol, centerRightSplit, 0.20); // ~20% cho cột trái
        lockRightMin(mainSplit, rightCol, 100);

        add(mainSplit, BorderLayout.CENTER);

        // defaults & bindings
        cboDisplayKind.setSelectedIndex(1);
        populateScreens();
        installKeyBindings();

        SwingUtilities.invokeLater(this::restoreSplitLocations);
    }

    /* =================== PUBLIC APIS =================== */

    public void setConnection(Connection connection) throws SQLException {
        this.conn = connection;
        // Khởi tạo services sau khi Connection được set
        this.noiDungService = new NoiDungService(new NoiDungRepository(conn));
        this.vdvService = new VanDongVienService(new VanDongVienRepository(conn));
        this.soDoCaNhanService = new SoDoCaNhanService(new SoDoCaNhanRepository(conn));
        this.soDoDoiService = new SoDoDoiService(new SoDoDoiRepository(conn));
        this.chiTietTranDauService = new ChiTietTranDauService(new ChiTietTranDauRepository(conn));
        this.chiTietVanService = new ChiTietVanService(new ChiTietVanRepository(conn));
        this.clbService = new CauLacBoService(new CauLacBoRepository(conn));
        reloadListsFromDb();
    }

    public void setNetworkInterface(NetworkInterface nif) {
        this.selectedIf = nif;
        updateRemoteLinkUi();
    }

    /** Set Court object - gọi từ MultiCourtControlPanel khi tạo sân */
    public void setCourt(Court court) {
        this.court = court;
        if (court != null) {
            this.courtId = court.getName();
            this.courtNumber = court.getCourtNumber();
            logger.logTs("Đặt Court - ID='%s', Số sân=%d", courtId, courtNumber);
        }
    }

    public Court getCourt() {
        return this.court;
    }

    /** Set courtId và parse số sân từ chuỗi courtId (ví dụ: "Sàn 1" -> 1) */
    public void setCourtId(String courtId) {
        this.courtId = courtId != null ? courtId : "";
        // Parse số sân từ courtId
        // Ví dụ: "Sàn 1" -> 1, "Sàn 2" -> 2, etc.
        this.courtNumber = parseCourtNumber(this.courtId);
        if (this.courtNumber <= 0) {
            this.courtNumber = 1; // Mặc định nếu không parse được
        }
        logger.logTs("Đặt courtId='%s', Số sân được parse=%d", this.courtId, this.courtNumber);
    }

    /** Parse số sân từ courtId string (ví dụ: "Sàn 1" -> 1) */
    private int parseCourtNumber(String courtId) {
        if (courtId == null || courtId.isEmpty()) {
            return 1;
        }

        // Tìm tất cả các chữ số trong chuỗi
        StringBuilder numbers = new StringBuilder();
        for (char c : courtId.toCharArray()) {
            if (Character.isDigit(c)) {
                numbers.append(c);
            } else if (!numbers.isEmpty()) {
                // Dừng ở ký tự đầu tiên không phải số
                break;
            }
        }

        try {
            if (numbers.length() > 0) {
                return Integer.parseInt(numbers.toString());
            }
        } catch (NumberFormatException e) {
            logger.logTs("⚠️ Không thể parse số sân từ '%s': %s", courtId, e.getMessage());
        }

        return 1; // Mặc định nếu không parse được
    }

    public void setClientName(String clientName) {
        this.customClientName = clientName;
    }

    public void startIdleBroadcast(String header) {
        try {
            String hdr = (header == null || header.isBlank()) ? "TRẬN ĐẤU" : header;
            String displayKind = (cboDisplayKind.getSelectedIndex() == 0) ? "VERTICAL" : "HORIZONTAL";
            String clientName = customClientName != null ? customClientName : System.getProperty("user.name", "client");
            String hostShown = "";

            scoreboardSvc.startBroadcast(match, selectedIf, clientName, hostShown, displayKind,
                    hdr, doubles.isSelected(), "", "", courtId);
        } catch (Exception ex) {
        }
    }

    public void stopBroadcast() {
        try {
            scoreboardSvc.stopBroadcast();
        } catch (Exception ignore) {
        }
    }

    public void saveSplitLocations() {
        try {
            if (mainSplit != null)
                prefs.putInt("split.main", mainSplit.getDividerLocation());
            if (centerRightSplit != null)
                prefs.putInt("split.centerRight", centerRightSplit.getDividerLocation());
            if (leftVert != null)
                prefs.putInt("split.leftVert", leftVert.getDividerLocation());
            if (midVert != null)
                prefs.putInt("split.midVert", midVert.getDividerLocation());
            if (rightVert != null)
                prefs.putInt("split.rightVert", rightVert.getDividerLocation());
        } catch (Exception ignore) {
        }
    }

    private JPanel buildConfigCard() {
        JPanel card = section("Cấu hình trận đấu");
        card.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;

        int r = 0;
        labHeaderSingles = addRow(card, c, r++, "Nội dung đơn", cboHeaderSingles);
        labA1 = addRow(card, c, r++, "Đội A (đơn)", cboNameA);
        labB1 = addRow(card, c, r++, "Đội B (đơn)", cboNameB);
        addRow(card, c, r++, "Thể thức", bestOf);

        labHeaderDoubles = addRow(card, c, r++, "Nội dung đôi", cboHeaderDoubles);
        labTeamA = addRow(card, c, r++, "Đội A (đôi)", cboTeamA);
        labTeamB = addRow(card, c, r++, "Đội B (đôi)", cboTeamB);
        addRow(card, c, r++, "Giao cầu trước", initialServer);

        GridBagConstraints cFull = (GridBagConstraints) c.clone();
        cFull.gridx = 0;
        cFull.gridy = r++;
        cFull.gridwidth = 2;
        cFull.weightx = 1.0;
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        doubles.setBorder(new EmptyBorder(0, 8, 0, 0));
        bottom.add(doubles, BorderLayout.WEST);
        card.add(bottom, cFull);

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = r;
        filler.gridwidth = 2;
        filler.weightx = 1.0;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.BOTH;
        card.add(Box.createGlue(), filler);

        setPlaceholdersAndVisibility();
        cboHeaderSingles.addActionListener(e -> {
            if (!doubles.isSelected() && !guard.isSuppressed())
                onHeaderSinglesChosen();
        });
        cboHeaderDoubles.addActionListener(e -> {
            if (doubles.isSelected() && !guard.isSuppressed())
                onHeaderDoublesChosen();
        });
        doubles.addActionListener(e -> {
            if (!guard.isSuppressed())
                toggleSinglesOrDoubles();
        });
        cboNameA.addActionListener(e -> ensureDifferentVdvAndUpdate());
        cboNameB.addActionListener(e -> ensureDifferentVdvAndUpdate());
        cboTeamA.addActionListener(e -> ensureDifferentTeamsAndUpdate());
        cboTeamB.addActionListener(e -> ensureDifferentTeamsAndUpdate());
        return card;
    }

    private JPanel buildControlsCard() {
        JPanel card = section("Điều khiển");
        card.setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridLayout(1, 2, 10, 0));
        top.setOpaque(false);
        top.add(labeled(cboScreen, "Màn hình hiển thị"));
        top.add(labeled(cboDisplayKind, "Kiểu bảng điểm"));

        JPanel buttons = new JPanel(new GridLayout(5, 2, 10, 0));
        buttons.setOpaque(false);
        btnStart = ButtonFactory.filled("Bắt đầu trận", COL_SUCCESS, Color.WHITE, BTN_CTRL, FONT_BTN);
        btnFinish = ButtonFactory.filled("Kết thúc trận", COL_DANGER, Color.WHITE, BTN_CTRL, FONT_BTN);
        btnFinish.setEnabled(false);
        btnOpenDisplay = ButtonFactory.outlined("Mở bảng dọc", COL_PRIMARY, BTN_CTRL, FONT_BTN);
        btnOpenDisplay.setEnabled(false);
        btnOpenDisplayH = ButtonFactory.outlined("Mở bảng ngang", COL_PRIMARY, BTN_CTRL, FONT_BTN);
        btnOpenDisplayH.setEnabled(false);
        btnCloseDisplay = ButtonFactory.outlined("Đóng", COL_NEUTRAL, BTN_CTRL, FONT_BTN);
        btnCloseDisplay.setEnabled(false);
        btnReset = ButtonFactory.outlined("Đặt lại", COL_WARNING, BTN_CTRL, FONT_BTN);
        btnReset.setEnabled(false);
        // Nút tạm dừng/tiếp tục
        pauseResume = ButtonFactory.outlined("Tạm dừng", COL_WARNING, BTN_CTRL, FONT_BTN);
        pauseResume.setEnabled(false);

        // Nút chụp ảnh bảng điểm
        JButton btnCapture = ButtonFactory.outlined("📸 Chụp ảnh", COL_NEUTRAL, BTN_CTRL, FONT_BTN);
        btnCapture.setToolTipText("Chụp ảnh bảng điểm mini hiện tại");
        btnCapture.addActionListener(e -> captureMiniScoreboard());

        // Nút reload danh sách
        btnReloadLists = ButtonFactory.outlined("🔄 Làm mới", COL_PRIMARY, BTN_CTRL, FONT_BTN);
        btnReloadLists.setToolTipText("Làm mới danh sách nội dung và VĐV");
        btnReloadLists.addActionListener(e -> {
            try {
                reloadListsFromDb();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        });

        for (JButton b : new JButton[] { btnStart, btnFinish, btnOpenDisplay, btnOpenDisplayH, btnCloseDisplay,
                btnReset, pauseResume, btnReloadLists }) {
            b.setPreferredSize(BTN_CTRL);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, BTN_CTRL.height));
        }

        btnStart.addActionListener(e -> onStart());
        btnFinish.addActionListener(e -> onFinish(false));
        btnReset.addActionListener(e -> onReset());
        btnOpenDisplay.addActionListener(e -> openDisplayVertical());
        btnOpenDisplayH.addActionListener(e -> openDisplayHorizontal());
        btnCloseDisplay.addActionListener(e -> closeDisplays());
        pauseResume.addActionListener(e -> onTogglePause());

        buttons.add(btnStart);
        buttons.add(btnFinish);
        buttons.add(btnReset);
        buttons.add(btnReloadLists);
        buttons.add(btnOpenDisplay);
        buttons.add(btnOpenDisplayH);
        buttons.add(btnCloseDisplay);
        buttons.add(pauseResume);
        buttons.add(btnCapture);
        buttons.add(Box.createGlue());

        card.add(top, BorderLayout.NORTH);
        card.add(buttons, BorderLayout.CENTER);
        return card;
    }

    private void onTogglePause() {
        try {
            var s = match.snapshot();
            if (s.matchFinished) {
                JOptionPane.showMessageDialog(this, "Trận đã kết thúc", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (s.betweenGamesInterval) {
                JOptionPane.showMessageDialog(this, "Đang nghỉ giữa ván. Dùng 'Ván tiếp theo' để tiếp tục.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            boolean manualPaused = false;
            try {
                java.lang.reflect.Method m = match.getClass().getDeclaredMethod("isManualPaused");
                m.setAccessible(true);
                manualPaused = (Boolean) m.invoke(match);
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException
                    | InvocationTargetException ignore) {
            }

            if (!manualPaused) {
                try {
                    java.lang.reflect.Method m = match.getClass().getDeclaredMethod("pauseManual");
                    m.setAccessible(true);
                    m.invoke(match);
                    lblStatus.setText("Tạm dừng");
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException
                        | InvocationTargetException ex) {
                }
            } else {
                try {
                    java.lang.reflect.Method m = match.getClass().getDeclaredMethod("resumeManual");
                    m.setAccessible(true);
                    m.invoke(match);
                    lblStatus.setText("Đang thi đấu");
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException
                        | InvocationTargetException ex) {
                }
            }
            updatePauseButtonText();
            updateControlsEnabledAccordingToState();
        } catch (HeadlessException ex) {
            logger.logTs("Lỗi toggle pause: %s", ex.getMessage());
        }
    }

    private void updatePauseButtonText() {
        boolean manualPaused = false;
        try {
            java.lang.reflect.Method m = match.getClass().getDeclaredMethod("isManualPaused");
            m.setAccessible(true);
            manualPaused = (Boolean) m.invoke(match);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException
                | InvocationTargetException ignore) {
        }
        if (pauseResume != null)
            pauseResume.setText(manualPaused ? "Tiếp tục" : "Tạm dừng");
    }

    private void updateControlsEnabledAccordingToState() {
        var s = match.snapshot();
        boolean manualPaused = false;
        try {
            java.lang.reflect.Method m = match.getClass().getDeclaredMethod("isManualPaused");
            m.setAccessible(true);
            manualPaused = (Boolean) m.invoke(match);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException
                | InvocationTargetException ignore) {
        }
        boolean canScore = hasStarted && !s.matchFinished && !s.betweenGamesInterval && !manualPaused;
        setScoreButtonsEnabled(canScore);
        nextGame.setEnabled(!s.matchFinished && s.betweenGamesInterval);
        if (pauseResume != null)
            pauseResume.setEnabled(hasStarted && !s.matchFinished && !s.betweenGamesInterval);
    }

    private JPanel buildLiveCard() {
        JPanel card = section("Xem trước (live)");
        card.setLayout(new BorderLayout(8, 8));
        miniContainer = new JPanel(new GridLayout(1, 1, 10, 0));
        miniContainer.setOpaque(false);
        miniContainer.setPreferredSize(new Dimension(320, 180));

        if (mini != null) {
            mini.setBorder(new EmptyBorder(6, 6, 6, 6));
            miniContainer.add(mini);
        }

        card.add(miniContainer, BorderLayout.CENTER);
        return card;
    }

    /** Ở giữa: Điểm số + (QR + Link để sau) */
    private JPanel buildScoreAndQrCard() {
        JPanel card = section("Điểm số / QR / Link");
        card.setLayout(new BorderLayout(8, 8));

        JPanel scoreButtons = new JPanel(new GridLayout(2, 4, 10, 10));
        scoreButtons.setOpaque(false);

        aPlus = ButtonFactory.filled("+1 A", COL_SUCCESS, Color.WHITE, BTN_SCORE, FONT_BTN);
        bPlus = ButtonFactory.filled("+1 B", COL_SUCCESS, Color.WHITE, BTN_SCORE, FONT_BTN);
        aMinus = ButtonFactory.filled("-1 A", COL_DANGER, Color.WHITE, BTN_SCORE, FONT_BTN);
        bMinus = ButtonFactory.filled("-1 B", COL_DANGER, Color.WHITE, BTN_SCORE, FONT_BTN);
        undo = ButtonFactory.outlined("Hoàn tác", COL_NEUTRAL, BTN_UTILITY, FONT_BTN);
        nextGame = ButtonFactory.outlined("Ván tiếp theo", COL_PRIMARY, BTN_UTILITY, FONT_BTN);
        swapEnds = ButtonFactory.outlined("Đổi sân", COL_WARNING, BTN_UTILITY, FONT_BTN);
        toggleServe = ButtonFactory.outlined("Đổi giao cầu", COL_WARNING, BTN_UTILITY, FONT_BTN);

        for (JButton b : new JButton[] { aPlus, bPlus, aMinus, bMinus, undo, nextGame, swapEnds, toggleServe }) {
            b.setPreferredSize(BTN_SCORE);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, BTN_SCORE.height));
            scoreButtons.add(b);
        }

        // Trước khi bắt đầu trận: khóa toàn bộ
        setScoreButtonsEnabled(false);
        nextGame.setEnabled(false);

        // === LOGGING SUPPORT ===
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Runnable logScore = () -> {
            var s = match.snapshot();
            logger.log("[%s] Tỉ số: %d - %d  |  Ván %d / BO%d",
                    sdf.format(new Date()), s.score[0], s.score[1], s.gameNumber, s.bestOf);
        };

        // === ACTION LISTENERS ===
        aPlus.addActionListener(e -> {
            synchronized (ScoreboardRemote.get().lock()) {

                // Không cho cộng nếu trận đã kết thúc
                if (match.snapshot().matchFinished) {
                    return;
                }

                // 1. Cộng điểm
                match.pointTo(0);

                // 2. Ghi DB NGAY SAU KHI cộng điểm
                updateChiTietVanOnPoint(0);
            }

            // 3. Log & UI (ngoài lock)
            logger.log("[%s] +1 A", sdf.format(new Date()));
            logScore.run();
        });

        bPlus.addActionListener(e -> {
            synchronized (ScoreboardRemote.get().lock()) {

                // Không cho cộng nếu trận đã kết thúc
                if (match.snapshot().matchFinished) {
                    return;
                }

                // 1. Cộng điểm
                match.pointTo(1);

                // 2. Ghi DB NGAY SAU KHI cộng điểm
                updateChiTietVanOnPoint(1);
            }

            // 3. Log & UI (ngoài lock)
            logger.log("[%s] +1 B", sdf.format(new Date()));
            logScore.run();
        });

        aMinus.addActionListener(e -> {
            synchronized (ScoreboardRemote.get().lock()) {

                // Không cho trừ điểm nếu trận đã kết thúc
                if (match.snapshot().matchFinished) {
                    return;
                }

                // 1. Trừ điểm
                match.pointDown(0, -1);

                // 2. Đồng bộ lại tổng điểm (KHÔNG thêm token)
                updateChiTietVanTotalsOnly();
            }

            // 3. Log & UI ngoài lock
            logger.log("[%s] -1 A", sdf.format(new Date()));
            logScore.run();
        });

        bMinus.addActionListener(e -> {
            synchronized (ScoreboardRemote.get().lock()) {

                // Không cho trừ điểm nếu trận đã kết thúc
                if (match.snapshot().matchFinished) {
                    return;
                }

                // 1. Trừ điểm
                match.pointDown(1, -1);

                // 2. Đồng bộ lại tổng điểm (KHÔNG thêm token)
                updateChiTietVanTotalsOnly();
            }

            // 3. Log & UI ngoài lock
            logger.log("[%s] -1 B", sdf.format(new Date()));
            logScore.run();
        });

        undo.addActionListener(e -> {
            synchronized (ScoreboardRemote.get().lock()) {

                // Không undo nếu trận đã kết thúc
                if (match.snapshot().matchFinished) {
                    return;
                }

                match.undo();

                // Đồng bộ lại tổng điểm sau undo
                updateChiTietVanTotalsOnly();
            }

            logger.log("[%s] Hoàn tác", sdf.format(new Date()));
            logScore.run();
        });

        nextGame.addActionListener(e -> {
            synchronized (ScoreboardRemote.get().lock()) {

                // Chỉ cho sang ván khi đang giữa các ván
                if (!match.snapshot().betweenGamesInterval) {
                    return;
                }

                match.nextGame();
            }

            var s = match.snapshot();
            logger.log("[%s] Sang ván %d (BO%d) — Ván thắng: %d - %d",
                    sdf.format(new Date()),
                    s.gameNumber,
                    s.bestOf,
                    s.games[0],
                    s.games[1]);
        });

        swapEnds.addActionListener(e -> {
            synchronized (ScoreboardRemote.get().lock()) {

                if (match.snapshot().matchFinished) {
                    return;
                }

                match.swapEnds();

                // Ghi mốc SWAP + sync lại tổng điểm
                appendSwapMarkerAndResyncChiTietVan();
            }

            logger.log("[%s] Đổi sân", sdf.format(new Date()));
            logScore.run();
        });

        toggleServe.addActionListener(e -> {
            synchronized (ScoreboardRemote.get().lock()) {

                if (match.snapshot().matchFinished) {
                    return;
                }

                match.toggleServer();
            }

            var s = match.snapshot();
            logger.log("[%s] Đổi giao cầu → %s",
                    sdf.format(new Date()),
                    s.server == 0 ? "A" : "B");
        });

        card.add(scoreButtons, BorderLayout.NORTH);

        // Remote link + QR inline (no popup)
        JPanel remoteBox = new JPanel(new BorderLayout(8, 8));
        remoteBox.setOpaque(false);
        JLabel cap = new JLabel("Điều khiển trên điện thoại (cùng Wi‑Fi)");
        cap.setFont(FONT_LABEL);
        remoteBox.add(cap, BorderLayout.NORTH);

        // Hiển thị mã PIN và hướng dẫn
        JPanel pinPanel = new JPanel(new BorderLayout(8, 0));
        pinPanel.setOpaque(false);
        JLabel pinLabel = new JLabel("Mã PIN: " + getCourtPinCode());
        pinLabel.setFont(FONT_VALUE);
        pinLabel.setForeground(COL_PRIMARY);
        pinPanel.add(pinLabel, BorderLayout.WEST);
        remoteBox.add(pinPanel, BorderLayout.CENTER);

        // Hướng dẫn nhập PIN
        JPanel instructionPanel = new JPanel(new BorderLayout(8, 0));
        instructionPanel.setOpaque(false);
        JLabel instructionLabel = new JLabel("📱 Hướng dẫn: Mở trình duyệt → Nhập mã PIN → Điều khiển điểm số");
        instructionLabel.setFont(FONT_LABEL.deriveFont(Font.PLAIN, 11f));
        instructionLabel.setForeground(COL_NEUTRAL);
        instructionPanel.add(instructionLabel, BorderLayout.CENTER);
        remoteBox.add(instructionPanel, BorderLayout.CENTER);

        JPanel linkAndQr = new JPanel(new BorderLayout(12, 8));
        linkAndQr.setOpaque(false);

        // Panel chứa link và nhóm nút (ẩn/hiện + copy)
        JPanel linkPanel = new JPanel(new BorderLayout(8, 0));
        linkPanel.setOpaque(false);
        lblRemoteUrl.setFont(FONT_VALUE);
        linkPanel.add(lblRemoteUrl, BorderLayout.CENTER);

        // Nhóm nút 2 cột, căn sát phải: [Ẩn/Hiện QR] [Ẩn/Hiện Link]
        // [ trống ] [Copy]
        JPanel rightBtnBox = new JPanel(new GridLayout(0, 2, 6, 4));
        rightBtnBox.setOpaque(false);
        btnToggleLinkVisible = ButtonFactory.outlined(remoteUrlVisible ? "Ẩn link" : "Hiện link", COL_NEUTRAL,
                new Dimension(110, 30), FONT_BTN);
        btnToggleLinkVisible.setToolTipText("Ẩn/hiện đường link bấm điểm");
        btnToggleLinkVisible.addActionListener(e -> {
            remoteUrlVisible = !remoteUrlVisible;
            updateRemoteUrlDisplay();
            btnToggleLinkVisible.setText(remoteUrlVisible ? "Ẩn link" : "Hiện link");
        });

        JButton btnCopyLink = ButtonFactory.outlined("Copy", COL_PRIMARY, new Dimension(100, 30), FONT_BTN);
        btnCopyLink.setToolTipText("Copy link vào clipboard");
        btnCopyLink.addActionListener(e -> copyLinkToClipboard());

        btnToggleQrVisible = ButtonFactory.outlined(qrCodeVisible ? "Ẩn QR" : "Hiện QR", COL_NEUTRAL,
                new Dimension(100, 30), FONT_BTN);
        btnToggleQrVisible.setToolTipText("Ẩn/hiện mã QR code");
        btnToggleQrVisible.addActionListener(e -> {
            qrCodeVisible = !qrCodeVisible;
            updateQrCodeDisplay();
            btnToggleQrVisible.setText(qrCodeVisible ? "Ẩn QR" : "Hiện QR");
        });

        rightBtnBox.add(btnToggleQrVisible);
        rightBtnBox.add(btnToggleLinkVisible);
        rightBtnBox.add(Box.createHorizontalStrut(0)); // filler để Copy nằm cột phải hàng dưới
        rightBtnBox.add(btnCopyLink);
        linkPanel.add(rightBtnBox, BorderLayout.EAST);

        // Thêm link /pin để nhập mã PIN
        JPanel pinLinkPanel = new JPanel(new BorderLayout(8, 0));
        pinLinkPanel.setOpaque(false);
        JLabel pinLinkLabel = new JLabel("🔗 Link nhập PIN: " + getPinEntryUrl());
        pinLinkLabel.setFont(FONT_LABEL.deriveFont(Font.PLAIN, 11f));
        pinLinkLabel.setForeground(COL_PRIMARY);
        pinLinkPanel.add(pinLinkLabel, BorderLayout.CENTER);

        // Nút copy link PIN
        JButton btnCopyPinLink = ButtonFactory.outlined("📋 Copy PIN", COL_PRIMARY, new Dimension(110, 30), FONT_BTN);
        btnCopyPinLink.setToolTipText("Copy link nhập PIN vào clipboard");
        btnCopyPinLink.addActionListener(e -> copyPinLinkToClipboard());
        pinLinkPanel.add(btnCopyPinLink, BorderLayout.EAST);

        linkAndQr.add(linkPanel, BorderLayout.NORTH);
        lblRemoteQr.setHorizontalAlignment(SwingConstants.LEFT);
        linkAndQr.add(lblRemoteQr, BorderLayout.CENTER);
        remoteBox.add(linkAndQr, BorderLayout.CENTER);

        card.add(remoteBox, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatusCard() {
        JPanel statusCard = section("Trạng thái trận");
        statusCard.setLayout(new GridLayout(0, 1, 10, 6));
        styleInfo(lblGame);
        styleInfo(lblGamesWon);
        styleInfo(lblServer);
        styleInfo(lblStatus);
        styleInfo(lblWinner);
        statusCard.add(kv("Ván", lblGame));
        statusCard.add(kv("Ván thắng", lblGamesWon));
        statusCard.add(kv("Giao cầu", lblServer));
        statusCard.add(kv("Trạng thái", lblStatus));
        statusCard.add(kv("Người thắng", lblWinner));
        return statusCard;
    }

    private JPanel buildLogCard() {
        JPanel logBox = section("Log lựa chọn");
        logBox.setLayout(new BorderLayout());
        JTextArea area = logger.getLogArea(); // đã gom chung Log
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Giới hạn độ rộng tối đa và bật word wrap để tránh cuộn ngang
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMaximumSize(new Dimension(350, Integer.MAX_VALUE));

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        logBox.add(scrollPane, BorderLayout.CENTER);
        return logBox;
    }

    /* =================== LAYOUT HELPERS =================== */

    private JSplitPane hSplit(Component a, Component b, double ratio) {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, a, b);
        sp.setContinuousLayout(true);
        sp.setOneTouchExpandable(true);
        sp.setDividerSize(8);
        sp.setResizeWeight(ratio);
        sp.setDividerLocation(ratio);
        return sp;
    }

    private JSplitPane vSplit(Component top, Component bottom, double ratio) {
        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        sp.setContinuousLayout(true);
        sp.setOneTouchExpandable(true);
        sp.setDividerSize(8);
        sp.setResizeWeight(ratio);
        sp.setDividerLocation(ratio);
        return sp;
    }

    private JPanel wrapWithSize(Component c, Dimension min, Dimension pref) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(c, BorderLayout.CENTER);
        p.setMinimumSize(min);
        p.setPreferredSize(pref);
        return p;
    }

    private void lockRightMin(JSplitPane root, Component rightMost, int minWidth) {
        root.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int total = root.getWidth();
                int curRight = rightMost.getWidth();
                if (curRight < minWidth) {
                    root.setDividerLocation(total - minWidth - root.getDividerSize());
                }
            }
        });
    }

    /* =================== UI helpers =================== */

    private JPanel section(String title) {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(true);
        card.setBackground(new Color(250, 250, 250));
        card.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                new EmptyBorder(6, 10, 6, 10)));
        JLabel lab = new JLabel(title);
        lab.setFont(FONT_SECTION);
        head.add(lab, BorderLayout.WEST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 10, 10, 10));

        card.add(head, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        return body;
    }

    private JLabel addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent comp) {
        JLabel lab = new JLabel(label);
        lab.setFont(FONT_LABEL);
        GridBagConstraints lc = (GridBagConstraints) c.clone();
        lc.gridx = 0;
        lc.gridy = row;
        lc.weightx = 0.0;
        lc.insets = new Insets(6, 8, 0, 8);
        p.add(lab, lc);

        GridBagConstraints vc = (GridBagConstraints) c.clone();
        vc.gridx = 1;
        vc.gridy = row;
        vc.weightx = 1.0;
        vc.insets = new Insets(2, 8, 6, 8);
        comp.setFont(FONT_VALUE);
        p.add(comp, vc);
        return lab;
    }

    private JPanel labeled(JComponent comp, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel lab = new JLabel(title);
        lab.setFont(FONT_LABEL);
        p.add(lab, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel kv(String k, JLabel v) {
        JLabel key = new JLabel(k + ": ");
        key.setFont(FONT_LABEL);
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.add(key, BorderLayout.WEST);
        line.add(v, BorderLayout.CENTER);
        return line;
    }

    private void styleInfo(JLabel l) {
        l.setFont(FONT_VALUE.deriveFont(Font.BOLD, 14f));
        l.setBorder(new EmptyBorder(4, 6, 4, 6));
    }

    private void setPlaceholdersAndVisibility() {
        guard.runSilently(() -> {
            setPlaceholder(cboHeaderSingles, PH_HEADER_S, true);
            setPlaceholder(cboHeaderDoubles, PH_HEADER_D, true);
            setPlaceholder(cboNameA, PH_PLAYER, false);
            setPlaceholder(cboNameB, PH_PLAYER, false);
            setTeamPlaceholder(cboTeamA);
            setTeamPlaceholder(cboTeamB);
            cboHeaderDoubles.setEnabled(false);
        });
        setSinglesVisible(true);
        setTeamsVisible(false);
        setHeadersVisibility(true, false);
    }

    private void setPlaceholder(JComboBox<String> combo, String ph, boolean enabled) {
        combo.removeAllItems();
        combo.addItem(ph);
        combo.setSelectedIndex(0);
        combo.setEnabled(enabled);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null && value.toString().equals(ph))
                    setForeground(Color.GRAY);
                return c;
            }
        });
    }

    private void setTeamPlaceholder(JComboBox<DangKiDoi> combo) {
        combo.removeAllItems();
        // tạo đối tượng giả làm placeholder
        DangKiDoi ph = new DangKiDoi();
        ph.setIdTeam(-1);
        ph.setTenTeam(PH_TEAM);
        combo.addItem(ph);
        combo.setSelectedIndex(0);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DangKiDoi dk && PH_TEAM.equals(dk.getTenTeam()))
                    setForeground(Color.GRAY);
                return c;
            }
        });
    }

    private void setSinglesVisible(boolean vis) {
        if (labA1 != null)
            labA1.setVisible(vis);
        if (labB1 != null)
            labB1.setVisible(vis);
        cboNameA.setVisible(vis);
        cboNameB.setVisible(vis);
    }

    private void setTeamsVisible(boolean vis) {
        if (labTeamA != null)
            labTeamA.setVisible(vis);
        if (labTeamB != null)
            labTeamB.setVisible(vis);
        cboTeamA.setVisible(vis);
        cboTeamB.setVisible(vis);
    }

    private void setHeadersVisibility(boolean singlesVisible, boolean doublesVisible) {
        if (labHeaderSingles != null)
            labHeaderSingles.setVisible(singlesVisible);
        cboHeaderSingles.setVisible(singlesVisible);
        if (labHeaderDoubles != null)
            labHeaderDoubles.setVisible(doublesVisible);
        cboHeaderDoubles.setVisible(doublesVisible);
        revalidate();
        repaint();
    }

    private static boolean isPh(String s) {
        return PH_PLAYER.equals(s) || PH_HEADER_S.equals(s) || PH_HEADER_D.equals(s);
    }

    private String sel(JComboBox<String> cb) {
        Object v = cb.getSelectedItem();
        String s = v == null ? "" : v.toString().trim();
        return isPh(s) ? "" : s;
    }

    private String currentHeader() {
        return doubles.isSelected() ? sel(cboHeaderDoubles) : sel(cboHeaderSingles);
    }

    /* =================== DATA LOADS =================== */

    private void reloadListsFromDb() throws SQLException {
        guard.runSilently(() -> {
            setPlaceholder(cboHeaderSingles, PH_HEADER_S, true);
            setPlaceholder(cboHeaderDoubles, PH_HEADER_D, true);
        });
        headerKnrSingles.clear();
        headerKnrDoubles.clear();

        if (conn != null) {
            Map<String, Integer>[] maps = noiDungService.getAllNoiDungType();
            maps[0].forEach((k, v) -> {
                cboHeaderSingles.addItem(k);
                headerKnrSingles.put(k, v);
            });
            maps[1].forEach((k, v) -> {
                cboHeaderDoubles.addItem(k);
                headerKnrDoubles.put(k, v);
            });
        }

        guard.runSilently(() -> {
            setPlaceholder(cboNameA, PH_PLAYER, false);
            setPlaceholder(cboNameB, PH_PLAYER, false);
            setTeamPlaceholder(cboTeamA);
            cboTeamA.setEnabled(false);
            setTeamPlaceholder(cboTeamB);
            cboTeamB.setEnabled(false);
        });
    }

    private void onHeaderSinglesChosen() {
        String header = sel(cboHeaderSingles);
        if (header.isBlank() || conn == null) {
            guard.runSilently(() -> {
                setPlaceholder(cboNameA, PH_PLAYER, false);
                setPlaceholder(cboNameB, PH_PLAYER, false);
            });
            mini.setHeader("TRẬN ĐẤU");
            setSinglesVisible(true);
            setTeamsVisible(false);
            return;
        }
        Integer knr = headerKnrSingles.get(header);
        int vernr = new Prefs().getInt("selectedGiaiDauId", -1);
        if (knr == null || vernr <= 0)
            return;

        singlesNameToId.clear();
        singlesNameToId.putAll(vdvService.loadSinglesNames(knr, vernr));

        guard.runSilently(() -> {
            cboNameA.removeAllItems();
            cboNameB.removeAllItems();
            for (String nm : singlesNameToId.keySet()) {
                cboNameA.addItem(nm);
                cboNameB.addItem(nm);
            }
            if (cboNameA.getItemCount() >= 2) {
                guard.runSilently(() -> {
                    cboNameA.setSelectedIndex(0);
                    cboNameB.setSelectedIndex(1);
                });
            } else if (cboNameA.getItemCount() == 1) {
                guard.runSilently(() -> {
                    cboNameA.setSelectedIndex(0);
                    cboNameB.setSelectedIndex(0);
                });
            }
            cboNameA.setEnabled(true);
            cboNameB.setEnabled(true);
            cboTeamA.setEnabled(false);
            cboTeamB.setEnabled(false);
        });

        mini.setHeader(header);
        logger.chooseSinglesHeader(header, knr);
        updateFromVdv();
    }

    private void onHeaderDoublesChosen() {
        String header = sel(cboHeaderDoubles);
        if (header.isBlank() || conn == null) {
            guard.runSilently(() -> {
                setTeamPlaceholder(cboTeamA);
                setTeamPlaceholder(cboTeamB);
            });
            mini.setHeader("TRẬN ĐẤU");
            setSinglesVisible(false);
            setTeamsVisible(true);
            return;
        }
        Integer knr = headerKnrDoubles.get(header);
        int vernr = new Prefs().getInt("selectedGiaiDauId", -1);
        if (knr == null || vernr <= 0)
            return;

        DoiService doiService = new DoiService(conn);
        List<DangKiDoi> teams = doiService.getTeamsByNoiDungVaGiai(knr, vernr);

        guard.runSilently(() -> {
            setTeamPlaceholder(cboTeamA);
            setTeamPlaceholder(cboTeamB);
            for (DangKiDoi t : teams) {
                cboTeamA.addItem(t);
                cboTeamB.addItem(t);
            }
            cboTeamA.setEnabled(true);
            cboTeamB.setEnabled(true);
            if (cboTeamA.getItemCount() > 1)
                guard.runSilently(() -> cboTeamA.setSelectedIndex(1));
            if (cboTeamB.getItemCount() > 2)
                guard.runSilently(() -> cboTeamB.setSelectedIndex(2));
        });

        mini.setHeader(header);
        logger.chooseDoublesHeader(header, knr);
        updateFromTeams();
    }

    private void updateFromTeams() {
        if (!doubles.isSelected())
            return;
        DangKiDoi ta = (DangKiDoi) cboTeamA.getSelectedItem();
        DangKiDoi tb = (DangKiDoi) cboTeamB.getSelectedItem();
        if (ta == null || tb == null || ta.getIdTeam() == null || tb.getIdTeam() == null || ta.getIdTeam() < 0
                || tb.getIdTeam() < 0)
            return;
        match.setNames(ta.getTenTeam(), tb.getTenTeam());
        // Lấy tên CLB: ưu tiên CLB của đội; nếu trống thì rơi xuống CLB của VĐV trong
        // đội
        try {
            DoiService doiService = new DoiService(conn);
            VanDongVien[] pa = doiService.getTeamPlayers(ta.getIdTeam());
            VanDongVien[] pb = doiService.getTeamPlayers(tb.getIdTeam());
            String clubA = resolveClubForTeam(ta, pa);
            String clubB = resolveClubForTeam(tb, pb);
            match.setClubs(clubA, clubB);
        } catch (Exception ex) {
            // Fallback an toàn nếu có lỗi I/O
            match.setClubs(getClubNameById(ta.getIdClb()), getClubNameById(tb.getIdClb()));
        }
        logger.chooseTeamA(ta.getTenTeam(), ta.getIdTeam());
        logger.chooseTeamB(tb.getTenTeam(), tb.getIdTeam());
    }

    /** cập nhật tên khi đấu đơn, dùng String + map id */
    private void updateFromVdv() {
        if (doubles.isSelected())
            return; // chỉ áp dụng cho ĐƠN
        String nameA = sel(cboNameA);
        String nameB = sel(cboNameB);
        if (nameA.isBlank() || nameB.isBlank())
            return;
        match.setNames(nameA, nameB);
        Integer idA = singlesNameToId.getOrDefault(nameA, -1);
        Integer idB = singlesNameToId.getOrDefault(nameB, -1);
        // Lấy CLB từ VĐV và set vào match
        String clubA = getClubNameByVdvId(idA);
        String clubB = getClubNameByVdvId(idB);
        match.setClubs(clubA, clubB);
        logger.choosePlayerA(nameA, idA);
        logger.choosePlayerB(nameB, idB);
    }

    private void ensureDifferentTeamsAndUpdate() {
        if (!doubles.isSelected() || guard.isSuppressed())
            return;
        DangKiDoi ta = (DangKiDoi) cboTeamA.getSelectedItem();
        DangKiDoi tb = (DangKiDoi) cboTeamB.getSelectedItem();
        if (ta != null && tb != null && ta.getIdTeam() != null && tb.getIdTeam() != null
                && ta.getIdTeam() >= 0 && tb.getIdTeam() >= 0 && ta.getIdTeam().equals(tb.getIdTeam())) {
            guard.runSilently(() -> {
                if (cboTeamB.getItemCount() > 2) {
                    int alt = (cboTeamA.getSelectedIndex() == 1) ? 2 : 1;
                    if (alt < cboTeamB.getItemCount())
                        cboTeamB.setSelectedIndex(alt);
                }
            });
            JOptionPane.showMessageDialog(this, "Đội A và Đội B không được trùng nhau.", "Chọn đội",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        updateFromTeams();
    }

    // vdv a và vdv b ko trùng nhau (đơn)
    private void ensureDifferentVdvAndUpdate() {
        if (doubles.isSelected() || guard.isSuppressed())
            return;
        String nameA = (String) cboNameA.getSelectedItem();
        String nameB = (String) cboNameB.getSelectedItem();
        if (nameA != null && nameB != null && !isPh(nameA) && !isPh(nameB) && nameA.equals(nameB)) {
            guard.runSilently(() -> {
                if (cboNameB.getItemCount() > 1) {
                    int alt = (cboNameA.getSelectedIndex() == 0) ? 1 : 0;
                    if (alt < cboNameB.getItemCount())
                        cboNameB.setSelectedIndex(alt);
                }
            });
            JOptionPane.showMessageDialog(this, "VĐV A và VĐV B không được trùng nhau.", "Chọn VĐV",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        updateFromVdv();
    }

    private void toggleSinglesOrDoubles() {
        boolean isD = doubles.isSelected();

        setHeadersVisibility(!isD, isD);
        setSinglesVisible(!isD);
        setTeamsVisible(isD);

        cboHeaderSingles.setEnabled(!isD);
        cboHeaderDoubles.setEnabled(isD);

        cboNameA.setEnabled(!isD);
        cboNameB.setEnabled(!isD);
        cboTeamA.setEnabled(isD);
        cboTeamB.setEnabled(isD);

        if (isD)
            onHeaderDoublesChosen();
        else
            onHeaderSinglesChosen();
    }

    /* =================== MATCH LIFECYCLE =================== */

    private void onStart() {
        cancelFinishTimer();
        String header = currentHeader();

        if (header.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Nội dung.", "Thiếu nội dung",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int bo = switch (bestOf.getSelectedIndex()) {
            case 0 -> 1;
            case 1 -> 3;
            default -> 3;
        };
        match.setBestOf(bo);

        String displayKind = (cboDisplayKind.getSelectedIndex() == 0) ? "VERTICAL" : "HORIZONTAL";
        String clientName = customClientName != null ? customClientName : System.getProperty("user.name", "client");
        String hostShown = "";

        if (doubles.isSelected()) {
            DangKiDoi ta = (DangKiDoi) cboTeamA.getSelectedItem();
            DangKiDoi tb = (DangKiDoi) cboTeamB.getSelectedItem();
            if (ta == null || tb == null || ta.getIdTeam() == null || tb.getIdTeam() == null || ta.getIdTeam() < 0
                    || tb.getIdTeam() < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn Đội A/B.", "Thiếu đội", JOptionPane.WARNING_MESSAGE);
                return;
            }
            DoiService doiService = new DoiService(conn);
            VanDongVien[] pa = doiService.getTeamPlayers(ta.getIdTeam());
            VanDongVien[] pb = doiService.getTeamPlayers(tb.getIdTeam());
            if (pa == null || pa.length == 0 || pb == null || pb.length == 0) {
                JOptionPane.showMessageDialog(this, "Đội chưa có đủ VĐV.", "Thiếu dữ liệu đội",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String fullNameA = buildFullTeamName(pa);
            String fullNameB = buildFullTeamName(pb);
            match.setDoubles(true);
            match.setNames(fullNameA, fullNameB);
            // Set CLB ưu tiên theo đội, nếu trống thì dùng CLB của VĐV trong đội
            Integer idA = ta.getIdTeam();
            Integer idB = tb.getIdTeam();
            match.setClubs(doiService.getClubNameByTeamId(idA), doiService.getClubNameByTeamId(idB));
            mini.setHeader(header);

            // IMPORTANT: Gọi startBroadcast TRƯỚC match.startMatch để tránh property change
            // trigger broadcaster cũ
            scoreboardSvc.startBroadcast(match, selectedIf, clientName, hostShown, displayKind,
                    header, true, fullNameA, fullNameB, courtId);

            match.startMatch(initialServer.getSelectedIndex());
            // Lấy hoặc tạo ID trận cho lựa chọn hiện tại, rồi liên kết vào sơ đồ
            try {
                if (conn != null) {
                    int theThuc = (bo == 1 ? 1 : 3); // map BO -> theThuc
                    int san = courtNumber; // lấy số sân từ courtNumber
                    int idGiai = prefs.getInt("selectedGiaiDauId", -1);
                    // Lấy ID_NOIDUNG từ map dropdown thay vì tìm lại từ DB
                    Integer idNoiDungObj = headerKnrDoubles.get(header);
                    if (idNoiDungObj == null) {
                        throw new SQLException("Không tìm thấy Nội Dung: " + header);
                    }
                    int idNoidung = idNoiDungObj;
                    int soDo = -1;
                    // Chỉ tìm soDo nếu đã có matchId (trận đã được tạo trước đó)
                    String currentMatchIdStr = match.getMatchId();
                    if (currentMatchIdStr != null && !currentMatchIdStr.isBlank()) {
                        soDo = soDoDoiService.findSoDoByMatchId(idGiai, idNoidung, currentMatchIdStr);
                    } else {
                        soDo = soDoDoiService.findSoDoByTeamNames(idGiai, idNoidung, ta.getTenTeam(), tb.getTenTeam());
                    }
                    String existing = resolveExistingMatchId(header, /* isDoubles */ true, null, null, ta, tb, soDo);
                    if (existing != null && !existing.isBlank()) {
                        currentMatchId = existing;
                        match.setMatchId(currentMatchId);
                        try {
                            boolean reset = maybePromptResetExistingMatch(currentMatchId, header, true, fullNameA,
                                    fullNameB);
                            if (reset) {
                                restartSetPending = true;
                            }
                        } catch (Exception exPrompt) {
                        }
                        ensureAndAlignMatchRecord(currentMatchId, theThuc, san);
                    } else {
                        currentMatchId = chiTietTranDauService.createV7(LocalDateTime.now(), theThuc, san);
                        match.setMatchId(currentMatchId);
                        soDoDoiService.linkTranDauByTeamNames(idGiai, idNoidung, soDo, ta.getTenTeam(), tb.getTenTeam(),
                                currentMatchId);
                    }
                }
            } catch (Exception ex) {
            }
            SoundPlayer.playStartIfEnabled();
            hasStarted = true;
            afterStartUi();
            logger.startDoubles(header, ta.getTenTeam(), ta.getIdTeam(), tb.getTenTeam(), tb.getIdTeam(), bo);
            updateRemoteLinkUi();
        } else {
            String nameA = sel(cboNameA);
            String nameB = sel(cboNameB);

            if (nameA.isBlank() || nameB.isBlank()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn VĐV cho Đội A và Đội B.", "Thiếu VĐV",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            match.setDoubles(false);
            match.setNames(nameA, nameB);
            // Set CLB theo VĐV
            Integer idA = singlesNameToId.getOrDefault(nameA, -1);
            Integer idB = singlesNameToId.getOrDefault(nameB, -1);
            match.setClubs(getClubNameByVdvId(idA), getClubNameByVdvId(idB));
            mini.setHeader(header);

            // IMPORTANT: Gọi startBroadcast TRƯỚC match.startMatch để tránh property change
            // trigger broadcaster cũ
            scoreboardSvc.startBroadcast(match, selectedIf, clientName, hostShown, displayKind,
                    header, false, nameA, nameB, courtId);

            match.startMatch(initialServer.getSelectedIndex());
            // Lấy hoặc tạo ID trận cho lựa chọn hiện tại, rồi liên kết vào sơ đồ
            try {
                if (conn != null) {
                    int theThuc = (bo == 1 ? 1 : 3);
                    int san = courtNumber; // lấy số sân từ courtNumber
                    Integer idAVal = singlesNameToId.getOrDefault(nameA, -1);
                    Integer idBVal = singlesNameToId.getOrDefault(nameB, -1);
                    int idGiai = prefs.getInt("selectedGiaiDauId", -1);
                    System.out.println("competition:" + idGiai);
                    // Lấy ID_NOIDUNG từ map dropdown thay vì tìm lại từ DB
                    Integer idNoiDungObj = headerKnrSingles.get(header);
                    if (idNoiDungObj == null) {
                        throw new SQLException("Không tìm thấy Nội Dung: " + header);
                    }
                    int idNoidung = idNoiDungObj;
                    int soDo = -1;
                    soDo = soDoCaNhanService.findSoDoByPlayerIds(idGiai, idNoidung, idA, idB);
                    String existing = resolveExistingMatchId(header, /* isDoubles */ false, idAVal, idBVal, null, null,
                            soDo);
                    if (existing != null && !existing.isBlank()) {
                        currentMatchId = existing;
                        match.setMatchId(currentMatchId);
                        try {
                            boolean reset = maybePromptResetExistingMatch(currentMatchId, header, false, nameA, nameB);
                            if (reset) {
                                // Lần +1 đầu tiên sau khi đặt lại phải ghi mới
                                restartSetPending = true;
                            }
                        } catch (Exception exPrompt) {
                            logger.logTs("Lỗi khi xác nhận đặt lại trận có sẵn: %s", exPrompt.getMessage());
                        }
                        ensureAndAlignMatchRecord(currentMatchId, theThuc, san);
                    } else {
                        currentMatchId = chiTietTranDauService.createV7(java.time.LocalDateTime.now(), theThuc, san);
                        soDoCaNhanService.linkTranDauByVdvIds(idGiai, idNoidung, idA, idB, soDo, currentMatchId);
                        match.setMatchId(currentMatchId);
                    }
                }
            } catch (Exception ex) {
            }
            SoundPlayer.playStartIfEnabled();

            hasStarted = true;
            afterStartUi();
            // openDisplayAuto();

            logger.startSingles(header, nameA, idA, nameB, idB, bo);
            updateRemoteLinkUi();
        }
    }

    private boolean maybePromptResetExistingMatch(String matchId, String header, boolean isDoubles, String nameA,
            String nameB) {
        if (conn == null || matchId == null || matchId.isBlank())
            return false;
        try {
            var cur = chiTietTranDauService.get(matchId);
            List<ChiTietVan> sets = chiTietVanService.listByMatch(matchId);

            StringBuilder sb = new StringBuilder();
            sb.append("Trận này đã có sẵn trong CSDL.\n\n");
            sb.append("ID: ").append(matchId).append('\n');
            sb.append("Nội dung: ").append(header).append(isDoubles ? " (Đôi)" : " (Đơn)").append('\n');
            sb.append("A: ").append(nameA != null ? nameA : "-").append('\n');
            sb.append("B: ").append(nameB != null ? nameB : "-").append('\n');
            try {
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter
                        .ofPattern("dd/MM/yyyy HH:mm:ss");
                sb.append("Thể thức: BO").append(cur.getTheThuc() != null ? cur.getTheThuc() : 0);
                sb.append(", Sân: ").append(cur.getSan() != null ? cur.getSan() : 0).append('\n');
                sb.append("Bắt đầu: ")
                        .append(cur.getBatDau() != null ? cur.getBatDau().format(dtf) : "-")
                        .append("  |  Cập nhật: ")
                        .append(cur.getKetThuc() != null ? cur.getKetThuc().format(dtf) : "-")
                        .append('\n');
            } catch (Exception ignore) {
            }
            sb.append("Số chi tiết ván hiện có: ").append(sets != null ? sets.size() : 0).append('\n');
            // Hiển thị điểm các ván và tỉ số ván thắng tổng
            if (sets != null && !sets.isEmpty()) {
                try {
                    // Sắp xếp theo số ván tăng dần nếu có
                    sets.sort(Comparator.comparing(
                            com.example.btms.model.match.ChiTietVan::getSetNo,
                            Comparator.nullsLast(Integer::compareTo)));
                } catch (Exception ignore) {
                }
                int gamesA = 0, gamesB = 0;
                sb.append("Điểm các ván:\n");
                for (var v : sets) {
                    Integer setNo = v.getSetNo();
                    int d1 = v.getTongDiem1() != null ? v.getTongDiem1() : 0;
                    int d2 = v.getTongDiem2() != null ? v.getTongDiem2() : 0;
                    sb.append("  Ván ").append(setNo != null ? setNo : 0).append(": ")
                            .append(d1).append(" - ").append(d2).append('\n');
                    if (d1 != d2) {
                        if (d1 > d2)
                            gamesA++;
                        else
                            gamesB++;
                    }
                }
                sb.append("Ván thắng: ").append(gamesA).append(" - ").append(gamesB).append("\n\n");
            } else {
                sb.append('\n');
            }
            sb.append(
                    "Bạn có muốn ĐẶT LẠI trận này?\nChọn 'Có' để xóa toàn bộ chi tiết ván (CHI_TIET_VAN) và ghi lại từ đầu.\nChọn 'Không' để tiếp tục sử dụng dữ liệu hiện có.");

            int ans = JOptionPane.showConfirmDialog(this, sb.toString(),
                    "Trận đã có ID — đặt lại?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans == JOptionPane.YES_OPTION) {
                int deleted = 0;
                if (sets != null) {
                    for (var v : sets) {
                        try {
                            if (v != null && v.getSetNo() != null)
                                chiTietVanService.delete(matchId, v.getSetNo());
                            deleted++;
                        } catch (Exception ignore) {
                        }
                    }
                }
                logger.logTs("Đã xóa %d bản ghi CHI_TIET_VAN cho matchId=%s", deleted, matchId);
                try {
                    JOptionPane.showMessageDialog(this,
                            "Đã đặt lại trận. Chi tiết ván đã được xóa (" + deleted + ").\nBạn có thể ghi lại từ đầu.",
                            "Đặt lại thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ignore) {
                }
                return true;
            } else {
                try {
                    restoreMatchStateFromDatabase(matchId);
                } catch (Exception exRestore) {
                    logger.logTs("❌ Lỗi restore match state: %s", exRestore.getMessage());
                }
                return false;
            }
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * Ghi log trạng thái bracket để theo dõi, KHÔNG xóa ID trận cũ.
     * Giữ nguyên lịch sử các trận đấu để có thể xem chi tiết sau này.
     */
    private void cleanupDuplicateMatchIds(String header, boolean isDoubles, String newMatchId,
            Integer idVdvA, Integer idVdvB, DangKiDoi teamA, DangKiDoi teamB) {
        if (conn == null || newMatchId == null || newMatchId.isBlank())
            return;

        try {
            int idGiai = new Prefs().getInt("selectedGiaiDauId", -1);
            if (idGiai <= 0)
                return;
            Integer idNoiDung = isDoubles ? headerKnrDoubles.get(header) : headerKnrSingles.get(header);
            if (idNoiDung == null || idNoiDung <= 0)
                return;

            if (!isDoubles && idVdvA != null && idVdvA > 0 && idVdvB != null && idVdvB > 0) {
                int soDo = soDoCaNhanService.findSoDoByMatchId(idGiai, idNoiDung, newMatchId);
                List<SoDoCaNhan> rows = soDoCaNhanService.list(idGiai, idNoiDung, soDo);

                Map<String, List<SoDoCaNhan>> matchGroups = new HashMap<>();
                for (var r : rows) {
                    if (r.getIdTranDau() != null && !r.getIdTranDau().isBlank()) {
                        matchGroups.computeIfAbsent(r.getIdTranDau(), k -> new ArrayList<>()).add(r);
                    }
                }
                Map<String, List<Integer>> existingMatches = new HashMap<>();

                for (var r : rows) {
                    if (r.getIdTranDau() != null && !r.getIdTranDau().isBlank()) {
                        existingMatches.computeIfAbsent(r.getIdTranDau(), k -> new ArrayList<>())
                                .add(r.getIdVdv());
                    }
                }

                for (var entry : existingMatches.entrySet()) {
                    boolean hasA = entry.getValue().contains(idVdvA);
                    boolean hasB = entry.getValue().contains(idVdvB);
                    logger.logTs("Trận hiện có %s: VĐVs %s (có A=%s, có B=%s)",
                            entry.getKey(), entry.getValue(), hasA, hasB);
                }
            }
        } catch (Exception ex) {
            logger.logTs("Lỗi khi làm sạch ID trận trùng lặp: %s", ex.getMessage());
        }
    }

    private void linkMatchIdToBracketForCurrentSelection(String header, boolean isDoubles, String matchId,
            Integer idVdvA, Integer idVdvB, DangKiDoi teamA, DangKiDoi teamB) {
        String nameA, nameB, keyA = "", keyB = "", clubA, clubB;
        if (conn == null || matchId == null || matchId.isBlank())
            return;
        try {
            int idGiai = new Prefs().getInt("selectedGiaiDauId", -1);
            if (idGiai <= 0) {
                return;
            }
            Integer idNoiDung = isDoubles ? headerKnrDoubles.get(header) : headerKnrSingles.get(header);
            if (idNoiDung == null || idNoiDung <= 0) {
                return;
            }

            if (!isDoubles) {
                cleanupDuplicateMatchIds(header, isDoubles, matchId, idVdvA, idVdvB, teamA, teamB);

                int updated = 0;
                int expectedUpdates = 0;
                int updateA = 0, updateB = 0;

                if (idVdvA != null && idVdvA > 0) {
                    expectedUpdates++;
                    int soDoA = soDoCaNhanService.findSoDoByMatchId(idGiai, idNoiDung, matchId);
                    updateA = soDoCaNhanService.linkTranDauByVdv(idGiai, idNoiDung, idVdvA, soDoA, matchId);
                    updated += updateA;
                }
                if (idVdvB != null && idVdvB > 0) {
                    expectedUpdates++;
                    int soDoB = soDoCaNhanService.findSoDoByMatchId(idGiai, idNoiDung, matchId);
                    updateB = soDoCaNhanService.linkTranDauByVdv(idGiai, idNoiDung, idVdvB, soDoB, matchId);
                    updated += updateB;
                }
                // Nếu chỉ 1 VĐV được gán ID trận mới, cảnh báo rõ ràng
                if ((updateA == 0 && updateB > 0) || (updateA > 0 && updateB == 0)) {
                    logger.logTs(
                            "CẢNH BÁO QUAN TRỌNG: Chỉ 1 VĐV được gán ID trận mới! Hãy kiểm tra lại bracket (sơ đồ) để đảm bảo cả 2 VĐV đều có slot trống ở vòng này. Nếu thiếu, cần bổ sung slot cho VĐV còn lại.");
                }
            } else {
                int soDo = soDoDoiService.findSoDoByMatchId(idGiai, idNoiDung, matchId);
                List<SoDoDoi> rows = soDoDoiService.list(idGiai, idNoiDung, soDo);
                int updated = 0;
                if (teamA != null && teamA.getTenTeam() != null) {
                    clubA = getClubNameById(teamA.getIdClb());
                    nameA = teamA.getTenTeam();
                    keyA = (clubA != null && !clubA.isBlank()) ? (nameA + " - " + clubA) : nameA;
                    // Tìm chính xác label đang lưu trong sơ đồ để cập nhật theo đúng chuỗi đó
                    String labelInBracketA = findBracketTeamLabel(rows, nameA, clubA);
                    if (labelInBracketA != null && !labelInBracketA.isBlank()) {
                        updated += soDoDoiService.linkTranDauByTeamName(idGiai, idNoiDung, soDo, labelInBracketA,
                                matchId);
                    } else {
                        // Fallback: thử cả key (Team - Club) và tên đội trần
                        int u1 = soDoDoiService.linkTranDauByTeamName(idGiai, idNoiDung, soDo, keyA, matchId);
                        if (u1 == 0 && nameA != null)
                            u1 = soDoDoiService.linkTranDauByTeamName(idGiai, idNoiDung, soDo, nameA, matchId);
                        updated += u1;
                    }
                }
                if (teamB != null && teamB.getTenTeam() != null) {
                    clubB = getClubNameById(teamB.getIdClb());
                    nameB = teamB.getTenTeam();
                    keyB = (clubB != null && !clubB.isBlank()) ? (nameB + " - " + clubB) : nameB;
                    String labelInBracketB = findBracketTeamLabel(rows, nameB, clubB);
                    if (labelInBracketB != null && !labelInBracketB.isBlank()) {
                        updated += soDoDoiService.linkTranDauByTeamName(idGiai, idNoiDung, soDo, labelInBracketB,
                                matchId);
                    } else {
                        int u2 = soDoDoiService.linkTranDauByTeamName(idGiai, idNoiDung, soDo, keyB, matchId);
                        if (u2 == 0 && nameB != null)
                            u2 = soDoDoiService.linkTranDauByTeamName(idGiai, idNoiDung, soDo, nameB, matchId);
                        updated += u2;
                    }
                }
                logger.logTs("SO_DO_DOI: đã liên kết ID_TRAN_DAU=%s cho %d vị trí (giai=%d, nd=%d, teamA=%s, teamB=%s)",
                        matchId, updated,
                        idGiai, idNoiDung, keyA, keyB);
            }
        } catch (Exception ex) {
            logger.logTs("Lỗi liên kết ID_TRAN_DAU vào sơ đồ: %s", ex.getMessage());
        }
    }

    private String resolveExistingMatchId(String header, boolean isDoubles,
            Integer idVdvA, Integer idVdvB, DangKiDoi teamA, DangKiDoi teamB, int soDo) {
        int idGiai = new Prefs().getInt("selectedGiaiDauId", -1);
        Integer idNoiDung = isDoubles ? headerKnrDoubles.get(header) : headerKnrSingles.get(header);
        try {
            String idA = null;
            String idB = null;
            if (!isDoubles) {
                List<SoDoCaNhan> rows = soDoCaNhanService.list(idGiai, idNoiDung, soDo);

                String commonMatchId = null;
                if (idVdvA != null && idVdvA > 0 && idVdvB != null && idVdvB > 0) {
                    // Nhóm các slots theo ID_TRAN_DAU
                    java.util.Map<String, java.util.List<SoDoCaNhan>> matchToSlots = new java.util.HashMap<>();
                    for (var r : rows) {
                        if (r.getIdTranDau() != null && !r.getIdTranDau().isBlank()) {
                            matchToSlots.computeIfAbsent(r.getIdTranDau(), k -> new java.util.ArrayList<>()).add(r);
                        }
                    }

                    for (var entry : matchToSlots.entrySet()) {
                        var slots = entry.getValue();
                        boolean hasA = false, hasB = false;
                        int minPos = Integer.MAX_VALUE, maxPos = Integer.MIN_VALUE;

                        for (var slot : slots) {
                            if (slot.getIdVdv() != null) {
                                if (slot.getIdVdv().intValue() == idVdvA.intValue())
                                    hasA = true;
                                if (slot.getIdVdv().intValue() == idVdvB.intValue())
                                    hasB = true;
                                minPos = Math.min(minPos, slot.getViTri());
                                maxPos = Math.max(maxPos, slot.getViTri());
                            }
                        }

                        if (hasA && hasB && (maxPos - minPos) <= 4) { // Threshold: cùng vòng
                            commonMatchId = entry.getKey();
                            logger.logTs("Tìm thấy trận chung của VĐV %d và %d: %s (vị trí %d-%d)",
                                    idVdvA, idVdvB, commonMatchId, minPos, maxPos);
                            break;
                        }
                    }
                }

                if (commonMatchId != null) {
                    return commonMatchId; // Trả về ID để maybePromptResetExistingMatch() xử lý
                } else {
                    return null; // Tạo mới
                }
            } else {
                List<SoDoDoi> rows = soDoDoiService.list(idGiai, idNoiDung, soDo);
                String nameA = teamA != null ? teamA.getTenTeam() : null;
                String clubA = (teamA != null) ? getClubNameById(teamA.getIdClb()) : null;
                String keyA = (nameA != null)
                        ? ((clubA != null && !clubA.isBlank()) ? (nameA + " - " + clubA) : nameA)
                        : null;
                String nameB = teamB != null ? teamB.getTenTeam() : null;
                String clubB = (teamB != null) ? getClubNameById(teamB.getIdClb()) : null;
                String keyB = (nameB != null)
                        ? ((clubB != null && !clubB.isBlank()) ? (nameB + " - " + clubB) : nameB)
                        : null;

                String normKeyA = normalizeTeamKey(keyA);
                String normNameA = normalizeTeamKey(nameA);
                String baseA = baseTeamName(nameA);
                String normBaseA = normalizeTeamKey(baseA);

                for (var r : rows) {
                    if (r.getIdTranDau() == null || r.getIdTranDau().isBlank())
                        continue;
                    String ten = r.getTenTeam();
                    if (ten == null)
                        continue;
                    String nTen = normalizeTeamKey(ten);
                    String nBaseTen = normalizeTeamKey(baseTeamName(ten));
                    if ((normKeyA != null && nTen.equals(normKeyA))
                            || (normNameA != null && nTen.equals(normNameA))
                            || (normBaseA != null && nBaseTen.equals(normBaseA))) {
                        idA = r.getIdTranDau();
                        break;
                    }
                }

                String normKeyB = normalizeTeamKey(keyB);
                String normNameB = normalizeTeamKey(nameB);
                String baseB = baseTeamName(nameB);
                String normBaseB = normalizeTeamKey(baseB);

                for (var r : rows) {
                    if (r.getIdTranDau() == null || r.getIdTranDau().isBlank())
                        continue;
                    String ten = r.getTenTeam();
                    if (ten == null)
                        continue;
                    String nTen = normalizeTeamKey(ten);
                    String nBaseTen = normalizeTeamKey(baseTeamName(ten));
                    if ((normKeyB != null && nTen.equals(normKeyB))
                            || (normNameB != null && nTen.equals(normNameB))
                            || (normBaseB != null && nBaseTen.equals(normBaseB))) {
                        idB = r.getIdTranDau();
                        break;
                    }
                }
            }

            if (idA != null && idB != null) {
                if (idA.equals(idB)) {
                    return idA;
                } else {
                    logger.logTs("CẢNH BÁO: Hai bên có ID_TRẬN khác nhau (A=%s, B=%s). Bỏ qua để tạo ID mới.", idA,
                            idB);
                    return null;
                }
            }
            if (idA != null)
                return idA;
            if (idB != null)
                return idB;
        } catch (Exception ex) {
            logger.logTs("Lỗi kiểm tra ID_TRẬN sẵn có: %s", ex.getMessage());
        }
        return null;
    }

    private void ensureAndAlignMatchRecord(String matchId, int theThuc, int san) {
        if (conn == null || matchId == null || matchId.isBlank())
            return;
        try {
            try {
                var cur = chiTietTranDauService.get(matchId);
                boolean needUpdate = false;
                Integer curTheThuc = cur.getTheThuc();
                Integer curSan = cur.getSan();
                int newTheThuc = (curTheThuc != null) ? curTheThuc : theThuc;
                int newSan = (curSan != null) ? curSan : san;
                if (newTheThuc != theThuc) {
                    newTheThuc = theThuc; // align to current selection
                    needUpdate = true;
                }
                if (newSan != san) {
                    newSan = san; // align to current court
                    needUpdate = true;
                }
                if (needUpdate) {
                    chiTietTranDauService.update(matchId, newTheThuc, cur.getIdVdvThang(), cur.getBatDau(),
                            cur.getKetThuc(), newSan);
                }
            } catch (Exception notFound) {
                // Không tồn tại: tạo mới theo cấu hình hiện tại, GIỮ NGUYÊN ID
                var now = java.time.LocalDateTime.now();
                chiTietTranDauService.create(matchId, theThuc, 0 /* chưa biết VĐV thắng */, now, now, san);
            }
        } catch (Exception ex) {
        }
    }

    private void afterStartUi() {
        btnStart.setEnabled(false);
        btnFinish.setEnabled(true);
        btnOpenDisplay.setEnabled(true);
        btnOpenDisplayH.setEnabled(true);
        btnCloseDisplay.setEnabled(true);
        btnReset.setEnabled(true);
        // Hiển thị trạng thái đang thi đấu ngay khi bấm Bắt đầu
        lblStatus.setText("Đang thi đấu");
        if (pauseResume != null) {
            pauseResume.setEnabled(true);
            updatePauseButtonText();
        }
        setScoreButtonsEnabled(true);
        nextGame.setEnabled(false);

        // Disable các controls liên quan đến việc chọn nội dung và VĐV
        disableConfigControls();
    }

    /**
     * Disable các controls liên quan đến việc chọn nội dung và VĐV khi trận đấu đã
     * bắt đầu
     */
    private void disableConfigControls() {
        // Disable combobox nội dung
        cboHeaderSingles.setEnabled(false);
        cboHeaderDoubles.setEnabled(false);

        // Disable combobox VĐV/đội
        cboNameA.setEnabled(false);
        cboNameB.setEnabled(false);
        cboTeamA.setEnabled(false);
        cboTeamB.setEnabled(false);

        // Disable các options khác
        bestOf.setEnabled(false);
        doubles.setEnabled(false);
        initialServer.setEnabled(false);

        // Disable nút reload
        btnReloadLists.setEnabled(false);
    }

    /**
     * Enable lại các controls liên quan đến việc chọn nội dung và VĐV khi kết thúc
     * trận đấu
     */
    private void enableConfigControls() {
        // Enable combobox nội dung
        cboHeaderSingles.setEnabled(true);
        cboHeaderDoubles.setEnabled(true);

        // Enable combobox VĐV/đội
        cboNameA.setEnabled(true);
        cboNameB.setEnabled(true);
        cboTeamA.setEnabled(true);
        cboTeamB.setEnabled(true);

        // Enable các options khác
        bestOf.setEnabled(true);
        doubles.setEnabled(true);
        initialServer.setEnabled(true);

        // Enable nút reload
        btnReloadLists.setEnabled(true);
    }

    // Removed unused no-arg onFinish() to avoid "never used" warning

    // auto=true: gọi từ hẹn giờ khi trận đã kết thúc, bỏ qua xác nhận
    private void onFinish(boolean auto) {
        if (!auto) {
            int ans = JOptionPane.showConfirmDialog(this,
                    "Kết thúc trận hiện tại và sẵn sàng cho trận mới?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) {
                btnFinish.setEnabled(true);
                return;
            }
        }

        closeDisplays();
        try {
            if (conn != null && currentMatchId != null && !currentMatchId.isBlank()) {
                autoAdvanceWinnerToNextRound(currentMatchId);
                var now = LocalDateTime.now();
                // Lấy record hiện tại để lấy các trường khác
                var cur = chiTietTranDauService.get(currentMatchId);
                // Xác định ID VĐV thắng nếu là đơn, nếu không xác định được thì giữ giá trị cũ
                Integer curWinner = cur.getIdVdvThang();
                int idVdvThang = computeWinnerVdvIdOrDefault(curWinner != null ? curWinner : 0);
                chiTietTranDauService.update(currentMatchId, cur.getTheThuc(), idVdvThang, cur.getBatDau(), now,
                        cur.getSan());

                // Đồng thời cập nhật tỉ số cuối cùng vào cột DIEM cho 2 VĐV/đội trong sơ đồ
                try {
                    updateBracketScoresOnFinish(currentMatchId);
                } catch (Exception ex2) {
                    logger.logTs("Lỗi cập nhật DIEM cho sơ đồ khi kết thúc: %s", ex2.getMessage());
                }
            }
        } catch (Exception ex) {
            logger.logTs("Lỗi cập nhật kết thúc trận: %s", ex.getMessage());
        } finally {
            currentMatchId = null;
        }

        // Cập nhật trạng thái sân: đặt trận về trạng thái sẵn sàng (không thi đấu)
        try {
            synchronized (ScoreboardRemote.get().lock()) {
                match.resetAll();
            }
        } catch (Exception ignore) {
        }
        // Báo cho listeners (overview/tổng quan) cập nhật ngay
        try {
            firePropertyChange("matchFinishedManual", false, true);
        } catch (Exception ignore) {
        }

        hasStarted = false;
        btnStart.setEnabled(true);
        btnFinish.setEnabled(false);
        btnOpenDisplay.setEnabled(false);
        btnOpenDisplayH.setEnabled(false);
        btnCloseDisplay.setEnabled(false);
        btnReset.setEnabled(false);
        if (pauseResume != null) {
            pauseResume.setEnabled(false);
            pauseResume.setText("Tạm dừng");
        }

        setScoreButtonsEnabled(false);
        nextGame.setEnabled(false);

        // Enable lại các controls liên quan đến việc chọn nội dung và VĐV
        enableConfigControls();

        lblStatus.setText("Sẵn sàng");
        lblGame.setText("Ván 1");
        lblGamesWon.setText("Ván: 0 - 0");
        lblServer.setText("Giao cầu: A (R)");

        logger.finishMatch();

        // Reset cờ auto-finish (nếu có) để tránh các lần gọi sau hiểu nhầm trạng thái
        cancelFinishTimer();
    }

    /**
     * Trả về ID_VDV_THANG nếu trận là ĐƠN và xác định được bên thắng từ tên VĐV
     * hiện tại;
     * nếu không, trả về giá trị mặc định (thường là giá trị đang có trong DB hoặc
     * 0).
     */
    private int computeWinnerVdvIdOrDefault(int defaultValue) {
        try {
            if (match != null && !match.isDoubles() && match.isMatchFinished()) {
                int[] games = match.getGames();
                if (games[0] == games[1])
                    return defaultValue; // không rõ bên thắng
                int winnerSide = (games[0] > games[1]) ? 0 : 1;
                String[] names = match.getNames();
                String winnerName = names[winnerSide];
                if (winnerName != null && !winnerName.isBlank()) {
                    Integer id = singlesNameToId.get(winnerName);
                    if (id != null && id > 0)
                        return id;
                }
            }
        } catch (Exception ignore) {
        }
        return defaultValue;
    }

    private void updateBracketScoresOnFinish(String matchId) {
        if (conn == null || matchId == null || matchId.isBlank())
            return;
        // NOTE: Auto-advance đã được gọi trong onFinish(), không gọi lại ở đây để tránh
        // trùng lặp

        // Lấy context hiện tại
        String header = currentHeader();
        int idGiai = new Prefs().getInt("selectedGiaiDauId", -1);
        if (header == null || header.isBlank() || idGiai <= 0)
            return; // thiếu ngữ cảnh

        boolean isDoubles = doubles.isSelected();
        Integer idNoiDung = isDoubles ? headerKnrDoubles.get(header) : headerKnrSingles.get(header);
        if (idNoiDung == null || idNoiDung <= 0)
            return;

        // Tính tỉ số cuối (số ván thắng) từ match hiện tại
        int[] games = match != null ? match.getGames() : new int[] { 0, 0 };
        int diemA = games[0];
        int diemB = games[1];

        if (!isDoubles) {
            // ĐƠN: xác định theo ID_VDV A/B + ID_TRAN_DAU
            int soDo = soDoCaNhanService.findSoDoByMatchId(idGiai, idNoiDung, matchId);
            List<SoDoCaNhan> rows = soDoCaNhanService.list(idGiai, idNoiDung, soDo);
            String nameA = sel(cboNameA);
            String nameB = sel(cboNameB);
            Integer idVdvA = (nameA == null || nameA.isBlank()) ? null : singlesNameToId.get(nameA);
            Integer idVdvB = (nameB == null || nameB.isBlank()) ? null : singlesNameToId.get(nameB);
            int updatedA = 0, updatedB = 0;
            for (var r : rows) {
                if (r.getIdTranDau() != null && r.getIdTranDau().equals(matchId)) {
                    if (idVdvA != null && idVdvA > 0 && r.getIdVdv() != null && r.getIdVdv().equals(idVdvA)) {
                        soDoCaNhanService.setDiem(idGiai, idNoiDung, r.getViTri(), diemA);
                        updatedA++;
                    } else if (idVdvB != null && r.getIdVdv() != null
                            && r.getIdVdv().equals(idVdvB)) {
                        soDoCaNhanService.setDiem(idGiai, idNoiDung, r.getViTri(), diemB);
                        updatedB++;
                    }
                }
            }
            logger.logTs("Cập nhật DIEM (đơn): A=%d (%d vị trí), B=%d (%d vị trí) [giai=%d, nd=%d]",
                    diemA, updatedA, diemB, updatedB, idGiai, idNoiDung);
        } else {
            // ĐÔI: xác định theo TEN_TEAM A/B + ID_TRAN_DAU (linh hoạt theo tên lưu trong
            // sơ đồ)
            int soDo = soDoDoiService.findSoDoByMatchId(idGiai, idNoiDung, matchId);
            List<SoDoDoi> rows = soDoDoiService.list(idGiai, idNoiDung, soDo);
            DangKiDoi teamA = (DangKiDoi) cboTeamA.getSelectedItem();
            DangKiDoi teamB = (DangKiDoi) cboTeamB.getSelectedItem();
            String tenA = teamA != null ? teamA.getTenTeam() : null;
            String tenB = teamB != null ? teamB.getTenTeam() : null;
            String nA = normalizeTeamKey(tenA);
            String nB = normalizeTeamKey(tenB);
            String nbA = normalizeTeamKey(baseTeamName(tenA));
            String nbB = normalizeTeamKey(baseTeamName(tenB));
            int updatedA = 0, updatedB = 0;
            for (var r : rows) {
                if (r.getIdTranDau() != null && r.getIdTranDau().equals(matchId)) {
                    String rowTeam = r.getTenTeam();
                    String nRow = normalizeTeamKey(rowTeam);
                    String nbRow = normalizeTeamKey(baseTeamName(rowTeam));
                    boolean matchA = (nA != null && nA.equals(nRow)) || (nbA != null && nbA.equals(nbRow));
                    boolean matchB = (nB != null && nB.equals(nRow)) || (nbB != null && nbB.equals(nbRow));
                    if (matchA && !matchB) {
                        soDoDoiService.setDiem(idGiai, idNoiDung, r.getViTri(), diemA);
                        updatedA++;
                    } else if (matchB && !matchA) {
                        soDoDoiService.setDiem(idGiai, idNoiDung, r.getViTri(), diemB);
                        updatedB++;
                    } else if (!matchA && !matchB) {
                        // Không khớp rõ ràng: ưu tiên gán theo lượt đầu tiên gặp (tránh bỏ sót)
                        if (updatedA == 0) {
                            soDoDoiService.setDiem(idGiai, idNoiDung, r.getViTri(), diemA);
                            updatedA++;
                        } else if (updatedB == 0) {
                            soDoDoiService.setDiem(idGiai, idNoiDung, r.getViTri(), diemB);
                            updatedB++;
                        }
                    }
                }
            }
            logger.logTs("Cập nhật DIEM (đôi): A=%d (%d vị trí), B=%d (%d vị trí) [giai=%d, nd=%d]",
                    diemA, updatedA, diemB, updatedB, idGiai, idNoiDung);
        }
    }

    private void onReset() {
        if (!hasStarted) {
            onStart();
            return;
        }
        cancelFinishTimer();
        String header = currentHeader();
        if (header.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Nội dung.", "Thiếu nội dung",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bo = switch (bestOf.getSelectedIndex()) {
            case 0 -> 1;
            case 1 -> 3;
            default -> 5;
        };
        match.setBestOf(bo);

        String displayKind = (cboDisplayKind.getSelectedIndex() == 0) ? "VERTICAL" : "HORIZONTAL";
        String clientName = customClientName != null ? customClientName : System.getProperty("user.name", "client");
        String hostShown = "";

        if (doubles.isSelected()) {
            DangKiDoi ta = (DangKiDoi) cboTeamA.getSelectedItem();
            DangKiDoi tb = (DangKiDoi) cboTeamB.getSelectedItem();
            if (ta == null || tb == null || ta.getIdTeam() == null || tb.getIdTeam() == null || ta.getIdTeam() < 0
                    || tb.getIdTeam() < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn Đội A/B.", "Thiếu đội",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            DoiService doiService = new DoiService(conn);
            VanDongVien[] pa = doiService.getTeamPlayers(ta.getIdTeam());
            VanDongVien[] pb = doiService.getTeamPlayers(tb.getIdTeam());
            if (pa == null || pa.length == 0 || pb == null || pb.length == 0) {
                JOptionPane.showMessageDialog(this, "Đội chưa có đủ VĐV.", "Thiếu dữ liệu đội",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String fullNameA = buildFullTeamName(pa);
            String fullNameB = buildFullTeamName(pb);
            match.setDoubles(true);
            match.setNames(fullNameA, fullNameB);
            // Set CLB ưu tiên theo đội, nếu trống thì dùng CLB của VĐV trong đội
            match.setClubs(
                    resolveClubForTeam(ta, pa),
                    resolveClubForTeam(tb, pb));
            mini.setHeader(header);
            match.startMatch(initialServer.getSelectedIndex());
            hasStarted = true;
            // Đánh dấu ván mới bắt đầu lại → lần +1 đầu tiên sẽ ghi mới (xóa set cũ nếu có)
            restartSetPending = true;
            afterStartUi();
            openDisplayAuto();
            scoreboardSvc.startBroadcast(
                    match, selectedIf, clientName, hostShown, displayKind,
                    header, true, fullNameA, fullNameB, courtId);
            logger.logTs("ĐẶT LẠI ĐÔI: TEAM A=%s (TEAMID=%d) vs TEAM B=%s (TEAMID=%d)",
                    ta.getTenTeam(), ta.getIdTeam(), tb.getTenTeam(), tb.getIdTeam());
            updateRemoteLinkUi();
        } else {
            String nameA = sel(cboNameA);
            String nameB = sel(cboNameB);
            if (nameA.isBlank() || nameB.isBlank()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn VĐV cho Đội A và Đội B.", "Thiếu VĐV",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            match.setDoubles(false);
            match.setNames(nameA, nameB);
            // Set CLB theo VĐV
            Integer idA = singlesNameToId.getOrDefault(nameA, -1);
            Integer idB = singlesNameToId.getOrDefault(nameB, -1);
            match.setClubs(getClubNameByVdvId(idA), getClubNameByVdvId(idB));
            mini.setHeader(header);
            match.startMatch(initialServer.getSelectedIndex());

            hasStarted = true;
            // Đánh dấu ván mới bắt đầu lại → lần +1 đầu tiên sẽ ghi mới (xóa set cũ nếu có)
            restartSetPending = true;
            afterStartUi();
            openDisplayAuto();
            scoreboardSvc.startBroadcast(
                    match, selectedIf, clientName, hostShown, displayKind,
                    header, false, nameA, nameB, courtId);

            // idA/idB đã được khai báo ở trên để set CLB, tái sử dụng cho log
            logger.logTs("ĐẶT LẠI ĐƠN: A=%s (NNR=%d) vs B=%s (NNR=%d)", nameA,
                    singlesNameToId.getOrDefault(nameA, -1), nameB,
                    singlesNameToId.getOrDefault(nameB, -1));
            updateRemoteLinkUi();
        }
    }

    private void openDisplayVertical() {
        if (!hasStarted) {
            JOptionPane.showMessageDialog(this, "Hãy bấm \"Bắt đầu trận\" trước khi mở bảng điểm.", "Chưa bắt đầu",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        logger.openDisplayVertical();
        scoreboardSvc.openVertical(match, Math.max(0, cboScreen.getSelectedIndex()));
        btnCloseDisplay.setEnabled(true);
    }

    private void openDisplayHorizontal() {
        if (!hasStarted) {
            JOptionPane.showMessageDialog(this, "Hãy bấm \"Bắt đầu trận\" trước khi mở bảng điểm.", "Chưa bắt đầu",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        logger.openDisplayHorizontal();
        scoreboardSvc.openHorizontal(match, Math.max(0, cboScreen.getSelectedIndex()), currentHeader());
        btnCloseDisplay.setEnabled(true);
    }

    private void closeDisplays() {
        logger.closeDisplays();
        scoreboardSvc.closeDisplays();
    }

    private void openDisplayAuto() {
        if (cboDisplayKind.getSelectedIndex() == 0)
            openDisplayVertical();
        else
            openDisplayHorizontal();
    }

    private void updateRemoteLinkUi() {
        try {
            String ip = NetworkUtil.getLocalIpv4(selectedIf);

            // Kiểm tra IP có hợp lệ không
            if (ip == null || ip.isEmpty()) {
                lblRemoteUrl.setText("<html><b style='color:red;'>LỖI: Interface '" +
                        (selectedIf != null ? selectedIf.getDisplayName() : "null") +
                        "' không có IPv4 address. Vui lòng chọn interface khác.</b></html>");
                lblRemoteQr.setIcon(null);
                logger.logTs("LỖI: Không thể lấy IP từ interface '%s'. Cần chọn interface khác.",
                        selectedIf != null ? selectedIf.getDisplayName() : "null");
                return;
            }

            // Sử dụng port mặc định
            int port = 2345;

            // Tạo URL với mã PIN
            String pinCode = getCourtPinCode();
            String url = "http://" + ip + ":" + port + "/scoreboard/" + pinCode;

            logger.logTs("Điều khiển trên điện thoại: %s (port %d, IP: %s)", url, port, ip);
            // Lưu URL và cập nhật hiển thị theo trạng thái ẩn/hiện
            currentRemoteUrl = url;
            updateRemoteUrlDisplay();

            // Chỉ tạo và hiển thị QR code khi qrCodeVisible = true
            if (qrCodeVisible) {
                var img = QRCodeUtil.generate(url, 150);
                lblRemoteQr.setIcon(new ImageIcon(img));
            } else {
                lblRemoteQr.setIcon(null);
                lblRemoteQr.setText("");
            }

            // Cập nhật link PIN entry nếu có
            SwingUtilities.invokeLater(() -> {
                try {
                    // Tìm và cập nhật label link PIN
                    for (java.awt.Component comp : getComponents()) {
                        if (comp instanceof JPanel panel) {
                            updatePinLinkInPanel(panel);
                        }
                    }
                } catch (Exception ex) {
                    logger.logTs("Lỗi khi cập nhật link PIN: %s", ex.getMessage());
                }
            });
        } catch (WriterException ex) {
            lblRemoteUrl.setText("<html><b style='color:red;'>LỖI: " + ex.getMessage() + "</b></html>");
            lblRemoteQr.setIcon(null);
            lblRemoteQr.setText("");
            logger.logTs("Lỗi khi cập nhật remote link UI: %s", ex.getMessage());
        }
    }

    private void copyLinkToClipboard() {
        try {
            // Ưu tiên dùng URL hiện tại nếu đã có
            String url = currentRemoteUrl;
            if (url == null || url.isBlank()) {
                String ip = NetworkUtil.getLocalIpv4(selectedIf);
                if (ip == null || ip.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Không thể copy link - Interface '" +
                                    (selectedIf != null ? selectedIf.getDisplayName() : "null") +
                                    "' không có IPv4 address.\nVui lòng chọn interface khác.",
                            "Lỗi copy",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int port = 2345;
                url = "http://" + ip + ":" + port + "/scoreboard/" + getCourtPinCode();
            }

            StringSelection stringSelection = new StringSelection(url);
            Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            logger.logTs("Đã copy link vào clipboard: %s", url);

            // Hiển thị thông báo ngắn
            JOptionPane.showMessageDialog(this,
                    "Đã copy link vào clipboard!\n" + url,
                    "Copy thành công",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (HeadlessException ex) {
            logger.logTs("Lỗi khi copy link: %s", ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi copy link: " + ex.getMessage(),
                    "Lỗi copy",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Cập nhật phần hiển thị link theo trạng thái ẩn/hiện
    private void updateRemoteUrlDisplay() {
        try {
            if (currentRemoteUrl == null || currentRemoteUrl.isBlank()) {
                lblRemoteUrl.setText("-");
                if (btnToggleLinkVisible != null)
                    btnToggleLinkVisible.setEnabled(false);
                return;
            }
            if (btnToggleLinkVisible != null)
                btnToggleLinkVisible.setEnabled(true);
            if (remoteUrlVisible) {
                lblRemoteUrl.setText("<html><b>" + currentRemoteUrl + "</b></html>");
            } else {
                lblRemoteUrl.setText("<html><b>••••••••••</b></html>");
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * Cập nhật hiển thị QR code theo trạng thái ẩn/hiện
     */
    private void updateQrCodeDisplay() {
        try {
            if (qrCodeVisible) {
                // Hiển thị QR code bình thường
                updateRemoteLinkUi(); // Gọi lại để tạo QR code
            } else {
                // Ẩn QR code bằng cách xóa nội dung
                lblRemoteQr.setIcon(null);
                lblRemoteQr.setText("");
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * Chụp ảnh bảng điểm mini và lưu vào folder
     */
    private void captureMiniScoreboard() {
        try {
            // Sử dụng thư mục screenshots trong project
            File projectDir = new File(System.getProperty("user.dir"));
            File screenshotDir = new File(projectDir, "screenshots");
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }

            // Tạo tên file theo ID_TRẬN + thời gian
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new Date());
            String idForName = (currentMatchId != null && !currentMatchId.isBlank()) ? currentMatchId : "no_match_id";
            String fileName = String.format("%s_%s.png", idForName, timestamp);
            File outputFile = new File(screenshotDir, fileName);

            // Chụp ảnh bảng điểm mini
            BufferedImage image = new BufferedImage(
                    mini.getWidth(), mini.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            mini.paint(g2d);
            g2d.dispose();

            // Lưu ảnh
            ImageIO.write(image, "PNG", outputFile);

        } catch (HeadlessException | IOException ex) {
            logger.logTs("Lỗi khi chụp ảnh bảng điểm: %s", ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi chụp ảnh: " + ex.getMessage(),
                    "Lỗi chụp ảnh",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Bỏ phương thức gửi ảnh qua mạng; ảnh được lưu cục bộ và xem trong tab Lịch sử

    private void cancelFinishTimer() {
        if (finishTimer != null) {
            finishTimer.stop();
            finishTimer = null;
        }
        finishScheduled = false;
        screenshotTaken = false; // Reset screenshot flag
        lastScreenshotTime = 0; // Reset timestamp
    }

    /* =================== MATCH -> UI =================== */

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        String propName = evt.getPropertyName();
        BadmintonMatch.Snapshot s = match.snapshot();

        // === 1. Update UI cơ bản ===
        lblGame.setText("Ván " + s.gameNumber + " / BO" + s.bestOf);
        lblGamesWon.setText("Ván: " + s.games[0] + " - " + s.games[1]);

        String court = " (" + (s.score[s.server] % 2 == 0 ? "R" : "L") + ")";
        lblServer.setText("Giao cầu: " + (s.server == 0 ? "A" : "B") + court);

        boolean manualPaused = false;
        try {
            manualPaused = match.isManualPaused();
        } catch (Throwable ignore) {
        }

        // =========================
        // 2. KẾT THÚC TRẬN – CHỈ THEO EVENT
        // =========================
        if ("matchEnd".equals(propName)) {

            if (finishScheduled)
                return;
            finishScheduled = true;

            lblStatus.setText("Trận đấu đã kết thúc");
            lblWinner.setText(s.games[0] > s.games[1] ? s.names[0] : s.names[1]);

            setScoreButtonsEnabled(false);
            nextGame.setEnabled(false);

            logger.logTs("Match finished detected");

            try {
                btnFinish.setEnabled(false);
            } catch (Exception ignored) {
            }

            try {
                if (conn != null && currentMatchId != null && !currentMatchId.isBlank()) {
                    updateBracketScoresOnFinish(currentMatchId);

                    var cur = chiTietTranDauService.get(currentMatchId);
                    Integer curWinner = cur.getIdVdvThang();
                    int idVdvThang = computeWinnerVdvIdOrDefault(curWinner != null ? curWinner : 0);

                    if (idVdvThang != (curWinner != null ? curWinner : 0)) {
                        chiTietTranDauService.update(
                                currentMatchId,
                                cur.getTheThuc(),
                                idVdvThang,
                                cur.getBatDau(),
                                cur.getKetThuc(),
                                cur.getSan());
                    }
                }
            } catch (Exception ex) {
                logger.logTs("Lỗi cập nhật kết quả thắng: %s", ex.getMessage());
            }

            if (!screenshotTaken) {
                screenshotTaken = true;
                captureMiniScoreboard();
            }

            com.example.btms.util.sound.SoundPlayer.playEndIfEnabled();

            cancelFinishTimer();
            finishTimer = new javax.swing.Timer(3000, e -> onFinish(true));
            finishTimer.setRepeats(false);
            finishTimer.start();

            return; // ⛔ cực kỳ quan trọng
        }

        // =========================
        // 3. CÁC TRẠNG THÁI KHÁC
        // =========================
        if (!hasStarted) {

            setScoreButtonsEnabled(false);
            nextGame.setEnabled(false);

        } else if (s.betweenGamesInterval) {

            lblStatus.setText("Nghỉ giữa ván - bấm \"Ván tiếp theo\"");
            setScoreButtonsEnabled(false);
            nextGame.setEnabled(true);

            finishScheduled = false;
            screenshotTaken = false;

            if (pauseResume != null)
                pauseResume.setEnabled(false);

        } else if (manualPaused) {

            lblStatus.setText("Tạm dừng");
            setScoreButtonsEnabled(false);
            nextGame.setEnabled(false);

            finishScheduled = false;
            screenshotTaken = false;

            if (pauseResume != null) {
                pauseResume.setEnabled(true);
                pauseResume.setText("Tiếp tục");
            }

        } else {

            lblStatus.setText("Đang thi đấu");
            setScoreButtonsEnabled(true);
            nextGame.setEnabled(false);

            finishScheduled = false;
            screenshotTaken = false;

            if (pauseResume != null) {
                pauseResume.setEnabled(true);
                pauseResume.setText("Tạm dừng");
            }
        }

        // === 4. Swap ===
        if ("swap".equals(propName) && mini != null) {
            mini.forceRefresh();
        }

        // === 5. Kết thúc ván ===
        if ("gameEnd".equals(propName)) {
            updateChiTietVanTotalsOnly();
        }
    }
    /* =================== Misc =================== */

    private void setScoreButtonsEnabled(boolean on) {
        if (aPlus != null)
            aPlus.setEnabled(on);
        if (bPlus != null)
            bPlus.setEnabled(on);
        if (aMinus != null)
            aMinus.setEnabled(on);
        if (bMinus != null)
            bMinus.setEnabled(on);
        if (undo != null)
            undo.setEnabled(on);
        if (swapEnds != null)
            swapEnds.setEnabled(on);
        if (toggleServe != null)
            toggleServe.setEnabled(on);
    }

    private void installKeyBindings() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "aPlus");
        getActionMap().put("aPlus", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (aPlus != null && aPlus.isEnabled())
                    aPlus.doClick();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl ENTER"), "bPlus");
        getActionMap().put("bPlus", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bPlus != null && bPlus.isEnabled())
                    bPlus.doClick();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "undo");
        getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undo != null && undo.isEnabled())
                    undo.doClick();
            }
        });
    }

    private void populateScreens() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        cboScreen.removeAllItems();
        if (screens == null || screens.length == 0) {
            cboScreen.addItem("Màn hình 1 (mặc định)");
        } else {
            for (int i = 0; i < screens.length; i++) {
                Rectangle b = screens[i].getDefaultConfiguration().getBounds();
                String item = String.format("Màn hình %d — %dx%d @ (%d,%d)", i + 1, b.width, b.height, b.x, b.y);
                cboScreen.addItem(item);
            }
            cboScreen.setSelectedIndex(0);
        }
    }

    private void restoreSplitLocations() {
        try {
            int v;
            v = prefs.getInt("split.leftVert", -1);
            if (v >= 0 && leftVert != null)
                leftVert.setDividerLocation(v);
            v = prefs.getInt("split.midVert", -1);
            if (v >= 0 && midVert != null)
                midVert.setDividerLocation(v);
            v = prefs.getInt("split.rightVert", -1);
            if (v >= 0 && rightVert != null)
                rightVert.setDividerLocation(v);
            v = prefs.getInt("split.centerRight", -1);
            if (v >= 0 && centerRightSplit != null)
                centerRightSplit.setDividerLocation(v);
            v = prefs.getInt("split.main", -1);
            if (v >= 0 && mainSplit != null)
                mainSplit.setDividerLocation(v);
        } catch (Exception ignore) {
        }
    }

    /** Thu nhỏ (iconify) các cửa sổ scoreboard thuộc sân này */
    public void minimizeDisplays() {
        try {
            scoreboardSvc.minimizeDisplays();
        } catch (Throwable t) {
            // Fallback nhẹ nhàng
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && w instanceof Frame f) {
                    f.setState(Frame.ICONIFIED);
                }
            }
        }
    }

    /**
     * Lấy mã PIN của sân hiện tại
     * Cần được set từ MultiCourtControlPanel khi tạo sân
     */
    private String courtPinCode = "0000"; // Mặc định

    public void setCourtPinCode(String pinCode) {
        this.courtPinCode = pinCode;
        // Cập nhật match để sử dụng match của PIN này
        switchToMatchByPin();
    }

    private String getCourtPinCode() {
        return courtPinCode;
    }

    /**
     * Lấy URL để nhập mã PIN
     */
    private String getPinEntryUrl() {
        try {
            String ip = NetworkUtil.getLocalIpv4(selectedIf);
            if (ip == null || ip.isEmpty()) {
                return "LỖI: Interface không có IPv4";
            }
            int port = 2345;
            return "http://" + ip + ":" + port + "/pin";
        } catch (Exception ex) {
            return "LỖI: " + ex.getMessage();
        }
    }

    /* =================== TEAM NAME MATCHING HELPERS (đôi) =================== */

    /**
     * Chuẩn hoá key tên đội để so sánh linh hoạt:
     * - lower-case, trim
     * - thay các loại dấu gạch (–, —) thành '-'
     * - gom nhiều khoảng trắng về 1
     * - bỏ khoảng trắng thừa quanh dấu '-'
     */
    private static String normalizeTeamKey(String s) {
        if (s == null)
            return null;
        String t = s.toLowerCase().trim();
        // thay các dấu gạch dài/khác loại về '-'
        t = t.replace('–', '-').replace('—', '-');
        // chuẩn hoá khoảng trắng quanh '-'
        t = t.replaceAll("\\s*-\\s*", " - ");
        // gom nhiều khoảng trắng về 1
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    /** Lấy phần tên đội trước phần CLB nếu có dạng "Tên đội - Tên CLB" */
    private static String baseTeamName(String s) {
        if (s == null)
            return null;
        String t = s;
        int idx = t.indexOf("-");
        if (idx < 0) {
            // thử dấu gạch dài
            idx = t.indexOf('–');
            if (idx < 0)
                idx = t.indexOf('—');
        }
        if (idx >= 0)
            return t.substring(0, idx).trim();
        return t.trim();
    }

    /**
     * Tìm chính xác label (TEN_TEAM) đang lưu trong bảng sơ đồ dựa trên tên đội
     * hiện
     * chọn và CLB (nếu có). Trả về đúng chuỗi TEN_TEAM trong DB để update bằng
     * equals.
     */
    private String findBracketTeamLabel(List<com.example.btms.model.bracket.SoDoDoi> rows, String teamName,
            String clubName) {
        if (rows == null || rows.isEmpty() || (teamName == null || teamName.isBlank()))
            return null;

        String keyWithClub = (clubName != null && !clubName.isBlank()) ? (teamName + " - " + clubName) : null;

        // 1) So khớp EXACT (không phân biệt hoa/thường)
        for (var r : rows) {
            String ten = r.getTenTeam();
            if (ten == null)
                continue;
            if ((keyWithClub != null && ten.equalsIgnoreCase(keyWithClub)) || ten.equalsIgnoreCase(teamName))
                return ten;
        }

        // 2) So khớp NORMALIZED (chuẩn hoá gạch/khoảng trắng)
        String nKeyWithClub = normalizeTeamKey(keyWithClub);
        String nName = normalizeTeamKey(teamName);
        for (var r : rows) {
            String ten = r.getTenTeam();
            if (ten == null)
                continue;
            String nTen = normalizeTeamKey(ten);
            if ((nKeyWithClub != null && nTen.equals(nKeyWithClub)) || (nName != null && nTen.equals(nName)))
                return ten;
        }

        // 3) So khớp theo BASE NAME (bỏ phần sau dấu '-')
        String baseSel = baseTeamName(teamName);
        String nBaseSel = normalizeTeamKey(baseSel);
        for (var r : rows) {
            String ten = r.getTenTeam();
            if (ten == null)
                continue;
            String nBaseTen = normalizeTeamKey(baseTeamName(ten));
            if (nBaseSel != null && nBaseSel.equals(nBaseTen))
                return ten;
        }

        // 4) Fallback nhẹ: bắt đầu bằng hoặc chứa (tránh bắt trùng quá rộng)
        for (var r : rows) {
            String ten = r.getTenTeam();
            if (ten == null)
                continue;
            String nTen = normalizeTeamKey(ten);
            if (nTen != null && nName != null && (nTen.startsWith(nName) || nName.startsWith(nTen)))
                return ten;
        }

        return null;
    }

    /**
     * Copy link nhập PIN vào clipboard
     */
    private void copyPinLinkToClipboard() {
        try {
            String pinUrl = getPinEntryUrl();
            java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(pinUrl);
            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            logger.logTs("Đã copy link nhập PIN vào clipboard: %s", pinUrl);

            // Hiển thị thông báo ngắn
            JOptionPane.showMessageDialog(this,
                    "Đã copy link nhập PIN vào clipboard!",
                    "Copy thành công",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (HeadlessException ex) {
            logger.logTs("Lỗi khi copy link PIN: %s", ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi copy link PIN: " + ex.getMessage(),
                    "Lỗi copy",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Cập nhật link PIN trong panel
     */
    private void updatePinLinkInPanel(JPanel panel) {
        for (java.awt.Component comp : panel.getComponents()) {
            if (comp instanceof JLabel label) {
                if (label.getText() != null && label.getText().contains("Link nhập PIN:")) {
                    label.setText("🔗 Link nhập PIN: " + getPinEntryUrl());
                    return;
                }
            } else if (comp instanceof JPanel panel2) {
                updatePinLinkInPanel(panel2);
            }
        }
    }

    /**
     * Lấy tên CLB theo ID_CLB của ĐỘI (DangKiDoi.IdClb).
     * Trả về chuỗi rỗng nếu không có hoặc không tìm thấy.
     */
    private String getClubNameById(Integer idClb) {
        if (conn == null || idClb == null || idClb <= 0)
            return "";
        try {
            var clb = clbService.findOne(idClb);
            return clb != null && clb.getTenClb() != null ? clb.getTenClb() : "";
        } catch (Exception ex) {
            logger.logTs("Lỗi lấy tên CLB theo ID=%s: %s", String.valueOf(idClb), ex.getMessage());
            return "";
        }
    }

    /**
     * Lấy tên CLB theo ID của VĐV.
     * Trả về chuỗi rỗng nếu không có hoặc không tìm thấy.
     */
    private String getClubNameByVdvId(Integer vdvId) {
        if (conn == null || vdvId == null || vdvId <= 0)
            return "";
        try {
            String name = vdvService.getClubNameById(vdvId);
            return name != null ? name : "";
        } catch (Exception ex) {
            logger.logTs("Lỗi lấy tên CLB của VĐV ID=%s: %s", String.valueOf(vdvId), ex.getMessage());
            return "";
        }
    }

    /** Khôi phục & đưa các cửa sổ scoreboard ra trước */
    public void restoreDisplays() {
        try {
            scoreboardSvc.restoreDisplays();
        } catch (Throwable t) {
            for (java.awt.Window w : java.awt.Window.getWindows()) {
                if (w.isShowing() && w instanceof java.awt.Frame f) {
                    f.setState(java.awt.Frame.NORMAL);
                    f.toFront();
                    f.requestFocus();
                }
            }
        }
    }

    /**
     * Tạo tên hiển thị từ danh sách VĐV của đội
     * Chỉ trả về tên VĐV đầu tiên vì bảng điểm sẽ hiển thị mỗi tên 1 hàng
     */
    // (Đã bỏ createPlayerDisplayName vì không còn cần riêng hiển thị VĐV đầu tiên)

    /**
     * Tạo tên đầy đủ cho mỗi đội (mỗi VĐV 1 hàng)
     */
    private String buildFullTeamName(VanDongVien[] players) {
        if (players == null || players.length == 0) {
            return "";
        }
        StringBuilder fullName = new StringBuilder();
        for (int i = 0; i < players.length; i++) {
            VanDongVien v = players[i];
            fullName.append(v != null && v.getHoTen() != null ? v.getHoTen() : ("#" + i));
            if (i < players.length - 1) {
                fullName.append(" - ");
            }
        }
        return fullName.toString();
    }

    /**
     * Xác định tên CLB cho một đội đôi:
     * - Ưu tiên CLB gắn với đội (DangKiDoi.IdClb)
     * - Nếu trống, thử lấy CLB của VĐV trong đội:
     * + Nếu cả 2 cùng CLB: trả về tên CLB đó
     * + Nếu khác nhau: ghép "CLB1 / CLB2" (bỏ trùng, bỏ rỗng)
     */
    private String resolveClubForTeam(DangKiDoi team, VanDongVien[] players) {
        if (team != null) {
            String teamClub = getClubNameById(team.getIdClb());
            if (teamClub != null && !teamClub.isBlank())
                return teamClub;
        }
        if (players == null || players.length == 0)
            return "";

        String c1 = "";
        String c2 = "";
        try {
            if (players[0] != null) {
                c1 = getClubNameById(players[0].getIdClb());
            }
            if (players.length > 1 && players[1] != null) {
                c2 = getClubNameById(players[1].getIdClb());
            }
        } catch (Exception ignore) {
        }
        if (c1 == null)
            c1 = "";
        if (c2 == null)
            c2 = "";
        if (!c1.isBlank() && (c2.isBlank() || c1.equalsIgnoreCase(c2)))
            return c1;
        if (!c2.isBlank() && c1.isBlank())
            return c2;
        if (!c1.isBlank() && !c2.isBlank() && !c1.equalsIgnoreCase(c2))
            return c1 + " / " + c2;
        return "";
    }

    /* =================== Auto-advance winner to next round =================== */

    private void autoAdvanceWinnerToNextRound(String matchId) {
        String header = currentHeader();
        int idGiai = prefs.getInt("selectedGiaiDauId", -1);
        boolean isDoubles = doubles.isSelected();
        Integer idNoiDung = isDoubles ? headerKnrDoubles.get(header) : headerKnrSingles.get(header);
        int[] games = match.getGames();
        int winnerSide = (games[0] > games[1]) ? 0 : 1;
        String winnerName = winnerSide == 0 ? "Đội A" : "Đội B";
        if (!isDoubles) {
            int soDo = soDoCaNhanService.findSoDoByMatchId(idGiai, idNoiDung, matchId);
            autoAdvanceSingles(idGiai, idNoiDung, soDo, winnerSide);
        } else {
            int soDo = soDoDoiService.findSoDoByMatchId(idGiai, idNoiDung, matchId);
            autoAdvanceDoubles(idGiai, idNoiDung, soDo, winnerSide);
        }
    }

    private void autoAdvanceSingles(int idGiai, int idNoiDung, int soDo, int winnerSide) {
        try {
            List<SoDoCaNhan> rows = soDoCaNhanService.list(idGiai, idNoiDung, soDo);
            int rowsCount = rows.size();
            String nameA = sel(cboNameA);
            String nameB = sel(cboNameB);
            Integer idVdvA = (nameA == null || nameA.isBlank()) ? null : singlesNameToId.get(nameA);
            Integer idVdvB = (nameB == null || nameB.isBlank()) ? null : singlesNameToId.get(nameB);
            int columns = detectColumnsByMaxOrder(rows);
            int[] offsets = columnOffsets(columns);
            int winnerVdv = (winnerSide == 0 && idVdvA != null) ? idVdvA : (idVdvB != null ? idVdvB : 0);
            String winnerName = (winnerSide == 0) ? nameA : nameB;
            int currentCol = -1;
            if (idVdvA == null || idVdvB == null) {
                logger.logTs("Lỗi autoAdvanceSingles: idVdvA=%s, idVdvB=%s - hủy bỏ", idVdvA, idVdvB);
                return;
            }
            for (int col = 1; col <= columns; col++) {
                SoDoCaNhan rA = findRowByVdvAndCol(rows, idVdvA, col);
                SoDoCaNhan rB = findRowByVdvAndCol(rows, idVdvB, col);
                if (rA != null && rB != null) {
                    int tA = toThuTu(rA.getViTri(), col, offsets);
                    int tB = toThuTu(rB.getViTri(), col, offsets);
                    if (tA >= 0 && tB >= 0) {
                        int pairGroupA = tA / 2;
                        int pairGroupB = tB / 2;
                        if (pairGroupA == pairGroupB) {
                            currentCol = col;
                            break;
                        }
                    }
                }
            }
            int parentCol = currentCol + 1;

            // Lấy lại thông tin của hai VĐV ở cột hiện tại
            SoDoCaNhan rA = findRowByVdvAndCol(rows, idVdvA, currentCol);
            SoDoCaNhan rB = findRowByVdvAndCol(rows, idVdvB, currentCol);

            int tA = toThuTu(rA.getViTri(), currentCol, offsets);
            int tB = toThuTu(rB.getViTri(), currentCol, offsets);
            int parentThuTu = Math.min(tA, tB) / 2;
            int vitri = offsets[parentCol - 1] + parentThuTu + 1;

            try {
                SoDoCaNhan existing = null;
                try {
                    existing = soDoCaNhanService.getOne(idGiai, idNoiDung, vitri);
                } catch (Exception checkEx) {
                    logger.logTs("Upsert Bước 2: Ngoại lệ khi kiểm tra slot hiện có - %s", checkEx.getMessage());
                }

                if (existing != null) {
                    soDoCaNhanService.update(idGiai, idNoiDung, vitri,
                            winnerVdv,
                            existing.getToaDoX(), existing.getToaDoY(), soDo,
                            LocalDateTime.now(), null, null);
                } else {
                    int[] xy = computeSlotCoordinates(parentCol, parentThuTu);
                    soDoCaNhanService.create(idGiai, idNoiDung, winnerVdv,
                            xy[0], xy[1], vitri, soDo,
                            LocalDateTime.now(), null, null);
                }
            } catch (Exception ex) {
                logger.logTs("Upsert NGOẠI LỆ: Upsert slot đơn thất bại - %s", ex.getMessage());
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            logger.logTs("Lỗi trong autoAdvanceSingles: %s", ex.getMessage());
        }
    }

    private void autoAdvanceDoubles(int idGiai, int idNoiDung, int soDo, int winnerSide) {
        try {
            List<SoDoDoi> rows = soDoDoiService.list(idGiai, idNoiDung, soDo);
            DangKiDoi teamA = (DangKiDoi) cboTeamA.getSelectedItem();
            DangKiDoi teamB = (DangKiDoi) cboTeamB.getSelectedItem();
            String tenA = teamA != null ? teamA.getTenTeam() : null;
            String tenB = teamB != null ? teamB.getTenTeam() : null;
            if (tenA == null || tenA.isBlank() || tenB == null || tenB.isBlank()) {
                return;
            }
            int columns = detectColumnsByMaxOrder(rows);
            int[] offsets = columnOffsets(columns);
            String winnerTeamName = (winnerSide == 0) ? tenA : tenB;
            DangKiDoi winnerTeam = (winnerSide == 0) ? teamA : teamB;
            for (int col = 1; col < columns; col++) {
                SoDoDoi rA = findRowByTeamAndCol(rows, tenA, col);
                SoDoDoi rB = findRowByTeamAndCol(rows, tenB, col);
                int tA = toThuTu(rA.getViTri(), col, offsets);
                int tB = toThuTu(rB.getViTri(), col, offsets);
                int pairGroupA = tA / 2;
                int pairGroupB = tB / 2;
                if (pairGroupA == pairGroupB) {
                    int parentCol = col + 1;
                    if (parentCol > columns) {
                        return;
                    }

                    int parentThuTu = Math.min(tA, tB) / 2;
                    int vitri = offsets[parentCol - 1] + parentThuTu + 1;
                    Integer winnerClb = null;
                    try {
                        winnerClb = (winnerTeam != null) ? winnerTeam.getIdClb() : null;
                        if ((winnerClb == null || winnerClb <= 0) && winnerTeamName != null
                                && !winnerTeamName.isBlank()) {
                            DoiService ds = new DoiService(conn);
                            winnerClb = ds.getIdClbByTeamName(winnerTeamName, idNoiDung, idGiai);

                        }
                    } catch (Exception clbEx) {
                        logger.logTs("Đôi Bước 8 CẢNH BÁO: Xác định câu lạc bộ thất bại - %s", clbEx.getMessage());
                    }
                    try {
                        SoDoDoi existing = null;
                        existing = soDoDoiService.getOne(idGiai, idNoiDung, vitri);

                        if (existing != null) {
                            logger.logTs("Upsert Bước 3: CẬP NHẬT slot đôi hiện có");
                            logger.logTs("Upsert Bước 3: Cập nhật từ đội='%s' sang đội='%s', CLB từ %s sang %s",
                                    existing.getTenTeam(), winnerTeamName, existing.getIdClb(), winnerClb);

                            soDoDoiService.update(idGiai, idNoiDung, vitri,
                                    winnerClb, winnerTeamName,
                                    existing.getToaDoX(), existing.getToaDoY(), existing.getSoDo(),
                                    LocalDateTime.now(), null, null);
                        } else {
                            int[] xy = computeSlotCoordinates(parentCol, parentThuTu);
                            soDoDoiService.create(idGiai, idNoiDung, winnerClb, winnerTeamName,
                                    xy[0], xy[1], vitri, soDo,
                                    LocalDateTime.now(), null, null);
                        }
                    } catch (Exception ex) {
                        logger.logTs("Upsert NGOẠI LỆ: Upsert slot đôi thất bại - %s", ex.getMessage());
                        ex.printStackTrace();
                    }
                    return;
                }
            }
        } catch (Exception ex) {
            logger.logTs("Đôi NGOẠI LỆ: Tự động đưa người thắng thất bại - %s", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private int detectColumnsByMaxOrder(java.util.List<?> rows) {
        int maxOrder = 0;
        if (rows != null) {
            for (Object o : rows) {
                try {
                    int viTri;
                    if (o instanceof SoDoCaNhan) {
                        SoDoCaNhan r = (SoDoCaNhan) o;
                        viTri = r.getViTri();
                    } else if (o instanceof SoDoDoi) {
                        SoDoDoi r2 = (SoDoDoi) o;
                        viTri = r2.getViTri();
                    } else {
                        continue;
                    }
                    if (viTri > maxOrder)
                        maxOrder = viTri;
                } catch (Exception ignore) {
                }
            }
        }
        return (maxOrder > 31) ? 6 : 5;
    }

    private int[] columnOffsets(int columns) {
        if (columns >= 6) {
            return new int[] { 0, 32, 48, 56, 60, 62 };
        } else {
            return new int[] { 0, 16, 24, 28, 30 };
        }
    }

    private int toThuTu(int viTri, int col, int[] offsets) {
        if (col <= 0 || col > offsets.length)
            return -1;
        int base = offsets[col - 1];
        return viTri - base - 1;
    }

    private SoDoCaNhan findRowByVdvAndCol(
            List<SoDoCaNhan> rows, int idVdv, int col) {
        SoDoCaNhan best = null;
        for (var r : rows) {
            Integer rid = r.getIdVdv();
            if (rid != null && rid.equals(idVdv)) {
                if (best == null || r.getViTri() > best.getViTri()) {
                    best = r;
                }
            }
        }
        return best;
    }

    private SoDoDoi findRowByTeamAndCol(
            List<SoDoDoi> rows, String teamName, int col) {
        SoDoDoi best = null;
        if (teamName == null)
            return null;
        for (var r : rows) {
            if (r.getTenTeam() != null && r.getTenTeam().equalsIgnoreCase(teamName)) {
                if (best == null || r.getViTri() > best.getViTri())
                    best = r;
            }
        }
        return best;
    }

    private void upsertDoublesParentSlot(int idGiai, int idNoiDung, int parentCol, int parentThuTu, int parentOrder,
            String winnerTeamName, Integer winnerClb) {

    }

    private int[] computeSlotCoordinates(int col, int thuTu) {
        int x = 35 + (col - 1) * 200 + (col > 1 ? (col - 1) * 60 : 0);
        int step = (int) (40 * Math.pow(2, Math.max(0, col - 1)));
        int y;
        if (col <= 1) {
            y = 20 + thuTu * step;
        } else {
            y = 20 + thuTu * step + step / 2 - 20;
            if (y < 0)
                y = 0;
        }
        return new int[] { x, y };
    }

    /* =================== CHI_TIET_VAN (per-set logs) =================== */

    /**
     * Gọi khi ấn +1 cho A/B. side = 0 (A) hoặc 1 (B).
     * - Upsert CHI_TIET_VAN cho (currentMatchId, setNo = gameNumber)
     * - Cập nhật tổng điểm theo snapshot.score
     * - Append "P1@<millis>" hoặc "P2@<millis>" vào DAU_THOI_GIAN, ngăn bằng "; "
     */
    private void updateChiTietVanOnPoint(int side) {
        try {
            var s = match.snapshot();
            int setNo = Math.max(1, s.gameNumber);

            if (restartSetPending) {
                if (chiTietVanService.exists(currentMatchId, setNo)) {
                    chiTietVanService.delete(currentMatchId, setNo);
                }
            }

            String token = (side == 0 ? "P1@" : "P2@") + System.currentTimeMillis();

            String newTime;
            if (chiTietVanService.exists(currentMatchId, setNo)) {
                var cur = chiTietVanService.get(currentMatchId, setNo);
                String prev = cur.getDauThoiGian();

                newTime = (prev == null || prev.isBlank())
                        ? token
                        : (prev.endsWith(";") ? prev + " " + token : prev + "; " + token);
            } else {
                newTime = token;
            }

            int[] totals = computeTokenTotalsConsideringSwap(newTime);

            System.out.printf(
                    "Updating CHI_TIET_VAN totals to %d - %d based on tokens%n",
                    totals[0], totals[1]);

            if (chiTietVanService.exists(currentMatchId, setNo)) {
                chiTietVanService.update(
                        currentMatchId,
                        setNo,
                        totals[0], // ✅ DÙNG TOKEN
                        totals[1],
                        newTime);
            } else {
                chiTietVanService.addSet(
                        currentMatchId,
                        setNo,
                        totals[0], // ✅
                        totals[1],
                        newTime);
            }

            restartSetPending = false;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateChiTietVanTotalsOnly() {
        try {
            var s = match.snapshot();
            int setNo = Math.max(1, s.gameNumber);
            if (!chiTietVanService.exists(currentMatchId, setNo)) {
                return;
            }
            var cur = chiTietVanService.get(currentMatchId, setNo);
            String timeStr = cur.getDauThoiGian();
            if (timeStr == null || timeStr.isBlank()) {
                return; // service yêu cầu không rỗng; bỏ qua nếu trống
            }
            int[] totals = computeTokenTotalsConsideringSwap(timeStr);
            chiTietVanService.update(currentMatchId, setNo, totals[0], totals[1], timeStr);
        } catch (Exception ex) {
        }
    }

    /**
     * Ghi dấu mốc SWAP vào DAU_THOI_GIAN của set hiện tại và đồng bộ tổng điểm từ
     * token.
     * SWAP không làm thay đổi tổng điểm; chỉ đảo cách diễn giải P1/P2 cho các token
     * về sau.
     */
    private void appendSwapMarkerAndResyncChiTietVan() {
        try {
            var s = match.snapshot();
            int setNo = Math.max(1, s.gameNumber);
            String token = "SWAP@" + System.currentTimeMillis();
            if (chiTietVanService.exists(currentMatchId, setNo)) {
                var cur = chiTietVanService.get(currentMatchId, setNo);
                String prev = cur.getDauThoiGian();
                String newTime;
                if (prev == null || prev.isBlank()) {
                    newTime = token;
                } else {
                    newTime = prev.endsWith(";") ? (prev + " " + token) : (prev + "; " + token);
                }
                int[] totals = computeTokenTotalsConsideringSwap(newTime);
                chiTietVanService.update(currentMatchId, setNo, totals[0], totals[1], newTime);
            } else {
                // Chưa có bản ghi set: tạo mới với chỉ dấu SWAP, tổng điểm = 0-0
                chiTietVanService.addSet(currentMatchId, setNo, 0, 0, token);
            }
        } catch (Exception ex) {
        }
    }

    /**
     * Tính tổng điểm từ chuỗi token: đếm số lần xuất hiện của P1@ và P2@.
     * SWAP@ chỉ là dấu mốc (không ảnh hưởng tổng điểm), vì P1/P2 tương ứng với nhãn
     * A/B hiện tại.
     */
    private static int[] computeTokenTotalsConsideringSwap(String tokens) {
        int a = 0, b = 0;
        if (tokens == null || tokens.isBlank())
            return new int[] { 0, 0 };
        String[] parts = tokens.split(";");
        for (String raw : parts) {
            String t = raw.trim();
            if (t.isEmpty() || t.startsWith("SWAP@"))
                continue;
            if (t.startsWith("P1@"))
                a++;
            else if (t.startsWith("P2@"))
                b++;
        }
        return new int[] { a, b };
    }

    private void restoreMatchStateFromDatabase(String matchId) {
        if (matchId == null || matchId.isBlank() || conn == null) {
            logger.logTs("❌ Không thể restore: matchId hoặc connection null");
            return;
        }

        try {
            // Load chi tiết các ván từ database
            List<ChiTietVan> sets = chiTietVanService.listByMatch(matchId);

            if (sets == null || sets.isEmpty()) {
                logger.logTs("📊 Không có ván nào để restore cho match %s", matchId);
                return;
            }

            // Sắp xếp theo số ván
            sets.sort(Comparator.comparing(
                    ChiTietVan::getSetNo,
                    Comparator.nullsLast(Integer::compareTo)));

            // Tìm ván cuối cùng (ván hiện tại)
            var lastSet = sets.get(sets.size() - 1);
            int currentGameNumber = lastSet.getSetNo() != null ? lastSet.getSetNo() : 1;
            int scoreA = lastSet.getTongDiem1() != null ? lastSet.getTongDiem1() : 0;
            int scoreB = lastSet.getTongDiem2() != null ? lastSet.getTongDiem2() : 0;

            // Tính tổng ván thắng
            int gamesA = 0, gamesB = 0;
            for (var set : sets) {
                int d1 = set.getTongDiem1() != null ? set.getTongDiem1() : 0;
                int d2 = set.getTongDiem2() != null ? set.getTongDiem2() : 0;
                if (d1 > d2) {
                    gamesA++;
                } else if (d2 > d1) {
                    gamesB++;
                }
            }

            try {
                var matchClass = match.getClass();

                // Set điểm hiện tại
                var setScoreMethod = matchClass.getDeclaredMethod("setScore", int.class, int.class);
                setScoreMethod.setAccessible(true);
                setScoreMethod.invoke(match, scoreA, scoreB);

                // Set số ván hiện tại
                var gameNumberField = matchClass.getDeclaredField("gameNumber");
                gameNumberField.setAccessible(true);
                gameNumberField.set(match, currentGameNumber);

                // Set games won
                var gamesField = matchClass.getDeclaredField("games");
                gamesField.setAccessible(true);
                int[] games = new int[] { gamesA, gamesB };
                gamesField.set(match, games);

                // Set hasStarted
                var hasStartedField = matchClass.getDeclaredField("hasStarted");
                hasStartedField.setAccessible(true);
                hasStartedField.set(match, true);

                // Check nếu trận đã kết thúc
                var snapshot = match.snapshot();
                boolean matchFinished = snapshot.matchFinished;
                if (matchFinished) {
                    logger.logTs("🏆 Trận đã kết thúc: A thắng %d-%d (BO%d)", gamesA, gamesB, match.getBestOf());
                }

                logger.logTs("✅ Đã fire property change events để cập nhật UI");

                // Fire property change events để UI cập nhật
                firePropertyChange("score", null, new int[] { scoreA, scoreB });
                firePropertyChange("games", null, games);
                firePropertyChange("gameNumber", null, currentGameNumber);
                firePropertyChange("matchState", null, snapshot);
                firePropertyChange("hasStarted", null, true);
                SwingUtilities.invokeLater(() -> {
                    updateControlsEnabledAccordingToState();
                    repaint();
                });
            } catch (Exception reflectionEx) {
                logger.logTs("❌ Lỗi reflection khi restore state: %s", reflectionEx.getMessage());
            }

        } catch (Exception ex) {
            logger.logTs("❌ Lỗi restore match state từ database: %s", ex.getMessage());
        }
    }
}
