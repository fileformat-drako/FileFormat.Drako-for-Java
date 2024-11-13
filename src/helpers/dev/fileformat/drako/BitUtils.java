package dev.fileformat.drako;


import java.nio.ByteBuffer;

/**
 * Created by lexchou on 6/5/2017.
 */
@Internal
class BitUtils {

    public static final int SHORT_BYTES = 2;
    public static final int INTEGER_BYTES = 4;
    public static final int LONG_BYTES = 8;
    public static final int DOUBLE_BYTES = 8;
    public static final int FLOAT_BYTES = 4;

    public static int getInt32(byte[] array, int offset)
    {
        return ((array[offset + 3] & 0xff) << 24) |
                ((array[offset + 2] & 0xff) << 16)|
                ((array[offset + 1] & 0xff) << 8) |
                ((array[offset] & 0xff));
    }
    public static long getInt64(byte[] array, int offset) {
        long result = 0;
        for(int i = 0, p = offset + 7; i < 8; i++, p--)
        {
            result = result << 8;
            result |= array[p] & 0xff;
        }
        return result;
    }
    public static long getInt64(byte[] array)
    {
        return getInt64(array, 0);
    }
    public static short getInt16(byte[] array, int offset)
    {
        return (short)(((array[offset + 1] & 0xff) << 8) |
                ((array[offset] & 0xff)));
    }
    public static double getDouble(byte[] array, int offset)
    {
        long n = getInt64(array, offset);
        return Double.longBitsToDouble(n);
    }
    public static float getFloat(byte[] array, int offset)
    {
        int n = getInt32(array, offset);
        return Float.intBitsToFloat(n);
    }

    public static int hashCode(boolean value)
    {
        return value ? 1 : 0;
    }
    public static int hashCode(double value)
    {
        long bits = Double.doubleToLongBits(value);
        return (int)(bits ^ (bits >>> 32));
    }
    public static int hashCode(float value)
    {
        return Float.floatToIntBits(value);
    }
    public static int sizeof(MetaClass<?> type)
    {
        if(type == null)
            throw new IllegalArgumentException("Null type");
        return sizeof(type.classOf());
    }
    public static int sizeof(Class<?> type)
    {
        if(type == null)
            throw new IllegalArgumentException("Null type");

        if(type == byte.class || type == boolean.class)
            return 1;
        if(type == char.class || type == short.class)
            return 2;
        if(type == int.class || type == float.class)
            return 4;
        if(type == double.class || type == long.class)
            return 8;
        throw new IllegalArgumentException("Cannot get size of non-primitive type");
    }
    public static byte[] toBytes(Object val)
    {
        if(val == null)
            throw new IllegalArgumentException("Cannot convert null to bytes");
        Class<?> type = val.getClass();
        if(type == Integer.class)
            return toBytes(((Number)val).intValue());
        if(type == Short.class)
            return toBytes(((Number)val).shortValue());
        if(type == Long.class)
            return toBytes(((Number)val).longValue());
        if(type == Byte.class)
            return toBytes(((Number)val).byteValue());
        if(type == Float.class)
            return toBytes(((Number)val).floatValue());
        if(type == Double.class)
            return toBytes(((Number)val).doubleValue());

        if(type == int[].class)
            return toBytes((int[])val);
        if(type == short[].class)
            return toBytes((short[])val);
        if(type == long[].class)
            return toBytes((long[])val);
        if(type == byte[].class)
            return (byte[])val;
        if(type == float[].class)
            return toBytes((float[])val);
        if(type == double[].class)
            return toBytes((double[])val);
        throw new IllegalArgumentException("Cannot convert unsupported type to bytes");
    }

