package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.BackendException;
import com.smartparking.ui.backend.BackendFactory;
import com.smartparking.ui.backend.ParkingBackend.UserSession;
import com.smartparking.ui.backend.Role;
import com.smartparking.ui.component.CaptchaPane;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

/** 登录页：身份选择、账号密码、图形验证码、数据源切换。 */
public final class LoginView extends HBox {

    private final AppContext context;
    private final ComboBox<Role> roleBox = new ComboBox<>();
    private final TextField accountField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField captchaField = new TextField();
    private final CaptchaPane captchaPane = new CaptchaPane();
    private final Label errorLabel = Components.label("", "login-error");
    private final Button loginButton = Components.primary("登 录", Icons.ARROW_RIGHT);
    private final ComboBox<ModeOption> modeBox = new ComboBox<>();
    private final VBox hintBox = new VBox(8);
    private final VBox warningBox = new VBox(8);

    private boolean busy;
    private boolean switchingMode;

    public LoginView(AppContext context) {
        this.context = context;
        getStyleClass().add("login-page");
        setPadding(new Insets(28));
        setAlignment(Pos.CENTER);

        HBox card = new HBox(buildBrandPanel(), buildFormPanel());
        card.getStyleClass().add("login-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        getChildren().add(card);
        HBox.setHgrow(this, Priority.ALWAYS);

        updateModeUi();
        if (context.startupWarning() != null) {
            warningBox.getChildren().add(Components.callout(
                    context.startupWarning(), "callout-warn", Icons.INFO));
        }
        accountField.requestFocus();
    }

    // ------------------------------------------------------------------
    // 左侧品牌区
    // ------------------------------------------------------------------

    private StackPane buildBrandPanel() {
        Circle big = new Circle(150, Color.web("#3b82f6", 0.14));
        big.setLayoutX(520);
        big.setLayoutY(80);
        Circle small = new Circle(90, Color.web("#22d3ee", 0.12));
        small.setLayoutX(420);
        small.setLayoutY(650);
        Circle ring = new Circle(210, Color.TRANSPARENT);
        ring.setStroke(Color.web("#ffffff", 0.08));
        ring.setStrokeWidth(1.2);
        ring.setLayoutX(560);
        ring.setLayoutY(560);
        Pane decoration = new Pane(big, small, ring);
        decoration.setMouseTransparent(true);

        StackPane logo = new StackPane(Icons.of(Icons.CAR, 22, "#ffffff"));
        logo.getStyleClass().add("brand-logo");
        HBox logoRow = new HBox(12, logo,
                new VBox(1,
                        Components.label("Campus Parking", "brand-word"),
                        Components.label("校园访客停车模拟系统", "brand-word-sub")));
        logoRow.setAlignment(Pos.CENTER_LEFT);

        Label eyebrow = Components.label("SMART CAMPUS · VISITOR PARKING", "hero-eyebrow");
        Label title = Components.label("让每一次访客停车\n井然有序", "hero-title");
        Label subtitle = Components.label(
                "20 个车位的单车道停车场，用栈模拟通道、用队列模拟等候区，\n"
                        + "从车牌识别到抬杆放行，全流程可视化。",
                "hero-subtitle");

        VBox features = new VBox(12,
                feature("栈式调度", "入场压栈、离场挪车，真实还原单车道通行"),
                feature("满位排队", "车位满时自动进入等候队列，离场后队首补位"),
                feature("角色分流", "管理员、安保人员、访客看到各自的工作台"),
                feature("全流程留痕", "入场、离场、计费、黑名单拦截写入操作日志"));

        VBox texts = new VBox(12, logoRow, new Pane() {{
            setMinHeight(36);
        }}, eyebrow, title, subtitle, new Pane() {{
            setMinHeight(14);
        }}, features);

        VBox content = new VBox(texts, Components.spacer(),
                Components.label("JavaFX · 栈与队列 · 课程设计", "brand-footer"));
        content.setPadding(new Insets(42, 44, 34, 44));

        StackPane brand = new StackPane(decoration, content);
        brand.getStyleClass().add("login-brand");
        brand.setMinWidth(430);
        brand.setPrefWidth(560);
        HBox.setHgrow(brand, Priority.ALWAYS);
        return brand;
    }

    private HBox feature(String title, String description) {
        StackPane icon = new StackPane(Icons.of(Icons.CHECK, 13, "#34d399"));
        icon.getStyleClass().add("feature-check");
        VBox texts = new VBox(1,
                Components.label(title, "feature-title"),
                Components.label(description, "feature-desc"));
        HBox row = new HBox(12, icon, texts);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    // ------------------------------------------------------------------
    // 右侧表单
    // ------------------------------------------------------------------

    private StackPane buildFormPanel() {
        Label heading = Components.label("欢迎回来", "login-heading");
        Label subheading = Components.label("请选择身份并登录工作台", "login-subheading");

        roleBox.getItems().setAll(Role.values());
        roleBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role role) {
                return role == null ? "" : role.label();
            }

            @Override
            public Role fromString(String string) {
                return null;
            }
        });
        roleBox.getSelectionModel().select(Role.ADMIN);
        roleBox.setMaxWidth(Double.MAX_VALUE);

