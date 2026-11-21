package com.smartparking.model;

import com.smartparking.datastructure.Queue;
import com.smartparking.datastructure.QueueNode;

import java.util.Objects;

public class Car{
    private String licensePlate;

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Car(String licensePlate) {
        this.licensePlate = licensePlate;
    }

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
