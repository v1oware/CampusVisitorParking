package com.smartparking.ui.component;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 图形验证码：4 位、字母数字混合、忽略大小写，点击可换一张。
 * 对应 LoginSystem 验证码需求的界面侧实现；真正的账号校验由后端负责。
 */
public final class CaptchaPane extends StackPane {

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final Color[] INK = {
            Color.web("#1d4ed8"), Color.web("#0f766e"),
            Color.web("#b45309"), Color.web("#7c3aed")
    };

    private final Canvas canvas = new Canvas(118, 42);
    private final Random random = new SecureRandom();
    private String code = "";

    public CaptchaPane() {
        getStyleClass().add("captcha");
        setMinSize(118, 42);
        setPrefSize(118, 42);
        setMaxSize(118, 42);
        canvas.setMouseTransparent(true);
        getChildren().add(canvas);
        Tooltip tip = new Tooltip("看不清？点击换一张");
        tip.setShowDelay(Duration.millis(300));
        Tooltip.install(this, tip);
        setOnMouseClicked(event -> refresh());
        refresh();
    }

    public void refresh() {
        code = generateCode();
        draw();
    }

    public boolean matches(String input) {
        return input != null && code.equalsIgnoreCase(input.trim());
    }

    public String code() {
        return code;
    }

    private String generateCode() {
        List<Character> chars = new ArrayList<>(4);
        chars.add(LETTERS.charAt(random.nextInt(LETTERS.length())));
        chars.add(LETTERS.charAt(random.nextInt(LETTERS.length())));
        chars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        chars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        Collections.shuffle(chars, random);
        StringBuilder builder = new StringBuilder(4);
        chars.forEach(builder::append);
        return builder.toString();
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.web("#cbd5e1"));
        gc.setLineWidth(1.2);
        for (int i = 0; i < 5; i++) {
            gc.strokeLine(random.nextInt(118), random.nextInt(42),
                    random.nextInt(118), random.nextInt(42));
        }
        gc.setFill(Color.web("#94a3b8"));
        for (int i = 0; i < 36; i++) {
            gc.fillOval(random.nextInt(114) + 2, random.nextInt(38) + 2, 1.6, 1.6);
        }

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 25));
        for (int i = 0; i < code.length(); i++) {
            gc.save();
            gc.translate(13 + i * 26, 29 + random.nextInt(4) - 2);
            gc.rotate(-14 + random.nextInt(28));
            gc.setFill(INK[i % INK.length]);
            gc.fillText(String.valueOf(code.charAt(i)), 0, 0);
            gc.restore();
        }
    }
}
