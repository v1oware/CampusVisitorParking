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

    public int getMaxsize() {
        return maxsize;
    }


    /**
     *给已构造的stack扩容
     *如果给定数字比目前已有数据小，不能更改
     * 如果数字大于等于元素数量，可以更改
     */
    public boolean setMaxsize(int maxsize) {
        if(maxsize < this.size){
            return false;
        }
        this.maxsize = maxsize;
        return true;
    }

    public  boolean isEmpty(){
        return top == null;
    }

    public StackNode<E>  getTop(){
        return top;
    }

    public boolean push(E e){
        if(e == null){
            return false;
        }
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
            return true;
        }
        return size == maxsize;
    }

}

