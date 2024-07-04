package org.fileformat.drako;
import com.aspose.csporter.helpers.HashBuilder;
import com.aspose.csporter.helpers.Struct;
import java.io.Serializable;
class RAnsBitCodec
{    
    public static final class RansSym implements Struct<RansSym>, Serializable
    {
        public int prob;
        public int cumProb;
        @Override
        public String toString()
        {
            return String.format("prob = %d, cumProb=%d", prob, cumProb);
        }
        
        public RansSym()
        {
        }
        
        private RansSym(RansSym other)
        {
            this.prob = other.prob;
            this.cumProb = other.cumProb;
        }
        
        @Override
        public RansSym clone()
        {
            return new RansSym(this);
        }
        
        @Override
        public void copyFrom(RansSym src)
        {
            if (src == null)
                return;
            this.prob = src.prob;
            this.cumProb = src.cumProb;
        }
        
        static final long serialVersionUID = 803763152L;
        @Override
        public int hashCode()
        {
            HashBuilder builder = new HashBuilder();
            builder.hash(this.prob);
            builder.hash(this.cumProb);
            return builder.hashCode();
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof RansSym))
                return false;
            RansSym rhs = (RansSym)obj;
            if (this.prob != rhs.prob)
                return false;
            if (this.cumProb != rhs.cumProb)
                return false;
            return true;
        }
        
    }
    
    protected static final int ANSP8_PRECISION = 256;
    protected static final int ANSP8_SHIFT = 8;
    protected static final int ANSP10_PRECISION = 1024;
    protected static final int L_BASE = ANSP10_PRECISION * 4;
    protected static final int IO_BASE = 256;
    /**
     *  Computes the desired precision of the rANS method for the specified maximal
     *  symbol bit length of the input data.
     *
     * @param maxBitLength 
     */
    public static int computeRAnsUnclampedPrecision(int maxBitLength)
    {
        return 3 * maxBitLength / 2;
    }
    
    /**
     *  Computes the desired precision clamped to guarantee a valid funcionality of
     *  our rANS library (which is between 12 to 20 bits).
     *
     * @param maxBitLength 
     */
    public static int computeRAnsPrecisionFromMaxSymbolBitLength(int maxBitLength)
    {
        return RAnsBitCodec.computeRAnsUnclampedPrecision(maxBitLength) < 12 ? 12 : RAnsBitCodec.computeRAnsUnclampedPrecision(maxBitLength) > 20 ? 20 : RAnsBitCodec.computeRAnsUnclampedPrecision(maxBitLength);
    }
    
    
}
