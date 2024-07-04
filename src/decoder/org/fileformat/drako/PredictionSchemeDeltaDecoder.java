package org.fileformat.drako;
import com.aspose.csporter.helpers.IntSpan;
class PredictionSchemeDeltaDecoder extends PredictionScheme
{    
    public boolean getInitialized()
    {
        return true;
    }
    
    public int getPredictionMethod()
    {
        return PredictionSchemeMethod.DIFFERENCE;
    }
    
    public PredictionSchemeDeltaDecoder(PointAttribute attribute, PredictionSchemeTransform transform)
    {
        super(attribute, transform);
    }
    
    @Override
    public boolean computeCorrectionValues(IntSpan in_data, IntSpan out_corr, int size, int num_components, int[] entry_to_point_id_map)
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean computeOriginalValues(IntSpan in_corr, IntSpan out_data, int size, int num_components, int[] entry_to_point_id_map)
    {
        
        this.transform_.initializeDecoding(num_components);
        IntSpan zero_vals = IntSpan.wrap(new int[num_components]);
        this.transform_.computeOriginalValue(zero_vals, in_corr, out_data);
        
        // Decode data from the front using D(i) = D(i) + D(i - 1).
        for (int i = num_components; i < size; i += num_components)
        {
            this.transform_.computeOriginalValue(out_data, i - num_components, in_corr, i, out_data, i);
        }
        
        
        return true;
    }
    
}
