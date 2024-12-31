package dev.fileformat.drako;
/**
 *  A base class for decoding attribute values encoded by the
 *  SequentialAttributeEncoder.
 *
 */
class SequentialAttributeDecoder
{    
    protected PointCloudDecoder decoder;
    protected PointAttribute attribute;
    PointAttribute portableAttribute;
    private int attributeId;
    public void initialize(PointCloudDecoder decoder, int attributeId)
        throws DrakoException
    {
        this.decoder = decoder;
        this.attribute = decoder.getPointCloud().attribute(attributeId);
        this.attributeId = attributeId;
    }
    
    /**
     *  Intialization for a specific attribute. This can be used mostly for
     *  standalone decoding of an attribute without an PointCloudDecoder.
     *
     */
    public void initializeStandalone(PointAttribute attribute)
    {
        this.attribute = attribute;
        this.attributeId = -1;
    }
    
    public void decode(int[] pointIds, DecoderBuffer inBuffer)
        throws DrakoException
    {
        attribute.reset(pointIds.length);
        this.decodeValues(pointIds, inBuffer);
    }
    
    public PointAttribute getAttribute()
    {
        return attribute;
    }
    
    public int getAttributeId()
    {
        return attributeId;
    }
    
    public PointCloudDecoder getDecoder()
    {
        return decoder;
    }
    
    /**
     *  Should be used to initialize newly created prediction scheme.
     *  Returns false when the initialization failed (in which case the scheme
     *  cannot be used).
     *
     */
    protected void initPredictionScheme(PredictionScheme ps)
        throws DrakoException
    {
        for (int i = 0; i < ps.getNumParentAttributes(); ++i)
        {
            int attId = decoder.getPointCloud().getNamedAttributeId(ps.getParentAttributeType(i));
            if (attId == -1)
                throw DracoUtils.failed();
            // Requested attribute does not exist.
            PointAttribute parentAttribute;
            if (decoder.getBitstreamVersion() < 20)
            {
                parentAttribute = decoder.getPointCloud().attribute(attId);
            }
            else
            {
                parentAttribute = decoder.getPortableAttribute(attId);
            }
            
            if (parentAttribute == null)
                throw DracoUtils.failed();
            ps.setParentAttribute(parentAttribute);
        }
        
    }
    
    /**
     *  The actual implementation of the attribute decoding. Should be overriden
     *  for specialized decoders.
     *
     */
    protected void decodeValues(int[] pointIds, DecoderBuffer inBuffer)
        throws DrakoException
    {
        int numValues = pointIds.length;
        int entrySize = attribute.getByteStride();
        byte[] valueData = new byte[entrySize];
        int outBytePos = 0;
        // Decode raw attribute values in their original format.
        for (int i = 0; i < numValues; ++i)
        {
            if (!inBuffer.decode(valueData, entrySize))
                throw DracoUtils.failed();
            attribute.getBuffer().write(outBytePos, valueData, entrySize);
            outBytePos += entrySize;
        }
        
    }
    
    public void decodePortableAttribute(int[] pointIds, DecoderBuffer in_buffer)
        throws DrakoException
    {
        if (attribute.getComponentsCount() <= 0)
            throw DracoUtils.failed();
        attribute.reset(pointIds.length);
        this.decodeValues(pointIds, in_buffer);
    }
    
    public void decodeDataNeededByPortableTransform(int[] pointIds, DecoderBuffer in_buffer)
        throws DrakoException
    {
        // Default implementation does not apply any transform.
    }
    
    public void transformAttributeToOriginalFormat(int[] pointIds)
        throws DrakoException
    {
        // Default implementation does not apply any transform.
    }
    
    public PointAttribute getPortableAttribute()
    {
        // If needed, copy point to attribute value index mapping from the final
        // attribute to the portable attribute.
        if (!attribute.getIdentityMapping() && (portableAttribute != null) && portableAttribute.getIdentityMapping())
        {
            int indiceMapSize = attribute.getIndicesMap().length;
            portableAttribute.setExplicitMapping(indiceMapSize);
            for (int i = 0; i < indiceMapSize; ++i)
            {
                portableAttribute.setPointMapEntry(i, attribute.mappedIndex(i));
            }
            
        }
        
        
        return portableAttribute;
    }
    
    public void setPortableAttribute(PointAttribute value)
    {
        this.portableAttribute = value;
    }
    
    public SequentialAttributeDecoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            attributeId = -1;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
