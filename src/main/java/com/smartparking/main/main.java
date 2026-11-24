package com.smartparking.main;

import com.smartparking.ui.ParkingApp;
import javafx.application.Application;

/**
 * 程序入口。
 *
 * <p>运行方式：{@code mvn javafx:run}；也可以在 IDE 中直接运行本类。
 * 切换数据源：{@code mvn javafx:run -Dparking.backend=service}</p>
 */
public class main {

    public static void main(String[] args) {
        Application.launch(ParkingApp.class, args);
    }
}
