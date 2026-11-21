package com.smartparking.datastructure;

public class QueueNode<E>{
    private E data;
    private QueueNode<E> next;
    private int position;
    public QueueNode(E data){
            this.data = data;
            this.next = null;
            this.position = -1;
    }

    public E  getData(){
        return this.data;
    }

    public void setPosition(int pos){
        this.position = pos;
    }

    public int getPosition(){
        return this.position;
    }

    public QueueNode<E> getNext(){
        return this.next;
    }

}
