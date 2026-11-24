package com.smartparking.ui.view;

import com.smartparking.ui.AppContext;
import com.smartparking.ui.component.Toasts;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

/**
 * 页面基类：统一内边距与刷新容错。
 * 后端实现抛异常时界面不会崩溃，只给出一次限频提示。
 */
public abstract class BaseView extends VBox implements Refreshable {

    protected final AppContext context;
    private long lastErrorAt;

    protected BaseView(AppContext context) {
        this.context = context;
        getStyleClass().add("view");
        setPadding(new Insets(24, 26, 26, 26));
        setSpacing(18);
    }

    /** 包装数据读取，业务层尚未完善时不至于让界面白屏。 */
    protected void guard(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            long now = System.currentTimeMillis();
            if (now - lastErrorAt > 8000) {
                lastErrorAt = now;
                context.toast("数据读取失败：" + exception.getMessage(), Toasts.Kind.ERROR);
            }
        }
    }
}
