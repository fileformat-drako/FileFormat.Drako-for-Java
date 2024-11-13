package dev.fileformat.drako;
import dev.fileformat.drako.BitUtils;
import dev.fileformat.drako.ByteSpan;
import dev.fileformat.drako.IntSpan;
import java.util.Arrays;
/**
 *  Heap version of Span.
 *
 */
public class DataBuffer
{    
    private int version;
    private byte[] data;
    private int length;
    private final boolean extendable;
    public int getVersion()
    {
        return version;
    }
    
    public int getCapacity()
    {
        return data == null ? 0 : data.length;
    }
    
    public void setCapacity(int value)
    {
        this.ensureCapacity(value);
    }
    
    public DataBuffer()
    {
        this.extendable = true;
    }
    
    public DataBuffer(byte[] data)
    {
        this.data = data;
        this.extendable = false;
        this.length = data.length;
    }
    
    public DataBuffer(ByteSpan data)
    {
        this.data = new byte[data.size()];
        this.length = data.size();
        data.copyTo(this.data);
        this.extendable = false;
    }
    
    public void write(int offset, byte[] data, int len)
    {
        this.write(offset, data, 0, len);
    }
    
    public void write(int offset, byte val)
    {
        this.setLength(offset + 1);
        this.data[offset] = val;
    }
    
    public void write(int offset, short val)
    {
        this.write3(offset, val);
    }
    
    public void write(int offset, int val)
    {
        this.setLength(offset + 4);
        Unsafe.putLE32(this.data, offset, val);
    }
    
    public void write(int offset, float val)
    {
        this.setLength(offset + 4);
        int uval = Unsafe.floatToUInt32(val);
        Unsafe.putLE32(this.data, offset, uval);
    }
    
    public void write(int offset, float[] data)
    {
        this.setLength(offset + (data.length * 4));
        Unsafe.toByteArray(data, 0, data.length, this.data, offset);
    }
    
    public void write(int offset, byte[] data)
    {
        this.write(offset, data, 0, data.length);
    }
    
    public void write(int offset, byte[] data, int start, int len)
    {
        version++;
        this.setLength(offset + len);
        System.arraycopy(data, start, this.data, offset, len);
    }
    
    public int read(int offset, byte[] result)
    {
        return this.read(offset, result, 0, result.length);
    }
    
    public int read(int offset, byte[] result, int len)
    {
        return this.read(offset, result, 0, len);
    }
    
    public int read(int offset, byte[] result, int start, int len)
    {
        System.arraycopy(data, offset, result, start, len);
        return len;
    }
    
    public float readFloat(int offset)
    {
        return BitUtils.getFloat(data, offset);
    }
    
    public int readInt(int offset)
    {
        return Unsafe.getLE32(data, offset);
        //return BitConverter.ToInt32(data, offset);
    }
    
    public byte get(int offset)
    {
        return data[offset];
    }
    
    public void set(int offset, byte value)
    {
        data[offset] = value;
    }
    
    private void ensureCapacity(int cap)
    {
        if (data != null && (cap <= data.length))
            return;
        if (!extendable)
            throw new IllegalStateException("Cannot extend the fixed-length data buffer.");
        int newCap = data == null ? 0 : data.length;
        while (newCap < cap)
        {
            newCap += 1024;
        }
        
        this.data = this.data == null ? new byte[newCap] : Arrays.copyOf(this.data, newCap);
    }
    
    public void clear()
    {
        this.setLength(0);
    }
    
    public int getLength()
    {
        return length;
    }
    
    public void setLength(int value)
    {
        this.length = value;
        this.ensureCapacity(value);
    }
    
    public byte[] getBuffer()
    {
        return data;
    }
    
    public byte[] toArray()
    {
        byte[] ret = new byte[length];
        System.arraycopy(data, 0, ret, 0, length);
        return ret;
    }
    
    public ByteSpan asSpan()
    {
        return ByteSpan.wrap(data).slice(0, length);
    }
    
    @Override
    public String toString()
    {
        return String.format("Length=%d, Capacity = %d", length, this.getCapacity());
    }
    
    IntSpan asIntArray()
    {
        return ByteSpan.wrap(data, 0, length).asIntSpan();
    }
    
    public void write2(int offset, int val)
    {
        this.write(offset, val);
    }
    
    public void write3(int offset, short val)
    {
        this.setLength(offset + 2);
        Unsafe.putLE16(this.data, offset, val);
    }
    
}
