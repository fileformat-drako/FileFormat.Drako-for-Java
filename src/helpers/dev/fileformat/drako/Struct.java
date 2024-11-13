package dev.fileformat.drako;


import java.io.Serializable;

/**
 * Created by lexchou on 11/13/2017.
 *
 */
public interface Struct<T> extends Cloneable, Serializable {

    /**
     * Clone current instance
     * @return cloned instance
     */
    T clone();
    /**
     * Copy internal state from argument t
     * @param t source instance to copy
     */
    void copyFrom(T t);

    /**
     * Try to copy the input value if it's Struct
     * @param <T> Struct type
     * @param value input value to clone
     * @return null if input is null or cloned instance
     */
    public static <T extends Struct<T>> T byVal(T value) {
        if(value == null)
            return null;
       return value.clone();
    }
}
