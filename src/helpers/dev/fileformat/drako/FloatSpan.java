package dev.fileformat.drako;



@Internal
abstract class FloatSpan extends Span {

    private static final class ArraySpan extends FloatSpan {
        private final float[] array;
        public ArraySpan(float[] array, int offset, int length) {
            super(offset, length);
            this.array = array;
        }

        @Override
        public float get(int idx)
        {
            return array[idx + offset];
        }
        @Override
        public void put(int idx, float value)
        {
            rangeCheck(idx);
            array[idx + offset] = value;
        }

        @Override
        public FloatSpan slice(int offset, int size) {
            return new ArraySpan(array, offset + this.offset, size);
        }

    }
    private static final class BytesSpan extends FloatSpan {
        private final byte[] array;
        public BytesSpan(byte[] array, int offset, int length) {
            super(offset, length);
            this.array = array;
        }

        @Override
        public float get(int idx)
        {
            rangeCheck(idx);

            int ptr = (idx + offset) * 4;
            int n = getIntL(array, ptr);
            return Float.intBitsToFloat(n);
        }
        @Override
        public void put(int idx, float value)
        {
            rangeCheck(idx);
            int ptr = (idx + offset) * 4;
            putIntL(array, ptr, Float.floatToRawIntBits(value));
        }

        @Override
        public FloatSpan slice(int offset, int size) {
            return new BytesSpan(array, offset + this.offset, size);
        }

    }

    public static FloatSpan wrap(float[] array) {
        return new ArraySpan(array, 0, array.length);
    }
    public static FloatSpan wrap(float[] array, int offset, int length) {
        return new ArraySpan(array, offset, array.length);
    }
    public static FloatSpan wrap(byte[] array) {
        return new BytesSpan(array, 0, array.length / 4);
    }
    public static FloatSpan wrap(byte[] array, int offset, int length) {
        return new BytesSpan(array, offset / 4, length / 4);
    }

    protected FloatSpan(int offset, int length) {
        super(offset, length);
    }

    public abstract float get(int idx);
    public abstract void put(int idx, float value);

    public abstract FloatSpan slice(int offset, int size);
    public FloatSpan slice(int offset)
    {
        return slice(offset, this.length - offset);
    }


    public int compareTo(FloatSpan span) {
        int num = Math.min(size(), span.size());
        for(int i = 0; i < num; i++) {
            int n = Float.compare(get(i), span.get(i));
            if(n != 0)
                return n;
        }
        return Integer.compare(size(), span.size());
    }
    public boolean equals(FloatSpan span) {
       if(size() != span.size())
           return false;
       for(int i = 0; i < size(); i++) {
           if(get(i) != span.get(i))
               return false;
       }
       return true;
    }
    public void copyTo(FloatSpan span) {
        for(int i = 0; i < size(); i++) {
            span.put(i, get(i));
        }
    }
    public void fill(float v)
    {
        for(int i = 0; i < size(); i++) {
            put(i, v);
        }
    }

}
