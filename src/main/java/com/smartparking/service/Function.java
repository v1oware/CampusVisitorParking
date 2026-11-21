package com.smartparking.service;


import com.smartparking.model.Car;

public class Function {

    /*
    单位时间的停车费用，默认为0。
     */
    private int UnitCost = 0;

    /**
    访客预约功能
    预约时间为整点时间
    返回值判断预约是否成功
    一个访客账号在某天只能预约一次
     */
    static boolean Appointment(){
        return false;
    }

    /**
    安保人员登记信息
     */
    static void reservedInfo(){}

    /**

     */
    public void setUnitCost(int unitCost){
        this.UnitCost = unitCost;
    }

    /**
     *停车费用
     */
    public int getStoppingCost(Car car){
        return car.getStoppingHour()*this.UnitCost;
    }

}
