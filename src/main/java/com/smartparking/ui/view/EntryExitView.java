package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.Outcome;
import com.smartparking.ui.backend.ParkingBackend.Quote;
import com.smartparking.ui.backend.ParkingBackend.Snapshot;
import com.smartparking.ui.backend.Role;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.GateIndicator;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.component.Toasts;
import com.smartparking.ui.util.Fmt;
import com.smartparking.ui.util.Plates;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 出入场放行 / 缴费离场：车牌识别、黑名单与满位结果、费用试算、缴费、道闸动画。
 * 访客角色只展示离场部分。
 */
public final class EntryExitView extends BaseView {

    private final boolean visitorOnly;
    private final TextField entryPlate = new TextField();
    private final TextField exitPlate = new TextField();
    private final VBox entryResult = new VBox(5);
    private final VBox exitResult = new VBox(5);
    private final VBox quotePanel = new VBox(10);
    private final Label feeValue = Components.label("¥ 0", "quote-fee");
    private final Label quoteDetail = Components.label("", "quote-detail");
    private final Button confirmLeave = Components.success("确认缴费并离场", Icons.CHECK);
    private final GateIndicator gate = new GateIndicator();
    private final Label gateHint = Components.label("", "field-hint");
    private final VBox historyList = new VBox(6);
    private final Label ruleLabel = Components.label("", "info-value");
    private final Label parkedLabel = Components.label("", "info-value");
    private final Label queueLabel = Components.label("", "info-value");

    private final List<Outcome> outcomes = new ArrayList<>();
    private Quote pendingQuote;
    private PauseTransition autoClose;

    public EntryExitView(AppContext context, boolean visitorOnly) {
        super(context);
        this.visitorOnly = visitorOnly;
        getChildren().add(buildInfoBar());
        if (!visitorOnly) {
            HBox row = new HBox(16, buildEntryCard(), buildGateCard());
            HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
            ((VBox) row.getChildren().get(0)).setMaxWidth(Double.MAX_VALUE);
            getChildren().add(row);
        }
        HBox lower = new HBox(16, buildExitCard(), visitorOnly ? buildGateCard() : buildHistoryCard());
        HBox.setHgrow(lower.getChildren().get(0), Priority.ALWAYS);
        ((VBox) lower.getChildren().get(0)).setMaxWidth(Double.MAX_VALUE);
        getChildren().add(lower);

        if (visitorOnly) {
            getChildren().add(1, Components.callout(
                    "访客缴费离场：输入车牌查询应付费用，确认缴费后道闸自动抬起。",
                    "callout-info", Icons.INFO));
        }
        onShow();
    }

    @Override
    public void onShow() {
        guard(() -> {
            Snapshot snapshot = context.backend().snapshot();
            ruleLabel.setText("¥ " + snapshot.unitCost() + " / 小时");
            parkedLabel.setText(snapshot.occupied() + " / " + snapshot.capacity());
            queueLabel.setText(snapshot.queue().size() + " 辆");
            gate.setOpen(snapshot.gate().open());
            gate.setLastPlate(snapshot.gate().lastPlate());
            if (context.session().role() == Role.VISITOR) {
                gateHint.setText("访客不能手动控制道闸，缴费成功后自动抬杆");
            } else {
                gateHint.setText("抬杆后 3 秒自动落杆；也可手动落杆");
            }
        });
    }

    // ------------------------------------------------------------------
    // 信息栏
    // ------------------------------------------------------------------

    private HBox buildInfoBar() {
        HBox bar = new HBox(26,
                infoItem(Icons.MONEY, "计费规则", ruleLabel),
                infoItem(Icons.CAR, "在场车辆", parkedLabel),
                infoItem(Icons.QUEUE, "排队车辆", queueLabel),
                Components.spacer(),
                Components.chip("实时刷新", "badge-info"));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("info-bar");
        return bar;
    }

