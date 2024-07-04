package org.fileformat.drako;
import java.util.Arrays;
abstract class PointCloudDecoder
{    
    protected DecoderBuffer buffer;
    protected DracoPointCloud pointCloud;
    protected AttributesDecoder[] attributesDecoders;
    protected int geometryType;
    DracoLoadOptions options;
    // Map between attribute id and decoder id.
    // 
    private int[] attributeToDecoderMap;
    public int getBitstreamVersion()
    {
        return this.bitstreamVersion;
    }
    
    public void setBitstreamVersion(int value)
    {
        this.bitstreamVersion = value;
    }
    
    private int bitstreamVersion;
    protected PointCloudDecoder(int geometryType)
    {
        this.$initFields$();
        this.geometryType = geometryType;
    }
    
    public DracoPointCloud getPointCloud()
    {
        return pointCloud;
    }
    
    public int getGeometryType()
    {
        return geometryType;
    }
    
    public boolean decode(DracoHeader header, DecoderBuffer buffer, DracoPointCloud result, boolean decodeData)
    {
        this.buffer = buffer;
        this.pointCloud = result;
        this.setBitstreamVersion(header.version);
        if (header.version >= 13 && ((header.flags & (short)DracoHeader.METADATA_FLAG_MASK) == DracoHeader.METADATA_FLAG_MASK))
        {
            if (!this.decodeMetadata())
                return DracoUtils.failed();
        }
        
        if (!this.initializeDecoder())
            return DracoUtils.failed();
        if (!this.decodeGeometryData())
            return DracoUtils.failed();
        if (!this.decodePointAttributes(decodeData))
            return DracoUtils.failed();
        return true;
    }
    
    private boolean decodeMetadata()
    {
        MetadataDecoder decoder = new MetadataDecoder();
        GeometryMetadata metadata = decoder.decode(buffer);
        pointCloud.getMetadatas().add(metadata);
        return DracoUtils.failed();
    }
    
    public boolean setAttributesDecoder(int attDecoderId, AttributesDecoder decoder)
    {
        if (attDecoderId < 0)
            return false;
        if (attDecoderId >= attributesDecoders.length)
        {
            attributesDecoders = attributesDecoders == null ? new AttributesDecoder[attDecoderId + 1] : Arrays.copyOf(attributesDecoders, attDecoderId + 1);
        }
        
        attributesDecoders[attDecoderId] = decoder;
        return true;
    }
    
    public AttributesDecoder[] getAttributesDecoders()
    {
        return attributesDecoders;
    }
    
    protected boolean initializeDecoder()
    {
        return true;
    }
    
    protected boolean decodeGeometryData()
    {
        return true;
    }
    
    protected boolean decodePointAttributes(boolean decodeAttributeData)
    {
        byte numAttributesDecoders = 0;
        final byte[] ref0 = new byte[1];
        if (!buffer.decode3(ref0))
        {
            numAttributesDecoders = ref0[0];
            return DracoUtils.failed();
        }
        else
        {
            numAttributesDecoders = ref0[0];
        }
        
        //create attributes decoders
        for (int i = 0; i < (0xff & numAttributesDecoders); i++)
        {
            if (!this.createAttributesDecoder(i))
                return DracoUtils.failed();
        }
        
        //initialize all decoders
        for (AttributesDecoder dec : attributesDecoders)
        {
            if (!dec.initialize(this, pointCloud))
                return DracoUtils.failed();
        }
        
        //decode data
        for (AttributesDecoder dec : attributesDecoders)
        {
            if (!dec.decodeAttributesDecoderData(buffer))
                return DracoUtils.failed();
        }
        
        int maxAttrId = -1;
        
        for (int i = 0; i < attributesDecoders.length; ++i)
        {
            int numAttributes = attributesDecoders[i].getNumAttributes();
            for (int j = 0; j < numAttributes; ++j)
            {
                int attId = attributesDecoders[i].getAttributeId(j);
                maxAttrId = Math.max(attId, maxAttrId);
            }
            
        }
        
        this.attributeToDecoderMap = new int[maxAttrId + 1];
        for (int i = 0; i < attributesDecoders.length; ++i)
        {
            int numAttributes = attributesDecoders[i].getNumAttributes();
            for (int j = 0; j < numAttributes; ++j)
            {
                int attId = attributesDecoders[i].getAttributeId(j);
                attributeToDecoderMap[attId] = i;
            }
            
        }
        
        
        //decode attributes
        if (decodeAttributeData)
        {
            if (!this.decodeAllAttributes())
                return DracoUtils.failed();
        }
        
        if (!this.onAttributesDecoded())
            return DracoUtils.failed();
        return true;
    }
    
    protected abstract boolean createAttributesDecoder(int attrDecoderId);
    
    protected boolean onAttributesDecoded()
    {
        return true;
    }
    
    protected boolean decodeAllAttributes()
    {
        for (AttributesDecoder dec : attributesDecoders)
        {
            if (!dec.decodeAttributes(buffer))
                return DracoUtils.failed();
        }
        
        return true;
    }
    
    public DecoderBuffer getBuffer()
    {
        return buffer;
    }
    
    public void setBuffer(DecoderBuffer value)
    {
        this.buffer = value;
    }
    
    public PointAttribute getPortableAttribute(int attId)
    {
        if (attId < 0 || (attId >= pointCloud.getNumAttributes()))
            return null;
        int parentAttDecoderId = attributeToDecoderMap[attId];
        return attributesDecoders[parentAttDecoderId].getPortableAttribute(attId);
    }
    
    private void $initFields$()
    {
        try
        {
            attributesDecoders = new AttributesDecoder[0];
            options = new DracoLoadOptions();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
