package dev.fileformat.drako;


public interface Enumerable<T> {
    Enumerator<T> enumerator();
}