    private HBox infoItem(String icon, String label, Label value) {
        VBox texts = new VBox(1, Components.label(label, "info-label"), value);
        HBox item = new HBox(9, Icons.of(icon, 17, "#64748b"), texts);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    // ------------------------------------------------------------------
    // 入场
    // ------------------------------------------------------------------

    private VBox buildEntryCard() {
        entryPlate.setPromptText("例如：京A12345");
        entryPlate.setOnAction(event -> doEnter());
        var scan = Components.primary("识别并入场", Icons.GATE);
        scan.setOnAction(event -> doEnter());

        VBox form = new VBox(12,
                Components.field("车牌识别", entryPlate, "支持中文车牌、字母与数字，格式示例：京A12345"),
                new HBox(10, scan),
                entryResult);
        entryResult.setVisible(false);
        entryResult.setManaged(false);

        HBox body = new HBox(24, form, Components.spacer(), buildFlowPanel());
        HBox.setHgrow(form, Priority.ALWAYS);
        VBox card = Components.card("车辆入场", "识别车牌后自动校验黑名单与车位状态", body);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildFlowPanel() {
        VBox flow = new VBox(11,
                Components.label("识别流程", "field-label"),
                flowStep("1", "车牌识别", "摄像头读取车牌"),
                flowStep("2", "校验与判断", "黑名单 · 车位是否已满"),
                flowStep("3", "抬杆放行", "PASS / WAIT / REJECT"));
        flow.getStyleClass().add("entry-flow");
        flow.setMinWidth(220);
        flow.setPrefWidth(240);
        flow.setMaxWidth(270);
        return flow;
    }

    private HBox flowStep(String number, String title, String detail) {
        javafx.scene.layout.StackPane badge = new javafx.scene.layout.StackPane(
                Components.label(number, "step-number"));
        badge.getStyleClass().add("step-badge");
        Label detailLabel = Components.label(detail, "step-desc");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(180);
        VBox texts = new VBox(1,
                Components.label(title, "step-title"),
                detailLabel);
        HBox row = new HBox(9, badge, texts);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void doEnter() {
        String plate = Plates.normalize(entryPlate.getText());
        if (!Plates.looksValid(plate)) {
            showResult(entryResult, Outcome.fail("FAIL", "请输入有效车牌，例如 京A12345", plate));
            entryPlate.requestFocus();
            return;
        }
        Outcome outcome = context.backend().enter(plate);
        addHistory(outcome);
        showResult(entryResult, outcome);
        if (outcome.success()) {
            flashGate(outcome.plate());
            entryPlate.clear();
            context.toast(outcome.message(), outcome.code().equals("WAIT")
                    ? Toasts.Kind.INFO : Toasts.Kind.SUCCESS);
        } else {
            context.toast(outcome.message(), Toasts.Kind.ERROR);
        }
        onShow();
    }

    // ------------------------------------------------------------------
    // 离场
    // ------------------------------------------------------------------

    private VBox buildExitCard() {
        exitPlate.setPromptText("输入场内车牌查询费用");
        exitPlate.setOnAction(event -> queryQuote());
        exitPlate.textProperty().addListener((observable, oldValue, newValue) -> {
            // 查询费用后如果改了车牌，旧报价必须失效，避免缴错车。
            if (pendingQuote != null && !pendingQuote.plate().equalsIgnoreCase(
                    newValue == null ? "" : newValue.trim())) {
                pendingQuote = null;
                showQuote(null);
            }
        });

        var query = Components.button("查询费用", Icons.SEARCH, "#1d4ed8", "btn-soft");
        query.setOnAction(event -> queryQuote());

        confirmLeave.setDisable(true);
        confirmLeave.setOnAction(event -> confirmLeave());

        quotePanel.getStyleClass().add("quote-panel");
        quotePanel.setVisible(false);
        quotePanel.setManaged(false);
        Label feeLabel = Components.label("应付金额", "quote-label");
        VBox feeBox = new VBox(2, feeLabel, feeValue);
        VBox detailBox = new VBox(4, quoteDetail,
                Components.label("不足 1 小时按 1 小时计费", "quote-note"));
        HBox row = new HBox(18, feeBox, detailBox);
        row.setAlignment(Pos.CENTER_LEFT);
        quotePanel.getChildren().add(row);

        exitResult.setVisible(false);
        exitResult.setManaged(false);

        VBox form = new VBox(13,
                Components.field("车牌", exitPlate, "先查询费用，再确认缴费离场"),
                new HBox(10, query, confirmLeave),
                quotePanel,
                exitResult);

        String title = context.session().role() == Role.VISITOR ? "缴费离场" : "车辆离场";
        String subtitle = context.session().role() == Role.VISITOR
                ? "查询停车费用并完成缴费" : "计算停车费、挪车并自动补位";
        VBox card = Components.card(title, subtitle, form);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private void queryQuote() {
        String plate = Plates.normalize(exitPlate.getText());
        if (!Plates.looksValid(plate)) {
            pendingQuote = null;
            showQuote(null);
            showResult(exitResult, Outcome.fail("FAIL", "请输入有效车牌", plate));
            exitPlate.requestFocus();
            return;
        }
        Quote quote = context.backend().quote(plate);
        if (quote == null) {
            pendingQuote = null;
            showQuote(null);
            showResult(exitResult, Outcome.fail("FAIL", "该车辆不在场内，无法计费", plate));
            return;
        }
        pendingQuote = quote;
        showQuote(quote);
        exitResult.setVisible(false);
        exitResult.setManaged(false);
        confirmLeave.setDisable(false);
    }

    private void showQuote(Quote quote) {
        boolean show = quote != null;
        quotePanel.setVisible(show);
        quotePanel.setManaged(show);
        if (!show) {
            confirmLeave.setDisable(true);
            return;
        }
        feeValue.setText(Fmt.money(quote.fee()));
        quoteDetail.setText("车牌 " + quote.plate()
                + "　·　停车 " + quote.hours() + " 小时"
                + "　·　费率 ¥ " + quote.unitCost() + "/小时");
    }

    private void confirmLeave() {
        if (pendingQuote == null) {
            return;
        }
        String plate = pendingQuote.plate();
        Outcome outcome = context.backend().leave(plate);
        addHistory(outcome);
        showResult(exitResult, outcome);
        if (outcome.success()) {
            pendingQuote = null;
            showQuote(null);
            exitPlate.clear();
            flashGate(plate);
            context.toast(outcome.message(), Toasts.Kind.SUCCESS);
        } else {
            context.toast(outcome.message(), Toasts.Kind.ERROR);
        }
        onShow();
    }

    // ------------------------------------------------------------------
    // 道闸与结果区
    // ------------------------------------------------------------------

    private VBox buildGateCard() {
        var raise = Components.button("抬杆", Icons.GATE, "#166534", "btn-soft");
        var lower = Components.button("落杆", Icons.GATE, "#991b1b", "btn-soft");
        raise.setOnAction(event -> {
            context.backend().setGate(true);
            gate.setOpen(true);
            stopAutoClose();
            onShow();
        });
        lower.setOnAction(event -> {
            context.backend().setGate(false);
            gate.setOpen(false);
            stopAutoClose();
            onShow();
        });
        if (context.session().role() == Role.VISITOR) {
            raise.setDisable(true);
            lower.setDisable(true);
        }

        VBox body = new VBox(6, gate, new HBox(8, raise, lower), gateHint);
        body.setAlignment(Pos.CENTER);
        VBox card = Components.card("道闸状态", "识别成功时指示灯由红变绿", body);
        card.setMinWidth(300);
        card.setPrefWidth(320);
        return card;
    }

    private VBox buildHistoryCard() {
        historyList.setPadding(new Insets(2, 0, 0, 0));
        VBox card = Components.card("本次操作记录", "仅记录当前会话内的操作", historyList);
        card.setMinWidth(300);
        card.setPrefWidth(330);
        renderHistory();
        return card;
    }

    private void showResult(VBox box, Outcome outcome) {
        box.getChildren().clear();
        box.getStyleClass().removeAll("result-success", "result-warn", "result-error");
        String tone;
        Node icon;
        if (!outcome.success()) {
            tone = "result-error";
            icon = Icons.of(Icons.CLOSE, 17, "#dc2626");
        } else if ("WAIT".equals(outcome.code())) {
            tone = "result-warn";
            icon = Icons.of(Icons.INFO, 17, "#b45309");
        } else {
            tone = "result-success";
            icon = Icons.of(Icons.CHECK, 17, "#15803d");
        }
        box.getStyleClass().addAll("result-box", tone);
        Label title = Components.label(outcome.message(), "result-title");
        HBox head = new HBox(8, icon, title);
        head.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(head);
        if (outcome.plate() != null) {
            box.getChildren().add(Components.label(
                    "车牌 " + outcome.plate() + (outcome.fee() != null
                            ? "　·　费用 " + Fmt.money(outcome.fee()) : ""),
                    "result-detail"));
        }
        box.setVisible(true);
        box.setManaged(true);
    }

    private void flashGate(String plate) {
        gate.pulseOpen();
        gate.setLastPlate(plate);
        stopAutoClose();
        autoClose = new PauseTransition(Duration.seconds(3));
        autoClose.setOnFinished(event -> {
            context.backend().setGate(false);
            gate.setOpen(false);
        });
        autoClose.play();
    }

    private void stopAutoClose() {
        if (autoClose != null) {
            autoClose.stop();
            autoClose = null;
        }
    }

    private void addHistory(Outcome outcome) {
        outcomes.add(0, outcome);
        while (outcomes.size() > 6) {
            outcomes.remove(outcomes.size() - 1);
        }
        renderHistory();
    }

    private void renderHistory() {
        historyList.getChildren().clear();
        if (outcomes.isEmpty()) {
            historyList.getChildren().add(Components.emptyState(
                    "还没有操作", "入场或离场后，结果会显示在这里", Icons.HISTORY));
            return;
        }
        for (Outcome outcome : outcomes) {
            String tone = outcome.success()
                    ? ("WAIT".equals(outcome.code()) ? "badge-warn" : "badge-success")
                    : "badge-danger";
            HBox row = new HBox(8,
                    Components.label(Fmt.time(outcome.time()), "history-time"),
                    Components.label(outcome.plate() == null ? "道闸" : outcome.plate(),
                            "history-plate"),
                    Components.chip(outcome.success() ? "成功" : "失败", tone),
                    Components.label(outcome.message(), "history-message"));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("history-row");
            HBox.setHgrow(row.getChildren().get(3), Priority.ALWAYS);
            historyList.getChildren().add(row);
        }
    }
}
