package dev.fileformat.drako;
/**
 *  Attribute encoder that quantizes floating point attribute values. The
 *  quantized values can be optionally compressed using an entropy coding.
 *
 */
class SequentialQuantizationAttributeEncoder extends SequentialIntegerAttributeEncoder
{    
    private AttributeQuantizationTransform attribute_quantization_transform_;
    @Override
    public int getUniqueId()
    {
        return SequentialAttributeEncoderType.QUANTIZATION;
    }
    
    @Override
    public boolean isLossyEncoder()
    {
        return true;
    }
    
    @Override
    public boolean initialize(PointCloudEncoder encoder, int attributeId)
    {
        if (!super.initialize(encoder, attributeId))
            return false;
        PointAttribute attribute = this.getEncoder().getPointCloud().attribute(attributeId);
        if (attribute.getDataType() != DataType.FLOAT32)
            return false;
        int quantization_bits = encoder.getOptions().getQuantizationBits(attribute);
        if (quantization_bits < 1)
            return false;
        {
            // Compute quantization settings from the attribute values.
            attribute_quantization_transform_.computeParameters(attribute, quantization_bits);
        }
        
        
        return true;
    }
    
    @Override
    public boolean encodeDataNeededByPortableTransform(EncoderBuffer out_buffer)
    {
        return attribute_quantization_transform_.encodeParameters(out_buffer);
    }
    
    @Override
    protected boolean prepareValues(int[] pointIds, int numPoints)
    {
        PointAttribute portable_attribute = attribute_quantization_transform_.initTransformedAttribute(this.attribute, pointIds.length);
        if (!attribute_quantization_transform_.transformAttribute(this.attribute, pointIds, portable_attribute))
            return false;
        this.portableAttribute = portable_attribute;
        
        return true;
    }
    
    public SequentialQuantizationAttributeEncoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            attribute_quantization_transform_ = new AttributeQuantizationTransform();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
