package dev.fileformat.drako;
/**
 *  Class for encoding normal vectors using an octahedral encoding (see Cigolle
 *  et al.'14 “A Survey of Efficient Representations for Independent Unit
 *  Vectors”. Compared to the basic quantization encoder, this encoder results
 *  in a better compression rate under the same accuracy settings. Note that this
 *  encoder doesn't preserve the lengths of input vectors, therefore it will not
 *  work correctly when the input values are not normalized.
 *
 */
class SequentialNormalAttributeEncoder extends SequentialIntegerAttributeEncoder
{    
    private AttributeOctahedronTransform attribute_octahedron_transform_;
    @Override
    public int getUniqueId()
    {
        return SequentialAttributeEncoderType.NORMALS;
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
        // Currently this encoder works only for 3-component normal vectors.
        if (this.getAttribute().getComponentsCount() != 3)
            return false;
        int q = encoder.getOptions().getQuantizationBits(this.attribute);
        if (q < 1)
            return false;
        this.attribute_octahedron_transform_ = new AttributeOctahedronTransform(q);
        return true;
    }
    
    @Override
    public boolean encodeDataNeededByPortableTransform(EncoderBuffer out_buffer)
    {
        return attribute_octahedron_transform_.encodeParameters(out_buffer);
    }
    
    @Override
    protected boolean prepareValues(int[] pointIds, int numPoints)
    {
        this.portableAttribute = attribute_octahedron_transform_.generatePortableAttribute(this.getAttribute(), pointIds, numPoints);
        return true;
    }
    
    // Converts a unit vector into octahedral coordinates (0-1 range).
    // 
    static void unitVectorToOctahedralCoords(Vector3 vector, float[] outS, float[] outT)
    {
        float absSum = (float)(Math.abs(vector.x) + Math.abs(vector.y) + Math.abs(vector.z));
        Vector3 scaledVec = new Vector3();
        if (absSum > 1e-6)
        {
            float scale = 1.0f / absSum;
            scaledVec.x = vector.x * scale;
            scaledVec.y = vector.y * scale;
            scaledVec.z = vector.z * scale;
        }
        else
        {
            scaledVec.x = 1F;
            scaledVec.y = 0F;
            scaledVec.z = 0F;
        }
        
        
        if (scaledVec.x >= 0.0f)
        {
            // Right hemisphere.
            outS[0] = (float)((scaledVec.y + 1.0f) * 0.5f);
            outT[0] = (float)((scaledVec.z + 1.0f) * 0.5f);
        }
        else
        {
            // Left hemisphere.
            if (scaledVec.y < 0.0f)
            {
                outS[0] = (float)(0.5f * Math.abs(scaledVec.z));
            }
            else
            {
                outS[0] = (float)(0.5f * (2.0f - Math.abs(scaledVec.z)));
            }
            
            if (scaledVec.z < 0.0f)
            {
                outT[0] = (float)(0.5f * Math.abs(scaledVec.y));
            }
            else
            {
                outT[0] = (float)(0.5f * (2.0f - Math.abs(scaledVec.y)));
            }
            
        }
        
    }
    
    static void unitVectorToQuantizedOctahedralCoords(Vector3 vector, float maxQuantizedValue, int[] outS, int[] outT)
    {
        float maxValue = maxQuantizedValue - 1F;
        float ss;
        float tt;
        final float[] ref0 = new float[1];
        final float[] ref1 = new float[1];
        SequentialNormalAttributeEncoder.unitVectorToOctahedralCoords(vector, ref0, ref1);
        ss = ref0[0];
        tt = ref1[0];
        int s = (int)Math.floor(ss * maxValue + 0.5);
        int t = (int)Math.floor(tt * maxValue + 0.5);
        int centerValue = (int)(maxValue / 2F);
        
        // Convert all edge points in the top left and bottom right quadrants to
        // their corresponding position in the bottom left and top right quadrants.
        // Convert all corner edge points to the top right corner. This is necessary
        // for the inversion to occur correctly.
        if (s == 0 && (t == 0) || (s == 0 && (t == maxValue)) || (s == maxValue && (t == 0)))
        {
            s = (int)maxValue;
            t = (int)maxValue;
        }
        else if (s == 0 && (t > centerValue))
        {
            t = centerValue - (t - centerValue);
        }
        else if (s == maxValue && (t < centerValue))
        {
            t = centerValue + (centerValue - t);
        }
        else if (t == maxValue && (s < centerValue))
        {
            s = centerValue + (centerValue - s);
        }
        else if (t == 0 && (s > centerValue))
        {
            s = centerValue - (s - centerValue);
        }
        
        
        outS[0] = s;
        outT[0] = t;
    }
    
    @Override
    protected PredictionScheme createIntPredictionScheme(int method)
    {
        int quantizationBits = this.getEncoder().getOptions().getQuantizationBits(this.getAttribute());
        int maxValue = (1 << quantizationBits) - 1;
        PredictionSchemeNormalOctahedronCanonicalizedTransform transform = new PredictionSchemeNormalOctahedronCanonicalizedTransform(maxValue);
        int prediction_method = SequentialAttributeEncoder.selectPredictionMethod(this.getAttributeId(), this.getEncoder());
        if (prediction_method == PredictionSchemeMethod.GEOMETRIC_NORMAL || (prediction_method == PredictionSchemeMethod.DIFFERENCE))
            return PredictionScheme.create(this.getEncoder(), prediction_method, this.getAttributeId(), transform);
        return null;
    }
    
    
}
