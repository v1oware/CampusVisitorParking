package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.LogEntry;
import com.smartparking.ui.backend.ParkingBackend.QueueCar;
import com.smartparking.ui.backend.ParkingBackend.Snapshot;
import com.smartparking.ui.backend.Role;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.GateIndicator;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.component.Toasts;
import com.smartparking.ui.util.Fmt;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.List;

/** 工作台概览：关键指标、车位占用、道闸状态、最近操作与队列预览。 */
public final class DashboardView extends BaseView {

    private final Components.Stat occupiedStat =
            new Components.Stat("在场车辆", Icons.CAR, "stat-blue");
    private final Components.Stat freeStat =
            new Components.Stat("空余车位", Icons.GATE, "stat-green");
    private final Components.Stat queueStat =
            new Components.Stat("排队车辆", Icons.QUEUE, "stat-amber");
    private final Components.Stat rateStat =
            new Components.Stat("单位费率", Icons.MONEY, "stat-purple");

    private final Label greeting = Components.label("", "welcome-title");
    private final Label greetingSub = Components.label("", "welcome-sub");
    private final Label occupancyValue = Components.label("0 / 20", "occupancy-value");
    private final Label occupancyRate = Components.label("0%", "occupancy-rate");
    private final ProgressBar occupancyBar = new ProgressBar(0);
    private final HBox slotStrip = new HBox(4);
    private final Label legend = Components.label("", "occupancy-legend");

    private final GateIndicator gate = new GateIndicator();
    private final Button raiseButton = Components.button("抬杆", Icons.GATE, "#166534", "btn-soft");
    private final Button lowerButton = Components.button("落杆", Icons.GATE, "#991b1b", "btn-soft");

    private final TableView<LogEntry> logTable = Components.table("暂无操作记录", "关键操作会显示在这里");
    private final VBox queuePreview = new VBox(6);

    public DashboardView(AppContext context) {
        super(context);
        getChildren().addAll(buildBanner(), buildStats(),
                buildMiddleRow(), buildBottomRow());
        onShow();
    }

    @Override
    public void onShow() {
        guard(() -> {
            Snapshot snapshot = context.backend().snapshot();
            renderGreeting(snapshot);
            renderStats(snapshot);
            renderOccupancy(snapshot);
            renderGate(snapshot);
            renderLogs();
            renderQueue(snapshot.queue());
        });
    }

    // ------------------------------------------------------------------
    // 布局
    // ------------------------------------------------------------------

    private HBox buildBanner() {
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Role role = context.session().role();
        switch (role) {
            case ADMIN -> actions.getChildren().addAll(
                    quick("入场放行", Icons.GATE, "entryexit"),
                    quick("车位状态", Icons.CAR, "status"),
                    quick("计费设置", Icons.MONEY, "billing"));
            case STAFF -> actions.getChildren().addAll(
                    quick("访客登记", Icons.PERSON_ADD, "register"),
                    quick("入场放行", Icons.GATE, "entryexit"),
                    quick("车位状态", Icons.CAR, "status"));
            case VISITOR -> actions.getChildren().addAll(
                    quick("预约登记", Icons.CALENDAR, "appointment"),
                    quick("车位查询", Icons.CAR, "status"),
                    quick("缴费离场", Icons.MONEY, "exit"));
        }

        HBox banner = new HBox(20, new VBox(4, greeting, greetingSub), Components.spacer(), actions);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("welcome-banner");
        return banner;
    }

    private Button quick(String text, String icon, String navKey) {
        Button button = Components.button(text, icon, "#1e3a8a", "btn-soft");
        button.setOnAction(event -> context.open(navKey));
        return button;
    }

    private Button quickLink(String text, String navKey) {
        Button button = Components.ghost(text, Icons.ARROW_RIGHT);
        button.setOnAction(event -> context.open(navKey));
        return button;
    }

