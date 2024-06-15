package org.openize.drako;
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
    public boolean initialize(PointCloudDecoder decoder, int attributeId)
    {
        this.decoder = decoder;
        this.attribute = decoder.getPointCloud().attribute(attributeId);
        this.attributeId = attributeId;
        return true;
    }
    
    /**
     *  Intialization for a specific attribute. This can be used mostly for
     *  standalone decoding of an attribute without an PointCloudDecoder.
     *
     */
    public boolean initializeStandalone(PointAttribute attribute)
    {
        this.attribute = attribute;
        this.attributeId = -1;
        return true;
    }
    
    public boolean decode(int[] pointIds, DecoderBuffer inBuffer)
    {
        attribute.reset(pointIds.length);
        if (!this.decodeValues(pointIds, inBuffer))
            return DracoUtils.failed();
        return true;
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
    protected boolean initPredictionScheme(PredictionScheme ps)
    {
        for (int i = 0; i < ps.getNumParentAttributes(); ++i)
        {
            int attId = decoder.getPointCloud().getNamedAttributeId(ps.getParentAttributeType(i));
            if (attId == -1)
                return DracoUtils.failed();
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
            
            if (parentAttribute == null || !ps.setParentAttribute(parentAttribute))
                return DracoUtils.failed();
        }
        
        return true;
    }
    
    /**
     *  The actual implementation of the attribute decoding. Should be overriden
     *  for specialized decoders.
     *
     */
    protected boolean decodeValues(int[] pointIds, DecoderBuffer inBuffer)
    {
        int numValues = pointIds.length;
        int entrySize = attribute.getByteStride();
        byte[] valueData = new byte[entrySize];
        int outBytePos = 0;
        // Decode raw attribute values in their original format.
        for (int i = 0; i < numValues; ++i)
        {
            if (!inBuffer.decode(valueData, entrySize))
                return DracoUtils.failed();
            attribute.getBuffer().write(outBytePos, valueData, entrySize);
            outBytePos += entrySize;
        }
        
        return true;
    }
    
    public boolean decodePortableAttribute(int[] pointIds, DecoderBuffer in_buffer)
    {
        if (attribute.getComponentsCount() <= 0 || !attribute.reset(pointIds.length))
            return DracoUtils.failed();
        if (!this.decodeValues(pointIds, in_buffer))
            return DracoUtils.failed();
        return true;
    }
    
    public boolean decodeDataNeededByPortableTransform(int[] pointIds, DecoderBuffer in_buffer)
    {
        // Default implementation does not apply any transform.
        return true;
    }
    
    public boolean transformAttributeToOriginalFormat(int[] pointIds)
    {
        // Default implementation does not apply any transform.
        return true;
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
