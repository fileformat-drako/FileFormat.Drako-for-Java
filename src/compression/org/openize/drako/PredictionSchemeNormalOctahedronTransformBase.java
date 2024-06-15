package org.openize.drako;
class PredictionSchemeNormalOctahedronTransformBase extends PredictionSchemeTransform
{    
    protected OctahedronToolBox octahedronToolBox;
    public int getCenterValue()
    {
        return octahedronToolBox.getCenterValue();
    }
    
    public int getQuantizationBits()
    {
        return octahedronToolBox.getQuantizationBits();
    }
    
    protected boolean setMaxQuantizedValue(int value)
    {
        if (value % 2 == 0)
            return false;
        int q = DracoUtils.mostSignificantBit(value) + 1;
        octahedronToolBox.setQuantizationBits(q);
        return true;
    }
    
    // For correction values.
    // 
    protected int makePositive(int x)
    {
        return octahedronToolBox.makePositive(x);
    }
    
    protected int modMax(int x)
    {
        return octahedronToolBox.modMax(x);
    }
    
    protected boolean isInDiamond(int s, int t)
    {
        return octahedronToolBox.isInDiamond(s, t);
    }
    
    // We can return true as we keep correction values positive.
    // 
    @Override
    public boolean areCorrectionsPositive()
    {
        return true;
    }
    
    public PredictionSchemeNormalOctahedronTransformBase()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            octahedronToolBox = new OctahedronToolBox();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
