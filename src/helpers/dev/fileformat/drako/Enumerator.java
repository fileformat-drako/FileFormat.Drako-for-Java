package dev.fileformat.drako;



/**
 * Generic enumerator for collection of type T
 * @param <T> collection's element type
 */
public interface Enumerator<T> {
    boolean moveNext();
    T getCurrent();
}
