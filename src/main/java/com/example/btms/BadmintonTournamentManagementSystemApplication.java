package com.example.btms;

import java.awt.GraphicsEnvironment;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import com.example.btms.config.ConnectionConfig;
import com.example.btms.config.H2TcpServerConfig;
import com.example.btms.config.NetworkConfig;
import com.example.btms.config.Prefs;
import com.example.btms.ui.main.MainFrame;
import com.example.btms.ui.net.NetworkChooserDialog;
import com.example.btms.ui.theme.UITheme;
import com.example.btms.util.log.Log;
import com.example.btms.util.ui.IconUtil;

@SpringBootApplication
public class BadmintonTournamentManagementSystemApplication {

	private static final AtomicBoolean UI_STARTED = new AtomicBoolean(false);

	@Autowired
	private ConnectionConfig dbCfg;

	@Autowired
	private H2TcpServerConfig h2TcpServerConfig;

	@Autowired
	private ApplicationContext applicationContext;

	private final Log log = new Log();

	public static void main(String[] args) {
		// Tắt headless để cho phép mở Swing UI
		SpringApplication app = new SpringApplication(BadmintonTournamentManagementSystemApplication.class);
		app.setHeadless(false);

		// Add shutdown hook to ensure proper cleanup
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("🔄 Shutdown hook triggered - cleaning up...");
			try {
				// Force stop any remaining processes
				Thread.sleep(500); // Wait a bit for normal shutdown
			} catch (InterruptedException ignore) {
			}
			System.out.println("🔚 Shutdown hook completed");
		}, "shutdown-hook"));

		app.run(args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void launchSwingUI() {
		// Tránh mở UI 2 lần (devtools/restart)
		if (!UI_STARTED.compareAndSet(false, true))
			return;

		if (GraphicsEnvironment.isHeadless()) {
			// Headless environment detected. GUI will not be launched. Backend continues.
			return;
		}

		SwingUtilities.invokeLater(() -> {
			// Áp dụng theme (bo góc + FlatLaf) trước khi tạo bất kỳ frame/dialog nào
			UITheme.init();

			// Bước 1: Chọn network (không tạo thêm cửa sổ nào khác)
			NetworkChooserDialog dlg = new NetworkChooserDialog(null);
			dlg.setVisible(true);
			NetworkConfig cfg = dlg.getSelected();
			if (cfg == null) {
				// Người dùng hủy: thoát ứng dụng
				log.logTs("🚪 Người dùng hủy chọn network interface - thoát ứng dụng");
				System.exit(0);
				return;
			}

			// Lưu interface đã chọn để các màn khác dùng lại
			try {
				if (cfg.ifName() != null && !cfg.ifName().isBlank()) {
					Prefs p = new Prefs();
					p.put("net.ifName", cfg.ifName());
					p.put("ui.network.ifName", cfg.ifName());
				}

				// Khởi động H2 TCP Server với IP đã chọn
				try {
					h2TcpServerConfig.startTcpServer(cfg);
					log.logTs("✅ H2 TCP Server đã khởi động với IP: %s", cfg.ipv4Address());
					h2TcpServerConfig.showConnectionInfo();
				} catch (SQLException e) {
					log.logTs("❌ Không thể khởi động H2 TCP Server: %s", e.getMessage());
				}

			} catch (Throwable ignore) {
			}

			// Tạo MainFrame nhưng KHÔNG hiển thị; MainFrame sẽ tự hiển thị sau khi
			// hoàn tất kết nối DB + đăng nhập + chọn giải.
			MainFrame mf = new MainFrame(cfg, dbCfg, applicationContext);
			IconUtil.applyTo(mf);
		});
	}
}
