package dev.fileformat.drako;
import dev.fileformat.drako.ByteSpan;
import dev.fileformat.drako.IntSpan;
/**
 *  Attribute encoder designed for lossless encoding of integer attributes. The
 *  attribute values can be pre-processed by a prediction scheme and compressed
 *  with a built-in entropy coder.
 *
 */
class SequentialIntegerAttributeEncoder extends SequentialAttributeEncoder
{    
    /**
     *  Optional prediction scheme can be used to modify the integer values in
     *  order to make them easier to compress.
     *
     */
    private PredictionScheme predictionScheme;
    public SequentialIntegerAttributeEncoder()
    {
    }
    
    @Override
    public int getUniqueId()
    {
        return SequentialAttributeEncoderType.INTEGER;
    }
    
    @Override
    public boolean initialize(PointCloudEncoder encoder, int attributeId)
    {
        if (!super.initialize(encoder, attributeId))
            return false;
        
        // When encoding integers, this encoder currently works only for integer
        // attributes up to 32 bits.
        
        if (this.getUniqueId() == SequentialAttributeEncoderType.INTEGER && !DracoUtils.isIntegerType(this.getAttribute().getDataType()))
            return false;
        int predictionSchemeMethod = encoder.getOptions().getAttributePredictionScheme(this.getAttribute());
        
        this.predictionScheme = this.createIntPredictionScheme(predictionSchemeMethod);
        
        if (predictionScheme != null && !this.initPredictionScheme(predictionScheme))
        {
            this.predictionScheme = null;
        }
        
        
        return true;
    }
    
    @Override
    public boolean transformAttributeToPortableFormat(int[] point_ids)
    {
        if (this.getEncoder() != null)
        {
            if (!this.prepareValues(point_ids, this.getEncoder().getPointCloud().getNumPoints()))
                return false;
        }
        else if (!this.prepareValues(point_ids, 0))
            return false;
        
        // Update point to attribute mapping with the portable attribute if the
        // attribute is a parent attribute (for now, we can skip it otherwise).
        if (this.isParentEncoder())
        {
            PointAttribute orig_att = this.getAttribute();
            PointAttribute portable_att = this.portableAttribute;
            int[] value_to_value_map = new int[orig_att.getNumUniqueEntries()];
            for (int i = 0; i < point_ids.length; ++i)
            {
                value_to_value_map[orig_att.mappedIndex(point_ids[i])] = i;
            }
            
            
            if (portable_att.getIdentityMapping())
            {
                portable_att.setExplicitMapping(this.getEncoder().getPointCloud().getNumPoints());
            }
            
            // Go over all points of the original attribute and update the mapping in
            // the portable attribute.
            for (int i = 0; i < this.getEncoder().getPointCloud().getNumPoints(); ++i)
            {
                portable_att.setPointMapEntry(i, value_to_value_map[orig_att.mappedIndex(i)]);
            }
            
        }
        
        
        return true;
    }
    
