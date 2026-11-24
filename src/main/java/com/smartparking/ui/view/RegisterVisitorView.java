package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.Outcome;
import com.smartparking.ui.backend.ParkingBackend.VisitorRegistration;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.component.Toasts;
import com.smartparking.ui.util.Fmt;
import com.smartparking.ui.util.Plates;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.List;

/** 安保人员访客登记：姓名、电话、车牌与来访事由。 */
public final class RegisterVisitorView extends BaseView {

    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField plateField = new TextField();
    private final ComboBox<String> purposeBox = new ComboBox<>();
    private final TableView<VisitorRegistration> table = Components.table(
            "暂无登记记录", "登记后的访客信息会显示在这里");
    private final Components.Stat countStat =
            new Components.Stat("今日登记", Icons.PERSON_ADD, "stat-blue");
    private final Components.Stat lastStat =
            new Components.Stat("最近登记", Icons.CLOCK, "stat-green");

    public RegisterVisitorView(AppContext context) {
        super(context);
        configureTable();
        nameField.setPromptText("访客姓名");
        phoneField.setPromptText("联系电话");
        plateField.setPromptText("车牌，例如：京F12345");
        purposeBox.getItems().setAll("探亲访友", "公务来访", "学术交流", "其他");
        purposeBox.getSelectionModel().selectFirst();
        purposeBox.setMaxWidth(Double.MAX_VALUE);

        var submit = Components.primary("确认登记", Icons.PERSON_ADD);
        submit.setOnAction(event -> submit());

        VBox formCard = Components.card("访客登记", "登记后车辆可按访客身份入场",
                new VBox(13,
                        Components.field("姓名", nameField, null),
                        Components.field("联系电话", phoneField, "6 - 15 位数字"),
                        Components.field("车牌", plateField, "示例：京F12345"),
                        Components.field("来访事由", purposeBox, null),
                        submit,
                        Components.callout("登记信息用于安保核验；车牌将作为入场识别依据。",
                                "callout-info", Icons.INFO)));
        formCard.setMinWidth(340);
        formCard.setPrefWidth(380);

        VBox tableCard = Components.card("登记记录", "最新登记在前", table);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        tableCard.setMaxWidth(Double.MAX_VALUE);

        HBox stats = new HBox(14, countStat, lastStat);
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
            List<VisitorRegistration> entries = context.backend().visitorRegistrations();
            table.getItems().setAll(entries);
            countStat.setValue(entries.size() + " 人");
            countStat.setHint("当前会话累计登记");
            if (entries.isEmpty()) {
                lastStat.setValue("—");
                lastStat.setHint("暂无记录");
            } else {
                VisitorRegistration latest = entries.get(0);
                lastStat.setValue(latest.name());
                lastStat.setHint(latest.plate() + " · " + Fmt.time(latest.time()));
            }
        });
    }

    private void configureTable() {
        TableColumn<VisitorRegistration, String> name = Components.textColumn("姓名", 100,
                VisitorRegistration::name);
        TableColumn<VisitorRegistration, String> phone = Components.textColumn("电话", 130,
                VisitorRegistration::phone);
        TableColumn<VisitorRegistration, String> plate = Components.textColumn("车牌", 130,
                VisitorRegistration::plate);
        TableColumn<VisitorRegistration, String> purpose = Components.textColumn("事由", 120,
                VisitorRegistration::purpose);
        TableColumn<VisitorRegistration, String> time = Components.textColumn("登记时间", 130,
                entry -> Fmt.time(entry.time()));
        table.getColumns().setAll(List.of(name, phone, plate, purpose, time));
        table.setPrefHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void submit() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String plate = Plates.normalize(plateField.getText());
        if (name.isEmpty()) {
            context.toast("请填写访客姓名", Toasts.Kind.ERROR);
            nameField.requestFocus();
            return;
        }
        if (!phone.matches("\\d{6,15}")) {
            context.toast("联系电话应为 6 - 15 位数字", Toasts.Kind.ERROR);
            phoneField.requestFocus();
            return;
        }
        if (!Plates.looksValid(plate)) {
            context.toast("请输入有效车牌", Toasts.Kind.ERROR);
            plateField.requestFocus();
            return;
        }
        VisitorRegistration registration = new VisitorRegistration(name, phone, plate,
                purposeBox.getValue(), LocalDateTime.now());
        Outcome outcome = context.backend().registerVisitor(registration);
        context.toast(outcome.message(),
                outcome.success() ? Toasts.Kind.SUCCESS : Toasts.Kind.ERROR);
        if (outcome.success()) {
            nameField.clear();
            phoneField.clear();
            plateField.clear();
            purposeBox.getSelectionModel().selectFirst();
        }
        onShow();
    }
}
