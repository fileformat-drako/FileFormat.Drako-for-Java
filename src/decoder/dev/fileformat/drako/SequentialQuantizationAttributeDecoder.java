package dev.fileformat.drako;
import dev.fileformat.drako.ByteSpan;
import dev.fileformat.drako.IntSpan;
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
    public void initialize(PointCloudDecoder decoder, int attributeId)
        throws DrakoException
    {
        super.initialize(decoder, attributeId);
        PointAttribute attribute = decoder.getPointCloud().attribute(attributeId);
        // Currently we can quantize only floating point arguments.
        if (attribute.getDataType() != DataType.FLOAT32)
            throw DracoUtils.failed();
    }
    
    @Override
    public void decodeIntegerValues(int[] pointIds, DecoderBuffer inBuffer)
        throws DrakoException
    {
        if (this.getDecoder().getBitstreamVersion() < 20)
        {
            this.decodeQuantizedDataInfo();
        }
        
        super.decodeIntegerValues(pointIds, inBuffer);
    }
    
    @Override
    protected void storeValues(int numValues)
    {
        this.dequantizeValues(numValues);
    }
    
    @Override
    public void decodeDataNeededByPortableTransform(int[] pointIds, DecoderBuffer in_buffer)
        throws DrakoException
    {
        if (this.getDecoder().getBitstreamVersion() >= 20)
        {
            // Decode quantization data here only for files with bitstream version 2.0+
            this.decodeQuantizedDataInfo();
        }
        
        AttributeQuantizationTransform transform = new AttributeQuantizationTransform();
        transform.setParameters(quantizationBits, minValue, this.attribute.getComponentsCount(), maxValueDif);
        transform.transferToAttribute(this.getPortableAttribute());
    }
    
    private void decodeQuantizedDataInfo()
        throws DrakoException
    {
        int numComponents = this.getAttribute().getComponentsCount();
        this.minValue = new float[numComponents];
        if (!this.getDecoder().getBuffer().decode(minValue))
            throw DracoUtils.failed();
        this.maxValueDif = this.decoder.getBuffer().decodeF32();
        byte quantizationBits = this.getDecoder().getBuffer().decodeU8();
        if ((0xff & quantizationBits) > 31)
            throw DracoUtils.failed();
        this.quantizationBits = 0xff & quantizationBits;
    }
    
    private void dequantizeValues(int numValues)
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
        
    }
    
    
}
