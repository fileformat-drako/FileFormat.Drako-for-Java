package dev.fileformat.drako;
import java.util.ArrayList;
/**
 *  Encodes all attributes of a given PointCloud using one of the available
 *  Kd-tree compression methods.
 *
 */
class KdTreeAttributesEncoder extends AttributesEncoder
{    
    private ArrayList<AttributeQuantizationTransform> attribute_quantization_transforms_;
    // Min signed values are used to transform signed integers into unsigned ones
    // (by subtracting the min signed value for each component).
    // 
    private IntList min_signed_values_;
    private ArrayList<PointAttribute> quantized_portable_attributes_;
    private int num_components_;
    public KdTreeAttributesEncoder(int attId)
    {
        super(attId);
        this.$initFields$();
    }
    
    @Override
    public byte getUniqueId()
    {
        return (byte)((byte)AttributeEncoderType.KD_TREE);
    }
    
    @Override
    protected void transformAttributesToPortableFormat()
        throws DrakoException
    {
        int num_points = this.getEncoder().getPointCloud().getNumPoints();
        int num_components = 0;
        final int[] ref0 = new int[1];
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            int att_id = this.getAttributeId(i);
            PointAttribute att = this.getEncoder().getPointCloud().attribute(att_id);
            num_components += att.getComponentsCount();
        }
        
        
        this.num_components_ = num_components;
        