    private HBox buildStats() {
        HBox box = new HBox(14, occupiedStat, freeStat, queueStat, rateStat);
        for (var node : box.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((Region) node).setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private HBox buildMiddleRow() {
        VBox occupancyCard = Components.card("车位占用", "20 个访客车位的实时使用情况",
                new HBox(8,
                        Components.chip("实时", "badge-info"),
                        quickLink("查看车位", "status")),
                buildOccupancyBody());

        VBox gateCard = Components.card("道闸状态", "抬杆 / 落杆与最近识别车牌",
                buildGateBody());

        HBox row = new HBox(16, occupancyCard, gateCard);
        HBox.setHgrow(occupancyCard, Priority.ALWAYS);
        occupancyCard.setMaxWidth(Double.MAX_VALUE);
        gateCard.setMinWidth(300);
        gateCard.setPrefWidth(320);
        return row;
    }

    private VBox buildOccupancyBody() {
        occupancyBar.setMaxWidth(Double.MAX_VALUE);
        occupancyBar.getStyleClass().add("occupancy-bar");
        slotStrip.setAlignment(Pos.CENTER_LEFT);
        slotStrip.getStyleClass().add("slot-strip");
        VBox body = new VBox(12,
                new HBox(8, occupancyValue, Components.spacer(), occupancyRate),
                occupancyBar,
                slotStrip,
                legend);
        body.setPadding(new Insets(4, 0, 0, 0));
        return body;
    }

    private VBox buildGateBody() {
        gate.setPadding(new Insets(4, 0, 2, 0));
        if (context.session().role() == Role.VISITOR) {
            raiseButton.setDisable(true);
            lowerButton.setDisable(true);
        }
        raiseButton.setOnAction(event -> operateGate(true));
        lowerButton.setOnAction(event -> operateGate(false));
        HBox buttons = new HBox(8, raiseButton, lowerButton);
        buttons.setAlignment(Pos.CENTER);
        VBox body = new VBox(8, gate, buttons);
        body.setAlignment(Pos.CENTER);
        return body;
    }

    private HBox buildBottomRow() {
        configureLogTable();
        var logAction = Components.ghost("查看全部", Icons.ARROW_RIGHT);
        logAction.setOnAction(event -> context.open(context.session().role() == Role.ADMIN
                ? "logs" : "status"));
        VBox logsCard = Components.card("最近操作", "入场、离场与拦截记录",
                logAction, logTable);

        var queueAction = Components.ghost("查看全部", Icons.ARROW_RIGHT);
        queueAction.setOnAction(event -> context.open("queue"));
        VBox queueCard = Components.card("等候队列", "队首将在车位释放后自动补位",
                queueAction, queuePreview);

        HBox row = new HBox(16, logsCard, queueCard);
        HBox.setHgrow(logsCard, Priority.ALWAYS);
        logsCard.setMaxWidth(Double.MAX_VALUE);
        queueCard.setMinWidth(300);
        queueCard.setPrefWidth(330);
        return row;
    }

    private void configureLogTable() {
        TableColumn<LogEntry, String> time = Components.textColumn("时间", 80,
                entry -> Fmt.time(entry.time()));
        TableColumn<LogEntry, String> operator = Components.textColumn("操作人", 90,
                entry -> entry.operator());
        TableColumn<LogEntry, String> action = Components.textColumn("操作内容", 320,
                entry -> entry.action());
        TableColumn<LogEntry, String> result = Components.textColumn("结果", 70,
                entry -> entry.result());
        logTable.getColumns().setAll(List.of(time, operator, action, result));
        logTable.setPrefHeight(258);
        logTable.setMinHeight(220);
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    private void renderGreeting(Snapshot snapshot) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        String period = hour < 6 ? "凌晨好" : hour < 12 ? "早上好"
                : hour < 14 ? "中午好" : hour < 18 ? "下午好" : "晚上好";
        String name = context.session().displayName();
        greeting.setText(period + "，" + (name == null || name.isBlank() ? "用户" : name));
        greetingSub.setText("当前 " + snapshot.occupied() + " 辆车在场，"
                + snapshot.queue().size() + " 辆车排队等候 · 单位费率 ¥"
                + snapshot.unitCost() + "/小时");
    }

    private void renderStats(Snapshot snapshot) {
        occupiedStat.setValue(snapshot.occupied() + " 辆");
        occupiedStat.setHint("共 " + snapshot.capacity() + " 个车位");

        freeStat.setValue(snapshot.free() + " 个");
        freeStat.setHint(snapshot.full() ? "车位已满，新车辆将排队" : "可继续入场");

        queueStat.setValue(snapshot.queue().size() + " 辆");
        queueStat.setHint(snapshot.queue().isEmpty() ? "当前无人排队"
                : "队首 " + snapshot.queue().get(0).plate());

        rateStat.setValue("¥ " + snapshot.unitCost());
        rateStat.setHint("元 / 小时");
    }

    private void renderOccupancy(Snapshot snapshot) {
        int capacity = Math.max(1, snapshot.capacity());
        int occupied = Math.min(capacity, snapshot.occupied());
        occupancyValue.setText(occupied + " / " + capacity);
        occupancyBar.setProgress((double) occupied / capacity);
        occupancyRate.setText(Math.round((double) occupied / capacity * 100) + "%");

        slotStrip.getChildren().clear();
        for (int i = 0; i < capacity; i++) {
            Region cell = new Region();
            cell.getStyleClass().addAll("mini-slot", i < occupied ? "mini-slot-used" : "mini-slot-free");
            cell.setPrefHeight(14);
            cell.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cell, Priority.ALWAYS);
            slotStrip.getChildren().add(cell);
        }
        legend.setText("占用 " + occupied + " 个　·　空余 " + (capacity - occupied)
                + " 个　·　排队 " + snapshot.queue().size() + " 辆");
    }

    private void renderGate(Snapshot snapshot) {
        gate.setOpen(snapshot.gate().open());
        gate.setLastPlate(snapshot.gate().lastPlate());
    }

    private void renderLogs() {
        List<LogEntry> entries = context.backend().logs();
        logTable.getItems().setAll(entries.size() > 4 ? entries.subList(0, 4) : entries);
    }

    private void renderQueue(List<QueueCar> queue) {
        queuePreview.getChildren().clear();
        if (queue.isEmpty()) {
            queuePreview.getChildren().add(Components.emptyState(
                    "暂无排队车辆", "车位充足时车辆可直接入场", Icons.QUEUE));
            return;
        }
        int shown = Math.min(4, queue.size());
        for (int i = 0; i < shown; i++) {
            QueueCar car = queue.get(i);
            queuePreview.getChildren().add(queueRow(car));
        }
        if (queue.size() > shown) {
            queuePreview.getChildren().add(Components.label(
                    "还有 " + (queue.size() - shown) + " 辆排队中…", "queue-more"));
        }
    }

    private HBox queueRow(QueueCar car) {
        Label number = Components.label(String.valueOf(car.position()), "queue-pos-text");
        StackPane position = new StackPane(number);
        position.getStyleClass().add("queue-pos");
        VBox texts = new VBox(1,
                Components.label(car.plate(), "queue-plate"),
                Components.label("已等待 " + Fmt.duration(car.waitingMinutes()), "queue-time"));
        HBox row = new HBox(10, position, texts, Components.spacer(),
                Components.chip("第 " + car.position() + " 位", "badge-muted"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("queue-row");
        row.setPadding(new Insets(9, 10, 9, 10));
        return row;
    }

    private void operateGate(boolean open) {
        var outcome = context.backend().setGate(open);
        if (outcome.success()) {
            context.toast(outcome.message(), Toasts.Kind.SUCCESS);
        } else {
            context.toast(outcome.message(), Toasts.Kind.ERROR);
        }
        onShow();
    }
}
