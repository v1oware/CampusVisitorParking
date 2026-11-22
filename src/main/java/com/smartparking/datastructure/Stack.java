package com.smartparking.datastructure;

public class Stack<E> {

    private StackNode<E> top;
    private int maxsize;
    private int size = 0;

    public Stack(int maxsize){
        if (!(maxsize > 0)) {

            top = null;
            this.maxsize = 0;
        }else{
            this.maxsize = maxsize;
        }
    }

    public  boolean isEmpty(){
        return top == null;
    }

    public StackNode<E>  getTop(){
        return top;
    }

    public boolean push(E e){
        if (size < maxsize) {
            var node  = new StackNode<>(e);
            node.setNext(top);
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
        top = node.getNext();
        size--;
        return node.getData();
    }

    public void clear(){
        top = null;
        size = 0;
    }

    public int getSize() {
        return size;
    }

    public boolean isFull(){
        if(maxsize == 0){
            return false;
        }
        return size == maxsize;
    }

}

