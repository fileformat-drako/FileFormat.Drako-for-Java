package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
import java.util.ArrayList;
class KdTreeAttributesDecoder extends AttributesDecoder
{    
    private ArrayList<AttributeQuantizationTransform> attribute_quantization_transforms_;
    private IntList min_signed_values_;
    private ArrayList<PointAttribute> quantized_portable_attributes_;
    @Override
    protected boolean decodePortableAttributes(DecoderBuffer buffer)
    {
        final byte[] ref0 = new byte[1];
        if (buffer.getBitstreamVersion() < 23)
            return true;
        byte compression_level = 0;
        if (!buffer.decode3(ref0))
        {
            compression_level = ref0[0];
            return false;
        }
        else
        {
            compression_level = ref0[0];
        }
        
        int num_points = this.getDecoder().getPointCloud().getNumPoints();
        int num_attributes = this.getNumAttributes();
        int total_dimensionality = 0;// position is a required dimension
        
        PointAttributeInfo[] atts = new PointAttributeInfo[num_attributes];
        
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            int att_id = this.getAttributeId(i);
            PointAttribute att = this.getDecoder().getPointCloud().attribute(att_id);
            // All attributes have the same number of values and identity mapping
            // between PointIndex and AttributeValueIndex.
            att.reset(num_points);
            att.setIdentityMapping(true);
            PointAttribute target_att = null;
            if (att.getDataType() == DataType.UINT32 || (att.getDataType() == DataType.UINT16) || (att.getDataType() == DataType.UINT8))
            {
                // We can decode to these attributes directly.
                target_att = att;
            }
            else if (att.getDataType() == DataType.INT32 || (att.getDataType() == DataType.INT16) || (att.getDataType() == DataType.INT8))
            {
                // Prepare storage for data that is used to convert unsigned values back
                // to the signed ones.
                for (int c = 0; c < att.getComponentsCount(); ++c)
                {
                    min_signed_values_.add(0);
                }
                
                
                target_att = att;
            }
            else if (att.getDataType() == DataType.FLOAT32)
            {
                PointAttribute port_att = new PointAttribute(att.getAttributeType(), DataType.UINT32, att.getComponentsCount(), false, att.getComponentsCount() * DracoUtils.dataTypeLength(DataType.UINT32), 0);
                port_att.setIdentityMapping(true);
                port_att.reset(num_points);
                quantized_portable_attributes_.add(port_att);
                target_att = port_att;
            }
            else
                return false;
            int data_type = target_att.getDataType();
            int data_size = Math.max(0, DracoUtils.dataTypeLength(data_type));
            int num_components = target_att.getComponentsCount();
            atts[i] = new PointAttributeInfo(target_att, total_dimensionality, data_type, data_size, num_components);
            total_dimensionality += num_components;
        }
        
