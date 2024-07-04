package org.fileformat.drako;
import com.aspose.csporter.helpers.AsposeUtils;
import com.aspose.csporter.helpers.HashBuilder;
import com.aspose.csporter.helpers.Struct;
import java.io.Serializable;
import java.util.ArrayList;
/**
 *  Class for encoding a sequence of bits using rANS. The probability table used
 *  to encode the bits is based off the total counts of bits.
 *  TODO(fgalligan): Investigate using an adaptive table for more compression.
 *
 */
class RAnsBitEncoder extends RAnsBitCodec implements IBitEncoder
{    
    static final class AnsCoder implements Struct<AnsCoder>, Serializable
    {
        byte[] buf;
        int bufOffset;
        int state;
        public AnsCoder()
        {
        }
        
        private AnsCoder(AnsCoder other)
        {
            this.buf = other.buf;
            this.bufOffset = other.bufOffset;
            this.state = other.state;
        }
        
        @Override
        public AnsCoder clone()
        {
            return new AnsCoder(this);
        }
        
        @Override
        public void copyFrom(AnsCoder src)
        {
            if (src == null)
                return;
            this.buf = src.buf;
            this.bufOffset = src.bufOffset;
            this.state = src.state;
        }
        
        static final long serialVersionUID = 1008400197L;
        @Override
        public int hashCode()
        {
            HashBuilder builder = new HashBuilder();
            builder.hash(this.buf);
            builder.hash(this.bufOffset);
            builder.hash(this.state);
            return builder.hashCode();
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof AnsCoder))
                return false;
            AnsCoder rhs = (AnsCoder)obj;
            if (!AsposeUtils.equals(this.buf, rhs.buf))
                return false;
            if (this.bufOffset != rhs.bufOffset)
                return false;
            if (this.state != rhs.state)
                return false;
            return true;
        }
        
    }
    
    private long[] bitCounts;
    private ArrayList<Integer> bits;
    private int localBits;
    private int numLocalBits;
    /**
     *  Must be called before any Encode* function is called.
     *
     */
    @Override
    public void startEncoding()
    {
        this.clear();
    }
    
    /**
     *  Encode one bit. If |bit| is true encode a 1, otherwise encode a 0.
     *
     */
    public void encodeBit(boolean bit)
    {
        
        if (bit)
        {
            bitCounts[1]++;
            localBits |= (int)(1 << numLocalBits);
        }
        else
        {
            bitCounts[0]++;
        }
        
        numLocalBits++;
        
        if (numLocalBits == 32)
        {
            bits.add(localBits);
            this.numLocalBits = 0;
            this.localBits = 0;
        }
        
    }
    
    /**
     *  Encode |nibts| of |value|, starting from the least significant bit.
     *  |nbits| must be &gt; 0 and &lt;= 32.
     *
     */
    public void encodeLeastSignificantBits32(int nbits, int value)
    {
        int reversed = DracoUtils.reverseBits32(value) >>> (32 - nbits);
        int ones = DracoUtils.countOnes32(reversed);
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        final int[] ref2 = new int[1];
        bitCounts[0] += (long)(nbits - ones);
        bitCounts[1] += (long)ones;
        int remaining = (int)(32 - numLocalBits);
        
        if (nbits <= remaining)
        {
            ref0[0] = localBits;
            DracoUtils.copyBits32(ref0, numLocalBits, reversed, 0, nbits);
            localBits = ref0[0];
            numLocalBits += nbits;
            if (numLocalBits == 32)
            {
                bits.add(localBits);
                this.localBits = 0;
                this.numLocalBits = 0;
            }
            
        }
        else
        {
            ref1[0] = localBits;
            DracoUtils.copyBits32(ref1, numLocalBits, reversed, 0, remaining);
            localBits = ref1[0];
            bits.add(localBits);
            this.localBits = 0;
            ref2[0] = localBits;
            DracoUtils.copyBits32(ref2, 0, reversed, remaining, nbits - remaining);
            localBits = ref2[0];
            this.numLocalBits = (int)(nbits - remaining);
        }
        
    }
    
    /**
     *  Ends the bit encoding and stores the result into the targetBuffer.
     *
     */
    public void endEncoding(EncoderBuffer targetBuffer)
    {
        long total = bitCounts[1] + bitCounts[0];
        if (total == 0L)
        {
            total++;
        }
        
        int zeroProbRaw = (int)(bitCounts[0] / (double)total * 256.0 + 0.5);
        byte zeroProb = -1;
        if ((0xffffffffl & zeroProbRaw) < 255)
        {
            zeroProb = (byte)zeroProbRaw;
        }
        
        
        zeroProb += (byte)(zeroProb == 0 ? 1 : 0);
        byte[] buffer = new byte[(bits.size() + 8) * 8];
        AnsCoder ansCoder = new AnsCoder();
        RAnsBitEncoder.ansWriteInit(ansCoder, buffer);
        
        for (int i = numLocalBits - 1; i >= 0; --i)
        {
            int bit = localBits >>> i & 1;
            RAnsBitEncoder.rabsDescWrite(ansCoder, bit, zeroProb);
        }
        
        for (int j = bits.size() - 1; j >= 0; j--)
        {
            int nbits = this.bits.get(j);
            for (int i = 31; i >= 0; --i)
            {
                int bit = nbits >>> i & 1;
                RAnsBitEncoder.rabsDescWrite(ansCoder, bit, zeroProb);
            }
            
        }
        
        int sizeInBytes = RAnsBitEncoder.ansWriteEnd(ansCoder);
        targetBuffer.encode(zeroProb);
        Encoding.encodeVarint(sizeInBytes, targetBuffer);
        targetBuffer.encode(buffer, sizeInBytes);
        
        this.clear();
    }
    
    /**
     *  rABS with descending spread
     *  p or p0 takes the place of lS from the paper
     *  ansP8Precision is m
     *
     */
    static void rabsDescWrite(AnsCoder ans, int val, byte p0)
    {
        int p = ANSP8_PRECISION - (0xff & p0);
        int lS = val == 1 ? p : 0xff & p0;
        int quot;
        int rem;
        if (ans.state >= (L_BASE / ANSP8_PRECISION * IO_BASE * lS))
        {
            ans.buf[ans.bufOffset++] = (byte)(ans.state % IO_BASE);
            ans.state /= IO_BASE;
        }
        
        
        quot = (int)(ans.state / lS);
        rem = (int)(ans.state - (quot * lS));
        
        ans.state = (int)(quot * ANSP8_PRECISION + rem + (val == 1 ? 0 : p));
    }
    
    static void ansWriteInit(AnsCoder ans, byte[] buf)
    {
        ans.buf = buf;
        ans.bufOffset = 0;
        ans.state = L_BASE;
    }
    
    static int ansWriteEnd(AnsCoder ans)
    {
        int state;
        //assert(ans->state >= lBase);
        //assert(ans->state < lBase * ioBase);
        state = ans.state - L_BASE;
        if ((0xffffffffl & state) < (1 << 6))
        {
            ans.buf[ans.bufOffset] = (byte)((0x00 << 6) + state);
            return ans.bufOffset + 1;
        }
        else if ((0xffffffffl & state) < (1 << 14))
        {
            Unsafe.putLE16(ans.buf, ans.bufOffset, (short)((0x01 << 14) + state));
            return ans.bufOffset + 2;
        }
        else if ((0xffffffffl & state) < (1 << 22))
        {
            Unsafe.putLE24(ans.buf, ans.bufOffset, (int)((0x02 << 22) + state));
            return ans.bufOffset + 3;
        }
        else
            throw new RuntimeException("Invalid RAns state");
    }
    
    @Override
    public void clear()
    {
        
        bitCounts[0] = 0L;
        bitCounts[1] = 0L;
        bits.clear();
        this.localBits = 0;
        this.numLocalBits = 0;
    }
    
    public RAnsBitEncoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            bitCounts = new long[2];
            bits = new ArrayList<Integer>();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
