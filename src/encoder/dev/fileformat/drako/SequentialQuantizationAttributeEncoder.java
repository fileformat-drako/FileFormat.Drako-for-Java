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
    public void initialize(PointCloudEncoder encoder, int attributeId)
        throws DrakoException
    {
        super.initialize(encoder, attributeId);
        PointAttribute attribute = this.getEncoder().getPointCloud().attribute(attributeId);
        if (attribute.getDataType() != DataType.FLOAT32)
            throw DracoUtils.failed();
        int quantization_bits = encoder.getOptions().getQuantizationBits(attribute);
        if (quantization_bits < 1)
            throw DracoUtils.failed();
        {
            // Compute quantization settings from the attribute values.
            attribute_quantization_transform_.computeParameters(attribute, quantization_bits);
        }
        
    }
    
    @Override
    public void encodeDataNeededByPortableTransform(EncoderBuffer out_buffer)
        throws DrakoException
    {
        attribute_quantization_transform_.encodeParameters(out_buffer);
    }
    
    @Override
    protected void prepareValues(int[] pointIds, int numPoints)
    {
        PointAttribute portable_attribute = attribute_quantization_transform_.initTransformedAttribute(this.attribute, pointIds.length);
        attribute_quantization_transform_.transformAttribute(this.attribute, pointIds, portable_attribute);
        this.portableAttribute = portable_attribute;
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
