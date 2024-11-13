package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class PredictionSchemeNormalOctahedronCanonicalizedTransform extends PredictionSchemeNormalOctahedronCanonicalizedTransformBase
{    
    public PredictionSchemeNormalOctahedronCanonicalizedTransform()
    {
    }
    
    public PredictionSchemeNormalOctahedronCanonicalizedTransform(int mod_value)
    {
        super(mod_value);
    }
    
    @Override
    public void initializeDecoding(int numComponents)
    {
    }
    
    @Override
    public boolean decodeTransformData(DecoderBuffer buffer)
    {
        int max_quantized_value;
        int center_value;
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        if (!buffer.decode6(ref0))
        {
            max_quantized_value = ref0[0];
            return false;
        }
        else
        {
            max_quantized_value = ref0[0];
        }
        
        if (!buffer.decode6(ref1))
        {
            center_value = ref1[0];
            return false;
        }
        else
        {
            center_value = ref1[0];
        }
        
        
        if (!this.setMaxQuantizedValue(max_quantized_value))
            return false;
        // Account for reading wrong values, e.g., due to fuzzing.
        if (this.getQuantizationBits() < 2)
            return false;
        if (this.getQuantizationBits() > 30)
            return false;
        return true;
    }
    
    @Override
    public void computeOriginalValue(IntSpan predictedVals, int predictedOffset, IntSpan corrVals, int corrOffset, IntSpan outOriginalVals, int outOffset)
    {
        IntVector pred = new IntVector(predictedVals.get(predictedOffset + 0), predictedVals.get(predictedOffset + 1));
        IntVector corr = new IntVector(corrVals.get(corrOffset + 0), corrVals.get(corrOffset + 1));
        IntVector orig = this.computeOriginalValue(pred, corr);
        
        outOriginalVals.put(outOffset, orig.x);
        outOriginalVals.put(outOffset + 1, orig.y);
    }
    
    private IntVector computeOriginalValue(IntVector pred, IntVector corr)
    {
        pred = pred == null ? new IntVector() : pred.clone();
        IntVector t = new IntVector(this.getCenterValue(), this.getCenterValue());
        pred.x -= t.x;
        pred.y -= t.y;
        boolean pred_is_in_diamond = this.isInDiamond(pred.x, pred.y);
        if (!pred_is_in_diamond)
        {
            this.octahedronToolBox.invertDiamond(pred);
        }
        
        boolean pred_is_in_bottom_left = this.isInBottomLeft(pred);
        int rotation_count = this.getRotationCount(pred);
        if (!pred_is_in_bottom_left)
        {
            this.rotatePoint(pred, rotation_count);
        }
        
        IntVector orig = new IntVector();
        orig.x = this.modMax(pred.x + corr.x);
        orig.y = this.modMax(pred.y + corr.y);
        if (!pred_is_in_bottom_left)
        {
            int reverse_rotation_count = (4 - rotation_count) % 4;
            this.rotatePoint(orig, reverse_rotation_count);
        }
        
        
        if (!pred_is_in_diamond)
        {
            this.octahedronToolBox.invertDiamond(orig);
        }
        
        
        orig.x += t.x;
        orig.y += t.y;
        return orig;
    }
    
    @Override
    public boolean encodeTransformData(EncoderBuffer buffer)
    {
        buffer.encode2(this.octahedronToolBox.getMaxQuantizedValue());
        buffer.encode2(this.octahedronToolBox.getCenterValue());
        return true;
    }
    
    @Override
    public void computeCorrection(IntSpan originalVals, int originalOffset, IntSpan predictedVals, int predictedOffset, IntSpan outCorrVals, int outOffset, int valId)
    {
        IntVector orig = new IntVector(originalVals.get(0) - this.getCenterValue(), originalVals.get(1) - this.getCenterValue());
        IntVector pred = new IntVector(predictedVals.get(0) - this.getCenterValue(), predictedVals.get(1) - this.getCenterValue());
        if (!this.isInDiamond(pred.x, pred.y))
        {
            this.octahedronToolBox.invertDiamond(orig);
            this.octahedronToolBox.invertDiamond(pred);
        }
        
        
        if (!this.isInBottomLeft(pred))
        {
            int rotation_count = this.getRotationCount(pred);
            this.rotatePoint(orig, rotation_count);
            this.rotatePoint(pred, rotation_count);
        }
        
        
        outCorrVals.put(0, this.makePositive(orig.x - pred.x));
        outCorrVals.put(1, this.makePositive(orig.y - pred.y));
    }
    
}
