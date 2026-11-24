package com.smartparking.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** 底部浮动提示，用于操作结果反馈。 */
public final class Toasts {

    public enum Kind {
        SUCCESS("toast-success", Icons.CHECK, "#16a34a"),
        ERROR("toast-error", Icons.INFO, "#dc2626"),
        INFO("toast-info", Icons.INFO, "#2563eb");

        private final String styleClass;
        private final String icon;
        private final String color;

        Kind(String styleClass, String icon, String color) {
            this.styleClass = styleClass;
            this.icon = icon;
            this.color = color;
        }
    }

    private Toasts() {
    }

    public static void show(StackPane host, String message, Kind kind) {
        if (host == null || message == null || message.isBlank()) {
            return;
        }
        Label text = new Label(message);
        text.getStyleClass().add("toast-text");
        text.setWrapText(true);

        HBox toast = new HBox(10, Icons.of(kind.icon, 17, kind.color), text);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().addAll("toast", kind.styleClass);
        toast.setMaxWidth(Region.USE_PREF_SIZE);

        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 30, 0));
        host.getChildren().add(toast);

        toast.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(180), toast);
        in.setToValue(1);
        PauseTransition stay = new PauseTransition(Duration.millis(2400));
        FadeTransition out = new FadeTransition(Duration.millis(260), toast);
        out.setToValue(0);

        SequentialTransition sequence = new SequentialTransition(in, stay, out);
        sequence.setOnFinished(event -> host.getChildren().remove(toast));
        sequence.play();
    }
}
