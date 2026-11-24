package com.smartparking.ui.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 界面层格式化工具。 */
public final class Fmt {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Fmt() {
    }

    public static String time(LocalDateTime value) {
        return value == null ? "—" : TIME.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    public static String full(LocalDateTime value) {
        return value == null ? "—" : FULL.format(value);
    }

    public static String clock(LocalDateTime value) {
        return value == null ? "—" : CLOCK.format(value);
    }

    /** 把分钟数转成“2 小时 6 分”。 */
    public static String duration(long minutes) {
        if (minutes <= 0) {
            return "不足 1 分钟";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours == 0) {
            return rest + " 分钟";
        }
        return rest == 0 ? hours + " 小时" : hours + " 小时 " + rest + " 分";
    }

    public static String money(Integer amount) {
        return amount == null ? "—" : "¥ " + amount;
    }

    public static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
