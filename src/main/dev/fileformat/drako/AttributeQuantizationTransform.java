package dev.fileformat.drako;
import dev.fileformat.drako.FloatSpan;
import dev.fileformat.drako.IntSpan;
class AttributeQuantizationTransform extends AttributeTransform
{    
    public int quantization_bits_;
    // Minimal dequantized value for each component of the attribute.
    // 
    public float[] min_values_;
    // Bounds of the dequantized attribute (max delta over all components).
    // 
    public float range_;
    @Override
    protected int getTransformedDataType(PointAttribute attribute)
    {
        return DataType.UINT32;
    }
    
    @Override
    protected int getTransformedNumComponents(PointAttribute attribute)
    {
        return attribute.getComponentsCount();
    }
    
    // Copy parameter values into the provided AttributeTransformData instance.
    // 
    @Override
    public void copyToAttributeTransformData(AttributeTransformData out_data)
    {
        out_data.transformType = AttributeTransformType.QUANTIZATION_TRANSFORM;
        out_data.appendValue(quantization_bits_);
        for (int i = 0; i < min_values_.length; ++i)
        {
            out_data.appendValue(min_values_[i]);
        }
        
        
        out_data.appendValue(range_);
    }
    
    public boolean transformAttribute(PointAttribute attribute, int[] point_ids, PointAttribute target_attribute)
    {
        if (point_ids.length == 0)
        {
            this.generatePortableAttribute(attribute, target_attribute.getNumUniqueEntries(), target_attribute);
        }
        else
        {
            this.generatePortableAttribute(attribute, point_ids, target_attribute.getNumUniqueEntries(), target_attribute);
        }
        
        return true;
    }
    
    public void setParameters(int quantization_bits, float[] min_values, int num_components, float range)
    {
        this.quantization_bits_ = quantization_bits;
        this.min_values_ = (float[])(min_values.clone());
        this.range_ = range;
    }
    
    public boolean computeParameters(PointAttribute attribute, int quantization_bits)
    {
        if (quantization_bits_ != -1)
            return DracoUtils.failed();
        
        this.quantization_bits_ = quantization_bits;
        int num_components = attribute.getComponentsCount();
        this.range_ = 0.0f;
        this.min_values_ = new float[num_components];
        float[] max_values = new float[num_components];
        float[] att_val = new float[num_components];
        // Compute minimum values and max value difference.
        attribute.getValue(0, att_val);
        attribute.getValue(0, min_values_);
        attribute.getValue(0, max_values);
        
        for (int i = 1; i < attribute.getNumUniqueEntries(); ++i)
        {
            attribute.getValue(i, att_val);
            for (int c = 0; c < num_components; ++c)
            {
                if (min_values_[c] > att_val[c])
                {
                    min_values_[c] = att_val[c];
                }
                
                if (max_values[c] < att_val[c])
                {
                    max_values[c] = att_val[c];
                }
                
            }
            
        }
        
        
        for (int c = 0; c < num_components; ++c)
        {
            float dif = max_values[c] - min_values_[c];
            if (dif > range_)
            {
                this.range_ = dif;
            }
            
        }
        
        
        if (DracoUtils.isZero(range_))
        {
            this.range_ = 1.0f;
        }
        
        
        return true;
    }
    
    public boolean encodeParameters(EncoderBuffer encoder_buffer)
    {
        if (quantization_bits_ != -1)
        {
            encoder_buffer.encode(min_values_);
            encoder_buffer.encode(range_);
            encoder_buffer.encode((byte)quantization_bits_);
            return true;
        }
        
        
        return DracoUtils.failed();
    }
    
    public void generatePortableAttribute(PointAttribute attribute, int num_points, PointAttribute target_attribute)
    {
        int num_entries = num_points;
        int num_components = attribute.getComponentsCount();
        IntSpan portable_attribute_data = target_attribute.getAddress(0).asIntSpan();
        int max_quantized_value = (1 << quantization_bits_) - 1;
        Quantizer quantizer = new Quantizer(range_, max_quantized_value);
        int dst_index = 0;
        float[] att_val = new float[num_components];
        for (int i = 0; i < num_points; ++i)
        {
            int att_val_id = attribute.mappedIndex(i);
            attribute.getValue(att_val_id, att_val);
            for (int c = 0; c < num_components; ++c)
            {
                float value = att_val[c] - min_values_[c];
                int q_val = quantizer.quantizeFloat(value);
                portable_attribute_data.put(dst_index++, q_val);
            }
            
        }
        
    }
    
    public void generatePortableAttribute(PointAttribute attribute, int[] point_ids, int num_points, PointAttribute target_attribute)
    {
        int num_entries = point_ids.length;
        int num_components = attribute.getComponentsCount();
        IntSpan portable_attribute_data = target_attribute.getAddress(0).asIntSpan();
        int max_quantized_value = (1 << quantization_bits_) - 1;
        Quantizer quantizer = new Quantizer(range_, max_quantized_value);
        int dst_index = 0;
        FloatSpan att_val = FloatSpan.wrap(new float[num_components]);
        for (int i = 0; i < point_ids.length; ++i)
        {
            int att_val_id = attribute.mappedIndex(point_ids[i]);
            attribute.getValue(att_val_id, att_val);
            for (int c = 0; c < num_components; ++c)
            {
                float value = att_val.get(c) - min_values_[c];
                int q_val = quantizer.quantizeFloat(value);
                portable_attribute_data.put(dst_index++, q_val);
            }
            
        }
        
    }
    
    public AttributeQuantizationTransform()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            quantization_bits_ = -1;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
