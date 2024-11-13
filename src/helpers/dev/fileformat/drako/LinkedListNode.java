package dev.fileformat.drako;


/**
 * Created by lexchou on 6/27/2017.
 */
@Internal
final class LinkedListNode<E> {
    private LinkedListNode<E> next;
    LinkedListNode<E> prev;
    private Object value;
    LinkedList<E> list;
    public LinkedListNode(LinkedList<E> list, Object value)
    {
        this.list = list;
        this.value = value;
    }

    public LinkedList<E> getList() {
        return list;
    }

    public LinkedListNode<E> getNext() {
        return next;
    }
    public LinkedListNode<E> getPrevious() {
        return prev;
    }
    void setNext(LinkedListNode<E> next) {
        this.next = next;
    }

    public E getValue() {
        return (E)value;
    }
    public void setValue(E value)
    {
        this.value = value;
    }
}