    public static byte[] toBytes(byte val)
    {
        return new byte[]{val};
    }
    public static byte[] toBytes(short val)
    {
        byte[] ret = new byte[SHORT_BYTES];
        toBytes(ret, 0, val);
        return ret;
    }
    public static byte[] toBytes(int val)
    {
        byte[] ret = new byte[INTEGER_BYTES];
        toBytes(ret, 0, val);
        return ret;
    }
    public static byte[] toBytes(long val)
    {
        byte[] ret = new byte[LONG_BYTES];
        toBytes(ret, 0, val);
        return ret;
    }
    public static byte[] toBytes(float val)
    {
        byte[] ret = new byte[FLOAT_BYTES];
        toBytes(ret, 0, val);
        return ret;
    }
    public static byte[] toBytes(double val)
    {
        byte[] ret = new byte[DOUBLE_BYTES];
        toBytes(ret, 0, val);
        return ret;
    }
    public static byte[] toBytes(boolean[] vals)
    {
        final int size = 1;
        byte[] ret = new byte[vals.length * size];
        for(int i = 0, p = 0; i < vals.length; i++, p += size)
        {
            ret[p] = (byte)(vals[i] ? 1 : 0);
        }
        return ret;
    }
    public static byte[] toBytes(short[] vals)
    {
        final int size = SHORT_BYTES;
        byte[] ret = new byte[vals.length * size];
        for(int i = 0, p = 0; i < vals.length; i++, p += size)
        {
            toBytes(ret, p, vals[i]);
        }
        return ret;
    }
    public static byte[] toBytes(int[] vals)
    {
        final int size = INTEGER_BYTES;
        byte[] ret = new byte[vals.length * size];
        for(int i = 0, p = 0; i < vals.length; i++, p += size)
        {
            toBytes(ret, p, vals[i]);
        }
        return ret;
    }
    public static byte[] toBytes(long[] vals)
    {
        final int size = LONG_BYTES;
        byte[] ret = new byte[vals.length * size];
        for(int i = 0, p = 0; i < vals.length; i++, p += size)
        {
            toBytes(ret, p, vals[i]);
        }
        return ret;
    }
    public static byte[] toBytes(float[] vals)
    {
        final int size = FLOAT_BYTES;
        byte[] ret = new byte[vals.length * size];
        for(int i = 0, p = 0; i < vals.length; i++, p += size)
        {
            toBytes(ret, p, vals[i]);
        }
        return ret;
    }
    public static byte[] toBytes(double[] vals)
    {
        final int size = DOUBLE_BYTES;
        byte[] ret = new byte[vals.length * size];
        for(int i = 0, p = 0; i < vals.length; i++, p += size)
        {
            toBytes(ret, p, vals[i]);
        }
        return ret;
    }

    /**
     * Convert a ByteBuffer to byte[]
     * @param buffer
     * @return
     */
    public static byte[] toBytes(@Pure ByteBuffer buffer)
    {
        byte[] ret = new byte[buffer.remaining()];
        buffer.get(ret, 0, ret.length);
        return ret;
    }
    public static void toBytes(byte[] array, int offset, short[] val, int off, int len)
    {
        int p = offset;
        int end = off + len;
        for(int i = off; i < end; i++)
        {
            toBytes(array, p, val[i]);
            p += SHORT_BYTES;
        }
    }
    public static void toBytes(byte[] array, int offset, short val)
    {
        array[offset + 0] = (byte)(val & 0xff);
        array[offset + 1] = (byte)((val >> 8) & 0xff);

    }
    public static void toBytes(byte[] array, int offset, Integer[] val, int off, int len)
    {
        int p = offset;
        int end = off + len;
        for(int i = off; i < end; i++)
        {
            toBytes(array, p, val[i]);
            p += INTEGER_BYTES;
        }
    }
    public static void toBytes(byte[] array, int offset, int[] val, int off, int len)
    {
        int p = offset;
        int end = off + len;
        for(int i = off; i < end; i++)
        {
            toBytes(array, p, val[i]);
            p += INTEGER_BYTES;
        }
    }
    public static void toBytes(byte[] array, int offset, int val)
    {
        array[offset + 0] = (byte)(val & 0xff);
        array[offset + 1] = (byte)((val >> 8) & 0xff);
        array[offset + 2] = (byte)((val >> 16) & 0xff);
        array[offset + 3] = (byte)((val >> 24) & 0xff);

    }
    public static void toBytes(byte[] array, int offset, long[] val, int off, int len)
    {
        int p = offset;
        int end = off + len;
        for(int i = off; i < end; i++)
        {
            toBytes(array, p, val[i]);
            p += LONG_BYTES;
        }
    }
    public static void toBytes(byte[] array, int offset, long val)
    {
        array[offset + 0] = (byte)(val & 0xff);
        array[offset + 1] = (byte)((val >> 8) & 0xff);
        array[offset + 2] = (byte)((val >> 16) & 0xff);
        array[offset + 3] = (byte)((val >> 24) & 0xff);
        array[offset + 4] = (byte)((val >> 32) & 0xff);
        array[offset + 5] = (byte)((val >> 40) & 0xff);
        array[offset + 6] = (byte)((val >> 48) & 0xff);
        array[offset + 7] = (byte)((val >> 56) & 0xff);
    }
    public static void toBytes(byte[] array, int offset, float[] val, int off, int len)
    {
        int p = offset;
        int end = off + len;
        for(int i = off; i < end; i++)
        {
            toBytes(array, p, val[i]);
            p += FLOAT_BYTES;
        }
    }
    public static void toBytes(byte[] array, int offset, float f)
    {
        int val = Float.floatToRawIntBits(f);
        array[offset + 0] = (byte)(val & 0xff);
        array[offset + 1] = (byte)((val >> 8) & 0xff);
        array[offset + 2] = (byte)((val >> 16) & 0xff);
        array[offset + 3] = (byte)((val >> 24) & 0xff);
    }
    public static void toBytes(byte[] array, int offset, Double[] val, int off, int len)
    {
        int p = offset;
        int end = off + len;
        for(int i = off; i < end; i++)
        {
            toBytes(array, p, val[i]);
            p += DOUBLE_BYTES;
        }
    }
    public static void toBytes(byte[] array, int offset, double[] val, int off, int len)
    {
        int p = offset;
        int end = off + len;
        for(int i = off; i < end; i++)
        {
            toBytes(array, p, val[i]);
            p += DOUBLE_BYTES;
        }
    }
    public static void toBytes(byte[] array, int offset, double d)
    {
        long val = Double.doubleToRawLongBits(d);

        array[offset + 0] = (byte)(val & 0xff);
        array[offset + 1] = (byte)((val >> 8) & 0xff);
        array[offset + 2] = (byte)((val >> 16) & 0xff);
        array[offset + 3] = (byte)((val >> 24) & 0xff);
        array[offset + 4] = (byte)((val >> 32) & 0xff);
        array[offset + 5] = (byte)((val >> 40) & 0xff);
        array[offset + 6] = (byte)((val >> 48) & 0xff);
        array[offset + 7] = (byte)((val >> 56) & 0xff);
    }

