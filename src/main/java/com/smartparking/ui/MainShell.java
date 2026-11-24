package com.smartparking.ui;

import com.smartparking.ui.backend.ParkingBackend;
import com.smartparking.ui.backend.Role;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.util.Fmt;
import com.smartparking.ui.view.AppointmentView;
import com.smartparking.ui.view.BillingView;
import com.smartparking.ui.view.BlacklistView;
import com.smartparking.ui.view.DashboardView;
import com.smartparking.ui.view.EntryExitView;
import com.smartparking.ui.view.LogView;
import com.smartparking.ui.view.ParkingStatusView;
import com.smartparking.ui.view.QueryView;
import com.smartparking.ui.view.QueueView;
import com.smartparking.ui.view.Refreshable;
import com.smartparking.ui.view.RegisterVisitorView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 登录后的主框架：左侧角色导航、顶部状态栏、右侧内容区。
 * 所有页面都通过 {@link AppContext} 访问后端，角色不同看到的菜单不同。
 */
public final class MainShell extends BorderPane {

    private final AppContext context;
    private final StackPane content = new StackPane();
    private final Label pageTitle = Components.label("", "page-title");
    private final Label pageSubtitle = Components.label("", "page-subtitle");
    private final Label clockLabel = Components.label("", "header-clock");
    private final Map<String, NavEntry> entries = new LinkedHashMap<>();
    private final ToggleGroup navGroup = new ToggleGroup();
    private final Timeline autoRefresh;
    private final Timeline clockTimeline;

    private String activeKey;

