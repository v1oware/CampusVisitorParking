package com.smartparking.ui.backend;

/**
 * 数据源工厂。
 *
 * <p>默认使用 {@link DemoBackend}，保证在队友完成业务层之前，GUI 也能独立演示。
 * 通过登录页的“数据源”下拉框，或启动参数切换：</p>
 *
 * <pre>mvn javafx:run -Dparking.backend=service</pre>
 *
 * <p>队友完成后建议把默认值改成 {@link #MODE_SERVICE}。</p>
 */
public final class BackendFactory {

    public static final String MODE_DEMO = "demo";
    public static final String MODE_SERVICE = "service";

    private BackendFactory() {
    }

    /**
     * 启动参数指定的默认数据源。优先级：
     * {@code -Dparking.backend=...} &gt; 环境变量 {@code PARKING_BACKEND} &gt; 演示数据。
     */
    public static String defaultMode() {
        String mode = System.getProperty("parking.backend");
        if (mode == null || mode.isBlank()) {
            mode = System.getenv("PARKING_BACKEND");
        }
        return MODE_SERVICE.equalsIgnoreCase(mode) ? MODE_SERVICE : MODE_DEMO;
    }

    /**
     * @param mode {@link #MODE_DEMO} 或 {@link #MODE_SERVICE}
     * @throws BackendException 真实业务层初始化失败
     */
    public static ParkingBackend create(String mode) throws BackendException {
        if (MODE_SERVICE.equalsIgnoreCase(mode)) {
            return new ServiceBackend();
        }
        return new DemoBackend();
    }
}
