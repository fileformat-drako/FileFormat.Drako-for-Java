package org.openize.drako;
/**
 *  The class provides access to a specific attribute which is stored in a
 *  DataBuffer, such as normals or coordinates. However, the GeometryAttribute
 *  class does not own the buffer and the buffer itself may store other data
 *  unrelated to this attribute (such as data for other attributes in which case
 *  we can have multiple GeometryAttributes accessing one buffer). Typically,
 *  all attributes for a point (or corner, face) are stored in one block, which
 *  is advantageous in terms of memory access. The length of the entire block is
 *  given by the byteStride, the position where the attribute starts is given by
 *  the byteOffset, the actual number of bytes that the attribute occupies is
 *  given by the dataType and the number of components.
 *
 */
public class GeometryAttribute
{    
    public int getComponentsCount()
    {
        return this.componentsCount;
    }
    
    public void setComponentsCount(int value)
    {
        this.componentsCount = value;
    }
    
    private int componentsCount;
    public int getDataType()
    {
        return this.dataType;
    }
    
    public void setDataType(int value)
    {
        this.dataType = value;
    }
    
    private int dataType;
    public boolean getNormalized()
    {
        return this.normalized;
    }
    
    public void setNormalized(boolean value)
    {
        this.normalized = value;
    }
    
    private boolean normalized;
    public int getByteStride()
    {
        return this.byteStride;
    }
    
    public void setByteStride(int value)
    {
        this.byteStride = value;
    }
    
    private int byteStride;
    public int getByteOffset()
    {
        return this.byteOffset;
    }
    
    public void setByteOffset(int value)
    {
        this.byteOffset = value;
    }
    
    private int byteOffset;
    public int getAttributeType()
    {
        return this.attributeType;
    }
    
    public void setAttributeType(int value)
    {
        this.attributeType = value;
    }
    
    private int attributeType;
    /**
     */
    public short getUniqueId()
    {
        return this.uniqueId;
    }
    
    /**
     * @param value New value
     */
    public void setUniqueId(short value)
    {
        this.uniqueId = value;
    }
    
    private short uniqueId;
    @Override
    public String toString()
    {
        return String.format("#%4$d %1$d : %2$d[%3$d]", this.getAttributeType(), this.getDataType(), this.getComponentsCount(), this.getUniqueId());
    }
    
    public void copyFrom(GeometryAttribute attr)
    {
        this.setComponentsCount(attr.getComponentsCount());
        this.setDataType(attr.getDataType());
        this.setNormalized(attr.getNormalized());
        this.setByteStride(attr.getByteStride());
        this.setByteOffset(attr.getByteOffset());
        this.setAttributeType(attr.getAttributeType());
        this.setUniqueId(attr.getUniqueId());
    }
    
    
}
