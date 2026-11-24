package com.smartparking.ui.util;

/** 车牌输入的前置校验，仅用于界面提示；真正的业务校验由业务层负责。 */
public final class Plates {

    private Plates() {
    }

    public static String normalize(String plate) {
        if (plate == null) {
            return null;
        }
        String value = plate.replace(" ", "").replace("·", "").trim().toUpperCase();
        return value.isEmpty() ? null : value;
    }

    public static boolean looksValid(String plate) {
        String value = normalize(plate);
        return value != null && value.matches("[\\u4e00-\\u9fa5A-Z0-9]{5,9}");
    }
}