        accountField.setPromptText("请输入账号，例如 admin");
        passwordField.setPromptText("请输入密码");
        captchaField.setPromptText("输入右侧验证码");
        captchaField.setPrefWidth(150);

        var refreshCaptcha = Components.iconButton(Icons.REFRESH, "换一张验证码");
        refreshCaptcha.setOnAction(event -> {
            captchaPane.refresh();
            captchaField.clear();
        });
        HBox captchaRow = new HBox(10, captchaField, captchaPane, refreshCaptcha);
        captchaRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(captchaField, Priority.ALWAYS);

        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        loginButton.getStyleClass().add("btn-lg");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> attemptLogin());

        accountField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> captchaField.requestFocus());
        captchaField.setOnAction(event -> attemptLogin());

        modeBox.getItems().setAll(
                new ModeOption("演示数据（开箱即用）", BackendFactory.MODE_DEMO),
                new ModeOption("真实业务层（队友实现）", BackendFactory.MODE_SERVICE));
        modeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModeOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public ModeOption fromString(String string) {
                return null;
            }
        });
        modeBox.getSelectionModel().select(modeOptionFor(context.mode()));
        modeBox.setMaxWidth(Double.MAX_VALUE);
        modeBox.setOnAction(event -> changeMode());

        VBox form = new VBox(16,
                new VBox(5, heading, subheading),
                warningBox,
                Components.field("身份", roleBox, null),
                Components.field("账号", accountField, null),
                Components.field("密码", passwordField, null),
                Components.field("验证码", captchaRow, "看不清？点击图片或右侧按钮换一张"),
                errorLabel,
                loginButton,
                Components.field("数据源", modeBox, null),
                hintBox,
                Components.label("登录即表示你已了解本系统仅用于课程演示", "login-footnote"));
        form.setMaxWidth(400);
        form.setMinWidth(360);

        StackPane panel = new StackPane(form);
        panel.getStyleClass().add("login-form-panel");
        panel.setPadding(new Insets(34, 54, 30, 54));
        panel.setMinWidth(470);
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    // ------------------------------------------------------------------
    // 行为
    // ------------------------------------------------------------------

    private void attemptLogin() {
        if (busy) {
            return;
        }
        clearError();

        Role role = roleBox.getValue();
        String account = accountField.getText() == null ? "" : accountField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String captcha = captchaField.getText() == null ? "" : captchaField.getText().trim();

        if (role == null) {
            showError("请选择登录身份");
            return;
        }
        if (account.isEmpty()) {
            showError("请输入账号");
            accountField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("请输入密码");
            passwordField.requestFocus();
            return;
        }
        if (!captchaPane.matches(captcha)) {
            showError("验证码不正确，已为你换一张");
            captchaPane.refresh();
            captchaField.clear();
            captchaField.requestFocus();
            return;
        }

        busy = true;
        loginButton.setDisable(true);
        loginButton.setText("正在登录…");
        try {
            UserSession session = context.backend().login(role, account, password);
            if (session == null) {
                showError(context.backend().isDemo()
                        ? "账号或密码错误。演示账号见下方提示。"
                        : "账号或密码错误，或 LoginSystem.login 尚未完成。");
                captchaPane.refresh();
                captchaField.clear();
                return;
            }
            context.clearStartupWarning();
            resetLoginButton();
            context.enterWorkspace(session);
        } catch (BackendException exception) {
            showError(exception.getMessage());
            captchaPane.refresh();
            captchaField.clear();
        } finally {
            busy = false;
            resetLoginButton();
        }
    }

    private void changeMode() {
        if (switchingMode) {
            return;
        }
        ModeOption option = modeBox.getValue();
        if (option == null || option.key().equals(context.mode())) {
            return;
        }
        switchingMode = true;
        try {
            context.switchMode(option.key());
            warningBox.getChildren().clear();
            clearError();
            updateModeUi();
        } catch (BackendException exception) {
            showError(exception.getMessage());
            modeBox.getSelectionModel().select(modeOptionFor(context.mode()));
        } finally {
            switchingMode = false;
        }
    }

    private void updateModeUi() {
        hintBox.getChildren().clear();
        if (context.backend().isDemo()) {
            hintBox.getChildren().add(Components.callout(
                    "演示账号：admin / admin123　·　staff / staff123　·　visitor / visitor123",
                    "callout-info", Icons.INFO));
        } else {
            hintBox.getChildren().add(Components.callout(
                    "已连接真实业务层。登录、停车、排队、计费均由 model / service / util 处理。",
                    "callout-info", Icons.SHIELD));
        }
    }

    private void resetLoginButton() {
        loginButton.setDisable(false);
        loginButton.setText("登 录");
        loginButton.setGraphic(Icons.of(Icons.ARROW_RIGHT, 16, "#ffffff"));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private ModeOption modeOptionFor(String mode) {
        return modeBox.getItems().stream()
                .filter(option -> option.key().equals(mode))
                .findFirst()
                .orElse(modeBox.getItems().get(0));
    }

    private record ModeOption(String label, String key) {
    }
}
