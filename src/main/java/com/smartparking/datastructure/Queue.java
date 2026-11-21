package com.smartparking.datastructure;

public class Queue<E> {

    private QueueNode<E> top;
    private QueueNode<E> tail;
    private int size;

    public Queue(){
        top = null;
        tail = null;
        size = 0;
    }

    public boolean isEmpty(){
        return top == null;
    }

    public int getSize(){
        return size;
    }

    public QueueNode<E> getTop() {
        return top;
    }

    public void EnterQueue(E e){
        var temp = new QueueNode<>(e);
        tail.setNext(temp);
        tail = temp;
        tail.setPosition(getSize() + 1);
        if (isEmpty()) {
            top = tail;
        }
        size++;
    }

    public E DeleteQueue(){
        if (isEmpty()){
            return null;
        }
        var temp = top.getData();
        top = top.getNext();
        size--;

        //随出队给队内位置更新，便于队内节点查询位置
        if (!isEmpty()) {
            QueueNode<E> node = top;
            int i = 0;
            while(i<getSize()){
                node.setPosition(node.getPosition()-1);
                node = node.getNext();
                i++;
            }
        }

        return temp;
    }

}

