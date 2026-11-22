package com.smartparking.service;

import com.smartparking.model.ParkingLot;

public class ParkingManager {

    /**
     *先判断camera传输的车牌是否已经被拉黑
     *再判断栈是否已满
     *若车未被拉黑且栈未满
     *记录车辆进入时间，车牌，产生日志
     *车牌从cFront获取
     *抬杆
     *当cBack获取到刚才的车牌，落杆
     */
    public boolean enter(ParkingLot parkingLot, String licensePlate) {
        return false;
    }

    /**
     * 记录车辆离开时间，车牌，产生日志
     * 车牌从cBack获取
     * 计算时间收费
     * 如果缴费成功
     * 抬杆
     * 当cFront识别到刚才的车牌
     * 落杆
     */
    public void leave(ParkingLot parkingLot, String licensePlate) {

    }

}
