package org.fileformat.drako;
class SequentialAttributeDecodersController extends AttributesDecoder
{    
    private SequentialAttributeDecoder[] sequentialDecoders;
    private int[] pointIds;
    private PointsSequencer sequencer;
    public SequentialAttributeDecodersController(PointsSequencer sequencer)
    {
        this.sequencer = sequencer;
    }
    
    @Override
    public boolean decodeAttributesDecoderData(DecoderBuffer buffer)
    {
        final byte[] ref0 = new byte[1];
        
        if (!super.decodeAttributesDecoderData(buffer))
            return DracoUtils.failed();
        // Decode unique ids of all sequential encoders and create them.
        this.sequentialDecoders = new SequentialAttributeDecoder[this.getNumAttributes()];
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            byte decoderType;
            if (!buffer.decode3(ref0))
            {
                decoderType = ref0[0];
                return DracoUtils.failed();
            }
            else
            {
                decoderType = ref0[0];
            }
            
            // Create the decoder from the id.
            sequentialDecoders[i] = this.createSequentialDecoder((int)(0xff & decoderType));
            if (sequentialDecoders[i] == null)
                return DracoUtils.failed();
            if (!sequentialDecoders[i].initialize(this.getDecoder(), this.getAttributeId(i)))
                return DracoUtils.failed();
        }
        
        return true;
    }
    
    @Override
    public boolean decodeAttributes(DecoderBuffer buffer)
    {
        final int[][] ref1 = new int[1][];
        
        if (sequencer == null || !sequencer.generateSequence(ref1))
        {
            pointIds = ref1[0];
            return DracoUtils.failed();
        }
        else
        {
            pointIds = ref1[0];
        }
        
        // Initialize point to attribute value mapping for all decoded attributes.
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            PointAttribute pa = this.getDecoder().getPointCloud().attribute(this.getAttributeId(i));
            if (!sequencer.updatePointToAttributeIndexMapping(pa))
                return DracoUtils.failed();
        }
        
        return super.decodeAttributes(buffer);
    }
    
    @Override
    protected boolean decodePortableAttributes(DecoderBuffer buffer)
    {
        int num_attributes = this.getNumAttributes();
        for (int i = 0; i < num_attributes; ++i)
        {
            if (!sequentialDecoders[i].decodePortableAttribute(pointIds, buffer))
                return DracoUtils.failed();
        }
        
        
        return true;
    }
    
    @Override
    protected boolean decodeDataNeededByPortableTransforms(DecoderBuffer buffer)
    {
        int num_attributes = this.getNumAttributes();
        for (int i = 0; i < num_attributes; ++i)
        {
            if (!sequentialDecoders[i].decodeDataNeededByPortableTransform(pointIds, buffer))
                return DracoUtils.failed();
        }
        
        
        return true;
    }
    
    @Override
    protected boolean transformAttributesToOriginalFormat()
    {
        int num_attributes = this.getNumAttributes();
        for (int i = 0; i < num_attributes; ++i)
        {
            // Check whether the attribute transform should be skipped.
            if (this.getDecoder().options != null)
            {
                PointAttribute attribute = sequentialDecoders[i].getAttribute();
                if (this.getDecoder().options.skipAttributeTransform)
                {
                    // Attribute transform should not be performed. In this case, we replace
                    // the output geometry attribute with the portable attribute.
                    // TODO(ostava): We can potentially avoid this copy by introducing a new
                    // mechanism that would allow to use the final attributes as portable
                    // attributes for predictors that may need them.
                    sequentialDecoders[i].getAttribute().copyFrom(sequentialDecoders[i].getPortableAttribute());
                    continue;
                }
                
            }
            
            
            if (!sequentialDecoders[i].transformAttributeToOriginalFormat(pointIds))
                return DracoUtils.failed();
        }
        
        
        return true;
    }
    
    protected SequentialAttributeDecoder createSequentialDecoder(int decoderType)
    {
        switch(decoderType)
        {
            case SequentialAttributeEncoderType.GENERIC:
                return new SequentialAttributeDecoder();
            case SequentialAttributeEncoderType.INTEGER:
                return new SequentialIntegerAttributeDecoder();
            case SequentialAttributeEncoderType.QUANTIZATION:
                return new SequentialQuantizationAttributeDecoder();
            case SequentialAttributeEncoderType.NORMALS:
                return new SequentialNormalAttributeDecoder();
            default:
                return null;
        }
        
    }
    
    @Override
    public PointAttribute getPortableAttribute(int attId)
    {
        int loc_id = this.getLocalIdForPointAttribute(attId);
        if (loc_id < 0)
            return null;
        return sequentialDecoders[loc_id].getPortableAttribute();
    }
    
}
