package org.openize.drako;
import com.aspose.csporter.helpers.ByteSpan;
import com.aspose.csporter.helpers.IntSpan;
class SequentialNormalAttributeDecoder extends SequentialIntegerAttributeDecoder
{    
    private int quantizationBits;
    @Override
    protected int getNumValueComponents()
    {
        return 2;
    }
    
    @Override
    public boolean initialize(PointCloudDecoder decoder, int attributeId)
    {
        if (!super.initialize(decoder, attributeId))
            return DracoUtils.failed();
        // Currently, this encoder works only for 3-component normal vectors.
        if (this.getAttribute().getComponentsCount() != 3)
            return DracoUtils.failed();
        // Also the data type must be DTFLOAT32.
        if (this.getAttribute().getDataType() != DataType.FLOAT32)
            return DracoUtils.failed();
        return true;
    }
    
    @Override
    public boolean decodeIntegerValues(int[] pointIds, DecoderBuffer inBuffer)
    {
        byte quantizationBits;
        final byte[] ref0 = new byte[1];
        if (this.decoder.getBitstreamVersion() < 20)
        {
            if (!inBuffer.decode3(ref0))
            {
                quantizationBits = ref0[0];
                return DracoUtils.failed();
            }
            else
            {
                quantizationBits = ref0[0];
            }
            
            this.quantizationBits = 0xff & quantizationBits;
        }
        
        return super.decodeIntegerValues(pointIds, inBuffer);
    }
    
    @Override
    public boolean decodeDataNeededByPortableTransform(int[] pointIds, DecoderBuffer in_buffer)
    {
        final byte[] ref1 = new byte[1];
        if (this.decoder.getBitstreamVersion() >= 20)
        {
            byte quantization_bits;
            if (!in_buffer.decode3(ref1))
            {
                quantization_bits = ref1[0];
                return false;
            }
            else
            {
                quantization_bits = ref1[0];
            }
            
            this.quantizationBits = 0xff & quantization_bits;
        }
        
        AttributeOctahedronTransform octahedral_transform = new AttributeOctahedronTransform(quantizationBits);
        return octahedral_transform.transferToAttribute(this.getPortableAttribute());
    }
    
    @Override
    protected boolean storeValues(int numPoints)
    {
        int maxQuantizedValue = (1 << quantizationBits) - 1;
        float maxQuantizedValueF = (float)maxQuantizedValue;
        int numComponents = this.getAttribute().getComponentsCount();
        int entrySize = 4 * numComponents;
        float[] attVal = new float[3];
        int quantValId = 0;
        int outBytePos = 0;
        IntSpan values = ByteSpan.wrap(this.getPortableAttribute().getBuffer().getBuffer(), 0, numPoints * 2 * 4).asIntSpan();
        for (int i = 0; i < numPoints; ++i)
        {
            int s = values.get(quantValId++);
            int t = values.get(quantValId++);
            this.quantizedOctaherdalCoordsToUnitVector(s, t, maxQuantizedValueF, attVal);
            // Store the decoded floating point value into the attribute buffer.
            this.getAttribute().getBuffer().write(outBytePos, attVal);
            outBytePos += entrySize;
        }
        
        return true;
    }
    
    void octaherdalCoordsToUnitVector(float inS, float inT, float[] outVector)
    {
        float s = inS;
        float t = inT;
        float spt = s + t;
        float smt = s - t;
        float xSign = 1.0f;
        if (spt >= 0.5f && (spt <= 1.5f) && (smt >= -0.5f) && (smt <= 0.5f))
        {
            // Right hemisphere. Don't do anything.
        }
        else
        {
            // Left hemisphere.
            xSign = -1.0f;
            if (spt <= 0.5)
            {
                s = 0.5f - inT;
                t = 0.5f - inS;
            }
            else if (spt >= 1.5f)
            {
                s = 1.5f - inT;
                t = 1.5f - inS;
            }
            else if (smt <= -0.5f)
            {
                s = inT - 0.5f;
                t = inS + 0.5f;
            }
            else
            {
                s = inT + 0.5f;
                t = inS - 0.5f;
            }
            
            spt = s + t;
            smt = s - t;
        }
        
        float y = 2.0f * s - 1.0f;
        float z = 2.0f * t - 1.0f;
        float x = Math.min(Math.min(2.0f * spt - 1.0f, 3.0f - (2.0f * spt)), Math.min(2.0f * smt + 1.0f, 1.0f - (2.0f * smt))) * xSign;
        float normSquared = x * x + (y * y) + (z * z);
        if (normSquared < 1e-6)
        {
            outVector[0] = 0F;
            outVector[1] = 0F;
            outVector[2] = 0F;
        }
        else
        {
            float d = 1.0f / (float)Math.sqrt(normSquared);
            outVector[0] = x * d;
            outVector[1] = y * d;
            outVector[2] = z * d;
        }
        
    }
    
    void quantizedOctaherdalCoordsToUnitVector(int inS, int inT, float maxQuantizedValue, float[] outVector)
    {
        // In order to be able to represent the center normal we reduce the range
        // by one. Also note that we can not simply identify the lower left and the
        // upper right edge of the tile, which forces us to use one value less.
        maxQuantizedValue -= 1F;
        this.octaherdalCoordsToUnitVector(inS / maxQuantizedValue, inT / maxQuantizedValue, outVector);
    }
    
    @Override
    protected PredictionScheme createIntPredictionScheme(int method, int transformType)
    {
        
        // At this point the decoder has not read the quantization bits,
        // which is why we must construct the transform by default.
        // See Transform.DecodeTransformData for more details.
        if (transformType == PredictionSchemeTransformType.NORMAL_OCTAHEDRON)
            return PredictionScheme.create((MeshDecoder)(this.getDecoder()), method, this.getAttributeId(), new PredictionSchemeNormalOctahedronTransform());
        if (transformType == PredictionSchemeTransformType.NORMAL_OCTAHEDRON_CANONICALIZED)
            return PredictionScheme.create(this.getDecoder(), method, this.getAttributeId(), new PredictionSchemeNormalOctahedronCanonicalizedTransform());
        
        return null;
        // Currently, we support only octahedron transform and
        // octahedron transform canonicalized.
    }
    
    public SequentialNormalAttributeDecoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            quantizationBits = -1;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
