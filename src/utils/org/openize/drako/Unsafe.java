package org.openize.drako;
class Unsafe
{    
    public static float getFloat(byte[] arr, int off)
    {
        int v = Unsafe.getLE32(arr, off);
        return java.lang.Float.intBitsToFloat(v);
        
    }
    
    public static short getLE16(byte[] arr, int off)
    {
        if (true)
        {
            short val = (short)((0xff & arr[off + 1]) << 8);
            val |= (short)(0xff & arr[off]);
            return val;
        }
        else
        {
            short val = (short)((0xff & arr[off]) << 8);
            val |= (short)(0xff & arr[off + 1]);
            return val;
        }
        
    }
    
    public static short getLE16(byte[] arr)
    {
        return Unsafe.getLE16(arr, 0);
    }
    
    public static int getLE24(byte[] arr, int off)
    {
        int val = (int)((0xff & arr[off + 2]) << 16);
        val |= (int)((0xff & arr[off + 1]) << 8);
        val |= 0xff & arr[off + 0];
        return val;
    }
    
    public static int getLE32(byte[] arr, int off)
    {
        if (true)
        {
            int val = (int)((0xff & arr[off + 0]) << 0);
            val |= (int)((0xff & arr[off + 1]) << 8);
            val |= (int)((0xff & arr[off + 2]) << 16);
            val |= (0xff & arr[off + 3]) << 24;
            return val;
        }
        else if (off + 3 < arr.length)
        {
            {
                int val = (int)((0xff & arr[off + 0]) << 24);
                val |= (int)((0xff & arr[off + 1]) << 16);
                val |= (int)((0xff & arr[off + 2]) << 8);
                val |= 0xff & arr[off + 3];
                return val;
            }
            
        }
        else
            return 0;
    }
    
    public static long getLE64(byte[] arr, int off)
    {
        if (true)
        {
            long val = 0L;
            val |= (long)(0xff & arr[off + 7]) << 56;
            val |= (long)(0xff & arr[off + 6]) << 48;
            val |= (long)(0xff & arr[off + 5]) << 40;
            val |= (long)(0xff & arr[off + 4]) << 32;
            val |= (long)(0xff & arr[off + 3]) << 24;
            val |= (long)(0xff & arr[off + 2]) << 16;
            val |= (long)(0xff & arr[off + 1]) << 8;
            val |= (long)(0xff & arr[off + 0]) << 0;
            return val;
        }
        else
        {
            long val = 0L;
            val |= (long)(0xff & arr[off + 0]) << 56;
            val |= (long)(0xff & arr[off + 1]) << 48;
            val |= (long)(0xff & arr[off + 2]) << 40;
            val |= (long)(0xff & arr[off + 3]) << 32;
            val |= (long)(0xff & arr[off + 4]) << 24;
            val |= (long)(0xff & arr[off + 5]) << 16;
            val |= (long)(0xff & arr[off + 6]) << 8;
            val |= 0xff & arr[off + 7];
            return val;
        }
        
    }
    
    public static void putLE16(byte[] arr, int off, short val)
    {
        if (true)
        {
            arr[off + 1] = (byte)((0xffff & val) >>> 8 & 0xff);
            arr[off + 0] = (byte)((0xffff & val) >>> 0 & 0xff);
        }
        else
        {
            arr[off] = (byte)((0xffff & val) >>> 8 & 0xff);
            arr[off + 1] = (byte)((0xffff & val) >>> 0 & 0xff);
        }
        
    }
    
    public static void putLE24(byte[] arr, int off, int val)
    {
        arr[off] = (byte)(val >>> 0 & 0xff);
        arr[off + 1] = (byte)(val >>> 8 & 0xff);
        arr[off + 2] = (byte)(val >>> 16 & 0xff);
    }
    
    public static void putLE32(byte[] arr, int off, int val)
    {
        if (true)
        {
            arr[off + 0] = (byte)(val >>> 0 & 0xff);
            arr[off + 1] = (byte)(val >>> 8 & 0xff);
            arr[off + 2] = (byte)(val >>> 16 & 0xff);
            arr[off + 3] = (byte)(val >>> 24 & 0xff);
        }
        else
        {
            
            arr[off + 3] = (byte)(val >>> 0 & 0xff);
            arr[off + 2] = (byte)(val >>> 8 & 0xff);
            arr[off + 1] = (byte)(val >>> 16 & 0xff);
            arr[off + 0] = (byte)(val >>> 24 & 0xff);
        }
        
    }
    
    public static int floatToUInt32(float value)
    {
        return java.lang.Float.floatToIntBits(value);
        
    }
    
    public static void toByteArray(float[] arr, int start, int len, byte[] dst, int offset)
    {
        for (int i = 0,  psrc = start,  pdst = offset; i < len; psrc++, pdst += 4, i++)
        {
            int v = Unsafe.floatToUInt32(arr[psrc]);
            Unsafe.putLE32(dst, pdst, v);
        }
        
    }
    
    public static float[] toFloatArray(byte[] array, float[] ret)
    {
        if (ret.length == 0)
            return ret;
        for (int i = 0,  d = 0; i < ret.length; i += 4, d++)
        {
            ret[d] = Unsafe.getFloat(array, i);
        }
        
        return ret;
    }
    
    
}
