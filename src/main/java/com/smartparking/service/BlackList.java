package com.smartparking.service;

import com.smartparking.model.*;
import java.util.ArrayList;


public class BlackList {

    private ArrayList<Car> carList;

    public ArrayList<Car> getCarList() {
        return carList;
    }

    public void setCarList(ArrayList<Car> carList) {
        this.carList = carList;
    }

    /**
    根据车牌进行拉黑
     */
    public void add(String licensePlate){

    }

    /**
     *判断该车牌是否被拉黑
     */
    public boolean isBanned(String licensePlate){
        return false;
    }

    /**
     *for循环倒序匹配车牌号所对应car实例，删除
     */
    public void remove(String licensePlate){
    }
}