    /**
     * Convert bytes to doubles
     * @param bytes source data
     * @param offset offset in bytes
     * @param val values
     * @param off offset in element
     * @param len number of values to read
     */
    public static void fromBytes(byte[] bytes, int offset, double[] val, int off, int len) {
        int s = offset;
        int d = off;
        for(int i = 0; i < len; i++)
        {
            val[d] = getDouble(bytes, s);
            d++;
            s += DOUBLE_BYTES;
        }
    }
    /**
     * Convert bytes to floats
     * @param bytes source data
     * @param offset offset in bytes
     * @param val values
     * @param off offset in element
     * @param len number of values to read
     */
    public static void fromBytes(byte[] bytes, int offset, float[] val, int off, int len) {
        int s = offset;
        int d = off;
        for(int i = 0; i < len; i++)
        {
            val[d] = getFloat(bytes, s);
            d++;
            s += FLOAT_BYTES;
        }
    }
    /**
     * Convert bytes to int array
     * @param bytes source data
     * @param offset offset in bytes
     * @param val values
     * @param off offset in element
     * @param len number of values to read
     */
    public static void fromBytes(byte[] bytes, int offset, int[] val, int off, int len) {
        int s = offset;
        int d = off;
        for(int i = 0; i < len; i++)
        {
            val[d] = getInt32(bytes, s);
            d++;
            s += INTEGER_BYTES;
        }
    }
    /**
     * Convert bytes to long array
     * @param bytes source data
     * @param offset offset in bytes
     * @param val values
     * @param off offset in element
     * @param len number of values to read
     */
    public static void fromBytes(byte[] bytes, int offset, long[] val, int off, int len) {
        int s = offset;
        int d = off;
        for(int i = 0; i < len; i++)
        {
            val[d] = getInt64(bytes, s);
            d++;
            s += LONG_BYTES;
        }
    }

    /**
     * Perform a comparison as unsigned int
     * @param x
     * @param y
     * @return
     */
    public static int compareUnsigned(int x, int y)
    {
        return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }
    /**
     * Perform a comparison as unsigned byte
     * @param x
     * @param y
     * @return
     */
    public static int compareUnsigned(byte x, byte y)
    {
        return Integer.compare(0xff & x, 0xff & y);
    }
    /**
     * Perform a comparison as unsigned short
     * @param x
     * @param y
     * @return
     */
    public static int compareUnsigned(short x, short y)
    {
        return Integer.compare(0xffff & x, 0xffff & y);
    }
    /**
     * Perform a comparison as unsigned long
     * @param x
     * @param y
     * @return
     */
    public static int compareUnsigned(long x, long y)
    {
        return Long.compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE);
    }
}
