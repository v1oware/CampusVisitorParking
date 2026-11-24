package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.QueueCar;
import com.smartparking.ui.backend.ParkingBackend.Snapshot;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.util.Fmt;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** 等候队列：FIFO 排队顺序与队首补位说明。 */
public final class QueueView extends BaseView {

    private final TableView<QueueCar> table = Components.table(
            "暂无排队车辆", "车位充足时，车辆可直接入场");
    private final TextField filter = new TextField();
    private final Components.Stat countStat =
            new Components.Stat("排队车辆", Icons.QUEUE, "stat-amber");
    private final Components.Stat headStat =
            new Components.Stat("队首车牌", Icons.CAR, "stat-blue");
    private final Components.Stat maxWaitStat =
            new Components.Stat("最长等待", Icons.CLOCK, "stat-purple");

    private List<QueueCar> all = List.of();

    public QueueView(AppContext context) {
        super(context);
        configureTable();
        filter.setPromptText("按车牌筛选");
        filter.textProperty().addListener((observable, oldValue, newValue) -> applyFilter());

        var refresh = Components.ghost("刷新", Icons.REFRESH);
        refresh.setOnAction(event -> onShow());

        VBox tableCard = Components.card("排队列表", "按先来先服务（FIFO）顺序排列",
                new HBox(10, filter, refresh), table);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        tableCard.setMaxWidth(Double.MAX_VALUE);

        VBox ruleCard = Components.card("补位规则", "为什么队列能自动前进？", buildRuleBody());
        ruleCard.setMinWidth(300);
        ruleCard.setPrefWidth(320);

        HBox stats = new HBox(14, countStat, headStat, maxWaitStat);
        for (var node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((javafx.scene.layout.Region) node).setMaxWidth(Double.MAX_VALUE);
        }

        HBox body = new HBox(16, tableCard, ruleCard);
        getChildren().addAll(stats, body);
        onShow();
    }

    @Override
    public void onShow() {
        guard(() -> {
            Snapshot snapshot = context.backend().snapshot();
            all = snapshot.queue();
            applyFilter();
            countStat.setValue(all.size() + " 辆");
            countStat.setHint(all.isEmpty() ? "队列为空" : "最多等待 " + maxWait() + " 分钟");
            headStat.setValue(all.isEmpty() ? "—" : all.get(0).plate());
            headStat.setHint(all.isEmpty() ? "暂无队首" : "第 1 位");
            long max = all.stream().mapToLong(QueueCar::waitingMinutes).max().orElse(0);
            maxWaitStat.setValue(Fmt.duration(max));
            maxWaitStat.setHint("队首补位等待时间");
        });
    }

    private void configureTable() {
        TableColumn<QueueCar, String> position = Components.textColumn("排位", 80,
                car -> "第 " + car.position() + " 位");
        TableColumn<QueueCar, String> plate = Components.textColumn("车牌", 180,
                QueueCar::plate);
        TableColumn<QueueCar, String> since = Components.textColumn("加入时间", 130,
                car -> Fmt.dateTime(car.since()));
        TableColumn<QueueCar, String> waited = Components.textColumn("已等待", 130,
                car -> Fmt.duration(car.waitingMinutes()));
        TableColumn<QueueCar, String> state = Components.textColumn("状态", 120,
                car -> car.position() == 1 ? "下一位补位" : "等待中");
        table.getColumns().setAll(List.of(position, plate, since, waited, state));
        table.setPrefHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private VBox buildRuleBody() {
        VBox steps = new VBox(14,
                ruleStep("1", "车位释放", "场内车辆缴费离场，空出一个车位。"),
                ruleStep("2", "队首出队", "队列第 1 位车辆执行 dequeue，位置整体前移。"),
                ruleStep("3", "自动入场", "该车辆压入停车栈，道闸抬起放行。"),
                ruleStep("4", "位置更新", "Queue 在出队时统一重排，保证排位准确。"));
        return new VBox(14, steps,
                Components.callout("队列长度仅受内存限制，本系统 20 个车位、数据量较小。",
                        "callout-info", Icons.INFO));
    }

    private HBox ruleStep(String number, String title, String detail) {
        javafx.scene.layout.StackPane badge = new javafx.scene.layout.StackPane(
                Components.label(number, "step-number"));
        badge.getStyleClass().add("step-badge");
        VBox texts = new VBox(1,
                Components.label(title, "step-title"),
                Components.label(detail, "step-desc"));
        HBox row = new HBox(10, badge, texts);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void applyFilter() {
        String keyword = filter.getText() == null ? "" : filter.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            table.getItems().setAll(all);
        } else {
            table.getItems().setAll(all.stream()
                    .filter(car -> car.plate().toLowerCase().contains(keyword))
                    .toList());
        }
    }

    private long maxWait() {
        return all.stream().mapToLong(QueueCar::waitingMinutes).max().orElse(0);
    }
}
