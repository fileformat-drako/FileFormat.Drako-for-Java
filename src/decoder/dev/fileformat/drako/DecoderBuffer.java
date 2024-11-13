package dev.fileformat.drako;
import dev.fileformat.drako.BitUtils;
import dev.fileformat.drako.Struct;
final class DecoderBuffer
{    
    private final BitDecoder bitDecoder = new BitDecoder();
    private boolean bitMode = false;
    private int pos;
    private int length;
    private BytePointer data = new BytePointer();
    private byte[] tmp;
    public int getBitstreamVersion()
    {
        return this.bitstreamVersion;
    }
    
    public void setBitstreamVersion(int value)
    {
        this.bitstreamVersion = value;
    }
    
    private int bitstreamVersion;
    public DecoderBuffer(byte[] data)
    {
        this.$initFields$();
        this.initialize(new BytePointer(data), data.length);
    }
    
    private DecoderBuffer(BytePointer data, int length)
    {
        this.$initFields$();
        this.initialize(data, length);
    }
    
    public DecoderBuffer()
    {
        this.$initFields$();
    }
    
    public boolean getBitDecoderActive()
    {
        return bitMode;
    }
    
    public void copyFrom(DecoderBuffer src)
    {
        bitDecoder.copyFrom(src.bitDecoder);
        this.bitMode = src.bitMode;
        this.pos = src.pos;
        this.length = src.length;
        this.data.copyFrom(src.data);
        this.setBitstreamVersion(src.getBitstreamVersion());
    }
    
    public DecoderBuffer clone()
    {
        DecoderBuffer ret = new DecoderBuffer();
        ret.copyFrom(this);
        return ret;
    }
    
    private void initialize(BytePointer data, int length)
    {
        this.pos = 0;
        this.bitMode = false;
        
        this.data.copyFrom(data);
        this.length = length;
    }
    
    public int getRemainingSize()
    {
        return length - pos;
    }
    
    public DecoderBuffer subBuffer(int offset)
    {
        int length = this.length - this.pos - offset;
        DecoderBuffer ret = new DecoderBuffer(BytePointer.add(data, pos + offset), length);
        ret.setBitstreamVersion(this.getBitstreamVersion());
        return ret;
    }
    
    public int getDecodedSize()
    {
        return pos;
    }
    
    public int getBufferSize()
    {
        return length;
    }
    
    public BytePointer getPointer()
    {
        return Struct.byVal(data);
    }
    
    /**
     *  Discards #bytes from the input buffer.
     *
     */
    public void advance(int bytes)
    {
        pos += bytes;
    }
    
    public boolean startBitDecoding(boolean decodeSize, long[] outSize)
    {
        final long[] ref0 = new long[1];
        outSize[0] = 0L;
        if (decodeSize)
        {
            if (this.getBitstreamVersion() < 22)
            {
                if (!this.decode(outSize))
                    return false;
            }
            else
            {
                long n;
                if (!Decoding.decodeVarint(ref0, this))
                {
                    n = ref0[0];
                    return false;
                }
                else
                {
                    n = ref0[0];
                }
                
                outSize[0] = n;
            }
            
        }
        
        this.bitMode = true;
        bitDecoder.load(BytePointer.add(data, pos), length - pos);
        return true;
    }
    
    public void endBitDecoding()
    {
        this.bitMode = false;
        int bitsDecoded = bitDecoder.getBitsDecoded();
        int bytesDecoded = (bitsDecoded + 7) / 8;
        pos += bytesDecoded;
    }
    
    public boolean decode(byte[] buf, int len)
    {
        return this.decode(buf, 0, len);
    }
    
    public boolean decode(int[] values)
    {
        int bytes = values.length * 4;
        
        if (!this.remainingIsEnough(bytes))
            return false;
        int n = data.getOffset() + pos;
        byte[] buf = data.getBaseData();
        for (int i = 0; i < values.length; i++)
        {
            values[i] = Unsafe.getLE32(buf, n);
            n += 4;
            pos += 4;
        }
        
        return true;
    }
    
