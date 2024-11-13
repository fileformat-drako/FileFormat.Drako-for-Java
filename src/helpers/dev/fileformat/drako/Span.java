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

}
