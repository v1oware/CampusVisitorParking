package com.smartparking.datastructure;

public class Stack<E> {

    private StackNode<E> top;
    private int maxsize;
    private int size = 0;

    public Stack(int maxsize){
        top = null;
        this.maxsize = maxsize;
    }

    public  boolean isEmpty(){
        return top == null;
    }

    public boolean push(E e){
        if (size < maxsize) {
            var node  = new StackNode<>(e);
            node.next = top;
            top = node;
            size++;
            return true;
        }else return false;

    }

    public E pop(){
        if(isEmpty()){
            return null;
        }

        var node = top;
        top = node.next;
        size--;
        return  node.data;
    }

    public void clear(){
        top = null;
        size = 0;
    }

    public int getSize() {
        return size;
    }
}

