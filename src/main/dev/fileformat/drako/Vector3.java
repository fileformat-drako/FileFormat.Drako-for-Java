package dev.fileformat.drako;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
public final class Vector3 implements Struct<Vector3>, Serializable
{    
    public float x;
    public float y;
    public float z;
    public Vector3(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public static float dot(Vector3 v1, Vector3 v2)
    {
        return v1.x * v2.x + (v1.y * v2.y) + (v1.z * v2.z);
    }
    
    public float lengthSquared()
    {
        return this.x * this.x + (this.y * this.y) + (this.z * this.z);
    }
    
    public static Vector3 mul(Vector3 v1, float v2)
    {
        return new Vector3(v1.x * v2, v1.y * v2, v1.z * v2);
    }
    
    public static Vector3 sub(Vector3 v1, Vector3 v2)
    {
        return new Vector3(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
    }
    
    public Vector3()
    {
    }
    
    private Vector3(Vector3 other)
    {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }
    
    @Override
    public Vector3 clone()
    {
        return new Vector3(this);
    }
    
    @Override
    public void copyFrom(Vector3 src)
    {
        if (src == null)
            return;
        this.x = src.x;
        this.y = src.y;
        this.z = src.z;
    }
    
    static final long serialVersionUID = 966384595L;
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
        if (!(obj instanceof Vector3))
            return false;
        Vector3 rhs = (Vector3)obj;
        if (this.x != rhs.x)
            return false;
        if (this.y != rhs.y)
            return false;
        if (this.z != rhs.z)
            return false;
        return true;
    }
    
}
