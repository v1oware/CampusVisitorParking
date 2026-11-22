package com.smartparking.model;

import com.smartparking.datastructure.Queue;
import com.smartparking.datastructure.QueueNode;

public class WaitingQueue {

    private Queue<Car> queue = new Queue<>();

    public Queue<Car> getQueue() {
        return queue;
    }

    /**
     获取排队总数
     */
    public int getStatus(){
        return  queue.getSize();
    }

    /**
    获取排队位数
     */
    public int getWaitingQueueStatus(QueueNode<Car> node){
        if(node==null){
            return -1;
        }

        return node.getPosition();
    }

}
