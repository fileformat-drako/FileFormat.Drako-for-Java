package dev.fileformat.drako;


import java.lang.reflect.Array;

/**
 * Created by lexchou on 8/22/2017.
 */
@Internal
class MetaClass<T> {

    public T newInstance() {
        throw new NotImplementedException();
    }

    public Object newArray(int size) {
        throw new NotImplementedException();
    }

    public Class<T> classOf() {throw new NotImplementedException();}
    public T defaultValue() {
        return null;
    }

    /**
     * Cast generic object array into typed array
     * @param array
     * @return
     */
    public T[] unshadow(Object[] array) {
        if(array.length == 0)
            return (T[])array;
        T[] ret = (T[])Array.newInstance(array[0].getClass(), array.length);
        for(int i = 0; i < array.length; i++)
        {
            ret[i] = (T)array[i];
        }
        return ret;
    }
    public boolean isAssignableFrom(Class<?> clazz) {
        Class<T> self = classOf();
        return self.isAssignableFrom(clazz);
    }

    /**
     * Check if the clazz is the array of current MetaClass
     * @param clazz
     * @param rank
     * @return
     */
    public boolean isArray(Class<?> clazz, int rank) {
        Class<?> current = classOf();
        Class<?> t = clazz;
        if(rank <= 0)
            return false;
        for(int i = 0; i < rank; i++)
        {
            if(!t.isArray())
                return false;
            t = t.getComponentType();
        }
        return t == current;
    }
}