        // Go over all attributes and quantize them if needed.
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            int att_id = this.getAttributeId(i);
            PointAttribute att = this.getEncoder().getPointCloud().attribute(att_id);
            if (att.getDataType() == DataType.FLOAT32)
            {
                AttributeQuantizationTransform attribute_quantization_transform = new AttributeQuantizationTransform();
                int quantization_bits = this.getEncoder().getOptions().getQuantizationBits(att);
                if (quantization_bits < 1)
                    throw DracoUtils.failed();
                {
                    // Compute quantization settings from the attribute values.
                    attribute_quantization_transform.computeParameters(att, quantization_bits);
                }
                
                
                attribute_quantization_transforms_.add(attribute_quantization_transform);
                PointAttribute portable_att = attribute_quantization_transform.initTransformedAttribute(att, num_points);
                attribute_quantization_transform.generatePortableAttribute(att, num_points, portable_att);
                quantized_portable_attributes_.add(portable_att);
            }
            else if (att.getDataType() == DataType.INT32 || (att.getDataType() == DataType.INT16) || (att.getDataType() == DataType.INT8))
            {
                int[] min_value = new int[att.getComponentsCount()];
                for (int j = 0; j < min_value.length; j++)
                {
                    min_value[j] = Integer.MAX_VALUE;
                }
                
                int[] act_value = new int[att.getComponentsCount()];
                for (int avi = 0; avi < att.getNumUniqueEntries(); ++avi)
                {
                    att.convertValue(avi, ref0);
                    act_value[0] = ref0[0];
                    for (int c = 0; c < att.getComponentsCount(); ++c)
                    {
                        if (min_value[c] > act_value[c])
                        {
                            min_value[c] = act_value[c];
                        }
                        
                    }
                    
                }
                
                
                for (int c = 0; c < att.getComponentsCount(); ++c)
                {
                    min_signed_values_.add(min_value[c]);
                }
                
            }
            
        }
        
    }
    
    @Override
    protected void encodePortableAttributes(EncoderBuffer out_buffer)
        throws DrakoException
    {
        byte compression_level = (byte)Math.min(10 - this.getEncoder().getOptions().getSpeed(), 6);
        final int[] ref1 = new int[1];
        final int[] ref2 = new int[1];
        
        if (compression_level == 6 && (num_components_ > 15))
        {
            // Don't use compression level for CL >= 6. Axis selection is currently
            // encoded using 4 bits.
            compression_level = 5;
        }
        
        
        out_buffer.encode(compression_level);
        int num_points = this.getEncoder().getPointCloud().getNumPoints();
        int[][] point_vector = new int[num_points][];
        for (int i = 0; i < num_points; i++)
        {
            point_vector[i] = new int[num_components_];
        }
        
        int num_processed_components = 0;
        int num_processed_quantized_attributes = 0;
        int num_processed_signed_components = 0;
        // Copy data to the point vector.
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            int att_id = this.getAttributeId(i);
            PointAttribute att = this.getEncoder().getPointCloud().attribute(att_id);
            PointAttribute source_att = null;
            if (att.getDataType() == DataType.UINT32 || (att.getDataType() == DataType.UINT16) || (att.getDataType() == DataType.UINT8) || (att.getDataType() == DataType.INT32) || (att.getDataType() == DataType.INT16) || (att.getDataType() == DataType.INT8))
            {
                // Use the original attribute.
                source_att = att;
            }
            else if (att.getDataType() == DataType.FLOAT32)
            {
                // Use the portable (quantized) attribute instead.
                source_att = quantized_portable_attributes_.get(num_processed_quantized_attributes);
                num_processed_quantized_attributes++;
            }
            else
                throw DracoUtils.failed();
            
            if (source_att == null)
                throw DracoUtils.failed();
            
            // Copy source_att to the vector.
            if (source_att.getDataType() == DataType.UINT32)
            {
                // If the data type is the same as the one used by the point vector, we
                // can directly copy individual elements.
                for (int pi = 0; pi < num_points; ++pi)
                {
                    int avi = source_att.mappedIndex(pi);
                    int offset = source_att.getBytePos(avi);
                    this.copyAttribute(point_vector, source_att.getComponentsCount(), num_processed_components, pi, source_att.getBuffer().getBuffer(), offset);
                }
                
            }
            else if (source_att.getDataType() == DataType.INT32 || (source_att.getDataType() == DataType.INT16) || (source_att.getDataType() == DataType.INT8))
            {
                int[] signed_point = new int[source_att.getComponentsCount()];
                int[] unsigned_point = new int[source_att.getComponentsCount()];
                for (int pi = 0; pi < num_points; ++pi)
                {
                    int avi = source_att.mappedIndex(pi);
                    source_att.convertValue(avi, ref1);
                    signed_point[0] = ref1[0];
                    for (int c = 0; c < source_att.getComponentsCount(); ++c)
                    {
                        unsigned_point[c] = signed_point[c] - min_signed_values_.get(num_processed_signed_components + c);
                    }
                    
                    
                    this.copyAttribute(point_vector, num_processed_components, pi, unsigned_point);
                }
                
                
                num_processed_signed_components += source_att.getComponentsCount();
            }
            else
            {
                int[] point = new int[source_att.getComponentsCount()];
                for (int pi = 0; pi < num_points; ++pi)
                {
                    int avi = source_att.mappedIndex(pi);
                    source_att.convertValue(avi, ref2);
                    point[0] = ref2[0];
                    this.copyAttribute(point_vector, num_processed_components, pi, point);
                }
                
            }
            
            
            num_processed_components += source_att.getComponentsCount();
        }
        
        int num_bits = 0;
        for (int r = 0; r < num_points; r++)
        {
            for (int c = 0; c < num_components_; c++)
            {
                if ((point_vector[r])[c] > 0)
                {
                    int msb = DracoUtils.mostSignificantBit((point_vector[r])[c]) + 1;
                    if (msb > num_bits)
                    {
                        num_bits = msb;
                    }
                    
                }
                
            }
            
        }
        
        DynamicIntegerPointsKdTreeEncoder points_encoder = new DynamicIntegerPointsKdTreeEncoder(0xff & compression_level, num_components_);
        points_encoder.encodePoints(point_vector, num_bits, out_buffer);
    }
    
    @Override
    protected void encodeDataNeededByPortableTransforms(EncoderBuffer out_buffer)
        throws DrakoException
    {
        // Store quantization settings for all attributes that need it.
        for (int i = 0; i < attribute_quantization_transforms_.size(); ++i)
        {
            attribute_quantization_transforms_.get(i).encodeParameters(out_buffer);
        }
        
        
        // Encode data needed for transforming signed integers to unsigned ones.
        for (int i = 0; i < min_signed_values_.getCount(); ++i)
        {
            Encoding.encodeVarint(min_signed_values_.get(i), out_buffer);
        }
        
    }
    
    // Copy data directly off of an attribute buffer interleaved into internal
    // memory.
    // 
    void copyAttribute(int[][] attribute, int offset_dimensionality, int index, int[] attribute_item_data)
    {
        int copy_size = attribute_item_data.length;
        int[] face = attribute[index];
        for (int j = offset_dimensionality,  i = 0; i < copy_size; i++, j++)
        {
            face[j] = attribute_item_data[i];
        }
        
    }
    
    void copyAttribute(int[][] attribute, int attribute_dimensionality, int offset_dimensionality, int index, byte[] attribute_item_data, int offset)
    {
        int copy_size = attribute_dimensionality;
        int[] face = attribute[index];
        for (int j = offset_dimensionality,  i = 0; i < copy_size; i++, j++)
        {
            face[j] = Unsafe.getLE32(attribute_item_data, offset);
            offset += 4;
        }
        
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
