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
    public void initialize(PointCloudDecoder decoder, DracoPointCloud pointCloud)
    {
        this.pointCloud = pointCloud;
        this.pointCloudDecoder = decoder;
    }
    
    public void decodeAttributesDecoderData(DecoderBuffer inBuffer)
        throws DrakoException
    {
        int numAttributes;
        if (pointCloudDecoder.getBitstreamVersion() < 20)
        {
            numAttributes = inBuffer.decodeI32();
        }
        else
        {
            int n = Decoding.decodeVarintU32(inBuffer);
            numAttributes = n;
        }
        
        
        if (numAttributes <= 0)
            throw DracoUtils.failed();
        
        this.pointAttributeIds = new int[numAttributes];
        DracoPointCloud pc = pointCloud;
        int version = pointCloudDecoder.getBitstreamVersion();
        for (int i = 0; i < numAttributes; ++i)
        {
            byte attType;
            byte dataType;
            byte componentsCount;
            byte normalized;
            attType = inBuffer.decodeU8();
            dataType = inBuffer.decodeU8();
            componentsCount = inBuffer.decodeU8();
            normalized = inBuffer.decodeU8();
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
                customId = inBuffer.decodeU16();
            }
            else
            {
                customId = Decoding.decodeVarintU16(inBuffer);
            }
            
            int attId = pc.addAttribute(ga);
            ga.setUniqueId(customId);
            pointAttributeIds[i] = attId;
            
            // Update the inverse map.
            if (attId >= point_attribute_to_local_id_map_.getCount())
            {
                point_attribute_to_local_id_map_.resize(attId + 1, -1);
            }
            
            point_attribute_to_local_id_map_.set(attId, i);
        }
        
    }
    
    public void decodeAttributes(DecoderBuffer buffer)
        throws DrakoException
    {
        this.decodePortableAttributes(buffer);
        this.decodeDataNeededByPortableTransforms(buffer);
        this.transformAttributesToOriginalFormat();
    }
    
    protected abstract void decodePortableAttributes(DecoderBuffer buffer)
        throws DrakoException;
    
    protected void decodeDataNeededByPortableTransforms(DecoderBuffer buffer)
        throws DrakoException
    {
    }
    
    protected void transformAttributesToOriginalFormat()
        throws DrakoException
    {
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
