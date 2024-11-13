package dev.fileformat.drako;


import java.lang.reflect.Array;
import java.util.*;

@Internal
class ListUtils {
    /**
     * Created by lexchou on 9/1/2017.
     */
    public static class GeneralArrayListIterator<T> implements ListIterator<T> {
        private List<T> list;
        private int start;
        private int end;
        private int index;
        public GeneralArrayListIterator(List<T> list, int start)
        {
            this.list = list;
            this.start = start;
            this.end = list.size();
        }
        @Override
        public boolean hasNext() {
            return index < end;
        }

        @Override
        public T next() {
            return list.get(index++);
        }

        @Override
        public boolean hasPrevious() {
            return index > start;
        }

        @Override
        public T previous() {
            return list.get(--index);
        }

        @Override
        public int nextIndex() {
            return index + 1;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Created by lexchou on 8/31/2017.
     */
    public static class IntList extends AbstractCollection<Integer> implements List<Integer> {
        private int[] data;
        private int fromIndex;
        private int size;

        public IntList(int[] data)
        {
            this(data, 0, data.length);
        }
        private IntList(int[] data, int fromIndex, int size)
        {
            this.data = data;
            this.fromIndex = fromIndex;
            this.size = size;
        }
        @Override
        public Integer get(int index) {
            return data[index];
        }

        @Override
        public Integer set(int index, Integer element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, Integer element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            if(!(o instanceof Integer))
                return -1;
            int val = (Integer)o;
            for(int i = 0; i < data.length; i ++)
            {
                if(val == data[i])
                    return i;
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            if(!(o instanceof Integer))
                return -1;
            int val = (Integer)o;
            for(int i = data.length - 1; i >= 0; i--)
            {
                if(val == data[i])
                    return i;
            }
            return -1;
        }

        @Override
        public ListIterator<Integer> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<Integer> listIterator(int index) {
            return new GeneralArrayListIterator(this, index);
        }

        @Override
        public List<Integer> subList(int fromIndex, int toIndex) {
            return new IntList(data, this.fromIndex + fromIndex, this.fromIndex + toIndex);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) != -1;
        }

        @Override
        public Iterator<Integer> iterator() {
            return listIterator();
        }

        @Override
        public Object[] toArray() {
            return Convert.box(data);
        }

        public int[] toNativeArray() {
            throw new NotImplementedException();
        }

        public Integer[] toArray(Integer[] a) {
            Integer[] ret = a;
            if(a.length != size())
                ret = new Integer[size()];
            for(int i = 0; i < ret.length; i++)
                ret[i] = get(i);
            return ret;
        }

        @Override
        public boolean add(Integer integer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, Collection<? extends Integer> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Created by lexchou on 9/1/2017.
     * This class encapsulates an array into List interface
     */
    public static class NativeArrayList<T> extends AbstractList<T> implements List<T> {
        private Object array;
        private int size;

        public NativeArrayList(Object data)
        {
            this.array = data;
            size = Array.getLength(data);
        }

        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            return false;
        }

        @Override
        public T get(int index) {
            return (T)Array.get(array, index);
        }

        @Override
        public T set(int index, T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, T element) {
            throw new UnsupportedOperationException();

        }

        @Override
        public T remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            for(int i = 0; i < size; i++)
            {
                if(get(i) == o)
                    return i;
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            for(int i = size - 1; i >= 0; i--)
            {
                if(get(i) == o)
                    return i;
            }
            return -1;
        }

        @Override
        public ListIterator<T> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            return new GeneralArrayListIterator<T>(this, index);
        }

        @Override
        public int size() {
            return size;
        }
    }
}
