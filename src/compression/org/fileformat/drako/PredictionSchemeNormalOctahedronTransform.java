package org.fileformat.drako;
import com.aspose.csporter.helpers.IntSpan;
/**
 *  The transform works on octahedral coordinates for normals. The square is
 *  subdivided into four inner triangles (diamond) and four outer triangles. The
 *  inner trianlges are associated with the upper part of the octahedron and the
 *  outer triangles are associated with the lower part.
 *  Given a preditiction value P and the actual value Q that should be encoded,
 *  this transform first checks if P is outside the diamond. If so, the outer
 *  triangles are flipped towards the inside and vice versa. The actual
 *  correction value is then based on the mapped P and Q values. This tends to
 *  result in shorter correction vectors.
 *  This is possible since the P value is also known by the decoder, see also
 *  ComputeCorrection and ComputeOriginalValue functions.
 *  Note that the tile is not periodic, which implies that the outer edges can
 *  not be identified, which requires us to use an odd number of values on each
 *  axis.
 *  DataTypeT is expected to be some integral type.
 * 
 *  This relates
 *  * IDF# 44535
 *  * Patent Application: GP-200957-00-US
 *
 */
class PredictionSchemeNormalOctahedronTransform extends PredictionSchemeNormalOctahedronTransformBase
{    
    public PredictionSchemeNormalOctahedronTransform()
    {
    }
    
    public PredictionSchemeNormalOctahedronTransform(int maxQuantizedValue)
    {
        this.setMaxQuantizedValue(maxQuantizedValue);
    }
    
    public int getType()
    {
        return PredictionSchemeTransformType.NORMAL_OCTAHEDRON;
    }
    
    @Override
    public boolean decodeTransformData(DecoderBuffer buffer)
    {
        int maxQuantizedValue;
        int centerValue;
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        if (!buffer.decode6(ref0))
        {
            maxQuantizedValue = ref0[0];
            return false;
        }
        else
        {
            maxQuantizedValue = ref0[0];
        }
        
        if (buffer.getBitstreamVersion() < 22)
        {
            if (!buffer.decode6(ref1))
            {
                centerValue = ref1[0];
                return false;
            }
            else
            {
                centerValue = ref1[0];
            }
            
        }
        
        
        return this.setMaxQuantizedValue(maxQuantizedValue);
    }
    
    @Override
    public boolean encodeTransformData(EncoderBuffer buffer)
    {
        buffer.encode2(this.octahedronToolBox.getMaxQuantizedValue());
        return true;
    }
    
    @Override
    public void computeCorrection(IntSpan originalVals, int originalOffset, IntSpan predictedVals, int predictedOffset, IntSpan outCorrVals, int outOffset, int valId)
    {
        IntVector orig = new IntVector(originalVals.get(originalOffset), originalVals.get(originalOffset + 1));
        IntVector pred = new IntVector(predictedVals.get(predictedOffset), predictedVals.get(predictedOffset + 1));
        IntVector corr = this.computeCorrection(orig, pred);
        
        outCorrVals.put(outOffset + valId, corr.x);
        outCorrVals.put(outOffset + valId + 1, corr.y);
    }
    
    @Override
    public void computeOriginalValue(IntSpan predictedVals, int predictedOffset, IntSpan corrVals, int corrOffset, IntSpan outOriginalVals, int outOffset)
    {
        IntVector pred = new IntVector(predictedVals.get(predictedOffset + 0), predictedVals.get(predictedOffset + 1));
        IntVector corr = new IntVector(corrVals.get(corrOffset), corrVals.get(corrOffset + 1));
        IntVector orig = this.computeOriginalValue(pred, corr);
        
        outOriginalVals.put(outOffset + 0, orig.x);
        outOriginalVals.put(outOffset + 1, orig.y);
    }
    
    private IntVector computeCorrection(IntVector orig, IntVector pred)
    {
        orig = orig == null ? new IntVector() : orig.clone();
        pred = pred == null ? new IntVector() : pred.clone();
        IntVector t = new IntVector(this.getCenterValue(), this.getCenterValue());
        final int[] ref2 = new int[1];
        final int[] ref3 = new int[1];
        final int[] ref4 = new int[1];
        final int[] ref5 = new int[1];
        orig.copyFrom(IntVector.sub(orig, t));
        pred.copyFrom(IntVector.sub(pred, t));
        
        if (!this.isInDiamond(pred.x, pred.y))
        {
            ref2[0] = orig.x;
            ref3[0] = orig.y;
            this.octahedronToolBox.invertDiamond(ref2, ref3);
            orig.x = ref2[0];
            orig.y = ref3[0];
            ref4[0] = pred.x;
            ref5[0] = pred.y;
            this.octahedronToolBox.invertDiamond(ref4, ref5);
            pred.x = ref4[0];
            pred.y = ref5[0];
        }
        
        IntVector corr = IntVector.sub(orig, pred);
        corr.x = this.makePositive(corr.x);
        corr.y = this.makePositive(corr.y);
        return corr;
    }
    
    private IntVector computeOriginalValue(IntVector pred, IntVector corr)
    {
        pred = pred == null ? new IntVector() : pred.clone();
        IntVector t = new IntVector(this.getCenterValue(), this.getCenterValue());
        pred.copyFrom(IntVector.sub(pred, t));
        boolean predIsInDiamond = this.isInDiamond(pred.x, pred.y);
        if (!predIsInDiamond)
        {
            this.octahedronToolBox.invertDiamond(pred);
        }
        
        IntVector orig = IntVector.add(pred, corr);
        orig.x = this.modMax(orig.x);
        orig.y = this.modMax(orig.y);
        if (!predIsInDiamond)
        {
            this.octahedronToolBox.invertDiamond(orig);
        }
        
        orig.copyFrom(IntVector.add(orig, t));
        return orig;
    }
    
}
