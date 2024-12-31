package dev.fileformat.drako;
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
    
    public void decode(DracoHeader header, DecoderBuffer buffer, DracoPointCloud result, boolean decodeData)
        throws DrakoException
    {
        this.buffer = buffer;
        this.pointCloud = result;
        this.setBitstreamVersion(header.version);
        if (header.version >= 13 && ((0xffff & header.flags & (short)DracoHeader.METADATA_FLAG_MASK) == DracoHeader.METADATA_FLAG_MASK))
        {
            this.decodeMetadata();
        }
        
        this.initializeDecoder();
        this.decodeGeometryData();
        this.decodePointAttributes(decodeData);
    }
    
    private void decodeMetadata()
        throws DrakoException
    {
        MetadataDecoder decoder = new MetadataDecoder();
        GeometryMetadata metadata = decoder.decode(buffer);
        pointCloud.getMetadatas().add(metadata);
    }
    
    public void setAttributesDecoder(int attDecoderId, AttributesDecoder decoder)
        throws DrakoException
    {
        if (attDecoderId < 0)
            throw DracoUtils.failed();
        if (attDecoderId >= attributesDecoders.length)
        {
            attributesDecoders = attributesDecoders == null ? new AttributesDecoder[attDecoderId + 1] : Arrays.copyOf(attributesDecoders, attDecoderId + 1);
        }
        
        attributesDecoders[attDecoderId] = decoder;
    }
    
    public AttributesDecoder[] getAttributesDecoders()
    {
        return attributesDecoders;
    }
    
    protected void initializeDecoder()
        throws DrakoException
    {
    }
    
    protected void decodeGeometryData()
        throws DrakoException
    {
    }
    
    protected void decodePointAttributes(boolean decodeAttributeData)
        throws DrakoException
    {
        byte numAttributesDecoders = buffer.decodeU8();
        //create attributes decoders
        for (int i = 0; i < (0xff & numAttributesDecoders); i++)
        {
            this.createAttributesDecoder(i);
        }
        
        //initialize all decoders
        for (AttributesDecoder dec : attributesDecoders)
        {
            dec.initialize(this, pointCloud);
        }
        
        //decode data
        for (AttributesDecoder dec : attributesDecoders)
        {
            dec.decodeAttributesDecoderData(buffer);
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
            this.decodeAllAttributes();
        }
        
        this.onAttributesDecoded();
    }
    
    protected abstract void createAttributesDecoder(int attrDecoderId)
        throws DrakoException;
    
    protected void onAttributesDecoded()
    {
    }
    
    protected void decodeAllAttributes()
        throws DrakoException
    {
        for (AttributesDecoder dec : attributesDecoders)
        {
            dec.decodeAttributes(buffer);
        }
        
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
