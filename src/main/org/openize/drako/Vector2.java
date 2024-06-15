package org.openize.drako;
import com.aspose.csporter.helpers.HashBuilder;
import com.aspose.csporter.helpers.Struct;
import java.io.Serializable;
public final class Vector2 implements Struct<Vector2>, Serializable
{    
    public float x;
    public float y;
    public Vector2(float x, float y)
    {
        this.x = x;
        this.y = y;
    }
    
    public static boolean op_eq(Vector2 a, Vector2 b)
    {
        return a.x == b.x && (a.y == b.y);
    }
    
    public static boolean op_ne(Vector2 a, Vector2 b)
    {
        return a.x != b.x || (a.y != b.y);
    }
    
    public static float dot(Vector2 v1, Vector2 v2)
    {
        return v1.x * v2.x + (v1.y * v2.y);
    }
    
    public float lengthSquared()
    {
        return this.x * this.x + (this.y * this.y);
    }
    
    public static Vector2 mul(Vector2 v1, float v2)
    {
        return new Vector2(v1.x * v2, v1.y * v2);
    }
    
    public static Vector2 sub(Vector2 v1, Vector2 v2)
    {
        return new Vector2(v1.x - v2.x, v1.y - v2.y);
    }
    
    public Vector2()
    {
    }
    
    private Vector2(Vector2 other)
    {
        this.x = other.x;
        this.y = other.y;
    }
    
    @Override
    public Vector2 clone()
    {
        return new Vector2(this);
    }
    
    @Override
    public void copyFrom(Vector2 src)
    {
        if (src == null)
            return;
        this.x = src.x;
        this.y = src.y;
    }
    
    static final long serialVersionUID = -1206975156L;
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
        if (!(obj instanceof Vector2))
            return false;
        Vector2 rhs = (Vector2)obj;
        if (this.x != rhs.x)
            return false;
        if (this.y != rhs.y)
            return false;
        return true;
    }
    
}
