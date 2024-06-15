package org.openize.drako;
/**
 *  Class for dequantizing values that were previously quantized using the
 *  Quantizer class.
 *
 */
class Dequantizer
{    
    private float delta;
    /**
     *  Initializes the dequantizer. Both parameters must correspond to the values
     *  provided to the initializer of the Quantizer class.
     *
     */
    public Dequantizer(float range, int maxQuantizedValue)
    {
        if (maxQuantizedValue > 0)
        {
            this.delta = range / (float)maxQuantizedValue;
        }
        
    }
    
    public float dequantizeFloat(int v)
    {
        return v * delta;
    }
    
}
