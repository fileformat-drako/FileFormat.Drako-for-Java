package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
/**
 *  PredictionSchemeTransform is used to transform predicted values into
 *  correction values and vice versa.
 *  TDataType is the data type of predicted valuesPredictionSchemeTransform.
 *  CorrTypeT is the data type used for storing corrected values. It allows
 *  transforms to store corrections into a different type or format compared to
 *  the predicted data.
 *
 */
class PredictionSchemeTransform
{    
    protected int numComponents;
    public int getType()
    {
        return PredictionSchemeTransformType.DELTA;
    }
    
    /**
     *  Performs any custom initialization of the trasnform for the encoder.
     *  |size| = total number of values in |origData| (i.e., number of entries *
     *  number of components).
     *
     */
    public void initializeEncoding(IntSpan origData, int numComponents)
    {
        this.numComponents = numComponents;
    }
    
    public void initializeDecoding(int numComponents)
    {
        this.numComponents = numComponents;
    }
    
    /**
     *  Computes the corrections based on the input original values and the
     *  predicted values. The correction is always computed for all components
     *  of the input element. |valId| is the id of the input value
     *  (i.e., elementId * numComponents). The default implementation is equal to
     *  std::minus.
     *
     */
    public void computeCorrection(IntSpan originalVals, int originalOffset, IntSpan predictedVals, int predictedOffset, IntSpan outCorrVals, int outOffset, int valId)
    {
        outOffset += valId;
        for (int i = 0; i < numComponents; ++i)
        {
            outCorrVals.put(outOffset + i, originalVals.get(originalOffset + i) - predictedVals.get(predictedOffset + i));
        }
        
    }
    
    public void computeCorrection(IntSpan originalVals, IntSpan predictedVals, IntSpan outCorrVals, int valId)
    {
        this.computeCorrection(originalVals, 0, predictedVals, 0, outCorrVals, 0, valId);
    }
    
    /**
     *  Computes the original value from the input predicted value and the decoded
     *  corrections. The default implementation is equal to std:plus.
     *
     */
    public void computeOriginalValue(IntSpan predictedVals, int predictedOffset, IntSpan corrVals, int corrOffset, IntSpan outOriginalVals, int outOffset)
    {
        for (int i = 0; i < numComponents; ++i)
        {
            outOriginalVals.put(i + outOffset, predictedVals.get(i + predictedOffset) + corrVals.get(i + corrOffset));
        }
        
    }
    
    public void computeOriginalValue(IntSpan predictedVals, IntSpan corrVals, IntSpan outOriginalVals)
    {
        this.computeOriginalValue(predictedVals, 0, corrVals, 0, outOriginalVals, 0);
    }
    
    /**
     *  Encode any transform specific data.
     *
     */
    public void encodeTransformData(EncoderBuffer buffer)
    {
    }
    
    /**
     *  Decodes any transform specific data. Called before Initialize() method.
     *
     */
    public void decodeTransformData(DecoderBuffer buffer)
        throws DrakoException
    {
    }
    
    /**
     *  Should return true if all corrected values are guaranteed to be positive.
     *
     */
    public boolean areCorrectionsPositive()
    {
        return false;
    }
    
    
}
