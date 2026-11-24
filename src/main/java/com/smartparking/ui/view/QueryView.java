package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.PlateQuery;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.util.Fmt;
import com.smartparking.ui.util.Plates;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** 管理员综合查询：按车牌汇总在场、排队、黑名单与费用信息。 */
public final class QueryView extends BaseView {

    private final TextField plateField = new TextField();
    private final VBox resultArea = new VBox(16);
    private String lastQuery;

    public QueryView(AppContext context) {
        super(context);
        plateField.setPromptText("输入车牌，例如：京A12345");
        plateField.setOnAction(event -> search());
        var search = Components.primary("查询车辆", Icons.SEARCH);
        search.setOnAction(event -> search());

        HBox searchBar = new HBox(12, plateField, search);
        HBox.setHgrow(plateField, Priority.ALWAYS);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        HBox samples = new HBox(8,
                Components.label("示例", "field-hint"),
                sample("京A12345"), sample("京N00001"), sample("京G55667"));
        samples.setAlignment(Pos.CENTER_LEFT);
        VBox searchCard = Components.card("车辆综合查询", "一次查询所有相关状态",
                new VBox(10, searchBar,
                        Components.label("可查询在场车辆、排队车辆、黑名单车辆或陌生车牌。",
                                "field-hint"),
                        samples));

        showEmpty();
        getChildren().addAll(searchCard, resultArea);
        onShow();
    }

    @Override
    public void onShow() {
        if (lastQuery == null) {
            return;
        }
        guard(() -> {
            PlateQuery query = context.backend().query(lastQuery);
            render(query);
        });
    }

    private javafx.scene.control.Button sample(String plate) {
        var button = Components.button(plate, null, null, "btn-ghost", "btn-xs");
        button.setOnAction(event -> {
            plateField.setText(plate);
            search();
        });
        return button;
    }

    private void search() {
        String plate = Plates.normalize(plateField.getText());
        if (!Plates.looksValid(plate)) {
            context.toast("请输入有效车牌", com.smartparking.ui.component.Toasts.Kind.ERROR);
            plateField.requestFocus();
            return;
        }
        lastQuery = plate;
        PlateQuery query = context.backend().query(plate);
        render(query);
    }

    private void render(PlateQuery query) {
        Components.Stat parked = new Components.Stat("在场状态", Icons.CAR, "stat-blue");
        parked.setValue(query.parked() ? "在场" : "不在场");
        parked.setHint(query.parked()
                ? "车位 " + query.slot() + " · 已停 " + Fmt.duration(query.parkedMinutes())
                : "未找到入场记录");

        Components.Stat waiting = new Components.Stat("排队状态", Icons.QUEUE, "stat-amber");
        waiting.setValue(query.waiting() ? "第 " + query.queuePosition() + " 位" : "未排队");
        waiting.setHint(query.waiting() ? "等待队首补位" : "不在等候队列中");

        Components.Stat banned = new Components.Stat("黑名单", Icons.SHIELD,
                query.banned() ? "stat-red" : "stat-green");
        banned.setValue(query.banned() ? "已拉黑" : "正常");
        banned.setHint(query.banned() ? "入场将被拒绝" : "允许入场");

        Components.Stat fee = new Components.Stat("预估费用", Icons.MONEY, "stat-purple");
        fee.setValue(query.estimatedFee() == null ? "—" : Fmt.money(query.estimatedFee()));
        fee.setHint(query.parked() ? "按当前费率估算" : "车辆不在场");

        HBox stats = new HBox(14, parked, waiting, banned, fee);
        for (Node node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((Region) node).setMaxWidth(Double.MAX_VALUE);
        }

        VBox detail = new VBox(0,
                detailRow("车牌", query.plate()),
                detailRow("车位", query.parked() ? "车位 " + query.slot() : "—"),
                detailRow("入场时间", Fmt.full(query.entryTime())),
                detailRow("已停放", query.parked() ? Fmt.duration(query.parkedMinutes()) : "—"),
                detailRow("排队位置", query.waiting() ? "第 " + query.queuePosition() + " 位" : "—"),
                detailRow("黑名单", query.banned() ? "已拉黑，禁止入场" : "正常"),
                detailRow("预估费用", query.estimatedFee() == null ? "—" : Fmt.money(query.estimatedFee())));
        detail.getStyleClass().add("detail-list");

        VBox detailCard = Components.card("详细信息", "结果随业务数据每 4 秒自动刷新", detail);
        resultArea.getChildren().setAll(stats, detailCard);
    }

    private HBox detailRow(String label, String value) {
        HBox row = new HBox(12,
                Components.label(label, "detail-label"),
                Components.label(value, "detail-value"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("detail-row");
        row.setPadding(new Insets(9, 4, 9, 4));
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private void showEmpty() {
        VBox empty = new VBox(Components.emptyState("输入车牌开始查询",
                "查询结果会汇总在场、排队、黑名单与费用信息", Icons.SEARCH));
        empty.getStyleClass().add("card");
        resultArea.getChildren().setAll(empty);
    }
}
