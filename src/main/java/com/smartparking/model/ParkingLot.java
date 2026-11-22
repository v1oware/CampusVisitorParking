package com.smartparking.model;

import com.smartparking.datastructure.Stack;


/**
停车场内有20个停车位，单车道，类似断头路

 */
public class ParkingLot {
    
    private int gateSituation = 0;
    private final int UP = 1;
    private final int DOWN = 0;
    private final int MAXSIZE = 20;
    public Stack<Car> parkingLot = new Stack<>(MAXSIZE);


    public ParkingLot() {

    }

    public int getMAXSIZE() {
        return MAXSIZE;
    }

    public void setGateSituation(int gateSituation) {
        this.gateSituation = gateSituation;
    }

    public int getGateSituation() {
        return gateSituation;
    }

    /**
    抬杆状态设置为1
     */
    public void liftUp(){

    }

    /**
     落杆状态设置为0
     */
    public void liftDown(){

    }

    /**
    返回当前空位
     */
    public int getStatus(){
        return MAXSIZE - parkingLot.getSize();
    }

}
