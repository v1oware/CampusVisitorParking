package com.smartparking.model;

import java.util.Date;

public class Visitor extends User {
    private Car car;
    /**
    在预约时间前一个小时预留车位，到达预留时间或车辆提前到达则取消预留
     */
    private Date AppointmentDate;

    public Visitor(String account, String password, String name, String phoneNumber) {
        super(account, password, name, phoneNumber);
    }

    public Visitor(String account, String password, String name, String phoneNumber, Car car) {
        super(account, password, name, phoneNumber);
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Date getAppointmentDate() {
        return AppointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate) {
        AppointmentDate = appointmentDate;
    }
}
