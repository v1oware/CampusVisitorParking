package com.smartparking.model;

import java.util.Date;
import java.util.Objects;
import com.smartparking.datastructure.*;

abstract class User {
    String account;
    String password;
    String name;
    String phoneNumber;
}

class Admin extends User {

}

class Staff extends User {

}

class Visitor extends User {
    Car car;
    /*
    在预约时间前一个小时预留车位，到达预留时间或车辆提前到达则取消预留
     */
    Date AppointmentDate;


}
class Car{
    private String licensePlate;

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    Car(String licensePlate) {}

    public int getWaitingPosition(Queue<Car> queue ){
        QueueNode<Car> line = queue.getTop();
        while (line != null) {
            if(Objects.equals(this,line.getData())){
                return line.getPosition();
            }
            line =  line.getNext();
        }
        return -1;
    }
}