        PointAttributeVectorOutputIterator out_it = new PointAttributeVectorOutputIterator(atts);
        DynamicIntegerPointsKdTreeDecoder decoder = new DynamicIntegerPointsKdTreeDecoder(0xff & compression_level, total_dimensionality);
        if (!decoder.decodePoints(buffer, out_it))
            return false;
        return true;
    }
    
    @Override
    protected boolean decodeDataNeededByPortableTransforms(DecoderBuffer in_buffer)
    {
        final float[] ref1 = new float[1];
        final byte[] ref2 = new byte[1];
        final int[] ref4 = new int[1];
        if (in_buffer.getBitstreamVersion() >= 23)
        {
            float[] min_value;
            for (int i = 0; i < this.getNumAttributes(); ++i)
            {
                int att_id = this.getAttributeId(i);
                PointAttribute att = this.getDecoder().getPointCloud().attribute(att_id);
                if (att.getDataType() == DataType.FLOAT32)
                {
                    int num_components = att.getComponentsCount();
                    min_value = new float[num_components];
                    if (!in_buffer.decode(min_value))
                        return false;
                    float max_value_dif;
                    if (!in_buffer.decode4(ref1))
                    {
                        max_value_dif = ref1[0];
                        return false;
                    }
                    else
                    {
                        max_value_dif = ref1[0];
                    }
                    
                    byte quantization_bits;
                    final boolean tmp3 = !in_buffer.decode3(ref2);
                    quantization_bits = ref2[0];
                    if (tmp3 || ((0xff & quantization_bits) > 31))
                        return false;
                    AttributeQuantizationTransform transform = new AttributeQuantizationTransform();
                    transform.setParameters(0xff & quantization_bits, min_value, num_components, max_value_dif);
                    int num_transforms = attribute_quantization_transforms_.size();
                    if (!transform.transferToAttribute(quantized_portable_attributes_.get(num_transforms)))
                        return false;
                    attribute_quantization_transforms_.add(transform);
                }
                
            }
            
            
            // Decode transform data for signed integer attributes.
            for (int i = 0; i < min_signed_values_.getCount(); ++i)
            {
                int val;
                Decoding.decodeVarint(ref4, in_buffer);
                val = ref4[0];
                min_signed_values_.set(i, val);
            }
            
            
            return true;
        }
        
        return false;
    }
    
    boolean transformAttributeBackToSignedType_short(PointAttribute att, int num_processed_signed_components)
    {
        short[] unsigned_val = new short[att.getComponentsCount()];
        short[] signed_val = new short[att.getComponentsCount()];
        
        for (int avi = 0; avi < att.getNumUniqueEntries(); ++avi)
        {
            att.getValue(avi, unsigned_val);
            for (int c = 0; c < att.getComponentsCount(); ++c)
            {
                // Up-cast |unsigned_val| to int32_t to ensure we don't overflow it for
                // smaller data types.
                signed_val[c] = (short)((int)(0xffff & unsigned_val[c]) + min_signed_values_.get(num_processed_signed_components + c));
            }
            
            att.setAttributeValue(avi, signed_val);
        }
        
        return true;
    }
    
    boolean transformAttributeBackToSignedType_sbyte(PointAttribute att, int num_processed_signed_components)
    {
        byte[] unsigned_val = new byte[att.getComponentsCount()];
        byte[] signed_val = new byte[att.getComponentsCount()];
        
        for (int avi = 0; avi < att.getNumUniqueEntries(); ++avi)
        {
            att.getValue(avi, unsigned_val);
            for (int c = 0; c < att.getComponentsCount(); ++c)
            {
                // Up-cast |unsigned_val| to int32_t to ensure we don't overflow it for
                // smaller data types.
                signed_val[c] = (byte)((int)(0xff & unsigned_val[c]) + min_signed_values_.get(num_processed_signed_components + c));
            }
            
            att.setAttributeValue(avi, signed_val, 0);
        }
        
        return true;
    }
    
    boolean transformAttributeBackToSignedType_int(PointAttribute att, int num_processed_signed_components)
    {
        int[] unsigned_val = new int[att.getComponentsCount()];
        int[] signed_val = new int[att.getComponentsCount()];
        
        for (int avi = 0; avi < att.getNumUniqueEntries(); ++avi)
        {
            att.getValue(avi, unsigned_val);
            for (int c = 0; c < att.getComponentsCount(); ++c)
            {
                // Up-cast |unsigned_val| to int32_t to ensure we don't overflow it for
                // smaller data types.
                signed_val[c] = (int)(unsigned_val[c] + min_signed_values_.get(num_processed_signed_components + c));
            }
            
            att.setAttributeValue(avi, signed_val);
        }
        
        return true;
    }
    
    @Override
    protected boolean transformAttributesToOriginalFormat()
    {
        
        if (quantized_portable_attributes_.isEmpty() && (min_signed_values_.getCount() == 0))
            return true;
        int num_processed_quantized_attributes = 0;
        int num_processed_signed_components = 0;
        // Dequantize attributes that needed it.
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            int att_id = this.getAttributeId(i);
            PointAttribute att = this.getDecoder().getPointCloud().attribute(att_id);
            if (att.getDataType() == DataType.INT32 || (att.getDataType() == DataType.INT16) || (att.getDataType() == DataType.INT8))
            {
                int[] unsigned_val = new int[att.getComponentsCount()];
                int[] signed_val = new int[att.getComponentsCount()];
                // Values are stored as unsigned in the attribute, make them signed again.
                if (att.getDataType() == DataType.INT32)
                {
                    if (!this.transformAttributeBackToSignedType_int(att, num_processed_signed_components))
                        return false;
                }
                else if (att.getDataType() == DataType.INT16)
                {
                    if (!this.transformAttributeBackToSignedType_short(att, num_processed_signed_components))
                        return false;
                }
                else if (att.getDataType() == DataType.INT8)
                {
                    if (!this.transformAttributeBackToSignedType_sbyte(att, num_processed_signed_components))
                        return false;
                }
                
                num_processed_signed_components += att.getComponentsCount();
            }
            else if (att.getDataType() == DataType.FLOAT32)
            {
                PointAttribute src_att = quantized_portable_attributes_.get(num_processed_quantized_attributes);
                AttributeQuantizationTransform transform = attribute_quantization_transforms_.get(num_processed_quantized_attributes);
                
                num_processed_quantized_attributes++;
                
                if (this.getDecoder().options.skipAttributeTransform)
                {
                    // Attribute transform should not be performed. In this case, we replace
                    // the output geometry attribute with the portable attribute.
                    // TODO(ostava): We can potentially avoid this copy by introducing a new
                    // mechanism that would allow to use the final attributes as portable
                    // attributes for predictors that may need them.
                    att.copyFrom(src_att);
                    continue;
                }
                
                int max_quantized_value = (1 << transform.quantization_bits_) - 1;
                int num_components = att.getComponentsCount();
                int entry_size = 4 * num_components;
                float[] att_val = new float[num_components];
                int quant_val_id = 0;
                int out_byte_pos = 0;
                Dequantizer dequantizer = new Dequantizer(transform.range_, max_quantized_value);
                IntSpan portable_attribute_data = src_att.getBuffer().asIntArray();
                for (int j = 0; j < src_att.getNumUniqueEntries(); ++j)
                {
                    for (int c = 0; c < num_components; ++c)
                    {
                        float value = dequantizer.dequantizeFloat(portable_attribute_data.get(quant_val_id++));
                        value = value + transform.min_values_[c];
                        att_val[c] = value;
                    }
                    
                    
                    // Store the floating point value into the attribute buffer.
                    att.getBuffer().write(out_byte_pos, att_val);
                    out_byte_pos += entry_size;
                }
                
            }
            
        }
        
        
        return true;
    }
    
    @Override
    public PointAttribute getPortableAttribute(int attId)
    {
        return null;
    }
    
    public KdTreeAttributesDecoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            attribute_quantization_transforms_ = new ArrayList<AttributeQuantizationTransform>();
            min_signed_values_ = new IntList();
            quantized_portable_attributes_ = new ArrayList<PointAttribute>();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
