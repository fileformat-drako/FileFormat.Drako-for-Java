package dev.fileformat.drako;
/**
 *  Base class for decoding one or more attributes that were encoded with a
 *  matching AttributesEncoder. This base class provides only the basic interface
 *  that is used by the PointCloudDecoder. The actual encoding must be
 *  implemented in derived classes using the DecodeAttributes() method.
 *
 */
abstract class AttributesDecoder
{    
    private int[] pointAttributeIds;
    // Map between point attribute id and the local id (i.e., the inverse of the
    // |point_attribute_ids_|.
    // 
    private IntList point_attribute_to_local_id_map_;
    private PointCloudDecoder pointCloudDecoder;
    private DracoPointCloud pointCloud;
    public boolean initialize(PointCloudDecoder decoder, DracoPointCloud pointCloud)
    {
        this.pointCloud = pointCloud;
        this.pointCloudDecoder = decoder;
        return true;
    }
    
    public boolean decodeAttributesDecoderData(DecoderBuffer inBuffer)
    {
        int numAttributes;
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        final byte[] ref2 = new byte[1];
        final byte[] ref3 = new byte[1];
        final byte[] ref4 = new byte[1];
        final byte[] ref5 = new byte[1];
        final short[] ref6 = new short[1];
        final short[] ref7 = new short[1];
        if (pointCloudDecoder.getBitstreamVersion() < 20)
        {
            if (!inBuffer.decode6(ref0))
            {
                numAttributes = ref0[0];
                return DracoUtils.failed();
            }
            else
            {
                numAttributes = ref0[0];
            }
            
        }
        else
        {
            int n;
            if (!Decoding.decodeVarint(ref1, inBuffer))
            {
                n = ref1[0];
                return DracoUtils.failed();
            }
            else
            {
                n = ref1[0];
            }
            
            numAttributes = n;
        }
        
        
        if (numAttributes <= 0)
            return DracoUtils.failed();
        
        this.pointAttributeIds = new int[numAttributes];
        DracoPointCloud pc = pointCloud;
        int version = pointCloudDecoder.getBitstreamVersion();
        for (int i = 0; i < numAttributes; ++i)
        {
            byte attType;
            byte dataType;
            byte componentsCount;
            byte normalized;
            if (!inBuffer.decode3(ref2))
            {
                attType = ref2[0];
                return DracoUtils.failed();
            }
            else
            {
                attType = ref2[0];
            }
            
            if (!inBuffer.decode3(ref3))
            {
                dataType = ref3[0];
                return DracoUtils.failed();
            }
            else
            {
                dataType = ref3[0];
            }
            
            if (!inBuffer.decode3(ref4))
            {
                componentsCount = ref4[0];
                return DracoUtils.failed();
            }
            else
            {
                componentsCount = ref4[0];
            }
            
            if (!inBuffer.decode3(ref5))
            {
                normalized = ref5[0];
                return DracoUtils.failed();
            }
            else
            {
                normalized = ref5[0];
            }
            
            int dracoDt = (int)(0xff & dataType);
            PointAttribute ga = new PointAttribute();
            ga.setAttributeType((int)(0xff & attType));
            ga.setComponentsCount(0xff & componentsCount);
            ga.setDataType(dracoDt);
            ga.setNormalized((0xff & normalized) > 0);
            ga.setByteStride(DracoUtils.dataTypeLength(dracoDt) * (0xff & componentsCount));
            short customId;
            if (version < 13)
            {
                if (!inBuffer.decode(ref6))
                {
                    customId = ref6[0];
                    return DracoUtils.failed();
                }
                else
                {
                    customId = ref6[0];
                }
                
            }
            else
            {
                Decoding.decodeVarint(ref7, inBuffer);
                customId = ref7[0];
            }
            
            
            ga.setUniqueId(customId);
            int attId = pc.addAttribute(ga);
            pointAttributeIds[i] = attId;
            
            // Update the inverse map.
            if (attId >= point_attribute_to_local_id_map_.getCount())
            {
                point_attribute_to_local_id_map_.resize(attId + 1, -1);
            }
            
            point_attribute_to_local_id_map_.set(attId, i);
        }
        
        
        return true;
    }
    
    public boolean decodeAttributes(DecoderBuffer buffer)
    {
        if (!this.decodePortableAttributes(buffer))
            return DracoUtils.failed();
        if (!this.decodeDataNeededByPortableTransforms(buffer))
            return DracoUtils.failed();
        if (!this.transformAttributesToOriginalFormat())
            return DracoUtils.failed();
        return true;
    }
    
    protected abstract boolean decodePortableAttributes(DecoderBuffer buffer);
    
    protected boolean decodeDataNeededByPortableTransforms(DecoderBuffer buffer)
    {
        return true;
    }
    
    protected boolean transformAttributesToOriginalFormat()
    {
        return true;
    }
    
    public int getAttributeId(int i)
    {
        return pointAttributeIds[i];
    }
    
    public int getNumAttributes()
    {
        return pointAttributeIds.length;
    }
    
    public PointCloudDecoder getDecoder()
    {
        return pointCloudDecoder;
    }
    
    public abstract PointAttribute getPortableAttribute(int attId);
    
    protected int getLocalIdForPointAttribute(int point_attribute_id)
    {
        int id_map_size = point_attribute_to_local_id_map_.getCount();
        if (point_attribute_id >= id_map_size)
            return -1;
        return point_attribute_to_local_id_map_.get(point_attribute_id);
    }
    
    public AttributesDecoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            pointAttributeIds = new int[0];
            point_attribute_to_local_id_map_ = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
