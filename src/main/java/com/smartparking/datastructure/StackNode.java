package com.smartparking.datastructure;

public class StackNode<E>{
    private E data;
    private StackNode<E> next;

    public StackNode(E data) {
        this.next = null;
        this.data = data;
    }

    public StackNode<E> getNext() {
        return next;
    }

    public void setNext(StackNode<E> next) {
        this.next = next;
    }

    public E getData() {
        return data;
    }

    public void setData(E data) {
        this.data = data;
    }
}
