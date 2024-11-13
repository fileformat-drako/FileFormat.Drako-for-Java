package dev.fileformat.drako;


import java.util.ListIterator;

class LinkedListIterator<E> implements ListIterator<E> {
    LinkedListNode<E> p;

    public LinkedListIterator(LinkedListNode<E> head)
    {
        this.p = head;
    }
    @Override
    public boolean hasNext() {
        return p != null;
    }

    @Override
    public E next() {
        E ret = p.getValue();
        p = p.getNext();
        return ret;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public E previous() {
        return null;
    }

    @Override
    public int nextIndex() {
        return 0;
    }

    @Override
    public int previousIndex() {
        return 0;
    }

    @Override
    public void remove() {

    }

    @Override
    public void set(E e) {

    }

    @Override
    public void add(E e) {

    }
}

