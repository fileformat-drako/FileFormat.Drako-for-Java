package dev.fileformat.drako;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
/**
 *  Class for quantizing single precision floating point values. The values must
 *  be centered around zero and be within interval (-range, +range), where the
 *  range is specified in the Init() method.
 *
 */
final class Quantizer implements Struct<Quantizer>, Serializable
{    
    private float inverse_delta_;
    public Quantizer(float range, int maxQuantizedValue)
    {
        this.inverse_delta_ = (float)maxQuantizedValue / range;
    }
    
    public int quantizeFloat(float val)
    {
        val *= inverse_delta_;
        return (int)Math.floor(val + 0.5f);
    }
    
    public Quantizer()
    {
    }
    
    private Quantizer(Quantizer other)
    {
        this.inverse_delta_ = other.inverse_delta_;
    }
    
    @Override
    public Quantizer clone()
    {
        return new Quantizer(this);
    }
    
    @Override
    public void copyFrom(Quantizer src)
    {
        if (src == null)
            return;
        this.inverse_delta_ = src.inverse_delta_;
    }
    
    static final long serialVersionUID = 1166676417L;
    @Override
    public int hashCode()
    {
        HashBuilder builder = new HashBuilder();
        builder.hash(this.inverse_delta_);
        return builder.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Quantizer))
            return false;
        Quantizer rhs = (Quantizer)obj;
        if (this.inverse_delta_ != rhs.inverse_delta_)
            return false;
        return true;
    }
    
}
