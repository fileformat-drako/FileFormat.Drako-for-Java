package dev.fileformat.drako;
/**
 *  Class for decoding a sequence of bits that were encoded with RAnsBitEncoder.
 *
 */
class RAnsBitDecoder extends RAnsBitCodec implements IBitDecoder
{    
    private BytePointer buf = new BytePointer();
    private int offset;
    private int state;
    private byte probZero;
    /**
     *  Sets |sourceBuffer| as the buffer to decode bits from.
     *  Returns false when the data is invalid.
     *
     */
    public boolean startDecoding(DecoderBuffer sourceBuffer)
    {
        final byte[] ref0 = new byte[1];
        final int[] ref1 = new int[1];
        final int[] ref2 = new int[1];
        this.clear();
        
        if (!sourceBuffer.decode3(ref0))
        {
            probZero = ref0[0];
            return DracoUtils.failed();
        }
        else
        {
            probZero = ref0[0];
        }
        
        int sizeInBytes;
        if (sourceBuffer.getBitstreamVersion() < 22)
        {
            if (!sourceBuffer.decode5(ref1))
            {
                sizeInBytes = ref1[0];
                return DracoUtils.failed();
            }
            else
            {
                sizeInBytes = ref1[0];
            }
            
        }
        else if (!Decoding.decodeVarint(ref2, sourceBuffer))
        {
            sizeInBytes = ref2[0];
            return DracoUtils.failed();
        }
        else
        {
            sizeInBytes = ref2[0];
        }
        
        
        if ((0xffffffffl & sizeInBytes) > sourceBuffer.getRemainingSize())
            return DracoUtils.failed();
        
        if (this.aNSReadInit(BytePointer.add(sourceBuffer.getPointer(), sourceBuffer.getDecodedSize()), sizeInBytes) != 0)
            return DracoUtils.failed();
        sourceBuffer.advance(sizeInBytes);
        return true;
    }
    
    private int aNSReadInit(BytePointer buf, int offset)
    {
        buf = buf == null ? new BytePointer() : buf.clone();
        int x;
        if (offset < 1)
            return 1;
        this.buf.copyFrom(buf);
        x = (0xff & buf.get(offset - 1)) >>> 6;
        if (x == 0)
        {
            this.offset = offset - 1;
            this.state = (int)(0xff & buf.get(this.offset - 1) & 0x3F);
        }
        else if (x == 1)
        {
            if (offset < 2)
                return 1;
            this.offset = offset - 2;
            this.state = (int)(0xffff & buf.toUInt16LE(this.offset)) & 0x3FFF;
        }
        else if (x == 2)
        {
            if (offset < 3)
                return 1;
            this.offset = offset - 3;
            this.state = buf.toUInt24LE(this.offset) & 0x3FFFFF;
        }
        else
            return 1;
        state += L_BASE;
        if ((0xffffffffl & state) >= (L_BASE * IO_BASE))
            return 1;
        return 0;
    }
    
    boolean rabsRead(int p0)
    {
        boolean val;
        int quot;
        int rem;
        int x;
        int xn;
        int p = ANSP8_PRECISION - p0;
        if ((0xffffffffl & state) < L_BASE)
        {
            this.state = state * IO_BASE + (0xff & buf.get(--offset));
        }
        
        x = state;
        quot = x / ANSP8_PRECISION;
        rem = x % ANSP8_PRECISION;
        xn = (int)(quot * p);
        val = (0xffffffffl & rem) < p;
        if (val)
        {
            this.state = xn + rem;
        }
        else
        {
            // ans->state = quot * p0 + rem - p;
            this.state = (int)(x - xn - p);
        }
        
        return val;
    }
    
    /**
     *  Decode one bit. Returns true if the bit is a 1, otherwsie false.
     *
     */
    @Override
    public boolean decodeNextBit()
    {
        return this.rabsRead(0xff & probZero);
    }
    
    /**
     *  Decode the next |nbits| and return the sequence in |value|. |nbits| must be
     *  &gt; 0 and &lt;= 32.
     *
     */
    public int decodeLeastSignificantBits32(int nbits)
    {
        int result = 0;
        while (nbits > 0)
        {
            result = (int)((result << 1) + (this.decodeNextBit() ? 1 : 0));
            --nbits;
        }
        
        return result;
    }
    
    @Override
    public void endDecoding()
    {
    }
    
    public void clear()
    {
        this.state = L_BASE;
    }
    
    
}
