package org.fileformat.drako;
import com.aspose.csporter.helpers.IntSpan;
/**
 *  Basic prediction scheme based on computing backward differences between
 *  stored attribute values (also known as delta-coding). Usually works better
 *  than the reference point prediction scheme, because nearby values are often
 *  encoded next to each other.
 *
 */
class PredictionSchemeDifference extends PredictionScheme
{    
    public PredictionSchemeDifference(PointAttribute att, PredictionSchemeTransform transform_)
    {
        super(att, transform_);
    }
    
    public int getPredictionMethod()
    {
        return PredictionSchemeMethod.DIFFERENCE;
    }
    
    public boolean getInitialized()
    {
        return true;
    }
    
    @Override
    public boolean computeCorrectionValues(IntSpan inData, IntSpan outCorr, int size, int numComponents, int[] entryToPointIdMap)
    {
        
        this.transform_.initializeEncoding(inData, numComponents);
        // Encode data from the back using D(i) = D(i) - D(i - 1).
        for (int i = size - numComponents; i > 0; i -= numComponents)
        {
            this.transform_.computeCorrection(inData, i, inData, i - numComponents, outCorr, 0, i);
        }
        
        IntSpan zeroVals = IntSpan.wrap(new int[numComponents]);
        this.transform_.computeCorrection(inData, zeroVals, outCorr, 0);
        return true;
    }
    
    @Override
    public boolean computeOriginalValues(IntSpan inCorr, IntSpan outData, int size, int numComponents, int[] entryToPointIdMap)
    {
        this.transform_.initializeDecoding(numComponents);
        IntSpan zeroVals = IntSpan.wrap(new int[numComponents]);
        this.transform_.computeOriginalValue(zeroVals, inCorr, outData);
        
        // Decode data from the front using D(i) = D(i) + D(i - 1).
        for (int i = numComponents; i < size; i += numComponents)
        {
            this.transform_.computeOriginalValue(outData, i - numComponents, inCorr, i, outData, i);
        }
        
        return true;
    }
    
}
