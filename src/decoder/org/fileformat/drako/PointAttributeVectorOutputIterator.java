package org.fileformat.drako;
// Output iterator that is used to decode values directly into the data buffer
// of the modified PointAttribute.
// The extension of this iterator beyond the DT_UINT32 concerns itself only with
// the size of the data for efficiency, not the type.  DataType is conveyed in
// but is an unused field populated for any future logic/special casing.
// DT_UINT32 and all other 4-byte types are naturally supported from the size of
// data in the kd tree encoder.  DT_UINT16 and DT_UINT8 are supported by way
// of byte copies into a temporary memory buffer.
// 
class PointAttributeVectorOutputIterator
{    
    // preallocated memory for buffering different data sizes.  Never reallocated.
    // 
    byte[] data_;
    private PointAttributeInfo[] attributes_;
    int point_id_;
    public PointAttributeVectorOutputIterator(PointAttributeInfo[] atts)
    {
        this.attributes_ = atts;
        int required_decode_bytes = 0;
        for (int index = 0; index < attributes_.length; index++)
        {
            PointAttributeInfo att = attributes_[index];
            required_decode_bytes = Math.max(required_decode_bytes, att.data_size * att.num_components);
        }
        
        
        this.data_ = new byte[required_decode_bytes];
    }
    
    public void next()
    {
        ++point_id_;
    }
    
    // We do not want to do ANY copying of this constructor so this particular
    // operator is disabled for performance reasons.
    // Self operator++(int) {
    // Self copy = *this;
    // ++point_id_;
    // return copy;
    // }
    // Still needed in some cases.
    // TODO(hemmer): remove.
    // hardcoded to 3 based on legacy usage.
    // 
    public void setTriple(byte[] val)
    {
        PointAttributeInfo att = attributes_[0];
        PointAttribute attribute = att.attribute;
        attribute.setAttributeValue(attribute.mappedIndex(point_id_), val, att.offset_dimensionality);
    }
    
    // Additional operator taking std::vector as argument.
    // 
    private byte[] tmp;
    public void set(int[] val)
    {
        byte[] bytes = tmp == null || (tmp.length != val.length) ? new byte[val.length * 4] : tmp;
        this.tmp = bytes;
        int offset = 0;
        for (int i = 0; i < val.length; i++)
        {
            Unsafe.putLE32(bytes, offset, val[i]);
            offset += 4;
        }
        
        this.set(bytes);
    }
    
    public void set(byte[] val)
    {
        for (int index = 0; index < attributes_.length; index++)
        {
            PointAttributeInfo att = attributes_[index];
            PointAttribute attribute = att.attribute;
            byte[] src = val;
            int src_offset = att.offset_dimensionality;
            if (att.data_size != 4)
            {
                int dst_offset = 0;
                for (int i = 0; i < att.num_components; i += 1, dst_offset += att.data_size)
                {
                    System.arraycopy(src, src_offset + i, data_, dst_offset, att.data_size);
                }
                
                
                // redirect to copied data
                src = data_;
                src_offset = 0;
            }
            
            int avi = attribute.mappedIndex(point_id_);
            if (avi >= attribute.getNumUniqueEntries())
                return;
            attribute.setAttributeValue(avi, src, 0);
        }
        
    }
    
}
