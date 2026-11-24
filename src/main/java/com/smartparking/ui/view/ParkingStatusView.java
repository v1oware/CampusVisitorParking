package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.ParkedCar;
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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 车位状态：20 个车位的占用平面图、道闸状态与等候队列。 */
public final class ParkingStatusView extends BaseView {

    private final Components.Stat totalStat =
            new Components.Stat("总车位", Icons.CAR, "stat-blue");
    private final Components.Stat usedStat =
            new Components.Stat("已占用", Icons.GATE, "stat-amber");
    private final Components.Stat freeStat =
            new Components.Stat("空余", Icons.CHECK, "stat-green");
    private final Components.Stat queueStat =
            new Components.Stat("排队", Icons.QUEUE, "stat-purple");

    private final GridPane slotGrid = new GridPane();
    private final Label laneHint = Components.label("", "lane-hint");
    private final GateIndicator gate = new GateIndicator();
    private final TableView<QueueCar> queueTable = Components.table(
            "暂无排队车辆", "车位充足时新车辆可直接入场");

    public ParkingStatusView(AppContext context) {
        super(context);
        slotGrid.setHgap(8);
        slotGrid.setVgap(8);
        for (int i = 0; i < 5; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(20);
            column.setHgrow(Priority.ALWAYS);
            slotGrid.getColumnConstraints().add(column);
        }

        getChildren().addAll(buildStats(), buildMainRow(), buildQueueCard());
        onShow();
    }

    @Override
    public void onShow() {
        guard(() -> {
            Snapshot snapshot = context.backend().snapshot();
            renderStats(snapshot);
            renderGrid(snapshot);
            renderGate(snapshot);
            renderQueue(snapshot.queue());
        });
    }

    private HBox buildStats() {
        HBox box = new HBox(14, totalStat, usedStat, freeStat, queueStat);
        for (var node : box.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((javafx.scene.layout.Region) node).setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private HBox buildMainRow() {
        HBox laneHeader = new HBox(8,
                Components.chip("入口 / 出口", "badge-info"),
                Components.label("车位 1 最靠近道闸，车位 20 位于通道最深处", "lane-hint"));
        laneHeader.setAlignment(Pos.CENTER_LEFT);

        VBox laneBody = new VBox(12, laneHeader, slotGrid, laneHint);
        VBox laneCard = Components.card("停车场平面", "单车道通道，车辆按栈结构进出",
                Components.chip("LIVE", "badge-success"), laneBody);

        VBox gateCard = Components.card("道闸", "红绿灯与栏杆状态", buildGateBody());

        HBox row = new HBox(16, laneCard, gateCard);
        HBox.setHgrow(laneCard, Priority.ALWAYS);
        laneCard.setMaxWidth(Double.MAX_VALUE);
        gateCard.setMinWidth(300);
        gateCard.setPrefWidth(320);
        return row;
    }

    private VBox buildGateBody() {
        var raise = Components.button("抬杆", Icons.GATE, "#166534", "btn-soft");
        var lower = Components.button("落杆", Icons.GATE, "#991b1b", "btn-soft");
        raise.setOnAction(event -> operateGate(true));
        lower.setOnAction(event -> operateGate(false));

        VBox body = new VBox(8, gate, new HBox(8, raise, lower));
        body.setAlignment(Pos.CENTER);
        if (context.session().role() == Role.VISITOR) {
            raise.setDisable(true);
            lower.setDisable(true);
            body.getChildren().add(Components.label("访客可查看道闸状态，不能直接操作", "field-hint"));
        }
        return body;
    }

    private VBox buildQueueCard() {
        TableColumn<QueueCar, String> position = Components.textColumn("排位", 70,
                car -> "第 " + car.position() + " 位");
        TableColumn<QueueCar, String> plate = Components.textColumn("车牌", 160,
                QueueCar::plate);
        TableColumn<QueueCar, String> since = Components.textColumn("加入时间", 120,
                car -> Fmt.time(car.since()));
        TableColumn<QueueCar, String> waited = Components.textColumn("已等待", 120,
                car -> Fmt.duration(car.waitingMinutes()));
        queueTable.getColumns().setAll(List.of(position, plate, since, waited));
        queueTable.setPrefHeight(230);

        VBox card = Components.card("等候队列", "车位释放后，队首车辆自动补位",
                queueTable);
        VBox.setVgrow(queueTable, Priority.ALWAYS);
        return card;
    }

    private void renderStats(Snapshot snapshot) {
        totalStat.setValue(String.valueOf(snapshot.capacity()));
        totalStat.setHint("MAXSIZE = " + snapshot.capacity());
        usedStat.setValue(snapshot.occupied() + " 辆");
        usedStat.setHint("占用率 " + Math.round(snapshot.occupancyRate() * 100) + "%");
        freeStat.setValue(snapshot.free() + " 个");
        freeStat.setHint(context.session().role() == Role.VISITOR
                ? "访客当前可用车位" : "可继续入场");
        queueStat.setValue(snapshot.queue().size() + " 辆");
        queueStat.setHint(snapshot.queue().isEmpty() ? "队列为空"
                : "队首 " + snapshot.queue().get(0).plate());
    }

    private void renderGrid(Snapshot snapshot) {
        slotGrid.getChildren().clear();
        Map<Integer, ParkedCar> bySlot = new HashMap<>();
        for (ParkedCar car : snapshot.parkedTopFirst()) {
            bySlot.put(car.slot(), car);
        }
        int capacity = Math.max(1, snapshot.capacity());
        for (int slot = 1; slot <= capacity; slot++) {
            ParkedCar car = bySlot.get(slot);
            Node cell = car == null ? freeSlot(slot) : usedSlot(car);
            slotGrid.add(cell, (slot - 1) % 5, (slot - 1) / 5);
        }
        laneHint.setText("已占用 " + snapshot.occupied() + " 个车位，空余 "
                + snapshot.free() + " 个。车辆离场时，阻挡车辆会先移到辅助栈再归位。");
    }

    private VBox freeSlot(int slot) {
        VBox cell = new VBox(3,
                Components.label("车位 " + slot, "slot-index"),
                Components.label("空位", "slot-free-text"));
        cell.getStyleClass().addAll("slot", "slot-free");
        cell.setAlignment(Pos.CENTER);
        cell.setPrefHeight(62);
        cell.setMaxWidth(Double.MAX_VALUE);
        return cell;
    }

    private VBox usedSlot(ParkedCar car) {
        VBox cell = new VBox(3,
                Components.label("车位 " + car.slot(), "slot-index"),
                Components.label(car.plate(), "slot-plate"),
                Components.label(Fmt.duration(car.parkedMinutes()), "slot-time"));
        cell.getStyleClass().addAll("slot", "slot-used");
        cell.setAlignment(Pos.CENTER);
        cell.setPrefHeight(62);
        cell.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(cell, new Tooltip("车牌：" + car.plate()
                + "\n入场时间：" + Fmt.dateTime(car.entryTime())
                + "\n已停放：" + Fmt.duration(car.parkedMinutes())));
        return cell;
    }

    private void renderGate(Snapshot snapshot) {
        gate.setOpen(snapshot.gate().open());
        gate.setLastPlate(snapshot.gate().lastPlate());
    }

    private void renderQueue(List<QueueCar> queue) {
        queueTable.getItems().setAll(queue);
    }

    private void operateGate(boolean open) {
        var outcome = context.backend().setGate(open);
        context.toast(outcome.message(),
                outcome.success() ? Toasts.Kind.SUCCESS : Toasts.Kind.ERROR);
        onShow();
    }
}
