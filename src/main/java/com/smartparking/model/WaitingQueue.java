package com.smartparking.model;

import com.smartparking.datastructure.Queue;
import com.smartparking.datastructure.QueueNode;

public class WaitingQueue {

    Queue<QueueNode<Car>> queue = new Queue<>();

    /*
    获取排队总数
     */
    public int getStatus(){
        return  queue.getSize();
    }

    /*
    获取排队位数
     */
    int getWaitingQueueStatus(QueueNode<Car> node){
        return node.getPosition();
    }

}
