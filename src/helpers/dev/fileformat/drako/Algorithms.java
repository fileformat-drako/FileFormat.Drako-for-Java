package dev.fileformat.drako;



import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by lexchou on 6/26/2017.
 */
@Internal
final class Algorithms {


    /**
     * Find the index in specified element, or -1 will be returned.
     * @param array array to find
     * @param find which value to find
     * @return the index of the element in the array
     */
    public static <T> int indexOf(T[] array, T find) {
        for(int i = 0; i < array.length; i++) {
            if(array[i] == find)
                return i;
        }
        return -1;
    }
    public static <T> int indexOf(char[] array, char find) {
        for(int i = 0; i < array.length; i++) {
            if(array[i] == find)
                return i;
        }
        return -1;
    }

    public static int binarySearch(@Pure int[] array, int val, Comparator<Integer> comparator) {
        rangeCheck(array, array.length, 0, 0);
        return binarySearch(array, 0, array.length, val, comparator);
    }
    public static int binarySearch(int[] array, long val)
    {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = array[mid];

            if (midVal < val)//midVal < key)
                low = mid + 1;
            else if (midVal > val)//midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return ~low;//  // key not found.
    }
    public static int binarySearch(long[] array, long val)
    {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = array[mid];

            if (midVal < val)//midVal < key)
                low = mid + 1;
            else if (midVal > val)//midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return ~low;//  // key not found.
    }
    public static int binarySearch(int[] array, int start, int len, int val, Comparator<Integer> comparator)
    {
        rangeCheck(array, array.length, start, len);

        int low = start;
        int high = start + len - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = array[mid];

            if (comparator.compare(midVal, val) < 0)//midVal < key)
                low = mid + 1;
            else if (comparator.compare(midVal, val) > 0)//midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return ~low;//  // key not found.
    }

    private static void rangeCheck(Object array, int arrayLength, int start, int len)
    {
        if(array == null)
            throw new IllegalArgumentException("array cannot be null");
        int end = start + len;
        if(start < 0 || start >= arrayLength || end < 0 || end > arrayLength)
            throw new IndexOutOfBoundsException();
    }
    public static <T> void sort(T[] array, Comparator<T> comparator)
    {
        //rangeCheck(array, array.length, 0, 0);
        //sort(array, 0, array.length, comparator);
        Arrays.sort(array, 0, array.length, comparator);
    }
    public static <T> void sort(T[] array, int start, int len, Comparator<T> comparator)
    {
        //rangeCheck(array, array.length, start, len);
        //quickSort(array, start, start + len - 1, comparator);
        Arrays.sort(array, start, start + len, comparator);
    }
    public static void sort(int[] array, Comparator<Integer> comparator)
    {
        if(array.length > 0) {
            rangeCheck(array, array.length, 0, array.length);
            sort(array, 0, array.length, comparator);
        }
    }
    public static void sort(int[] array, int start, int len, Comparator<Integer> comparator)
    {
        if(array.length > 0) {
            rangeCheck(array, array.length, start, len);
            quickSort(array, start, start + len - 1, comparator);
        }
    }
    private static class Pair
    {
        public long key;
        public int value;
        public Pair(long key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    public static void sort(int[] keys, int[] values)
    {
        Pair[] pairs = new Pair[keys.length];
        for(int i = 0; i < keys.length; i++)
        {
            pairs[i] = new Pair(keys[i], values[i]);
        }
        Arrays.sort(pairs, new Comparator<Pair>() {
            @Override
            public int compare(Pair o1, Pair o2) {
                return Long.compare(o1.key, o2.key);
            }
        });
        for(int i = 0; i < pairs.length; i++)
        {
            Pair p = pairs[i];
            keys[i] = (int)p.key;
            values[i] = p.value;
        }
    }
    public static void sort(long[] keys, int[] values)
    {
        Pair[] pairs = new Pair[keys.length];
        for(int i = 0; i < keys.length; i++)
        {
            pairs[i] = new Pair(keys[i], values[i]);
        }
        Arrays.sort(pairs, new Comparator<Pair>() {
            @Override
            public int compare(Pair o1, Pair o2) {
                return Long.compare(o1.key, o2.key);
            }
        });
        for(int i = 0; i < pairs.length; i++)
        {
            Pair p = pairs[i];
            keys[i] = p.key;
            values[i] = p.value;
        }
    }
    private static void quickSort(int[] array, int lowerIndex, int higherIndex, Comparator<Integer> comparator) {

        int i = lowerIndex;
        int j = higherIndex;
        // calculate pivot number, I am taking pivot as middle index number
        Integer pivot = array[lowerIndex + (higherIndex - lowerIndex) / 2];
        // Divide into two arrays
        while (i <= j) {
            /**
             * In each iteration, we will identify a number from left side which
             * is greater then the pivot value, and also we will identify a number
             * from right side which is less then the pivot value. Once the search
             * is done, then we exchange both numbers.
             */
            while (comparator.compare(array[i], pivot) < 0) {
                i++;
            }
            while (comparator.compare(array[j], pivot) > 0) {
                j--;
            }
            if (i <= j) {
                //exchange elements
                int t = array[i];
                array[i] = array[j];
                array[j] = t;
                //move index to next position on both sides
                i++;
                j--;
            }
        }
        // call quickSort() method recursively
        if (lowerIndex < j)
            quickSort(array, lowerIndex, j, comparator);
        if (i < higherIndex)
            quickSort(array, i, higherIndex, comparator);
    }

    public static void reverse(Object[] array, int start, int len) {
        rangeCheck(array, array.length, start, len);
        int end = start + len / 2;
        for(int i = start, j = start + len - 1; i < end; i++, j--)
        {
            Object t = array[i];
            array[i] = array[j];
            array[j] = t;
        }

    }
    public static void reverse(int[] array, int start, int len) {
        rangeCheck(array, array.length, start, len);
        int end = start + len / 2;
        for(int i = start, j = start + len - 1; i < end; i++, j--)
        {
            int t = array[i];
            array[i] = array[j];
            array[j] = t;
        }

    }
}
