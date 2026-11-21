package com.smartparking.model;

import com.smartparking.datastructure.Stack;


/**
停车场内有20个停车位，单车道，类似断头路

 */
public class ParkingLot {

    private class Camera{

        String OCRLicensePlate(String LP){
            return LP;
        }

    }


    Camera cFront;
    Camera cBack;
    private int gateSituation = 0;
    private final int UP = 1;
    private final int DOWN = 0;
    private final int MAXSIZE = 20;
    Stack<Car> parkingLot = new Stack<>(MAXSIZE);

    public void setGateSituation(int gateSituation) {
        this.gateSituation = gateSituation;
    }

    public int getGateSituation() {
        return gateSituation;
    }

    /**
    抬杆状态设置为1
     */
    static void liftUp(){

    }

    /**
     落杆状态设置为0
     */
    static void liftDown(){

    }

    /**
     *记录车辆进入时间，车牌，产生日志
     *车牌从cFront获取
     *抬杆
     *当cBack获取到刚才的车牌，落杆
     */
    public void enter(){

    }

    /**
     * 记录车辆离开时间，车牌，产生日志
     * 车牌从cBack获取
     * 计算时间收费
     * 缴费成功
     * 抬杆
     * 当cFront识别到刚才的车牌
     * 落杆
     */
    public void leave(){

    }


    /**
    返回当前空位
     */
    public int getStatus(){
        return MAXSIZE - parkingLot.getSize();
    }

}
