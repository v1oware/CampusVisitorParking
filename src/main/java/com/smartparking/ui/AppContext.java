package com.smartparking.ui;

import com.smartparking.ui.backend.BackendException;
import com.smartparking.ui.backend.BackendFactory;
import com.smartparking.ui.backend.ParkingBackend;
import com.smartparking.ui.backend.ParkingBackend.UserSession;
import com.smartparking.ui.component.Toasts;
import com.smartparking.ui.view.LoginView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * 会话上下文：持有数据源、当前登录用户与根容器，负责登录 / 工作台 / 退出之间的切换。
 * 各页面通过本类访问后端，不直接持有业务对象。
 */
public final class AppContext {

    private final Stage stage;
    private final StackPane root;

    private ParkingBackend backend;
    private String mode;
    private UserSession session;
    private MainShell shell;
    private String startupWarning;

    public AppContext(Stage stage, StackPane root) {
        this.stage = stage;
        this.root = root;
        this.mode = BackendFactory.defaultMode();
        try {
            this.backend = BackendFactory.create(mode);
        } catch (BackendException e) {
            this.backend = new com.smartparking.ui.backend.DemoBackend();
            this.mode = BackendFactory.MODE_DEMO;
            this.startupWarning = "真实业务层初始化失败，已回退到演示数据：" + e.getMessage();
        }
    }

    public Stage stage() {
        return stage;
    }

    public StackPane root() {
        return root;
    }

    public ParkingBackend backend() {
        return backend;
    }

    public UserSession session() {
        return session;
    }

    public String mode() {
        return mode;
    }

    public String startupWarning() {
        return startupWarning;
    }

    public void clearStartupWarning() {
        startupWarning = null;
    }

    /** 登录页切换数据源。失败时保留原数据源并抛出异常。 */
    public void switchMode(String newMode) throws BackendException {
        ParkingBackend candidate = BackendFactory.create(newMode);
        this.backend = candidate;
        this.mode = newMode;
    }

    public void showLogin() {
        if (shell != null) {
            shell.dispose();
            shell = null;
        }
        session = null;
        root.getChildren().setAll(new LoginView(this));
    }

    public void enterWorkspace(UserSession userSession) {
        this.session = userSession;
        if (shell != null) {
            shell.dispose();
        }
        shell = new MainShell(this);
        root.getChildren().setAll(shell);
    }

    public void logout() {
        showLogin();
        toast("已安全退出登录", Toasts.Kind.INFO);
    }

    /** 供首页快捷入口跳转到指定导航项。 */
    public void open(String navKey) {
        if (shell != null) {
            shell.select(navKey);
        }
    }

    public void refreshCurrent() {
        if (shell != null) {
            shell.refreshActive();
        }
    }

    public void toast(String message, Toasts.Kind kind) {
        Toasts.show(root, message, kind);
    }

    /** 应用退出时调用，交给业务层保存数据。 */
    public void shutdown() {
        if (backend != null) {
            try {
                backend.shutdown();
            } catch (RuntimeException ignored) {
                // 退出阶段不再打断用户
            }
        }
    }
}
