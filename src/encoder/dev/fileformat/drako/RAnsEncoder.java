package dev.fileformat.drako;
/**
 *  Class for performing rANS encoding using a desired number of precision bits.
 *  The max number of precision bits is currently 19. The actual number of
 *  symbols in the input alphabet should be (much) smaller than that, otherwise
 *  the compression rate may suffer.
 *
 */
class RAnsEncoder extends RAnsBitCodec
{    
    private int precisionBits;
    private int ransPrecision;
    private int lRansBase;
    private BytePointer buf = new BytePointer();
    private int bufOffset;
    private int state;
    public RAnsEncoder(int precisionBits)
    {
        this.precisionBits = precisionBits;
        this.ransPrecision = 1 << precisionBits;
        this.lRansBase = ransPrecision * 4;
    }
    
    public void reset(BytePointer ptr)
    {
        this.buf.copyFrom(ptr);
        this.bufOffset = 0;
        this.state = lRansBase;
    }
    
    // Needs to be called after all symbols are encoded.
    // 
    public int writeEnd()
    {
        int state;
        //assert(ans.state >= lRansBase);
        //assert(ans.state < lRansBase * ioBase);
        state = this.state - lRansBase;
        if ((0xffffffffl & state) < (1 << 6))
        {
            this.buf.set(this.bufOffset, (byte)((0x00 << 6) + state));
            return this.bufOffset + 1;
        }
        else if ((0xffffffffl & state) < (1 << 14))
        {
            Unsafe.putLE16(this.buf.getBaseData(), buf.getOffset() + bufOffset, (short)((0x01 << 14) + state));
            return this.bufOffset + 2;
        }
        else if ((0xffffffffl & state) < (1 << 22))
        {
            Unsafe.putLE24(this.buf.getBaseData(), buf.getOffset() + bufOffset, (0x02 << 22) + state);
            return this.bufOffset + 3;
        }
        else if ((0xffffffffl & state) < (1 << 30))
        {
            Unsafe.putLE32(this.buf.getBaseData(), buf.getOffset() + bufOffset, (int)((0x03 << 30) + state));
            return bufOffset + 4;
        }
        else
            throw new RuntimeException("Invalid rANS state.");
    }
    
    /**
     *  rANS with normalization
     *  sym->prob takes the place of lS from the paper
     *  ransPrecision is m
     *
     */
    public void write(RAnsBitCodec.RansSym sym)
    {
        int p = sym.prob;
        while (this.state >= (lRansBase / ransPrecision * IO_BASE * p))
        {
            buf.set(bufOffset++, (byte)(this.state % IO_BASE));
            state /= IO_BASE;
        }
        
        // TODO(ostava): The division and multiplication should be optimized.
        this.state = (int)(state / p * ransPrecision + (state % p) + sym.cumProb);
    }
    
}
