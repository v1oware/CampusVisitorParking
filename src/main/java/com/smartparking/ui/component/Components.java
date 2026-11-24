package com.smartparking.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** 界面复用组件：卡片、统计块、按钮、表格、空状态。 */
public final class Components {

    private Components() {
    }

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        VBox.setVgrow(region, Priority.ALWAYS);
        return region;
    }

    public static HBox row(double spacing, Node... children) {
        HBox box = new HBox(spacing, children);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static VBox column(double spacing, Node... children) {
        return new VBox(spacing, children);
    }

    public static Button button(String text, String iconPath, String iconColor, String... styleClasses) {
        Button button = new Button(text);
        if (iconPath != null) {
            button.setGraphic(Icons.of(iconPath, 16, iconColor));
            button.setGraphicTextGap(8);
            button.setContentDisplay(ContentDisplay.LEFT);
        }
        button.getStyleClass().addAll("btn");
        button.getStyleClass().addAll(styleClasses);
        button.setCursor(javafx.scene.Cursor.HAND);
        return button;
    }

    public static Button primary(String text, String iconPath) {
        return button(text, iconPath, "#ffffff", "btn-primary");
    }

    public static Button success(String text, String iconPath) {
        return button(text, iconPath, "#ffffff", "btn-success");
    }

    public static Button ghost(String text, String iconPath) {
        return button(text, iconPath, "#475569", "btn-ghost");
    }

    public static Button danger(String text, String iconPath) {
        return button(text, iconPath, "#ffffff", "btn-danger");
    }

    public static Button iconButton(String iconPath, String tooltip) {
        Button button = new Button();
        button.setGraphic(Icons.of(iconPath, 18, "#475569"));
        button.getStyleClass().addAll("btn", "icon-btn");
        button.setCursor(javafx.scene.Cursor.HAND);
        if (tooltip != null) {
            Tooltip tip = new Tooltip(tooltip);
            tip.setShowDelay(Duration.millis(240));
            button.setTooltip(tip);
        }
        return button;
    }

    public static Label chip(String text, String... extraClasses) {
        Label label = new Label(text);
        label.getStyleClass().add("badge");
        label.getStyleClass().addAll(extraClasses);
        return label;
    }

    /**
     * 白色卡片。
     *
     * @param title    标题，可为 null
     * @param subtitle 副标题，可为 null
     * @param action   右上角操作节点，可为 null
     * @param body     内容
     */
    public static VBox card(String title, String subtitle, Node action, Node body) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        if (title != null) {
            VBox texts = new VBox(3);
            Label titleLabel = label(title, "card-title");
            texts.getChildren().add(titleLabel);
            if (subtitle != null && !subtitle.isBlank()) {
                texts.getChildren().add(label(subtitle, "card-subtitle"));
            }
            HBox header = new HBox(12, texts, spacer());
            header.setAlignment(Pos.TOP_LEFT);
            if (action != null) {
                header.getChildren().add(action);
            }
            card.getChildren().add(header);
        }
        if (body != null) {
            card.getChildren().add(body);
            VBox.setVgrow(body, Priority.ALWAYS);
        }
        return card;
    }

    public static VBox card(String title, String subtitle, Node body) {
        return card(title, subtitle, null, body);
    }

    public static VBox field(String labelText, Region input, String hint) {
        VBox box = new VBox(6);
        Label label = label(labelText, "field-label");
        box.getChildren().add(label);
        input.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(input);
        if (hint != null) {
            box.getChildren().add(label(hint, "field-hint"));
        }
        return box;
    }

    public static <T> TableView<T> table(String emptyTitle, String emptyDescription) {
        TableView<T> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(emptyState(emptyTitle, emptyDescription, Icons.EMPTY_BOX));
        table.setFocusTraversable(false);
        return table;
    }

    public static <T> TableColumn<T, String> textColumn(String title, double width,
                                                         java.util.function.Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> {
            String text = value.apply(data.getValue());
            return new javafx.beans.property.SimpleStringProperty(text == null ? "—" : text);
        });
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTooltip(empty || item == null || item.isBlank() ? null : new Tooltip(item));
            }
        });
        return column;
    }

    public static VBox emptyState(String title, String description, String iconPath) {
        StackPane icon = Icons.of(iconPath == null ? Icons.EMPTY_BOX : iconPath, 28, "#94a3b8");
        icon.getStyleClass().add("empty-icon");
        VBox box = new VBox(8, icon, label(title, "empty-title"), label(description, "empty-desc"));
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30, 16, 30, 16));
        return box;
    }

    public static HBox callout(String text, String tone, String iconPath) {
        Label message = label(text, "callout-text");
        message.setWrapText(true);
        message.setMaxWidth(Double.MAX_VALUE);
        HBox box = new HBox(10, Icons.of(iconPath == null ? Icons.INFO : iconPath, 17, "#475569"),
                message);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().addAll("callout", tone);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(message, Priority.ALWAYS);
        return box;
    }

    public static Region divider() {
        Region region = new Region();
        region.getStyleClass().add("divider");
        region.setMaxWidth(Double.MAX_VALUE);
        return region;
    }

    /** 顶部统计卡片，可动态刷新数值。 */
    public static final class Stat extends HBox {

        private final Label valueLabel = new Label("—");
        private final Label hintLabel = new Label("");

        public Stat(String title, String iconPath, String tone) {
            super(14);
            getStyleClass().addAll("stat-card", tone);
            setAlignment(Pos.CENTER_LEFT);
            setPadding(new Insets(18, 18, 18, 18));

            VBox texts = new VBox(4,
                    label(title, "stat-label"),
                    valueLabel,
                    hintLabel);
            valueLabel.getStyleClass().add("stat-value");
            hintLabel.getStyleClass().add("stat-hint");
            HBox.setHgrow(texts, Priority.ALWAYS);

            StackPane icon = Icons.of(iconPath, 22, "#ffffff");
            icon.getStyleClass().add("stat-icon");
            getChildren().addAll(texts, icon);
        }

        public void setValue(String value) {
            valueLabel.setText(value);
        }

        public void setHint(String hint) {
            hintLabel.setText(hint);
        }
    }
}
