package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.AppointmentEntry;
import com.smartparking.ui.backend.ParkingBackend.Outcome;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.component.Toasts;
import com.smartparking.ui.util.Fmt;
import com.smartparking.ui.util.Plates;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 访客预约登记：选择车牌、日期与整点时间，并管理已有预约。 */
public final class AppointmentView extends BaseView {

    private final TextField plateField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
    private final ComboBox<Integer> hourBox = new ComboBox<>();
    private final TableView<AppointmentEntry> table = Components.table(
            "暂无预约", "提交预约后会显示在这里");
    private final Components.Stat countStat =
            new Components.Stat("预约数量", Icons.CALENDAR, "stat-blue");
    private final Components.Stat nextStat =
            new Components.Stat("最近预约", Icons.CLOCK, "stat-purple");

    public AppointmentView(AppContext context) {
        super(context);
        configureTable();
        plateField.setPromptText("例如：京K12345");
        for (int hour = 0; hour < 24; hour++) {
            hourBox.getItems().add(hour);
        }
        hourBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer hour) {
                return hour == null ? "" : String.format("%02d:00", hour);
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        hourBox.getSelectionModel().select(Integer.valueOf(10));
        hourBox.setMaxWidth(Double.MAX_VALUE);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : dateFormat.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(text.trim(), dateFormat);
                } catch (RuntimeException exception) {
                    return null;
                }
            }
        });

        var submit = Components.primary("提交预约", Icons.CALENDAR);
        submit.setOnAction(event -> submit());
        var cancel = Components.danger("取消选中预约", Icons.DELETE);
        cancel.setOnAction(event -> cancelSelected());

        VBox formCard = Components.card("新建预约", "预约时间为整点，请提前规划到校时间",
                new VBox(13,
                        Components.field("车牌", plateField, "示例：京K12345"),
                        Components.field("预约日期", datePicker, null),
                        Components.field("预约时间", hourBox, null),
                        new HBox(10, submit),
                        Components.callout("预约成功后可随时取消；同一车牌请勿重复预约。",
                                "callout-info", Icons.INFO)));
        formCard.setMinWidth(340);
        formCard.setPrefWidth(380);

        VBox tableCard = Components.card("我的预约", "按预约时间先后排列",
                new HBox(10, cancel), table);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        tableCard.setMaxWidth(Double.MAX_VALUE);

        HBox stats = new HBox(14, countStat, nextStat);
        for (var node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((javafx.scene.layout.Region) node).setMaxWidth(Double.MAX_VALUE);
        }

        HBox body = new HBox(16, formCard, tableCard);
        getChildren().addAll(stats, body);
        onShow();
    }

    @Override
    public void onShow() {
        guard(() -> {
            List<AppointmentEntry> entries = context.backend().appointments();
            table.getItems().setAll(entries);
            countStat.setValue(entries.size() + " 条");
            countStat.setHint(entries.isEmpty() ? "暂无预约" : "共 " + entries.size() + " 条记录");
            if (entries.isEmpty()) {
                nextStat.setValue("—");
                nextStat.setHint("提交后可查看");
            } else {
                AppointmentEntry next = entries.get(0);
                nextStat.setValue(Fmt.dateTime(next.time()));
                nextStat.setHint(next.plate());
            }
        });
    }

    private void configureTable() {
        TableColumn<AppointmentEntry, String> plate = Components.textColumn("车牌", 160,
                AppointmentEntry::plate);
        TableColumn<AppointmentEntry, String> time = Components.textColumn("预约时间", 170,
                entry -> Fmt.dateTime(entry.time()));
        TableColumn<AppointmentEntry, String> status = Components.textColumn("状态", 110,
                AppointmentEntry::status);
        table.getColumns().setAll(List.of(plate, time, status));
        table.setPrefHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void submit() {
        String plate = Plates.normalize(plateField.getText());
        if (!Plates.looksValid(plate)) {
            context.toast("请输入有效车牌", Toasts.Kind.ERROR);
            plateField.requestFocus();
            return;
        }
        LocalDate date = datePicker.getValue();
        Integer hour = hourBox.getValue();
        if (date == null || hour == null) {
            context.toast("请选择预约日期与时间", Toasts.Kind.ERROR);
            return;
        }
        LocalDateTime time = LocalDateTime.of(date, LocalTime.of(hour, 0));
        Outcome outcome = context.backend().appoint(plate, time);
        context.toast(outcome.message(),
                outcome.success() ? Toasts.Kind.SUCCESS : Toasts.Kind.ERROR);
        if (outcome.success()) {
            plateField.clear();
        }
        onShow();
    }

    private void cancelSelected() {
        AppointmentEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            context.toast("请先在列表中选择要取消的预约", Toasts.Kind.INFO);
            return;
        }
        Outcome outcome = context.backend().cancelAppointment(selected.plate());
        context.toast(outcome.message(),
                outcome.success() ? Toasts.Kind.SUCCESS : Toasts.Kind.ERROR);
        onShow();
    }
}
