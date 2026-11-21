package com.smartparking.model;

import java.util.Date;

public class Visitor extends User {
    Car car;
    /*
    在预约时间前一个小时预留车位，到达预留时间或车辆提前到达则取消预留
     */
    Date AppointmentDate;


}
