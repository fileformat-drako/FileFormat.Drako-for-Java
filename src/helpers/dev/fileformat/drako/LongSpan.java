package dev.fileformat.drako;



@Internal
abstract class LongSpan extends Span {

    private static final class ArraySpan extends LongSpan {
        private final long[] array;
        public ArraySpan(long[] array, int offset, int length) {
            super(offset, length);
            this.array = array;
        }

        @Override
        public long get(int idx)
        {
            return array[idx + offset];
        }
        @Override
        public void put(int idx, long value)
        {
            rangeCheck(idx);
            array[idx + offset] = value;
        }

        @Override
        public LongSpan slice(int offset, int size) {
            return new ArraySpan(array, offset + this.offset, size);
        }

    }
    private static final class BytesSpan extends LongSpan {
        private final byte[] array;
        public BytesSpan(byte[] array, int offset, int length) {
            super(offset, length);
            this.array = array;
        }

        @Override
        public long get(int idx)
        {
            rangeCheck(idx);
            int ptr = (idx + offset) * 8;
            return getLongL(array, ptr);
        }
        @Override
        public void put(int idx, long value)
        {
            rangeCheck(idx);
            int ptr = (idx + offset) * 8;
            putLongL(array, ptr, value);
        }

        @Override
        public LongSpan slice(int offset, int size) {
            return new BytesSpan(array, offset + this.offset, size);
        }

    }

    public static LongSpan wrap(long[] array) {
        return new ArraySpan(array, 0, array.length);
    }
    public static LongSpan wrap(long[] array, int offset, int length) {
        return new ArraySpan(array, offset, array.length);
    }
    public static LongSpan wrap(byte[] array) {
        return new BytesSpan(array, 0, array.length / 8);
    }
    public static LongSpan wrap(byte[] array, int offset, int length) {
        return new BytesSpan(array, offset / 8, length / 8);
    }

    protected LongSpan(int offset, int length) {
        super(offset, length);
    }

    public abstract long get(int idx);
    public abstract void put(int idx, long value);

    public abstract LongSpan slice(int offset, int size);
    public LongSpan slice(int offset)
    {
        return slice(offset, this.length - offset);
    }


    public int compareTo(LongSpan span) {
        int num = Math.min(size(), span.size());
        for(int i = 0; i < num; i++) {
            int n = Float.compare(get(i), span.get(i));
            if(n != 0)
                return n;
        }
        return Integer.compare(size(), span.size());
    }
    public boolean equals(LongSpan span) {
       if(size() != span.size())
           return false;
       for(int i = 0; i < size(); i++) {
           if(get(i) != span.get(i))
               return false;
       }
       return true;
    }
    public void copyTo(LongSpan span) {
        for(int i = 0; i < size(); i++) {
            span.put(i, get(i));
        }
    }
    public void fill(long v)
    {
        for(int i = 0; i < size(); i++) {
            put(i, v);
        }
    }

}
