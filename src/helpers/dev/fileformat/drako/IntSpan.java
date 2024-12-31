package dev.fileformat.drako;



import java.util.Iterator;

@Internal
abstract class IntSpan extends Span {

    private static final class ArraySpan extends IntSpan {
        private final int[] array;
        public ArraySpan(int[] array, int offset, int length) {
            super(offset, length);
            this.array = array;
        }

        @Override
        public int get(int idx)
        {
            return array[idx + offset];
        }
        @Override
        public void put(int idx, int value)
        {
            rangeCheck(idx);
            array[idx + offset] = value;
        }

        @Override
        public IntSpan slice(int offset, int size) {
            return new ArraySpan(array, offset + this.offset, size);
        }

    }
    private static final class BytesSpan extends IntSpan {
        private final byte[] array;
        public BytesSpan(byte[] array, int offset, int length) {
            super(offset, length);
            this.array = array;
        }

        @Override
        public int get(int idx)
        {
            rangeCheck(idx);

            int ptr = (idx + offset) * 4;
            return getIntL(array, ptr);
        }
        @Override
        public void put(int idx, int value)
        {
            rangeCheck(idx);
            int ptr = (idx + offset) * 4;
            putIntL(array, ptr, value);
        }

        @Override
        public IntSpan slice(int offset, int size) {
            return new BytesSpan(array, offset + this.offset, size);
        }

    }

    public static IntSpan wrap(int[] array) {
        return new ArraySpan(array, 0, array.length);
    }
    public static IntSpan wrap(int[] array, int offset, int length) {
        return new ArraySpan(array, offset, length);
    }
    public static IntSpan wrap(int[] array, int offset) {
        return new ArraySpan(array, offset, array.length - offset);
    }
    public static IntSpan wrap(byte[] array) {
        return new BytesSpan(array, 0, array.length / 4);
    }
    public static IntSpan wrap(byte[] array, int offset, int length) {
        return new BytesSpan(array, offset / 4, length / 4);
    }

    protected IntSpan(int offset, int length) {
        super(offset, length);
    }

    public int compareTo(IntSpan span) {
        int num = Math.min(size(), span.size());
        for(int i = 0; i < num; i++) {
            int n = Integer.compare(get(i), span.get(i));
            if(n != 0)
                return n;
        }
        return Integer.compare(size(), span.size());
    }
    public boolean equals(IntSpan span) {
        if(size() != span.size())
            return false;
        for(int i = 0; i < size(); i++) {
            if(get(i) != span.get(i))
                return false;
        }
        return true;
    }
    public void copyTo(IntSpan span) {
        for(int i = 0; i < size(); i++) {
            span.put(i, get(i));
        }
    }
    public void fill(int v)
    {
        for(int i = 0; i < size(); i++) {
            put(i, v);
        }
    }
    public int[] toArray() {
        int[] ret = new int[length];

        for(int i = 0; i < length; i++) {
            ret[i] = get(i);
        }
        return ret;
    }
    public abstract int get(int idx);
    public abstract void put(int idx, int value);

    public abstract IntSpan slice(int offset, int size);
    public IntSpan slice(int offset)
    {
        return slice(offset, this.length - offset);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 0; i < Math.min(10, length); i++) {
            if(i > 0)
                sb.append(",");
            sb.append(get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
