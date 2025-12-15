package com.example.btms.ui.referee;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import java.sql.Connection;

import com.example.btms.model.db.SQLSRVConnectionManager;
import com.example.btms.repository.club.CauLacBoRepository;
import com.example.btms.service.club.CauLacBoService;
import com.example.btms.service.db.DatabaseService;
import com.example.btms.service.referee.PhanCongTrongTaiService;
import com.example.btms.service.referee.TrongTaiService;

/**
 * Demo frame để test panel lịch sử phân công trọng tài
 * Sử dụng trong MainFrame thực tế
 * 
 * @author BTMS Team
 */
public class RefereeManagementFrame extends JFrame {
    private TrongTaiService trongTaiService;
    private PhanCongTrongTaiService phanCongService;
    private CauLacBoService clbService;

    public RefereeManagementFrame() {
        try {
            // Khởi tạo services (trong thực tế, các service này sẽ được inject từ
            // MainFrame)
            SQLSRVConnectionManager manager = new SQLSRVConnectionManager();
            DatabaseService dbService = new DatabaseService(manager);

            // Giả lập kết nối database (demo purposes)
            // Trong thực tế, connection sẽ được setup từ MainFrame
            Connection conn = null; // Sẽ cần setup thực tế

            // Khởi tạo repositories với connection
            CauLacBoRepository clbRepo = new CauLacBoRepository(conn);

            // Khởi tạo services - các service referee sử dụng default constructor
            this.trongTaiService = new TrongTaiService();
            this.phanCongService = new PhanCongTrongTaiService();
            this.clbService = new CauLacBoService(clbRepo);

            initFrame();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback để có thể chạy UI test
            this.trongTaiService = null;
            this.phanCongService = null;
            this.clbService = null;
            initFrame();
        }
    }

    private void initFrame() {
        setTitle("🏸 BTMS - Quản lý Trọng tài");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Tạo tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab quản lý trọng tài (với null safety cho demo)
        if (trongTaiService != null && clbService != null) {
            TrongTaiManagementPanel trongTaiPanel = new TrongTaiManagementPanel(trongTaiService, clbService);
            tabbedPane.addTab("👨‍⚖️ Quản lý trọng tài", trongTaiPanel);
        }

        // Tab lịch sử phân công (với null safety cho demo)
        if (phanCongService != null && trongTaiService != null) {
            PhanCongTrongTaiHistoryPanel historyPanel = new PhanCongTrongTaiHistoryPanel(phanCongService,
                    trongTaiService);
            tabbedPane.addTab("📋 Lịch sử phân công", historyPanel);
        }

        add(tabbedPane, BorderLayout.CENTER);

        // Set size và center
        setSize(1200, 700);
        setLocationRelativeTo(null);
    }

    /**
     * Demo method để test
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set look and feel
                javax.swing.UIManager.setLookAndFeel(
                        javax.swing.UIManager.getLookAndFeel().getClass().getName());

                // Tạo và hiển thị frame
                RefereeManagementFrame frame = new RefereeManagementFrame();
                frame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Method để integrate vào MainFrame
     */
    public static void addToMainFrame(JTabbedPane mainTabbedPane,
            TrongTaiService trongTaiService,
            PhanCongTrongTaiService phanCongService) {
        PhanCongTrongTaiHistoryPanel historyPanel = new PhanCongTrongTaiHistoryPanel(
                phanCongService, trongTaiService);
        mainTabbedPane.addTab("📋 Lịch sử phân công TT", historyPanel);
    }
}