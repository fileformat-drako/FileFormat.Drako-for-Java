package org.fileformat.drako;
class AttributeTransformData
{    
    public int transformType;
    private DataBuffer dataBuffer;
    public int getInt(int byteOffset)
    {
        return dataBuffer.readInt(byteOffset);
    }
    
    public float getFloat(int byteOffset)
    {
        return dataBuffer.readFloat(byteOffset);
    }
    
    public void appendValue(int value)
    {
        dataBuffer.write2(dataBuffer.getLength(), value);
    }
    
    public void appendValue(float value)
    {
        dataBuffer.write(dataBuffer.getLength(), value);
    }
    
    public AttributeTransformData()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            transformType = AttributeTransformType.INVALID_TRANSFORM;
            dataBuffer = new DataBuffer();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
