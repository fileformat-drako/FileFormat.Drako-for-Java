package dev.fileformat.drako;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
final class LongVector3 implements Struct<LongVector3>, Serializable
{    
    public long x;
    public long y;
    public long z;
    public LongVector3(long x, long y)
    {
        this.x = x;
        this.y = y;
        this.z = 0L;
    }
    
    public LongVector3(long x, long y, long z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public LongVector3()
    {
    }
    
    private LongVector3(LongVector3 other)
    {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }
    
    @Override
    public LongVector3 clone()
    {
        return new LongVector3(this);
    }
    
    @Override
    public void copyFrom(LongVector3 src)
    {
        if (src == null)
            return;
        this.x = src.x;
        this.y = src.y;
        this.z = src.z;
    }
    
    static final long serialVersionUID = -1377282380L;
    @Override
    public int hashCode()
    {
        HashBuilder builder = new HashBuilder();
        builder.hash(this.x);
        builder.hash(this.y);
        builder.hash(this.z);
        return builder.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof LongVector3))
            return false;
        LongVector3 rhs = (LongVector3)obj;
        if (this.x != rhs.x)
            return false;
        if (this.y != rhs.y)
            return false;
        if (this.z != rhs.z)
            return false;
        return true;
    }
    
}