    public boolean decode(float[] floats)
    {
        int bytes = floats.length * 4;
        if (bytes >= tmp.length)
        {
            this.tmp = new byte[bytes];
        }
        
        if (!this.decode(tmp, bytes))
            return false;
        Unsafe.toFloatArray(tmp, floats);
        return true;
    }
    
    public boolean decode(byte[] buf)
    {
        return this.decode(buf, 0, buf.length);
    }
    
    public boolean decode(byte[] buf, int start, int len)
    {
        if (!this.remainingIsEnough(len))
            return false;
        this.data.copy(pos, buf, start, len);
        pos += len;
        return true;
    }
    
    public boolean decode(short[] val)
    {
        if (!this.remainingIsEnough(2))
        {
            val[0] = 0;
            return false;
        }
        else
        {
            val[0] = data.toUInt16LE(pos);
            pos += 2;
            return true;
        }
        
    }
    
    public boolean decode(long[] val)
    {
        if (!this.remainingIsEnough(8))
        {
            val[0] = 0L;
            return false;
        }
        else
        {
            val[0] = data.toUInt64LE(pos);
            pos += 8;
            return true;
        }
        
    }
    
    private boolean remainingIsEnough(int size)
    {
        return length >= (pos + size);
    }
    
    public boolean peek(byte[] result, int size)
    {
        if (data.isOverflow(pos + size))
            return false;
        //overflow
        this.data.copy(pos, result, 0, size);
        return true;
    }
    
    /**
     *  Decodes up to 32 bits into outVal. Can be called only in between
     *  StartBitDecoding and EndBitDeoding. Otherwise returns false.
     *
     * @param nbits 
     * @param value 
     */
    public boolean decodeLeastSignificantBits32(int nbits, int[] value)
    {
        if (!bitMode)
        {
            value[0] = 0;
            return false;
        }
        
        value[0] = bitDecoder.getBits(nbits);
        return true;
    }
    
    @Override
    public String toString()
    {
        int preview = Math.min(16, this.getRemainingSize());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("0x%04x ", pos + data.getOffset()));
        for (int i = 0,  p = pos; i < preview; i++, p++)
        {
            sb.append(String.format("%02x ", data.get(p)));
        }
        
        int rest = this.getRemainingSize() - preview;
        if (rest > 0)
        {
            sb.append(String.format("...(%d bytes rest)", rest));
        }
        
        sb.append(String.format(", pos = %d", pos));
        return sb.toString();
    }
    
    private void $initFields$()
    {
        try
        {
            tmp = new byte[8];
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
    public boolean decode2(byte[] val)
    {
        byte tmp;
        final byte[] ref1 = new byte[1];
        if (this.decode3(ref1))
        {
            tmp = ref1[0];
            val[0] = tmp;
            return true;
        }
        else
        {
            tmp = ref1[0];
        }
        
        val[0] = 0;
        return false;
    }
    
    public boolean decode3(byte[] val)
    {
        if (!this.remainingIsEnough(1))
        {
            val[0] = 0;
            return false;
        }
        else
        {
            val[0] = this.data.get(pos);
            pos++;
            return true;
        }
        
    }
    
    public boolean decode4(float[] val)
    {
        if (!this.decode(tmp, 4))
        {
            val[0] = 0F;
            return false;
        }
        
        val[0] = BitUtils.getFloat(tmp, 0);
        return true;
    }
    
    public boolean decode5(int[] val)
    {
        int v;
        final int[] ref2 = new int[1];
        boolean ret = this.decode6(ref2);
        v = ref2[0];
        val[0] = v;
        return ret;
    }
    
    public boolean decode6(int[] val)
    {
        if (!this.remainingIsEnough(4))
        {
            val[0] = 0;
            return false;
        }
        else
        {
            val[0] = data.toUInt32LE(pos);
            pos += 4;
            return true;
        }
        
    }
    
}
