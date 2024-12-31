package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class DracoUtils
{    
    
    public static void fill(int[] array, int value)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = value;
        }
        
    }
    
    /**
     *  Returns the point id of |c| without using a corner table.
     *
     */
    public static int cornerToPointId(int c, DracoMesh mesh)
    {
        return mesh.readCorner(c);
        // (c/3)[c%3];
    }
    
    public static int incrementMod(int I, int M)
    {
        return I == (M - 1) ? 0 : I + 1;
    }
    
    public static int dataTypeLength(int dt)
    {
        switch(dt)
        {
            case DataType.INT8:
            case DataType.UINT8:
                return 1;
            case DataType.INT16:
            case DataType.UINT16:
                return 2;
            case DataType.INT32:
            case DataType.UINT32:
                return 4;
            case DataType.INT64:
            case DataType.UINT64:
                return 8;
            case DataType.FLOAT32:
                return 4;
            case DataType.FLOAT64:
                return 8;
            case DataType.BOOL:
                return 1;
            default:
                return -1;
        }
        
    }
    
    /**
     *  Copies the |bnbits| from the src integer into the |dst| integer using the
     *  provided bit offsets |dst_offset| and |src_offset|.
     *
     */
    public static void copyBits32(int[] dst, int dst_offset, int src, int src_offset, int nbits)
    {
        int mask = ~0 >> (32 - nbits) << dst_offset;
        dst[0] = dst[0] & ~mask | (src >>> src_offset << dst_offset & mask);
    }
    
    /**
     *  Returns the number of '1' bits within the input 32 bit integer.
     *
     */
    public static int countOnes32(int n)
    {
        n -= n >>> 1 & 0x55555555;
        n = (n >>> 2 & 0x33333333) + (n & 0x33333333);
        return (int)((n + (n >>> 4) & 0xF0F0F0F) * 0x1010101 >>> 24);
    }
    
    public static int reverseBits32(int n)
    {
        n = n >>> 1 & 0x55555555 | ((n & 0x55555555) << 1);
        n = n >>> 2 & 0x33333333 | ((n & 0x33333333) << 2);
        n = n >>> 4 & 0x0F0F0F0F | ((n & 0x0F0F0F0F) << 4);
        n = n >>> 8 & 0x00FF00FF | ((n & 0x00FF00FF) << 8);
        return n >>> 16 | (n << 16);
    }
    
    public static boolean isIntegerType(int type)
    {
        switch(type)
        {
            case DataType.INT8:
            case DataType.UINT8:
            case DataType.INT16:
            case DataType.UINT16:
            case DataType.INT32:
            case DataType.UINT32:
                return true;
            default:
                return false;
        }
        
    }
    
    /**
     *  Returns the most location of the most significant bit in the input integer
     *  |n|.
     *  The funcionality is not defined for |n == 0|.
     *
     */
    public static int mostSignificantBit(int n)
    {
        int msb = -1;
        while ((0xffffffffl & n) != 0)
        {
            msb++;
            n >>= 1;
        }
        
        return msb;
    }
    
    public static LongVector3 add(LongVector3 a, LongVector3 b)
    {
        return new LongVector3(a.x + b.x, a.y + b.y, a.z + b.z);
    }
    
    public static LongVector3 sub(LongVector3 a, LongVector3 b)
    {
        return new LongVector3(a.x - b.x, a.y - b.y, a.z - b.z);
    }
    
    public static LongVector3 div(LongVector3 a, long b)
    {
        return new LongVector3(a.x / b, a.y / b, a.z / b);
    }
    
    public static LongVector3 mul(LongVector3 a, long b)
    {
        return new LongVector3(a.x * b, a.y * b, a.z * b);
    }
    
    public static int squaredNorm(LongVector3 a)
    {
        return (int)DracoUtils.dot(a, a);
    }
    
    public static long dot(LongVector3 a, LongVector3 b)
    {
        long ret = a.x * b.x + (a.y * b.y) + (a.z * b.z);
        return ret;
    }
    
    public static long absSum(IntSpan v)
    {
        long ret = 0L;
        for (int i = 0; i < v.size(); i++)
        {
            ret += Math.abs(v.get(i));
        }
        
        return ret;
    }
    
    public static long absSum(LongVector3 v)
    {
        long ret = Math.abs(v.x) + Math.abs(v.y) + Math.abs(v.z);
        return ret;
    }
    
    public static LongVector3 crossProduct(LongVector3 u, LongVector3 v)
    {
        LongVector3 r = new LongVector3();
        r.x = u.y * v.z - (u.z * v.y);
        r.y = u.z * v.x - (u.x * v.z);
        r.z = u.x * v.y - (u.y * v.x);
        return r;
    }
    
    public static DrakoException failed()
    {
        return new DrakoException();
    }
    
    static long intSqrt(long number)
    {
        if (number == 0L)
            return 0L;
        long actNumber = number;
        long squareRoot = 1L;
        while (actNumber >= 2L)
        {
            squareRoot *= 2L;
            actNumber /= 4L;
        }
        
        // Perform Newton's (or Babylonian) method to find the true floor(sqrt()).
        do
        {
            squareRoot = (squareRoot + (number / squareRoot)) / 2L;
        } while (squareRoot * squareRoot > number);
        
        return squareRoot;
    }
    
    public static boolean vecEquals(LongVector3 u, LongVector3 v)
    {
        return u.x == v.x && (u.y == v.y) && (u.z == v.z);
    }
    
    static int compare(byte[] data1, byte[] data2, int size)
    {
        return DracoUtils.compare(data1, 0, data2, 0, size);
    }
    
    static int compare(byte[] data1, int offset1, byte[] data2, int offset2, int size)
    {
        //TODO: range may out of bounds
        for (int i1 = offset1,  i2 = offset2,  i = 0; i < size; i++, i1++, i2++)
        {
            int v = Byte.compare(data1[i1], data2[i2]);
            if (v != 0)
                return v;
        }
        
        return 0;
    }
    
    static boolean isZero(float v)
    {
        return v == 0F;
    }
    
    
    static long hashCode(byte[] bytes, int offset, int size)
    {
        {
            final int p = 16777619;
            long hash = 2166136261L;
            int end = offset + size;
            for (int i = offset; i < end; i++)
            {
                hash = (hash ^ (0xff & bytes[i])) * p;
            }
            
            
            hash += hash << 13;
            hash ^= hash >> 7;
            hash += hash << 3;
            hash ^= hash >> 17;
            hash += hash << 5;
            
            return hash;
        }
        
    }
    
}
