package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
import java.util.Arrays;
/**
 *  PredictionSchemeWrapTransform uses the min and max bounds of the original
 *  data to wrap stored correction values around these bounds centered at 0,
 *  i.e., when the range of the original values O is between &lt;MIN, MAX&gt; and
 *  N = MAX-MIN, we can then store any correction X = O - P, as:
 *         X + N,   if X &lt; -N / 2
 *         X - N,   if X &gt; N / 2
 *         X        otherwise
 *  To unwrap this value, the decoder then simply checks whether the final
 *  corrected value F = P + X is out of the bounds of the input values.
 *  All out of bounds values are unwrapped using
 *         F + N,   if F &lt; MIN
 *         F - N,   if F &gt; MAX
 *  This wrapping can reduce the number of unique values, which translates to a
 *  better entropy of the stored values and better compression rates.
 *
 */
class PredictionSchemeWrapTransform extends PredictionSchemeTransform
{    
    private int minValue;
    private int maxValue;
    private int maxDif;
    private int maxCorrection;
    private int minCorrection;
    private int[] clampedValue;
    protected void initCorrectionBounds()
    {
        this.maxDif = 1 + maxValue - minValue;
        this.maxCorrection = maxDif / 2;
        this.minCorrection = -maxCorrection;
        if ((maxDif & 1) == 0)
        {
            maxCorrection -= 1;
        }
        
    }
    
    public int getType()
    {
        return PredictionSchemeTransformType.WRAP;
    }
    
    @Override
    public void initializeEncoding(IntSpan origData, int numComponents)
    {
        super.initializeEncoding(origData, numComponents);
        // Go over the original values and compute the bounds.
        if (origData.size() == 0)
            return;
        final int tmp0 = origData.get(0);
        this.maxValue = tmp0;
        this.minValue = tmp0;
        for (int i = 1; i < origData.size(); ++i)
        {
            if (origData.get(i) < minValue)
            {
                this.minValue = origData.get(i);
            }
            else if (origData.get(i) > maxValue)
            {
                this.maxValue = origData.get(i);
            }
            
        }
        
        this.initCorrectionBounds();
        clampedValue = clampedValue == null ? new int[numComponents] : Arrays.copyOf(clampedValue, numComponents);
    }
    
    @Override
    public void initializeDecoding(int numComponents)
    {
        super.initializeDecoding(numComponents);
        clampedValue = clampedValue == null ? new int[numComponents] : Arrays.copyOf(clampedValue, numComponents);
    }
    
    /**
     *  Computes the corrections based on the input original value and the
     *  predicted value. Out of bound correction values are wrapped around the max
     *  range of input values.
     *
     */
    @Override
    public void computeCorrection(IntSpan originalVals, int originalOffset, IntSpan predictedVals, int predictedOffset, IntSpan outCorrVals, int outOffset, int valId)
    {
        super.computeCorrection(originalVals, originalOffset, this.clampPredictedValue(predictedVals, predictedOffset), 0, outCorrVals, outOffset, valId);
        // Wrap around if needed.
        for (int i = 0; i < this.numComponents; ++i)
        {
            int idx = outOffset + valId + i;
            int corrVal = outCorrVals.get(idx);
            if (corrVal < minCorrection)
            {
                outCorrVals.put(idx, corrVal + maxDif);
            }
            else if (corrVal > maxCorrection)
            {
                outCorrVals.put(idx, corrVal - maxDif);
            }
            
        }
        
        
        //////////////////
        /*
        
            for (int i = 0; i < numComponents; ++i)        
            {        
                ClampPredictedValue(predictedVals, predictedOffset);        
                outCorrVals[i + outOffset] = originalVals[i + originalOffset] - predictedVals[i + predictedOffset];        
                // Wrap around if needed.        
                var corrVal = outCorrVals[i + originalOffset];        
                if (corrVal < minCorrection)        
                    corrVal += maxDif;        
                else if (corrVal > maxCorrection)        
                    corrVal -= maxDif;        
                outCorrVals[i + originalOffset] = corrVal;        
            }        
            */        
    }
    
    /**
     *  Computes the original value from the input predicted value and the decoded
     *  corrections. Values out of the bounds of the input values are unwrapped.
     *
     */
    @Override
    public void computeOriginalValue(IntSpan predictedVals, int predictedOffset, IntSpan corrVals, int corrOffset, IntSpan outOriginalVals, int outOffset)
    {
        //base.ComputeOriginalValue(ClampPredictedValue(predictedVals, predictedOffset), 0, corrVals, corrOffset, outOriginalVals, outOffset, valId);
        predictedVals = this.clampPredictedValue(predictedVals, predictedOffset);
        for (int i = 0; i < this.numComponents; ++i)
        {
            int n = i + outOffset;
            outOriginalVals.put(n, predictedVals.get(i) + corrVals.get(i + corrOffset));
            if (outOriginalVals.get(n) > maxValue)
            {
                outOriginalVals.put(n, outOriginalVals.get(n) - maxDif);
            }
            else if (outOriginalVals.get(n) < minValue)
            {
                outOriginalVals.put(n, outOriginalVals.get(n) + maxDif);
            }
            
        }
        
    }
    
    IntSpan clampPredictedValue(IntSpan predictedVal, int offset)
    {
        for (int i = 0; i < this.numComponents; ++i)
        {
            int v = predictedVal.get(i + offset);
            if (v > maxValue)
            {
                clampedValue[i] = maxValue;
            }
            else if (v < minValue)
            {
                clampedValue[i] = minValue;
            }
            else
            {
                clampedValue[i] = v;
            }
            
        }
        
        return IntSpan.wrap(clampedValue);
    }
    
    @Override
    public boolean encodeTransformData(EncoderBuffer buffer)
    {
        // Store the input value range as it is needed by the decoder.
        buffer.encode2(minValue);
        buffer.encode2(maxValue);
        return true;
    }
    
    @Override
    public boolean decodeTransformData(DecoderBuffer buffer)
    {
        final int[] ref1 = new int[1];
        final int[] ref2 = new int[1];
        if (!buffer.decode6(ref1))
        {
            minValue = ref1[0];
            return false;
        }
        else
        {
            minValue = ref1[0];
        }
        
        if (!buffer.decode6(ref2))
        {
            maxValue = ref2[0];
            return false;
        }
        else
        {
            maxValue = ref2[0];
        }
        
        this.initCorrectionBounds();
        return true;
    }
    
    public PredictionSchemeWrapTransform()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            clampedValue = null;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
