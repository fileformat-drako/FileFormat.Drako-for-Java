package dev.fileformat.drako;


import java.util.AbstractSequentialList;
import java.util.ListIterator;

/**
 * Created by lexchou on 6/27/2017.
 */
@Internal
class LinkedList<E> extends AbstractSequentialList<E> {

    private LinkedListNode<E> head;
    private LinkedListNode<E> tail;
    private int size;


    /**
     * Remove first node
     * @return the value of the first node
     */
    public E removeFirst() {
        if (head == null)
            return null;
        E ret = head.getValue();
        remove(head);
        return ret;
    }
    /**
     * Remove last node
     * @return the value of the last node
     */
    public E removeLast() {
        if(tail == null)
            return null;
        E ret = head.getValue();
        remove(tail);
        return ret;
    }

    /**
     * Gets the first node of the linked list.
     * @return the first node of the list
     */
    public LinkedListNode<E> getFirst() {
        return head;
    }

    /**
     * Gets the last node of the linked list.
     * @return the last node of the list
     */
    public LinkedListNode<E> getLast() {
        return tail;
    }

    @Override
    public E remove(int index) {
        rangeCheck(index);
        LinkedListNode<E> p = nodeAt(index);
        E ret = p.getValue();
        remove(p);


        return ret;
    }

    @Override
    public void add(int index, E value) {
        if (index == size) {
            addLast(value);
            return;
        }
        rangeCheck(index);
        if (index == 0)//add as first node
        {
            addFirst(value);
        } else {
            LinkedListNode<E> p = nodeAt(index);
            addBefore(p, value);
        }
    }

    @Override
    public boolean add(E value) {
        addLast(value);
        return true;
    }


    public LinkedListNode<E> addFirst(E value)
    {
        LinkedListNode<E> node = new LinkedListNode<E>(this, value);
        link(node, head);
        if(this.tail == null)
            this.tail = node;
        size++;
        return node;
    }

    /**
     * Add value before the specified node
     * @param node which node to add before
     * @param value value of the node
     * @return the new node with the given value
     */
    public LinkedListNode<E> addBefore(LinkedListNode<E> node, E value) {

        LinkedListNode<E> ret = new LinkedListNode<E>(this, value);
        addBefore(node, ret);
        return ret;

    }
    public void addBefore(LinkedListNode<E> node, LinkedListNode<E> newNode) {
        if(node.list != this)
            throw new IllegalStateException();
        newNode.list = this;
        LinkedListNode<E> prev = node.prev;
        size++;
        link(prev, newNode);
        link(newNode, node);
    }
    private void link(LinkedListNode<E> before, LinkedListNode<E> after) {
        if(after == head)
            head = before;
        if(before == tail)
            tail = after;
        if(before != null)
            before.setNext(after);
        if(after != null)
            after.prev = before;

    }


    /**
     * Add value as the last node of the linked list
     * @param value Value to be added to the list
     * @return the node with the given value
     */
    public LinkedListNode<E> addLast(E value) {
        LinkedListNode<E> node = new LinkedListNode<E>(this, value);
        addLast(node);
        return node;
    }
    public LinkedListNode<E> addLast(LinkedListNode<E> node) {
        if(tail != null) {
            tail.setNext(node);
            node.prev = tail;
        }
        node.list = this;
        tail = node;
        if(head == null)
            head = node;
        size++;
        return node;
    }
    public void remove(LinkedListNode<E> node) {
        if(node.list != this)
            throw new IllegalStateException("Cannot remove node from other list.");

        link(node.prev, node.getNext());

        if(node == head)
            head = node.getNext();
        if(node == tail)
            tail = node.prev;
        node.prev = null;
        node.list = null;
        node.setNext(null);
        size--;
    }

    public int size() {
        return size;
    }

    @Override
    public void clear() {
        head = tail = null;
        size = 0;
    }

    @Override
    public E get(int index) {
        LinkedListNode<E> p = nodeAt(index);
        return p.getValue();
    }

    @Override
    public E set(int index, E value) {
        LinkedListNode<E> p = nodeAt(index);
        E old = p.getValue();
        p.setValue(value);
        return old;
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
    }

    private LinkedListNode<E> nodeAt(int index) {
        LinkedListNode<E> p = head;
        int i = 0;
        rangeCheck(index);
        for (; i < index; i++, p = p.getNext()) ;
        return p;
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new LinkedListIterator(head);
    }
}
