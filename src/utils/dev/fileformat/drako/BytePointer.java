package dev.fileformat.drako;
import dev.fileformat.drako.AsposeUtils;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
/**
 *  This simulates a byte pointer used in Draco implementation, also makes it easier to be ported to Java using CsPorter made by Lex Chou.
 *  I've benchmarked this, it's okay to be used, I'll replace this by Span later.
 *  .NET Version: 8.0
 *  Build Type: Release
 *  Test memory block: 1GB
 *  Random read/write counts: 10M
 *  Test PC Setup: AMD 3700X, 96G Mem
 * 
 *  All benchmark tests are pre-warmed in JIT and memory access.
 *  The benchmark result:
 * 
 *  Array Write         00:00:00.2786953
 *  Unsafe Write        00:00:00.2074192
 *  Span Write          00:00:00.1917591
 *  BytePointer Write   00:00:00.2552571
 *  Memory Slice Write  00:00:00.2477228
 *  Memory Span Write   00:00:00.2477730
 * 
 *  Array Read          00:00:00.3126299
 *  Unsafe Read         00:00:00.3272010
 *  Span Read           00:00:00.3320909
 *  BytePointer Read    00:00:00.3381226
 *  Memory Span Read    00:00:00.3725757
 *  Memory Slice Read   00:00:00.6809910
 *
 */
final class BytePointer implements Struct<BytePointer>, Serializable
{    
    private byte[] data;
    private int offset;
    public BytePointer(byte[] data)
    {
        this.data = data;
        this.offset = 0;
    }
    
    public BytePointer(byte[] data, int offset)
    {
        this.data = data;
        this.offset = offset;
    }
    
    public int getOffset()
    {
        return offset;
    }
    
    public byte[] getBaseData()
    {
        return data;
    }
    
    public byte get(int offset)
    {
        return data[this.offset + offset];
    }
    
    public void set(int offset, byte value)
    {
        data[this.offset + offset] = value;
    }
    
    public byte toByte()
    {
        return data[this.offset];
    }
    
    public short toUInt16LE()
    {
        return Unsafe.getLE16(data, this.offset);
    }
    
    public short toUInt16LE(int offset)
    {
        return Unsafe.getLE16(data, this.offset + offset);
    }
    
    public int toUInt24LE(int offset)
    {
        return Unsafe.getLE24(data, this.offset + offset);
    }
    
    public int toUInt32LE(int offset)
    {
        return Unsafe.getLE32(data, this.offset + offset);
    }
    
    public long toUInt64LE(int offset)
    {
        return Unsafe.getLE64(data, this.offset + offset);
    }
    
    public float toSingle(int offset)
    {
        return Unsafe.getFloat(data, this.offset + offset);
    }
    
    public boolean isOverflow(int offset)
    {
        int p = offset + this.offset;
        return p >= data.length || (p < 0);
    }
    
    public static BytePointer add(BytePointer ptr, int offset)
    {
        return new BytePointer(ptr.data, ptr.offset + offset);
    }
    
    public void copy(int srcOffset, byte[] dst, int dstOffset, int len)
    {
        System.arraycopy(data, this.offset + srcOffset, dst, dstOffset, len);
    }
    
    @Override
    public String toString()
    {
        return String.format("byte[%d]+%d", data == null ? 0 : data.length, offset);
    }
    
    public BytePointer()
    {
    }
    
    private BytePointer(BytePointer other)
    {
        this.data = other.data;
        this.offset = other.offset;
    }
    
    @Override
    public BytePointer clone()
    {
        return new BytePointer(this);
    }
    
    @Override
    public void copyFrom(BytePointer src)
    {
        if (src == null)
            return;
        this.data = src.data;
        this.offset = src.offset;
    }
    
    static final long serialVersionUID = 1821034352L;
    @Override
    public int hashCode()
    {
        HashBuilder builder = new HashBuilder();
        builder.hash(this.data);
        builder.hash(this.offset);
        return builder.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof BytePointer))
            return false;
        BytePointer rhs = (BytePointer)obj;
        if (!AsposeUtils.equals(this.data, rhs.data))
            return false;
        if (this.offset != rhs.offset)
            return false;
        return true;
    }
    
}
