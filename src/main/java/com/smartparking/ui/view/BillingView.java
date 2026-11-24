package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.backend.ParkingBackend.Outcome;
import com.smartparking.ui.backend.ParkingBackend.Quote;
import com.smartparking.ui.component.Components;
import com.smartparking.ui.component.Icons;
import com.smartparking.ui.component.Toasts;
import com.smartparking.ui.util.Fmt;
import com.smartparking.ui.util.Plates;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** 管理员计费设置：单位费率调整与费用试算。 */
public final class BillingView extends BaseView {

    private final Spinner<Integer> costSpinner = new Spinner<>();
    private final Label currentRate = Components.label("¥ 0", "rate-value");
    private final Label rateHint = Components.label("", "rate-hint");
    private final TextField quotePlate = new TextField();
    private final VBox quoteResult = new VBox(6);
    private final Components.Stat rateStat =
            new Components.Stat("当前费率", Icons.MONEY, "stat-purple");
    private final Components.Stat formulaStat =
            new Components.Stat("计费方式", Icons.CLOCK, "stat-blue");

    public BillingView(AppContext context) {
        super(context);
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9999,
                        Math.max(0, context.backend().unitCost()));
        costSpinner.setValueFactory(factory);
        costSpinner.setEditable(true);
        costSpinner.setPrefWidth(150);

        var save = Components.primary("保存费率", Icons.CHECK);
        save.setOnAction(event -> saveRate());

        VBox setCard = Components.card("单位费率", "停车费用 = 向上取整小时数 × 单位费率",
                new VBox(14,
                        new HBox(16, currentRate, rateHint),
                        new HBox(10, costSpinner, Components.label("元 / 小时", "field-hint")),
                        save,
                        Components.divider(),
                        Components.callout(
                                "费率变更会记录到操作日志。修改后新产生的费用立即生效，已完成缴费不受影响。",
                                "callout-info", Icons.INFO)));
        setCard.setMinWidth(390);
        setCard.setPrefWidth(430);

        VBox quoteCard = Components.card("费用试算", "选择场内车辆，按当前费率估算应付金额",
                buildQuoteBody());
        HBox.setHgrow(quoteCard, Priority.ALWAYS);
        quoteCard.setMaxWidth(Double.MAX_VALUE);

        HBox stats = new HBox(14, rateStat, formulaStat);
        for (var node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((javafx.scene.layout.Region) node).setMaxWidth(Double.MAX_VALUE);
        }

        HBox body = new HBox(16, setCard, quoteCard);
        getChildren().addAll(stats, body, buildFormulaCard());
        onShow();
    }

    @Override
    public void onShow() {
        guard(() -> {
            int cost = context.backend().unitCost();
            currentRate.setText(Fmt.money(cost));
            rateHint.setText("元 / 小时");
            rateStat.setValue("¥ " + cost);
            rateStat.setHint("元 / 小时");
            formulaStat.setValue("按小时向上取整");
            formulaStat.setHint("不足 1 小时按 1 小时计");
        });
    }

    private VBox buildQuoteBody() {
        quotePlate.setPromptText("输入场内车牌，例如：京A12345");
        quotePlate.setOnAction(event -> quote());
        var button = Components.button("开始试算", Icons.SEARCH, "#1d4ed8", "btn-soft");
        button.setOnAction(event -> quote());

        quoteResult.setVisible(false);
        quoteResult.setManaged(false);
        quoteResult.getStyleClass().add("quote-panel");

        VBox body = new VBox(14,
                Components.field("车牌", quotePlate, "车辆必须在场内才能计算费用"),
                button,
                quoteResult);
        VBox.setVgrow(quoteResult, Priority.ALWAYS);
        return body;
    }

    private VBox buildFormulaCard() {
        HBox steps = new HBox(12,
                formulaStep("停车时长", "入场时间 → 当前时间"),
                arrow(),
                formulaStep("向上取整", "不足 1 小时按 1 小时"),
                arrow(),
                formulaStep("乘以费率", "小时数 × 单位费率"),
                arrow(),
                formulaStep("输出费用", "四舍五入到元"));
        steps.setAlignment(Pos.CENTER_LEFT);
        VBox card = Components.card("计费公式", "与 Function.getStoppingCost / Car.getStoppingHour 保持一致", steps);
        return card;
    }

    private VBox formulaStep(String title, String detail) {
        VBox box = new VBox(2,
                Components.label(title, "formula-title"),
                Components.label(detail, "formula-desc"));
        box.getStyleClass().add("formula-step");
        box.setAlignment(Pos.CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Label arrow() {
        return Components.label("→", "formula-arrow");
    }

    private void saveRate() {
        Integer value = costSpinner.getValue();
        if (value == null) {
            context.toast("请输入有效费率", Toasts.Kind.ERROR);
            return;
        }
        Outcome outcome = context.backend().setUnitCost(value);
        context.toast(outcome.message(),
                outcome.success() ? Toasts.Kind.SUCCESS : Toasts.Kind.ERROR);
        onShow();
    }

    private void quote() {
        String plate = Plates.normalize(quotePlate.getText());
        if (!Plates.looksValid(plate)) {
            showQuoteError("请输入有效车牌");
            return;
        }
        Quote quote = context.backend().quote(plate);
        if (quote == null) {
            showQuoteError("该车辆不在场内，无法计算费用");
            return;
        }
        quoteResult.getChildren().setAll(
                new HBox(10,
                        Components.label(Fmt.money(quote.fee()), "rate-value"),
                        Components.label("预计应付", "quote-label")),
                Components.label("停车 " + quote.hours() + " 小时　·　费率 ¥ "
                        + quote.unitCost() + "/小时　·　离场时间 " + Fmt.time(quote.exitTime()),
                        "quote-detail"));
        quoteResult.setVisible(true);
        quoteResult.setManaged(true);
    }

    private void showQuoteError(String message) {
        quoteResult.getChildren().setAll(Components.callout(message, "callout-warn", Icons.INFO));
        quoteResult.setVisible(true);
        quoteResult.setManaged(true);
    }
}
