package org.openize.drako;
import com.aspose.csporter.helpers.AsposeUtils;
import com.aspose.csporter.helpers.BitUtils;
import com.aspose.csporter.helpers.ByteSpan;
import com.aspose.csporter.helpers.FloatSpan;
import com.aspose.csporter.helpers.Struct;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
public class PointAttribute extends GeometryAttribute
{    
    static final class ValueKey implements Comparable<ValueKey>, Struct<ValueKey>, Serializable
    {
        private byte[] data;
        private int offset;
        private int size;
        private long hashCode;
        public int getOffset()
        {
            return offset;
        }
        
        public ValueKey(PointAttribute attribute, byte[] data, int index)
        {
            this.size = attribute.getByteStride();
            this.offset = attribute.getByteOffset() + (attribute.getByteStride() * index);
            this.data = data;
            this.hashCode = DracoUtils.hashCode(data, offset, size);
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%f, ", BitUtils.getFloat(data, offset)));
            sb.append(String.format("%f, ", BitUtils.getFloat(data, offset + 4)));
            sb.append(String.format("%f", BitUtils.getFloat(data, offset + 8)));
            
            return sb.toString();
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof ValueKey))
                return false;
            return this.compareTo((ValueKey)obj) == 0;
        }
        
        public int compareTo(ValueKey rhs)
        {
            if (rhs.hashCode != hashCode)
                return -1;
            if (rhs.offset == offset)
                return -1;
            int ret = DracoUtils.compare(data, offset, data, rhs.offset, size);
            return ret;
        }
        
        public ValueKey()
        {
        }
        
        private ValueKey(ValueKey other)
        {
            this.data = other.data;
            this.offset = other.offset;
            this.size = other.size;
            this.hashCode = other.hashCode;
        }
        
        @Override
        public ValueKey clone()
        {
            return new ValueKey(this);
        }
        
        @Override
        public void copyFrom(ValueKey src)
        {
            if (src == null)
                return;
            this.data = src.data;
            this.offset = src.offset;
            this.size = src.size;
            this.hashCode = src.hashCode;
        }
        
        static final long serialVersionUID = 1665760457L;
        @Override
        public int hashCode()
        {
            return (int)hashCode;
        }
        
    }
    
    private static final int K_INVALID_ATTRIBUTE_VALUE_INDEX = -1;
    private int[] indicesMap;
    private int numUniqueEntries;
    private DataBuffer buffer;
    public DataBuffer getBuffer()
    {
        return buffer;
    }
    
    public int getNumUniqueEntries()
    {
        return numUniqueEntries;
    }
    
    public void setNumUniqueEntries(int value)
    {
        this.numUniqueEntries = value;
    }
    
    public PointAttribute()
    {
    }
    
    public PointAttribute(int type, int dataType, int components, boolean normalized, int byteStride, int byteOffset, DataBuffer buffer)
    {
        this.setAttributeType(type);
        this.setComponentsCount(components);
        this.setDataType(dataType);
        this.setNormalized(normalized);
        this.setByteOffset(byteOffset);
        this.buffer = buffer;
        if (byteStride == -1)
        {
            this.setByteStride(DracoUtils.dataTypeLength(dataType) * components);
        }
        else
        {
            this.setByteStride(byteStride);
        }
        
        if (buffer != null)
        {
            this.numUniqueEntries = buffer.getLength() / this.getByteStride();
        }
        
    }
    
    public PointAttribute(int type, int dataType, int components, boolean normalized)
    {
        this(type, dataType, components, normalized, -1, 0, (DataBuffer)null);
    }
    
    public PointAttribute(int type, int dataType, int components, boolean normalized, int byteStride)
    {
        this(type, dataType, components, normalized, byteStride, 0, (DataBuffer)null);
    }
    
    public PointAttribute(int type, int dataType, int components, boolean normalized, int byteStride, int byteOffset)
    {
        this(type, dataType, components, normalized, byteStride, byteOffset, (DataBuffer)null);
    }
    
    /**
     *  Wrap Vector2 to PointAttribute
     *
     * @param type Attribute's type
     * @param vectors Attribute data
     */
    public static PointAttribute wrap(int type, Vector2[] vectors)
    {
        byte[] bytes = new byte[4 * 2 * vectors.length];
        for (int i = 0,  p = 0; i < vectors.length; i++)
        {
            Unsafe.putLE32(bytes, p, Unsafe.floatToUInt32(vectors[i].x));
            Unsafe.putLE32(bytes, p + 4, Unsafe.floatToUInt32(vectors[i].y));
            p += 8;
        }
        
        return new PointAttribute(type, DataType.FLOAT32, 2, false, -1, 0, new DataBuffer(bytes));
    }
    
    /**
     *  Wrap Vector3 to PointAttribute
     *
     * @param type Attribute's type
     * @param vectors Attribute data
     */
    public static PointAttribute wrap(int type, Vector3[] vectors)
    {
        byte[] bytes = new byte[4 * 3 * vectors.length];
        for (int i = 0,  p = 0; i < vectors.length; i++)
        {
            Unsafe.putLE32(bytes, p, Unsafe.floatToUInt32(vectors[i].x));
            Unsafe.putLE32(bytes, p + 4, Unsafe.floatToUInt32(vectors[i].y));
            Unsafe.putLE32(bytes, p + 8, Unsafe.floatToUInt32(vectors[i].z));
            p += 12;
        }
        
        return new PointAttribute(type, DataType.FLOAT32, 3, false, -1, 0, new DataBuffer(bytes));
    }
    /**
     *  Wrap float array to PointAttribute
     *
     * @param type Attribute's type
     * @param components Number of components
     * @param values Attribute data
     */
    public static PointAttribute wrap(int type, int components, float[] values)
    {
        byte[] bytes = new byte[4 * values.length];
        for (int i = 0,  p = 0; i < values.length; i++)
        {
            Unsafe.putLE32(bytes, p, Unsafe.floatToUInt32(values[i]));
            Unsafe.putLE32(bytes, p + 4, Unsafe.floatToUInt32(values[i]));
            p += 4;
        }
        return new PointAttribute(type, DataType.FLOAT32, components, false, -1, 0, new DataBuffer(bytes));
    }

    /**
     *  Fills outData with the raw value of the requested attribute entry.
     *  outData must be at least byteStride long.
     *
     */
    public void getValue(int attIndex, byte[] outData)
    {
        int bytePos = this.getByteOffset() + (this.getByteStride() * attIndex);
        buffer.read(bytePos, outData, this.getByteStride());
    }
    
    public int getBytePos(int attIndex)
    {
        return this.getByteOffset() + (this.getByteStride() * attIndex);
    }
    
    public void getValue(int attIndex, short[] v)
    {
        int bytePos = this.getByteOffset() + (this.getByteStride() * attIndex);
        byte[] data = buffer.getBuffer();
        for (int i = 0; i < v.length; i++)
        {
            v[i] = Unsafe.getLE16(data, bytePos);
            bytePos += 2;
        }
        
    }
    
    public void getValue(int attIndex, int[] v)
    {
        int bytePos = this.getByteOffset() + (this.getByteStride() * attIndex);
        byte[] data = buffer.getBuffer();
        for (int i = 0; i < v.length; i++)
        {
            v[i] = Unsafe.getLE32(data, bytePos);
            bytePos += 4;
        }
        
    }
    
    public void getValue(int attIndex, float[] v)
    {
        int bytePos = this.getByteOffset() + (this.getByteStride() * attIndex);
        for (int i = 0; i < v.length; i++)
        {
            v[i] = buffer.readFloat(bytePos);
            bytePos += 4;
        }
        
    }
    
    public void getValue(int attIndex, FloatSpan v)
    {
        int bytePos = this.getByteOffset() + (this.getByteStride() * attIndex);
        for (int i = 0; i < v.size(); i++)
        {
            v.put(i, buffer.readFloat(bytePos));
            bytePos += 4;
        }
        
    }
    
    public Vector3 getValueAsVector3(int attIndex)
    {
        int bytePos = this.getByteOffset() + (this.getByteStride() * attIndex);
        Vector3 v = new Vector3();
        v.x = buffer.readFloat(bytePos);
        bytePos += 4;
        v.y = buffer.readFloat(bytePos);
        bytePos += 4;
        v.z = buffer.readFloat(bytePos);
        return v;
    }
    
    /**
     *  Prepares the attribute storage for the specified number of entries.
     *
     * @param numAttributeValues 
     */
    public boolean reset(int numAttributeValues)
    {
        if (buffer == null)
        {
            this.buffer = new DataBuffer();
        }
        
        int entrySize = DracoUtils.dataTypeLength(this.getDataType()) * this.getComponentsCount();
        buffer.setCapacity(numAttributeValues * entrySize);
        buffer.setLength(numAttributeValues * entrySize);
        this.setByteStride(entrySize);
        this.setByteOffset(0);
        // Assign the new buffer to the parent attribute.
        this.numUniqueEntries = numAttributeValues;
        return true;
    }
    
    public int mappedIndex(int pointIndex)
    {
        if (indicesMap == null)
            return pointIndex;
        return indicesMap[pointIndex];
    }
    
    /**
     *  Sets the new number of unique attribute entries for the attribute.
     *
     */
    public void resize(int newNumUniqueEntries)
    {
        this.numUniqueEntries = newNumUniqueEntries;
    }
    
    /**
     *  Functions for setting the type of mapping between point indices and
     *  attribute entry ids.
     *  This function sets the mapping to implicit, where point indices are equal
     *  to attribute entry indices.
     *
     */
    public boolean getIdentityMapping()
    {
        return indicesMap == null;
    }
    
    /**
     *  Functions for setting the type of mapping between point indices and
     *  attribute entry ids.
     *  This function sets the mapping to implicit, where point indices are equal
     *  to attribute entry indices.
     *
     * @param value New value
     */
    public void setIdentityMapping(boolean value)
    {
        if (value)
        {
            this.indicesMap = null;
        }
        else if (indicesMap == null)
        {
            this.indicesMap = new int[0];
        }
        
    }
    
    public int[] getIndicesMap()
    {
        return indicesMap;
    }
    
    AttributeTransformData getAttributeTransformData()
    {
        return this.attributeTransformData;
    }
    
    void setAttributeTransformData(AttributeTransformData value)
    {
        this.attributeTransformData = value;
    }
    
    private AttributeTransformData attributeTransformData;
    /**
     *  This function sets the mapping to be explicitly using the indicesMap
     *  array that needs to be initialized by the caller.
     *
     */
    public void setExplicitMapping(int numPoints)
    {
        int fillStart = 0;
        if (indicesMap == null)
        {
            this.indicesMap = new int[numPoints];
        }
        else
        {
            fillStart = indicesMap.length;
            indicesMap = indicesMap == null ? new int[numPoints] : Arrays.copyOf(indicesMap, numPoints);
        }
        
        for (int i = fillStart; i < numPoints; i++)
        {
            indicesMap[i] = K_INVALID_ATTRIBUTE_VALUE_INDEX;
        }
        
    }
    
    /**
     *  Set an explicit map entry for a specific point index.
     *
     */
    public void setPointMapEntry(int pointIndex, int entryIndex)
    {
        indicesMap[pointIndex] = entryIndex;
    }
    
    public void convertValue(int attId, int[] i)
    {
        int pos = this.getByteOffset() + (this.getByteStride() * attId);
        i[0] = buffer.readInt(pos);
    }
    
    LongVector3 convertValue(int attId)
    {
        int pos = this.getByteOffset() + (this.getByteStride() * attId);
        if (this.getDataType() == DataType.INT32 || (this.getDataType() == DataType.UINT32))
        {
            long x = buffer.readInt(pos);
            long y = buffer.readInt(pos + 4);
            long z = buffer.readInt(pos + 8);
            return new LongVector3(x, y, z);
        }
        else
            throw new UnsupportedOperationException("Unsupported type cast");
    }
    
    public void deduplicateValues()
    {
        int offset = this.getByteOffset();
        int stride = this.getByteStride();
        HashMap<ValueKey, Integer> indiceMap = new HashMap<ValueKey, Integer>();
        HashMap<Integer, Integer> valueMap = new HashMap<Integer, Integer>();
        int uniqueValues = 0;
        byte[] tmp = buffer.toArray();
        final Integer[] ref0 = new Integer[1];
        for (int i = 0; i < this.getNumUniqueEntries(); i++, offset += stride)
        {
            ValueKey k = new ValueKey(this, tmp, i);
            int idx;
            if (!AsposeUtils.tryGetValue(indiceMap, k, ref0))
            {
                idx = ref0[0] == null ? 0 : ref0[0];
                this.setAttributeValue(uniqueValues, this.getBuffer().getBuffer(), k.getOffset());
                idx = uniqueValues++;
                indiceMap.put(Struct.byVal(k), idx);
            }
            else
            {
                idx = ref0[0] == null ? 0 : ref0[0];
            }
            
            valueMap.put(i, idx);
        }
        
        if (uniqueValues == numUniqueEntries)
            return;
        //cannot deduplicate values
        if (this.getIdentityMapping())
        {
            this.setExplicitMapping(numUniqueEntries);
            for (int i = 0; i < numUniqueEntries; i++)
            {
                this.setPointMapEntry(i, valueMap.get(i));
            }
            
        }
        else
        {
            for (int i = 0; i < this.indicesMap.length; i++)
            {
                this.setPointMapEntry(i, valueMap.get(this.indicesMap[i]));
            }
            
        }
        
        this.numUniqueEntries = uniqueValues;
        
    }
    
    /**
     *  Copy raw bytes from buffer with given offset to the attribute's internal buffer at specified element index
     *
     * @param index 
     * @param buffer 
     * @param offset 
     */
    public void setAttributeValue(int index, byte[] buffer, int offset)
    {
        int dstOffset = this.getByteOffset() + (this.getByteStride() * index);
        byte[] dst = this.buffer.getBuffer();
        System.arraycopy(buffer, offset, dst, dstOffset, this.getByteStride());
    }
    
    void setAttributeValue(int index, int[] vals)
    {
        int offset = this.getByteOffset() + (this.getByteStride() * index);
        byte[] dst = this.buffer.getBuffer();
        for (int i = 0; i < vals.length; i++)
        {
            Unsafe.putLE32(dst, offset, vals[i]);
            offset += 4;
        }
        
    }
    
    void setAttributeValue(int index, short[] vals)
    {
        int offset = this.getByteOffset() + (this.getByteStride() * index);
        byte[] dst = this.buffer.getBuffer();
        for (int i = 0; i < vals.length; i++)
        {
            Unsafe.putLE16(dst, offset, vals[i]);
            offset += 2;
        }
        
    }
    
    @Override
    public void copyFrom(GeometryAttribute attr)
    {
        
        if (buffer == null)
        {
            // If the destination attribute doesn't have a valid buffer, create it.
            this.buffer = new DataBuffer();
            this.setByteStride(0);
            this.setByteOffset(0);
        }
        
        
        super.copyFrom(attr);
        PointAttribute pa = (PointAttribute)attr;
        this.numUniqueEntries = pa.numUniqueEntries;
        if (pa.indicesMap != null)
        {
            this.indicesMap = (int[])(pa.indicesMap.clone());
        }
        
        /*
            if (pa.attributeTransformData)        
            {        
                attributeTransformData = pa.attributeTransformData.Clone();        
            }        
            else        
            {        
                attributeTransformData = null;        
            }        
            */    }
    
    ByteSpan getAddress(int attIndex)
    {
        int byte_pos = this.getBytePos(attIndex);
        return buffer.asSpan().slice(byte_pos);
    }
    
}
