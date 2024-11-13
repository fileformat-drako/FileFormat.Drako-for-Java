package dev.fileformat.drako;


import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utilities for converting between data types
 * Created by lexchou on 6/16/2017.
 */
@Internal
class Convert {
    // Mapping table from 6-bit nibbles to Base64 characters.
    private static final char[] map1 = new char[64];
    // Mapping table from Base64 characters to 6-bit nibbles.
    private static final byte[] map2 = new byte[128];
   static {
       int i = 0;
       for (char c = 'A'; c <= 'Z'; c++) map1[i++] = c;
       for (char c = 'a'; c <= 'z'; c++) map1[i++] = c;
       for (char c = '0'; c <= '9'; c++) map1[i++] = c;
       map1[i++] = '+';
       map1[i++] = '/';

       for (i = 0; i < map2.length; i++) map2[i] = -1;
       for (i = 0; i < 64; i++) map2[map1[i]] = (byte) i;
   }




    public static byte[] fromBase64(String str) {
       char[] chars = str.toCharArray();
       return decode(chars, 0, chars.length);
    }
    private static byte[] decode (char[] in, int iOff, int iLen) {
        if (iLen % 4 != 0)
            throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
        while (iLen > 0 && in[iOff + iLen - 1] == '=') iLen--;
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        int ip = iOff;
        int iEnd = iOff + iLen;
        int op = 0;
        while (ip < iEnd) {
            int i0 = in[ip++];
            int i1 = in[ip++];
            int i2 = ip < iEnd ? in[ip++] : 'A';
            int i3 = ip < iEnd ? in[ip++] : 'A';
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127)
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            int b0 = map2[i0];
            int b1 = map2[i1];
            int b2 = map2[i2];
            int b3 = map2[i3];
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            int o0 = (b0 << 2) | (b1 >>> 4);
            int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
            int o2 = ((b2 & 3) << 6) | b3;
            out[op++] = (byte) o0;
            if (op < oLen) out[op++] = (byte) o1;
            if (op < oLen) out[op++] = (byte) o2;
        }
        return out;
    }

    /**
     * Decode the UUID from bytes using a mixed endian mode.
     * whereby the first three components of the UUID are little-endian, and the last two are big-endian.
     * @param b
     * @return
     */
    public static UUID toUUID(byte[] b) {
        long most = ((long)(b[3]& 0xff) << 56L) | ((long)(b[2] & 0xff) << 48) | ((long)(b[1] & 0xff) << 40) | ((long)(b[0] & 0xff) << 32)
                | ((long)(b[5] & 0xff) << 24) | ((long)(b[4] & 0xff) << 16)
                | ((long)(b[7] & 0xff) << 8) | ((long)(b[6] & 0xff) << 0)
                ;
        long least =
                ((long)(b[8] & 0xff) << 56) | ((long)(b[9] & 0xff) << 48) |
                        ((long)(b[10] & 0xff) << 40) | ((long)(b[11] & 0xff) << 32) |
                        ((long)(b[12] & 0xff) << 24) | ((long)(b[13] & 0xff) << 16) | ((long)(b[14] & 0xff) << 8) | (long)(b[15] & 0xff);
        return new UUID(most, least);
    }

    public static String toBase64(byte[] bytes, int start, int len) {
       char[] chars = encode(bytes, start, len);
       return new String(chars);
    }
    public static String toBase64(byte[] bytes) {
        return toBase64(bytes, 0, bytes.length);
    }
    private static char[] encode (byte[] in, int iOff, int iLen) {
        int oDataLen = (iLen * 4 + 2) / 3;       // output length without padding
        int oLen = ((iLen + 2) / 3) * 4;         // output length including padding
        char[] out = new char[oLen];
        int ip = iOff;
        int iEnd = iOff + iLen;
        int op = 0;
        while (ip < iEnd) {
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iEnd ? in[ip++] & 0xff : 0;
            int i2 = ip < iEnd ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = map1[o0];
            out[op++] = map1[o1];
            out[op] = op < oDataLen ? map1[o2] : '=';
            op++;
            out[op] = op < oDataLen ? map1[o3] : '=';
            op++;
        }
        return out;
    }
    /**
     * Format date in ISO 8601 format
     * @param date
     * @return
     */
    public static String formatISO8601(Date date) {

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return df.format(date);
    }
    public static String formatISO8601(Calendar date) {
        return formatISO8601(date.getTime());
    }

    public static Calendar parseISO8601(String str) {
        long t = parseISO8601AsMilliseconds(str);
        if(t == -1)
            return null;
        Calendar ret = Calendar.getInstance();
        ret.setTimeInMillis(t);
        return ret;
    }
    private static SimpleDateFormat[] iso8601Formats = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    };
    private static long parseISO8601AsMilliseconds(String str)
    {
        for(int i = 0; i < iso8601Formats.length; i++) {
            synchronized (iso8601Formats[i]) {
                try {
                    Date date = iso8601Formats[i].parse(str);
                    return date.getTime();
                } catch (ParseException e) {

                }
            }
        }
        return -1;
    }

    public static void blockCopy(Object src, int srcOffset, Object dst, int dstOffset, int count) {
        if(dst == null || src == null)
            throw new IllegalArgumentException("Null argument");
        if(dst.getClass() != byte[].class)
            throw new IllegalArgumentException("Dst only supports byte[]");
        byte[] dest = (byte[])dst;

        if(src instanceof byte[])
            blockCopy((byte[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof boolean[])
            blockCopy((boolean[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof Boolean[])
            blockCopy((Boolean[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof int[])
            blockCopy((int[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof Integer[])
            blockCopy((Integer[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof short[])
            blockCopy((short[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof long[])
            blockCopy((long[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof float[])
            blockCopy((float[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof double[])
            blockCopy((double[])src, srcOffset, dest, dstOffset, count);
        else if(src instanceof Double[])
            blockCopy((Double[])src, srcOffset, dest, dstOffset, count);
        else
            throw new IllegalArgumentException("Unsupported src type");
    }
    private static void blockCopy(byte[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        System.arraycopy(src, srcOffset, dst, dstOffset, count);
    }
    private static void blockCopy(Boolean[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        final int size = 1;
        int rest = count;
        int psrc = srcOffset, pdst = dstOffset;
        for(int i = srcOffset; rest > size; rest -= size)
        {
            dst[pdst] = (byte)(src[psrc] ? 1 : 0);

            pdst += size;
            psrc++;
        }
    }
    private static void blockCopy(boolean[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        final int size = 1;
        int rest = count;
        int psrc = srcOffset, pdst = dstOffset;
        for(int i = srcOffset; rest > size; rest -= size)
        {
            dst[pdst] = (byte)(src[psrc] ? 1 : 0);

            pdst += size;
            psrc++;
        }
    }
    private static void blockCopy(Integer[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        int left = srcOffset & 3;
        int intOffset = srcOffset >> 2;
        int intSize = count >> 2;
        if(left != 0) {
            int head = src[0];
            switch(left) {
                case 1:
                    dst[dstOffset] = (byte) (head >>> 24);
                    break;
                case 2:
                    dst[dstOffset] = (byte) (head >>> 16);
                    dst[dstOffset + 1] = (byte) (head >>> 24);
                    break;
                case 3:
                    dst[dstOffset] = (byte) (head >>> 8);
                    dst[dstOffset + 1] = (byte) (head >>> 16);
                    dst[dstOffset + 2] = (byte) (head >>> 24);
                    break;
            }
            dstOffset += left;
            count -= left;
            //skip first int
            intOffset++;
            intSize--;
        }
        if(intSize != 0)
            BitUtils.toBytes(dst, dstOffset, src, intOffset, intSize);
        int right = count & 3;
        if(right > 0) {
            int tail = src[intOffset + intSize];
            int ptail = dstOffset + (intSize << 2);
            switch(right) {
                case 1:
                    dst[ptail] = (byte) (tail);
                    break;
                case 2:
                    dst[ptail] = (byte) (tail);
                    dst[ptail + 1] = (byte) (tail >>> 8);
                    break;
                case 3:
                    dst[ptail] = (byte) (tail);
                    dst[ptail + 1] = (byte) (tail >>> 8);
                    dst[ptail + 2] = (byte) (tail >>> 16);
                    break;
            }

        }
    }
    private static void blockCopy(int[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        int left = srcOffset & 3;
        int intOffset = srcOffset >> 2;
        int intSize = count >> 2;
        if(left != 0) {
            int head = src[0];
            switch(left) {
                case 1:
                    dst[dstOffset] = (byte) (head >>> 24);
                    break;
                case 2:
                    dst[dstOffset] = (byte) (head >>> 16);
                    dst[dstOffset + 1] = (byte) (head >>> 24);
                    break;
                case 3:
                    dst[dstOffset] = (byte) (head >>> 8);
                    dst[dstOffset + 1] = (byte) (head >>> 16);
                    dst[dstOffset + 2] = (byte) (head >>> 24);
                    break;
            }
            dstOffset += left;
            count -= left;
            //skip first int
            intOffset++;
            intSize--;
        }
        if(intSize != 0)
            BitUtils.toBytes(dst, dstOffset, src, intOffset, intSize);
        int right = count & 3;
        if(right > 0) {
            int tail = src[intOffset + intSize];
            int ptail = dstOffset + (intSize << 2);
            switch(right) {
                case 1:
                    dst[ptail] = (byte) (tail);
                    break;
                case 2:
                    dst[ptail] = (byte) (tail);
                    dst[ptail + 1] = (byte) (tail >>> 8);
                    break;
                case 3:
                    dst[ptail] = (byte) (tail);
                    dst[ptail + 1] = (byte) (tail >>> 8);
                    dst[ptail + 2] = (byte) (tail >>> 16);
                    break;
            }

        }
    }
    private static void blockCopy(short[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        if(srcOffset != 0 || dstOffset != 0 || ((count & 0x1) != 0))
            throw new IllegalArgumentException("Unsupported call to blockCopy");
        BitUtils.toBytes(dst, 0, src, srcOffset / BitUtils.SHORT_BYTES, count / BitUtils.SHORT_BYTES);
    }
    private static void blockCopy(long[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        if(srcOffset != 0 || dstOffset != 0 || ((count & 0x7) != 0))
            throw new IllegalArgumentException("Unsupported call to blockCopy");
        BitUtils.toBytes(dst, 0, src, srcOffset / BitUtils.LONG_BYTES, count / BitUtils.LONG_BYTES);
    }
    private static void blockCopy(float[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        if(srcOffset != 0 || dstOffset != 0 || ((count & 0x3) != 0))
            throw new IllegalArgumentException("Unsupported call to blockCopy");
        BitUtils.toBytes(dst, 0, src, srcOffset / BitUtils.FLOAT_BYTES, count / BitUtils.FLOAT_BYTES);
    }
    private static void blockCopy(double[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        if(srcOffset != 0 || dstOffset != 0 || ((count & 0x7) != 0))
            throw new IllegalArgumentException("Unsupported call to blockCopy");
        BitUtils.toBytes(dst, 0, src, srcOffset / BitUtils.DOUBLE_BYTES, count / BitUtils.DOUBLE_BYTES);
    }
    private static void blockCopy(Double[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
        if(srcOffset != 0 || dstOffset != 0 || ((count & 0x7) != 0))
            throw new IllegalArgumentException("Unsupported call to blockCopy");
        BitUtils.toBytes(dst, 0, src, srcOffset / BitUtils.DOUBLE_BYTES, count / BitUtils.DOUBLE_BYTES);
    }
    public static boolean tryParseLong(String str, @Out long[] ret) {
        try {
            ret[0] = Long.parseLong(trim(str));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean tryParseDouble(String str, @Out double[] ret) {
        try {
            ret[0] = Double.parseDouble(trim(str));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private static String trim(String s) {
        if(s == null || s.isEmpty())
            return s;
        if(Character.isWhitespace(s.charAt(0)) || Character.isWhitespace(s.charAt(s.length() - 1)))
            return s.trim();
        return s;
    }

    public static int tryParseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(trim(str));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    public static boolean tryParseInt(String str, @Out int[] ret) {
        try {
            ret[0] = Integer.parseInt(trim(str));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Object changeType(Object value, Class<?> clazz) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Get the underlying type of the enum
     * @param value
     * @return
     */
    public static Class<?> getUnderlyingType(Object value) {
        throw new RuntimeException("Not implemented");
    }
    public static boolean parseDateTime(String input, String pattern, @Out Calendar[] ret)
    {
        try {

            String newPattern = normalizeDateFormat(pattern);
            SimpleDateFormat sdf = new SimpleDateFormat(newPattern);
            Date d = sdf.parse(trim(input));
            ret[0] = Calendar.getInstance();
            ret[0].setTimeInMillis(d.getTime());
            return true;
        } catch (Exception e) {
            ret[0] = null;
            return false;

        }
    }

    /**
     * Convert .net style date format into java's style
     * @param netFormat
     * @return
     */
    public static String normalizeDateFormat(String netFormat) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < netFormat.length(); i++) {
            char ch = netFormat.charAt(i);
            if(ch == 'f')
                sb.append('S');
            else
                sb.append(ch);
        }
        return sb.toString();
    }


    /**
     * Convert an Iterable instance to ArrayList
     * @param collection
     * @param <T>
     * @return
     */
    public static <T> ArrayList<T> asList(Iterable<T> collection) {
        if(collection == null)
            throw new IllegalArgumentException("collection cannot be null");
        if(collection instanceof ArrayList)
            return (ArrayList)collection;
        ArrayList<T> ret = new ArrayList<>();
        for(T t : collection) {
            ret.add(t);
        }
        return ret;
    }

    /**
     * Convert array to List
     * @param array
     * @return
     */
    public static List<?> asList(Object array)
    {
        if(array == null)
            return null;
        if(array instanceof Iterable)
            return asList((Iterable)array);
        if(!(array.getClass().isArray()))
            throw new IllegalArgumentException("Cannot convert non-array into list");
        if(array instanceof int[])
            return asList((int[])array);
        if(array instanceof long[])
            return asList((long[])array);
        if(array instanceof float[])
            return asList((float[])array);
        if(array instanceof double[])
            return asList((double[])array);
        if(array instanceof boolean[])
            return asList((boolean[])array);
        if(array instanceof byte[])
            return asList((byte[])array);
        if(array instanceof short[])
            return asList((short[])array);
        if(array instanceof char[])
            return asList((char[])array);
        return Arrays.asList((Object[])array);
    }
    public static List<Character> asList(char[] array)
    {
        return new ListUtils.NativeArrayList<Character>(array);
    }
    public static List<Boolean> asList(boolean[] array)
    {
        return new ListUtils.NativeArrayList<Boolean>(array);
    }
    public static List<Byte> asList(byte[] array)
    {
        return new ListUtils.NativeArrayList<Byte>(array);
    }
    public static List<Short> asList(short[] array)
    {
        return new ListUtils.NativeArrayList<Short>(array);
    }
    public static List<Integer> asList(int[] array)
    {
        return new ListUtils.IntList(array);
    }
    public static List<Long> asList(long[] array)
    {
        return new ListUtils.NativeArrayList<Long>(array);
    }
    public static List<Float> asList(float[] array)
    {
        return new ListUtils.NativeArrayList<Float>(array);
    }
    public static List<Double> asList(double[] array)
    {
        return new ListUtils.NativeArrayList<Double>(array);
    }


    /**
     * Unbox the boxed array to primitive array
     * e.g. Convert Object[] to  int[]
     * Call unbox(array, int.class)
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static Object unbox(Object[] array, Class<?> elementType)
    {
        if(array == null)
            throw new IllegalArgumentException("array cannot be null");
        if(elementType == null)
            throw new IllegalArgumentException("elementType cannot be null");

        Object ret = Array.newInstance(elementType, array.length);
        if(array.length > 0) {
            if(elementType == int.class) {
                int[] ints = (int[]) ret;
                for (int i = 0; i < array.length; i++) {
                    ints[i] = ((Number) array[i]).intValue();
                }
            } else if(elementType == short.class) {
                short[] vals = (short[]) ret;
                for (int i = 0; i < array.length; i++) {
                    vals[i] = ((Number) array[i]).shortValue();
                }
            } else if(elementType == byte.class) {
                byte[] vals = (byte[]) ret;
                for (int i = 0; i < array.length; i++) {
                    vals[i] = ((Number) array[i]).byteValue();
                }
            } else if(elementType == long.class) {
                long[] vals = (long[]) ret;
                for (int i = 0; i < array.length; i++) {
                    vals[i] = ((Number) array[i]).longValue();
                }
            } else if(elementType == double.class) {
                double[] vals = (double[]) ret;
                for (int i = 0; i < array.length; i++) {
                    vals[i] = ((Number) array[i]).doubleValue();
                }
            } else if(elementType == float.class) {
                float[] vals = (float[]) ret;
                for (int i = 0; i < array.length; i++) {
                    vals[i] = ((Number) array[i]).floatValue();
                }
            } else if(elementType == boolean.class) {
                boolean[] vals = (boolean[]) ret;
                for (int i = 0; i < array.length; i++) {
                    vals[i] = (Boolean) array[i];
                }
            } else if(elementType == char.class) {
                char[] vals = (char[]) ret;
                for (int i = 0; i < array.length; i++) {
                    vals[i] = (Character) array[i];
                }
            } else {
                //generic version, slow but for all types
                for (int i = 0; i < array.length; i++) {
                    Array.set(ret, i, array[i]);
                }
            }
        }
        return ret;

    }
    /**
     * Unbox the boxed array to primitive array
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static char[] unbox(Character[] array)
    {
        if(array == null)
            return null;
        char[] ret = new char[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Unbox the boxed array to primitive array
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static byte[] unbox(Byte[] array)
    {
        if(array == null)
            return null;
        byte[] ret = new byte[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Unbox the boxed array to primitive array
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static boolean[] unbox(Boolean[] array)
    {
        if(array == null)
            return null;
        boolean[] ret = new boolean[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Unbox the boxed array to primitive array
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static short[] unbox(Short[] array)
    {
        if(array == null)
            return null;
        short[] ret = new short[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Unbox the boxed array to primitive array
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static int[] unbox(Integer[] array)
    {

        if(array == null)
            return null;
        int[] ret = new int[array.length];
        if((Class<?>)array.getClass() == Object[].class) {
            Object[] objects = (Object[]) array;
            for(int i = 0; i < objects.length; i++) {
                if(objects[i] != null)
                    ret[i] = (Integer) objects[i];
            }
        } else {
            for (int i = 0; i < ret.length; i++) {
                if(array[i] != null)
                    ret[i] = array[i];
            }
        }
        return ret;
    }

    /**
     * Unbox the boxed array to primitive array
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static long[] unbox(Long[] array)
    {
        if(array == null)
            return null;
        long[] ret = new long[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Unbox the boxed array to primitive array
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static float[] unbox(Float[] array)
    {
        if(array == null)
            return null;
        float[] ret = new float[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Unbox the boxed array to primitive array
     * @param array array of boxed type
     * @return array of primitive type
     */
    public static double[] unbox(Double[] array)
    {
        if(array == null)
            return null;
        double[] ret = new double[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Box the primitive array to boxed object array
     * @param array array of primitive type
     * @return array of boxed type
     */
    public static Character[] box(char[] array)
    {
        if(array == null)
            return null;
        Character[] ret = new Character[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Box the primitive array to boxed object array
     * @param array array of primitive type
     * @return array of boxed type
     */
    public static Byte[] box(byte[] array)
    {
        if(array == null)
            return null;
        Byte[] ret = new Byte[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Box the primitive array to boxed object array
     * @param array array of primitive type
     * @return array of boxed type
     */
    public static Boolean[] box(boolean[] array)
    {
        if(array == null)
            return null;
        Boolean[] ret = new Boolean[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Box the primitive array to boxed object array
     * @param array array of primitive type
     * @return array of boxed type
     */
    public static Short[] box(short[] array)
    {
        if(array == null)
            return null;
        Short[] ret = new Short[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Box the primitive array to boxed object array
     * @param array array of primitive type
     * @return array of boxed type
     */
    public static Integer[] box(int[] array)
    {

        if(array == null)
            return null;
        Integer[] ret = new Integer[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Box the primitive array to boxed object array
     * @param array array of primitive type
     * @return array of boxed type
     */
    public static Long[] box(long[] array)
    {
        if(array == null)
            return null;
        Long[] ret = new Long[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Box the primitive array to boxed object array
     * @param array array of primitive type
     * @return array of boxed type
     */
    public static Float[] box(float[] array)
    {
        if(array == null)
            return null;
        Float[] ret = new Float[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
    /**
     * Box the primitive array to boxed object array
     * @param array array of primitive type
     * @return array of boxed type
     */
    public static Double[] box(double[] array)
    {
        if(array == null)
            return null;
        Double[] ret = new Double[array.length];
        for(int i = 0; i < ret.length; i++)
            ret[i] = array[i];
        return ret;
    }
}
