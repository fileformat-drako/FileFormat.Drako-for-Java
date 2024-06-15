package org.openize.drako;
import com.aspose.csporter.helpers.ByteSpan;
import com.aspose.csporter.helpers.IntSpan;
class SequentialQuantizationAttributeDecoder extends SequentialIntegerAttributeDecoder
{    
    /**
     *  Max number of quantization bits used to encode each component of the
     *  attribute.
     *
     */
    private int quantizationBits;
    private float[] minValue;
    private float maxValueDif;
    @Override
    public boolean initialize(PointCloudDecoder decoder, int attributeId)
    {
        if (!super.initialize(decoder, attributeId))
            return DracoUtils.failed();
        PointAttribute attribute = decoder.getPointCloud().attribute(attributeId);
        // Currently we can quantize only floating point arguments.
        if (attribute.getDataType() != DataType.FLOAT32)
            return DracoUtils.failed();
        return true;
    }
    
    @Override
    public boolean decodeIntegerValues(int[] pointIds, DecoderBuffer inBuffer)
    {
        if (this.getDecoder().getBitstreamVersion() < 20 && !this.decodeQuantizedDataInfo())
            return DracoUtils.failed();
        return super.decodeIntegerValues(pointIds, inBuffer);
    }
    
    @Override
    protected boolean storeValues(int numValues)
    {
        return this.dequantizeValues(numValues);
    }
    
    @Override
    public boolean decodeDataNeededByPortableTransform(int[] pointIds, DecoderBuffer in_buffer)
    {
        if (this.getDecoder().getBitstreamVersion() >= 20)
        {
            // Decode quantization data here only for files with bitstream version 2.0+
            if (!this.decodeQuantizedDataInfo())
                return DracoUtils.failed();
        }
        
        AttributeQuantizationTransform transform = new AttributeQuantizationTransform();
        transform.setParameters(quantizationBits, minValue, this.attribute.getComponentsCount(), maxValueDif);
        return transform.transferToAttribute(this.getPortableAttribute());
    }
    
    private boolean decodeQuantizedDataInfo()
    {
        int numComponents = this.getAttribute().getComponentsCount();
        final float[] ref0 = new float[1];
        final byte[] ref1 = new byte[1];
        this.minValue = new float[numComponents];
        if (!this.getDecoder().getBuffer().decode(minValue))
            return DracoUtils.failed();
        if (!this.getDecoder().getBuffer().decode4(ref0))
        {
            maxValueDif = ref0[0];
            return DracoUtils.failed();
        }
        else
        {
            maxValueDif = ref0[0];
        }
        
        byte quantizationBits;
        final boolean tmp2 = !this.getDecoder().getBuffer().decode3(ref1);
        quantizationBits = ref1[0];
        if (tmp2 || ((0xff & quantizationBits) > 31))
            return DracoUtils.failed();
        this.quantizationBits = 0xff & quantizationBits;
        return true;
    }
    
    private boolean dequantizeValues(int numValues)
    {
        int maxQuantizedValue = (1 << quantizationBits) - 1;
        int numComponents = this.getAttribute().getComponentsCount();
        int entrySize = 4 * numComponents;
        float[] attVal = new float[numComponents];
        int quantValId = 0;
        int outBytePos = 0;
        Dequantizer dequantizer = new Dequantizer(maxValueDif, maxQuantizedValue);
        IntSpan values = ByteSpan.wrap(this.getPortableAttribute().getBuffer().getBuffer(), 0, numValues * numComponents * 4).asIntSpan();
        for (int i = 0; (0xffffffffl & i) < numValues; ++i)
        {
            for (int c = 0; c < numComponents; ++c)
            {
                float value = dequantizer.dequantizeFloat(values.get(quantValId++));
                value = value + minValue[c];
                attVal[c] = value;
            }
            
            // Store the floating point value into the attribute buffer.
            this.getAttribute().getBuffer().write(outBytePos, attVal);
            outBytePos += entrySize;
        }
        
        return true;
    }
    
    
}
