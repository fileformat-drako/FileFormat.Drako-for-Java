package dev.fileformat.drako;


import java.util.Iterator;

@Internal
class Span {

    protected final int offset;
    protected final int length;

    protected Span(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }
    protected final void rangeCheck(int idx) {
        if(idx > length || idx < 0)
            throw new IndexOutOfBoundsException();
    }

    public final int offset() {
        return offset;
    }

    public final int size() {
        return length;
    }

    /**
     * Get int from array using little endian order
     * @param array
     * @param offset
     * @return
     */
    protected static int getIntL(byte[] array, int offset)
    {
        return (array[offset] & 0xFF)
                | ((array[offset + 1] & 0xFF) << 8)
                | ((array[offset + 2] & 0xFF) << 16)
                | ((array[offset + 3] & 0xFF) << 24);
    }
    protected static long getLongL(byte[] array, int offset)
    {
        return (array[offset] & 0xFFL)
                | ((array[offset + 1] & 0xFF) << (8))
                | ((array[offset + 2] & 0xFF) << (2 * 8L)
                | ((long)(array[offset + 3] & 0xFF) << (3 * 8)))
                | ((long)(array[offset + 4] & 0xFF) << (4 * 8))
                | ((long)(array[offset + 5] & 0xFF) << (5 * 8))
                | ((long)(array[offset + 6] & 0xFF) << (6 * 8))
                | ((long)(array[offset + 7] & 0xFF) << (7 * 8))

                ;
    }

    /**
     * Put int to array using little endian order
     * @param array
     * @param offset
     * @param value
     */
    protected static void putIntL(byte[] array, int offset, int value)
    {
        array[offset] = (byte)value;
        array[offset + 1] = (byte)(value >> 8);
        array[offset + 2] = (byte)(value >> 16);
        array[offset + 3] = (byte)(value >>> 24);
    }
    /**
     * Put long to array using little endian order
     * @param array
     * @param offset
     * @param value
     */
    protected static void putLongL(byte[] array, int offset, long value)
    {
        array[offset] = (byte)value;
        array[offset + 1] = (byte)(value >> 8);
        array[offset + 2] = (byte)(value >> 16);
        array[offset + 3] = (byte)(value >>> 24);
        array[offset + 4] = (byte)(value >>> 32);
        array[offset + 5] = (byte)(value >>> 40);
        array[offset + 6] = (byte)(value >>> 48);
        array[offset + 7] = (byte)(value >>> 56);
    }

}
