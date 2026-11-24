package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.LogEntry;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.util.Fmt;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** 操作日志：入场、离场、计费、黑名单等关键动作的留痕。 */
public final class LogView extends BaseView {

    private final TableView<LogEntry> table = Components.table(
            "暂无操作日志", "关键操作会自动写入 operation.log");
    private final TextField filter = new TextField();
    private final ComboBox<String> resultFilter = new ComboBox<>();
    private final Components.Stat totalStat =
            new Components.Stat("日志总数", Icons.HISTORY, "stat-blue");
    private final Components.Stat successStat =
            new Components.Stat("成功操作", Icons.CHECK, "stat-green");
    private final Components.Stat failStat =
            new Components.Stat("失败 / 拦截", Icons.SHIELD, "stat-red");

    private List<LogEntry> all = List.of();

    public LogView(AppContext context) {
        super(context);
        configureTable();

        filter.setPromptText("搜索操作人、车牌或操作内容");
        filter.textProperty().addListener((observable, oldValue, newValue) -> applyFilter());

        resultFilter.getItems().setAll("全部结果", "成功", "失败");
        resultFilter.getSelectionModel().selectFirst();
        resultFilter.setOnAction(event -> applyFilter());

        var refresh = Components.ghost("刷新", Icons.REFRESH);
        refresh.setOnAction(event -> onShow());

        VBox card = Components.card("操作日志", "最新记录在前；数据由 util.Log 提供",
                new HBox(10, filter, resultFilter, refresh), table);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox stats = new HBox(14, totalStat, successStat, failStat);
        for (var node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((javafx.scene.layout.Region) node).setMaxWidth(Double.MAX_VALUE);
        }

        getChildren().addAll(stats, card);
        onShow();
    }

    @Override
    public void onShow() {
        guard(() -> {
            all = context.backend().logs();
            applyFilter();
            long success = all.stream().filter(LogEntry::success).count();
            totalStat.setValue(all.size() + " 条");
            totalStat.setHint(all.isEmpty() ? "等待业务层接入" : "最近 " + Fmt.dateTime(all.get(0).time()));
            successStat.setValue(success + " 条");
            successStat.setHint(all.isEmpty() ? "—" : Math.round(success * 100.0 / all.size()) + "% 成功率");
            failStat.setValue((all.size() - success) + " 条");
            failStat.setHint("含黑名单拦截与失败操作");
        });
    }

    private void configureTable() {
        TableColumn<LogEntry, String> time = Components.textColumn("时间", 150,
                entry -> Fmt.full(entry.time()));
        TableColumn<LogEntry, String> operator = Components.textColumn("操作人", 110,
                LogEntry::operator);
        TableColumn<LogEntry, String> action = Components.textColumn("操作内容", 480,
                LogEntry::action);
        TableColumn<LogEntry, String> result = Components.textColumn("结果", 90,
                entry -> entry.result() + (entry.success() ? "" : "（失败）"));
        table.getColumns().setAll(List.of(time, operator, action, result));
        table.setPrefHeight(460);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void applyFilter() {
        String keyword = filter.getText() == null ? "" : filter.getText().trim().toLowerCase();
        String result = resultFilter.getValue();
        table.getItems().setAll(all.stream()
                .filter(entry -> keyword.isEmpty()
                        || lower(entry.operator()).contains(keyword)
                        || lower(entry.action()).contains(keyword)
                        || lower(entry.result()).contains(keyword))
                .filter(entry -> result == null || "全部结果".equals(result)
                        || ("成功".equals(result) && entry.success())
                        || ("失败".equals(result) && !entry.success()))
                .toList());
    }
}
