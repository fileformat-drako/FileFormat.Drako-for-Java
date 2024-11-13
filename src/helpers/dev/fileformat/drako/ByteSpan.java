package dev.fileformat.drako;


import java.util.Arrays;

/**
 * Simulation of .NET's Span&lt;byte&gt;
 */
@Internal
class ByteSpan extends Span {
    final byte[] array;

    public static final ByteSpan EMPTY = wrap(null, 0, 0);

    public ByteSpan(byte[] array, int offset, int length) {
        super(offset, length);
        this.array = array;
    }

    public byte[] array() {
        return array;
    }

    public static ByteSpan wrap(byte[] array)
    {
        return new ByteSpan(array, 0, array.length);
    }
    public static ByteSpan wrap(byte[] array, int offset)
    {
        return new ByteSpan(array, offset, array.length - offset);
    }
    public static ByteSpan wrap(byte[] array, int offset, int length)
    {
        return new ByteSpan(array, offset, length);
    }

    public byte get(int idx)
    {
        rangeCheck(idx);
        return array[idx + offset];
    }
    public void put(int idx, byte value) {
        rangeCheck(idx);
        array[idx + offset] = value;
    }

    public IntSpan asIntSpan() {
        return IntSpan.wrap(array, offset, length);
    }
    public FloatSpan asFloatSpan() {
        return FloatSpan.wrap(array, offset, length);
    }
    public int compareTo(ByteSpan span) {
        int num = Math.min(size(), span.size());
        for(int i = 0; i < num; i++) {
            int n = Byte.compare(get(i), span.get(i));
            if(n != 0)
                return n;
        }
        return Integer.compare(size(), span.size());
    }
    public boolean equals(ByteSpan span) {
        if(size() != span.size())
            return false;
        for(int i = 0; i < size(); i++) {
            if(get(i) != span.get(i))
                return false;
        }
        return true;
    }
    public final void copyTo(byte[] span) {
        System.arraycopy(array, offset, span, 0, length);
    }
    public final void copyTo(ByteSpan span) {
        System.arraycopy(array, offset, span.array, span.offset, length);
    }
    public final void fill(byte v)
    {
        Arrays.fill(array, offset, offset + length, v);
    }

    public ByteSpan slice(int offset, int size) {
        return new ByteSpan(array, offset + this.offset, size);
    }
    public ByteSpan slice(int offset) {
        return new ByteSpan(array, offset + this.offset, this.size() - offset);
    }
}
