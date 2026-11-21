package com.smartparking.datastructure;

public class StackNode<E>{
    E data;
    StackNode<E> next;

    public StackNode(E data) {
        this.next = null;
        this.data = data;
    }
}