    @Override
    protected boolean encodeValues(int[] pointIds, EncoderBuffer outBuffer)
    {
        PointAttribute attrib = this.getAttribute();
        if (attrib.getNumUniqueEntries() == 0)
            return true;
        byte prediction_scheme_method = (byte)PredictionSchemeMethod.NONE;
        if (predictionScheme != null)
        {
            if (!this.setPredictionSchemeParentAttributes(predictionScheme))
                return false;
            
            prediction_scheme_method = (byte)(predictionScheme.getPredictionMethod());
        }
        
        
        outBuffer.encode(prediction_scheme_method);
        if (predictionScheme != null)
        {
            outBuffer.encode((byte)(predictionScheme.getTransformType()));
        }
        
        int num_components = this.portableAttribute.getComponentsCount();
        int num_values = num_components * this.portableAttribute.getNumUniqueEntries();
        IntSpan portable_attribute_data = this.getPortableAttributeData();
        IntSpan encoded_data = IntSpan.wrap(new int[num_values]);
        
        // All integer values are initialized. Process them using the prediction
        // scheme if we have one.
        if (predictionScheme != null)
        {
            predictionScheme.computeCorrectionValues(portable_attribute_data, encoded_data, num_values, num_components, pointIds);
        }
        
        
        if (predictionScheme == null || !predictionScheme.areCorrectionsPositive())
        {
            IntSpan input = predictionScheme != null ? encoded_data : portable_attribute_data;
            Encoding.convertSignedIntsToSymbols(input, num_values, encoded_data);
        }
        
        
        if (this.getEncoder() == null || this.getEncoder().getOptions().useBuiltinAttributeCompression)
        {
            outBuffer.encode((byte)1);
            DracoEncodeOptions symbol_encoding_options = new DracoEncodeOptions();
            if (this.getEncoder() != null)
            {
                symbol_encoding_options.setCompressionLevel(this.getEncoder().getOptions().getCompressionLevel());
                //SetSymbolEncodingCompressionLevel(&symbol_encoding_options, 10 - encoder().options().GetSpeed());
            }
            
            
            
            if (!Encoding.encodeSymbols(encoded_data, pointIds.length * num_components, num_components, symbol_encoding_options, outBuffer))
                return false;
        }
        else
        {
            int masked_value = 0;
            for (int i = 0; i < num_values; ++i)
            {
                masked_value |= encoded_data.get(i);
            }
            
            int value_msb_pos = 0;
            if ((0xffffffffl & masked_value) != 0)
            {
                value_msb_pos = DracoUtils.mostSignificantBit(masked_value);
            }
            
            int num_bytes = 1 + (value_msb_pos / 8);
            
            outBuffer.encode((byte)0);
            outBuffer.encode((byte)num_bytes);
            
            if (num_bytes == DracoUtils.dataTypeLength(DataType.INT32))
            {
                outBuffer.encode(encoded_data, 4 * num_values);
            }
            else
            {
                for (int i = 0; i < num_values; ++i)
                {
                    outBuffer.encode(encoded_data, i * 4, num_bytes);
                }
                
            }
            
        }
        
        
        if (predictionScheme != null)
        {
            predictionScheme.encodePredictionData(outBuffer);
        }
        
        
        return true;
    }
    
    private IntSpan getPortableAttributeData()
    {
        int num_components = this.portableAttribute.getComponentsCount();
        int num_values = num_components * this.portableAttribute.getNumUniqueEntries();
        byte[] buf = this.portableAttribute.getBuffer().getBuffer();
        return ByteSpan.wrap(buf, this.portableAttribute.getByteOffset(), num_values * 4).asIntSpan();
    }
    
    /**
     *  Returns a prediction scheme that should be used for encoding of the
     *  integer values.
     *
     */
    protected PredictionScheme createIntPredictionScheme(int method)
    {
        //AttributeId, Encoder;
        //PredictionSchemeWrapTransform
        
        return PredictionScheme.create(this.getEncoder(), method, this.getAttributeId(), new PredictionSchemeWrapTransform());
    }
    
    /**
     *  Prepares the integer values that are going to be encoded.
     *
     */
    protected boolean prepareValues(int[] pointIds, int numPoints)
    {
        PointAttribute attrib = this.getAttribute();
        int numComponents = attrib.getComponentsCount();
        int numEntries = pointIds.length;
        final int[] ref0 = new int[1];
        this.preparePortableAttribute(numEntries, numComponents, numPoints);
        int dstIndex = 0;
        IntSpan portable_attribute_data = this.getPortableAttributeData();
        for (int i = 0; i < numEntries; ++i)
        {
            int attId = attrib.mappedIndex(pointIds[i]);
            int tmp;
            attrib.convertValue(attId, ref0);
            tmp = ref0[0];
            portable_attribute_data.put(dstIndex, tmp);
            dstIndex += numComponents;
        }
        
        return true;
    }
    
    private void preparePortableAttribute(int num_entries, int num_components, int num_points)
    {
        PointAttribute va = new PointAttribute();
        va.setAttributeType(this.attribute.getAttributeType());
        va.setComponentsCount(this.attribute.getComponentsCount());
        va.setDataType(DataType.INT32);
        va.setNormalized(false);
        va.setByteStride(num_components * DracoUtils.dataTypeLength(DataType.INT32));
        va.reset(num_entries);
        this.portableAttribute = va;
        if (num_points != 0)
        {
            this.portableAttribute.setExplicitMapping(num_points);
        }
        
    }
    
}