    public MainShell(AppContext context) {
        this.context = context;
        getStyleClass().add("shell");

        buildEntries();
        setLeft(buildSidebar());
        setCenter(buildCenter());
        select(entries.keySet().iterator().next());

        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(4), event -> refreshActive()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();

        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1),
                event -> clockLabel.setText(Fmt.clock(LocalDateTime.now()))));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    public void dispose() {
        autoRefresh.stop();
        clockTimeline.stop();
    }

    public void select(String key) {
        NavEntry entry = entries.get(key);
        if (entry == null || key.equals(activeKey)) {
            return;
        }
        if (entry.node == null) {
            entry.node = entry.factory.get();
        }
        content.getChildren().setAll(entry.node);
        pageTitle.setText(entry.label);
        pageSubtitle.setText(entry.subtitle);
        entry.button.setSelected(true);
        activeKey = key;

        FadeTransition fade = new FadeTransition(Duration.millis(170), entry.node);
        fade.setFromValue(0.35);
        fade.setToValue(1);
        fade.play();

        if (entry.node instanceof Refreshable refreshable) {
            refreshable.onShow();
        }
    }

    public void refreshActive() {
        if (getScene() == null || getScene().getWindow() == null
                || !getScene().getWindow().isShowing()) {
            return;
        }
        clockLabel.setText(Fmt.clock(LocalDateTime.now()));
        NavEntry entry = entries.get(activeKey);
        if (entry != null && entry.node instanceof Refreshable refreshable) {
            refreshable.onShow();
        }
    }

    // ------------------------------------------------------------------
    // 布局
    // ------------------------------------------------------------------

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(248);
        sidebar.setMinWidth(248);
        sidebar.setMaxWidth(248);

        StackPane logo = new StackPane(Icons.of(Icons.CAR, 24, "#ffffff"));
        logo.getStyleClass().add("brand-logo");
        VBox brandTexts = new VBox(1,
                Components.label("校园访客停车", "brand-title"),
                Components.label("出入管理模拟系统", "brand-subtitle"));
        HBox brand = new HBox(12, logo, brandTexts);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        Role role = context.session().role();
        HBox roleCard = new HBox(10,
                Icons.of(Icons.USER, 18, "#93c5fd"),
                new VBox(1,
                        Components.label(role.label(), "role-name"),
                        Components.label("已登录", "role-hint")));
        roleCard.setAlignment(Pos.CENTER_LEFT);
        roleCard.getStyleClass().add("role-card");

        Label navLabel = Components.label("工作台", "nav-section-label");
        VBox navBox = new VBox(4);
        navBox.getStyleClass().add("nav-box");
        for (NavEntry entry : entries.values()) {
            ToggleButton button = new ToggleButton(entry.label);
            button.setGraphic(Icons.of(entry.icon, 18, "#cbd5e1"));
            button.setGraphicTextGap(12);
            button.getStyleClass().add("nav-btn");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setToggleGroup(navGroup);
            button.setOnAction(event -> {
                button.setSelected(true);
                select(entry.key);
            });
            entry.button = button;
            navBox.getChildren().add(button);
        }

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label mode = Components.chip(context.backend().isDemo() ? "演示数据" : "真实业务层",
                context.backend().isDemo() ? "badge-warn" : "badge-success");
        mode.setMaxWidth(Double.MAX_VALUE);
        mode.setAlignment(Pos.CENTER);
        VBox footer = new VBox(8,
                Components.label("数据源", "sidebar-footer-label"),
                mode,
                Components.label("课程设计 · v1.0", "sidebar-footer-version"));
        footer.getStyleClass().add("sidebar-footer");

        sidebar.getChildren().addAll(brand, roleCard, navLabel, navBox, spacer, footer);
        return sidebar;
    }

    private BorderPane buildCenter() {
        BorderPane center = new BorderPane();
        center.setTop(buildHeader());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("content-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        center.setCenter(scroll);
        return center;
    }

    private HBox buildHeader() {
        clockLabel.setText(Fmt.clock(LocalDateTime.now()));
        HBox clock = new HBox(6, Icons.of(Icons.CLOCK, 15, "#94a3b8"), clockLabel);
        clock.setAlignment(Pos.CENTER_LEFT);
        clock.getStyleClass().add("header-clock-box");

        Label modeChip = Components.label("● " + context.backend().modeLabel(),
                "mode-chip", context.backend().isDemo() ? "mode-demo" : "mode-live");
        Tooltip.install(modeChip, new Tooltip(context.backend().isDemo()
                ? "当前为内置演示数据，不会读写 JSON。可在登录页切换为真实业务层。"
                : "当前连接业务层，数据来自 model / service / util。"));

        var refresh = Components.iconButton(Icons.REFRESH, "立即刷新当前页面");
        refresh.setOnAction(event -> {
            refreshActive();
            context.toast("已刷新", com.smartparking.ui.component.Toasts.Kind.INFO);
        });

        String displayName = context.session().displayName();
        Label avatarText = Components.label(
                displayName == null || displayName.isBlank() ? "U" : displayName.substring(0, 1),
                "avatar-text");
        StackPane avatar = new StackPane(avatarText);
        avatar.getStyleClass().add("avatar");
        VBox userTexts = new VBox(0,
                Components.label(displayName == null ? "用户" : displayName, "header-user-name"),
                Components.label(context.session().role().label(), "header-user-role"));
        HBox user = new HBox(9, avatar, userTexts);
        user.setAlignment(Pos.CENTER_LEFT);
        user.getStyleClass().add("header-user");

        var logout = Components.iconButton(Icons.LOGOUT, "退出登录");
        logout.setOnAction(event -> context.logout());

        HBox header = new HBox(16,
                new VBox(2, pageTitle, pageSubtitle),
                Components.spacer(),
                clock,
                modeChip,
                refresh,
                new Region() {{
                    getStyleClass().add("header-divider");
                    setMinSize(1, 28);
                    setPrefSize(1, 28);
                }},
                user,
                logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return header;
    }

    // ------------------------------------------------------------------
    // 导航定义
    // ------------------------------------------------------------------

    private void buildEntries() {
        Role role = context.session().role();
        add("dashboard", "工作台概览", "系统运行状态与常用入口", Icons.DASHBOARD,
                () -> new DashboardView(context));

        switch (role) {
            case ADMIN -> {
                add("status", "车位状态", "20 个车位的实时占用与道闸状态", Icons.CAR,
                        () -> new ParkingStatusView(context));
                add("entryexit", "出入场放行", "车牌识别、抬杆与缴费离场", Icons.GATE,
                        () -> new EntryExitView(context, false));
                add("queue", "等候队列", "车位满时的排队与自动补位情况", Icons.QUEUE,
                        () -> new QueueView(context));
                add("blacklist", "黑名单管理", "拦截违规车辆入场", Icons.SHIELD,
                        () -> new BlacklistView(context));
                add("logs", "操作日志", "入场、离场、计费与拦截记录", Icons.HISTORY,
                        () -> new LogView(context));
                add("billing", "计费设置", "单位时间费率与费用试算", Icons.MONEY,
                        () -> new BillingView(context));
                add("query", "综合查询", "按车牌查询在场、排队与黑名单状态", Icons.SEARCH,
                        () -> new QueryView(context));
            }
            case STAFF -> {
                add("register", "访客登记", "登记访客与车辆信息", Icons.PERSON_ADD,
                        () -> new RegisterVisitorView(context));
                add("entryexit", "出入场放行", "车牌识别、抬杆与缴费离场", Icons.GATE,
                        () -> new EntryExitView(context, false));
                add("status", "车位状态", "20 个车位的实时占用与道闸状态", Icons.CAR,
                        () -> new ParkingStatusView(context));
                add("queue", "等候队列", "车位满时的排队情况", Icons.QUEUE,
                        () -> new QueueView(context));
            }
            case VISITOR -> {
                add("appointment", "预约登记", "预约入场时间并管理预约", Icons.CALENDAR,
                        () -> new AppointmentView(context));
                add("status", "车位查询", "查看访客可用车位与等候队列", Icons.CAR,
                        () -> new ParkingStatusView(context));
                add("queue", "等候队列", "查看当前排队车辆与位置", Icons.QUEUE,
                        () -> new QueueView(context));
                add("exit", "缴费离场", "查询费用并缴费离场", Icons.MONEY,
                        () -> new EntryExitView(context, true));
            }
        }
    }

    private void add(String key, String label, String subtitle, String icon,
                     Supplier<Node> factory) {
        entries.put(key, new NavEntry(key, label, subtitle, icon, factory));
    }

    private static final class NavEntry {
        private final String key;
        private final String label;
        private final String subtitle;
        private final String icon;
        private final Supplier<Node> factory;
        private Node node;
        private ToggleButton button;

        private NavEntry(String key, String label, String subtitle, String icon,
                         Supplier<Node> factory) {
            this.key = key;
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon;
            this.factory = factory;
        }
    }
}
