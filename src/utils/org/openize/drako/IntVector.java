package org.openize.drako;
import com.aspose.csporter.helpers.HashBuilder;
import com.aspose.csporter.helpers.Struct;
import java.io.Serializable;
final class IntVector implements Struct<IntVector>, Serializable
{    
    public int x;
    public int y;
    public IntVector(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
    
    public static IntVector sub(IntVector a, IntVector b)
    {
        return new IntVector(a.x - b.x, a.y - b.y);
    }
    
    public static IntVector add(IntVector a, IntVector b)
    {
        return new IntVector(a.x + b.x, a.y + b.y);
    }
    
    public IntVector()
    {
    }
    
    private IntVector(IntVector other)
    {
        this.x = other.x;
        this.y = other.y;
    }
    
    @Override
    public IntVector clone()
    {
        return new IntVector(this);
    }
    
    @Override
    public void copyFrom(IntVector src)
    {
        if (src == null)
            return;
        this.x = src.x;
        this.y = src.y;
    }
    
    static final long serialVersionUID = -803036800L;
    @Override
    public int hashCode()
    {
        HashBuilder builder = new HashBuilder();
        builder.hash(this.x);
        builder.hash(this.y);
        return builder.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof IntVector))
            return false;
        IntVector rhs = (IntVector)obj;
        if (this.x != rhs.x)
            return false;
        if (this.y != rhs.y)
            return false;
        return true;
    }
    
}
