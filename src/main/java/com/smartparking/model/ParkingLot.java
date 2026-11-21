package com.smartparking.model;

import com.smartparking.datastructure.Stack;
import com.smartparking.datastructure.StackNode;


/*
停车场内有20个停车位，单车道，类似断头路

 */
public class ParkingLot {
    private final int MAXSIZE = 20;
    Stack<StackNode<Car>> parkingLot = new Stack<>(MAXSIZE);

    /*
    停车场抬杆
    同时保存车牌，进入时间，登记信息name,phoneNumber
    如果有预约，取消预约预留车位
     */
    static void liftUp(){

    }

    /*
    停车场落杆
     */
    static void liftDown(){

    }

    /*
    返回当前空位
     */
    int getStatus(){
        return MAXSIZE - parkingLot.getSize();
    }

}
