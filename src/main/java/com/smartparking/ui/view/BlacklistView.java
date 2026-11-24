package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.BlacklistEntry;
import com.smartparking.ui.backend.ParkingBackend.Outcome;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.component.Toasts;
import com.smartparking.ui.util.Fmt;
import com.smartparking.ui.util.Plates;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** 黑名单管理：加入、移出与查询。 */
public final class BlacklistView extends BaseView {

    private final TextField plateField = new TextField();
    private final TextField filterField = new TextField();
    private final TableView<BlacklistEntry> table = Components.table(
            "黑名单为空", "被拉黑的车辆将无法入场");
    private final Components.Stat countStat =
            new Components.Stat("黑名单车辆", Icons.SHIELD, "stat-red");
    private final Components.Stat tipStat =
            new Components.Stat("拦截规则", Icons.INFO, "stat-blue");

    private List<BlacklistEntry> all = List.of();

    public BlacklistView(AppContext context) {
        super(context);
        configureTable();
        plateField.setPromptText("输入车牌，例如：京N00001");
        plateField.setOnAction(event -> add());
        filterField.setPromptText("按车牌筛选");
        filterField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter());

        var addButton = Components.primary("加入黑名单", Icons.ADD);
        addButton.setOnAction(event -> add());
        var removeButton = Components.danger("移出黑名单", Icons.DELETE);
        removeButton.setOnAction(event -> removeSelected());

        VBox addCard = Components.card("加入黑名单", "入场识别命中后将直接拒绝",
                new VBox(12,
                        Components.field("车牌", plateField, "示例：京N00001"),
                        new HBox(10, addButton, removeButton)));

        VBox tableCard = Components.card("黑名单列表", "移出后车辆即可再次入场",
                filterField, table);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        tableCard.setMaxWidth(Double.MAX_VALUE);

        HBox stats = new HBox(14, countStat, tipStat);
        for (var node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((javafx.scene.layout.Region) node).setMaxWidth(Double.MAX_VALUE);
        }

        HBox body = new HBox(16, tableCard, addCard);
        addCard.setMinWidth(300);
        addCard.setPrefWidth(330);
        getChildren().addAll(stats, body);
        onShow();
    }

    @Override
    public void onShow() {
        guard(() -> {
            all = context.backend().blacklist();
            applyFilter();
            countStat.setValue(all.size() + " 辆");
            countStat.setHint(all.isEmpty() ? "暂无黑名单车辆" : "最近加入：" + Fmt.dateTime(all.get(0).addedAt()));
            tipStat.setValue("REJECT");
            tipStat.setHint("命中黑名单时入场结果码");
        });
    }

    private void configureTable() {
        TableColumn<BlacklistEntry, String> plate = new TableColumn<>("车牌");
        plate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().plate()));
        plate.setPrefWidth(200);
        TableColumn<BlacklistEntry, String> time = Components.textColumn("加入时间", 160,
                entry -> Fmt.full(entry.addedAt()));
        TableColumn<BlacklistEntry, String> status = Components.textColumn("状态", 120,
                entry -> "禁止入场");
        table.getColumns().setAll(List.of(plate, time, status));
        table.setPrefHeight(380);
    }

    private void add() {
        String plate = Plates.normalize(plateField.getText());
        if (!Plates.looksValid(plate)) {
            context.toast("请输入有效车牌", Toasts.Kind.ERROR);
            plateField.requestFocus();
            return;
        }
        Outcome outcome = context.backend().addToBlacklist(plate);
        context.toast(outcome.message(), outcome.success() ? Toasts.Kind.SUCCESS : Toasts.Kind.ERROR);
        if (outcome.success()) {
            plateField.clear();
        }
        onShow();
    }

    private void removeSelected() {
        BlacklistEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            context.toast("请先在列表中选择要移出的车辆", Toasts.Kind.INFO);
            return;
        }
        Outcome outcome = context.backend().removeFromBlacklist(selected.plate());
        context.toast(outcome.message(), outcome.success() ? Toasts.Kind.SUCCESS : Toasts.Kind.ERROR);
        onShow();
    }

    private void applyFilter() {
        String keyword = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            table.getItems().setAll(all);
        } else {
            table.getItems().setAll(all.stream()
                    .filter(entry -> entry.plate().toLowerCase().contains(keyword))
                    .toList());
        }
    }
}
