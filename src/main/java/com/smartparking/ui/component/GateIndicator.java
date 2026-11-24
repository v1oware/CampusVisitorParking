package com.smartparking.ui.component;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * 道闸状态可视化：栏杆抬起 / 落下、指示灯红绿切换、显示最近识别车牌。
 * 对应 NFR-1“识别成功时道闸图标由红变绿”。
 */
public final class GateIndicator extends VBox {

    private final Rotate armRotation = new Rotate(0, 0, 4);
    private final Circle lamp = new Circle(7);
    private final Label stateLabel = new Label("已落杆");
    private final Label plateLabel = new Label("等待识别");
    private Timeline timeline;
    private boolean open;

    public GateIndicator() {
        getStyleClass().add("gate-indicator");
        setAlignment(Pos.CENTER);
        setSpacing(4);

        Pane canvas = new Pane();
        canvas.setPrefSize(184, 96);
        canvas.setMinSize(184, 96);
        canvas.setMaxSize(184, 96);

        Rectangle ground = new Rectangle(4, 84, 176, 2);
        ground.setFill(Color.web("#e2e8f0"));

        Rectangle post = new Rectangle(28, 22, 8, 62);
        post.setArcWidth(4);
        post.setArcHeight(4);
        post.setFill(Color.web("#334155"));

        Rectangle armBase = new Rectangle(0, 0, 96, 7);
        armBase.setArcWidth(3);
        armBase.setArcHeight(3);
        armBase.setFill(Color.web("#ef4444"));

        Group armGroup = new Group(armBase);
        for (int x : new int[]{22, 52, 82}) {
            Rectangle stripe = new Rectangle(x, 0, 7, 7);
            stripe.setFill(Color.web("#fff7ed"));
            armGroup.getChildren().add(stripe);
        }
        armGroup.setLayoutX(36);
        armGroup.setLayoutY(27);
        armGroup.getTransforms().add(armRotation);

        Circle pivot = new Circle(40, 30, 5);
        pivot.setFill(Color.web("#0f172a"));

        lamp.setCenterX(32);
        lamp.setCenterY(12);
        lamp.setFill(Color.web("#dc2626"));
        lamp.setStroke(Color.web("#ffffff"));
        lamp.setStrokeWidth(2);

        canvas.getChildren().addAll(ground, post, armGroup, pivot, lamp);

        stateLabel.getStyleClass().add("gate-state");
        plateLabel.getStyleClass().add("gate-plate");
        getChildren().addAll(canvas, stateLabel, plateLabel);
        updateVisual(false);
    }

    /** 直接同步到后端状态（用于周期刷新）。 */
    public void setOpen(boolean open) {
        if (this.open == open && timeline == null) {
            updateVisual(open);
            return;
        }
        this.open = open;
        animateArm(open ? -78 : 0);
        updateVisual(open);
    }

    /** 入场/离场成功后播放一次抬杆动画。 */
    public void pulseOpen() {
        this.open = true;
        animateArm(-78);
        updateVisual(true);
    }

    public void setLastPlate(String plate) {
        plateLabel.setText(plate == null || plate.isBlank() ? "等待识别" : "最近车牌  " + plate);
    }

    public boolean isOpen() {
        return open;
    }

    private void updateVisual(boolean isOpen) {
        stateLabel.setText(isOpen ? "道闸已抬起 · 允许通行" : "道闸已落下 · 禁止通行");
        lamp.setFill(Color.web(isOpen ? "#16a34a" : "#dc2626"));
        stateLabel.getStyleClass().removeAll("gate-state-open", "gate-state-closed");
        stateLabel.getStyleClass().add(isOpen ? "gate-state-open" : "gate-state-closed");
    }

    private void animateArm(double target) {
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(new KeyFrame(Duration.millis(420),
                new KeyValue(armRotation.angleProperty(), target, Interpolator.EASE_BOTH)));
        timeline.play();
    }
}
