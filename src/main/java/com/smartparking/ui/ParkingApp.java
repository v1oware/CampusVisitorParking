package com.smartparking.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Objects;

/** JavaFX 应用入口。 */
public final class ParkingApp extends Application {

    private AppContext context;

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.getStyleClass().add("app-root");

        context = new AppContext(stage, root);

        Scene scene = new Scene(root, 1240, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                ParkingApp.class.getResource("/css/app.css"),
                "缺少样式文件 /css/app.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("校园访客停车模拟系统");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        context.showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.shutdown();
        }
    }
}
