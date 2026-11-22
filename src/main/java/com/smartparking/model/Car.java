package com.smartparking.model;

import com.smartparking.datastructure.Queue;
import com.smartparking.datastructure.QueueNode;

import java.util.Date;
import java.util.Objects;

public class Car{
    private String licensePlate;
    private Date entryTime = null;
    private Date exitTime = null;

    /**
     *调用构造方法前验证车牌是否已有对应car类
     * 并且过滤非期望格式
     */
    public Car(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public Date getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(Date entryTime) {
        this.entryTime = entryTime;
    }

    public Date getExitTime() {
        return exitTime;
    }

    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return Objects.equals(licensePlate, car.licensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(licensePlate);
    }

    /**
     *获取车辆停靠时间，以便统计和收费
     *按停靠小时算，不满一小时的部分记为一小时
     */
    public long getStoppingHour(){
        long ONE_HOUR_MILLIS = 3600_000L;
        if (entryTime == null || exitTime == null) {
            return -1;
        }

        long diffMillis = Math.abs(exitTime.getTime() - entryTime.getTime());

        // 相同时刻，返回 0
        if (diffMillis == 0) {
            return 0L;
        }

        // 向上取整：(diff + 3600000 - 1) / 3600000
        return (diffMillis + ONE_HOUR_MILLIS - 1) / ONE_HOUR_MILLIS;
    }
}
