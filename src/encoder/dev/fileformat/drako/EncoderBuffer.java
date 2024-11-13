package dev.fileformat.drako;
import dev.fileformat.drako.ByteSpan;
import dev.fileformat.drako.IntSpan;
/**
 *  Class representing a buffer that can be used for either for byte-aligned
 *  encoding of arbitrary data structures or for encoding of varialble-length
 *  bit data.
 *
 */
class EncoderBuffer
{    
    private BitEncoder bitEncoder;
    private DataBuffer buffer;
    /**
     *  The number of bytes reserved for bit encoder.
     *  Values > 0 indicate we are in the bit encoding mode.
     *
     */
    private long bitEncoderReservedBytes;
    /**
     *  Flag used indicating that we need to store the length of the currently
     *  processed bit sequence.
     *
     */
    private boolean encodeBitSequenceSize;
    public void encode(short val)
    {
        int offset = buffer.getLength();
        this.debugBreak(2);
        buffer.setLength(buffer.getLength() + 2);
        Unsafe.putLE16(buffer.getBuffer(), offset, val);
    }
    
    public boolean encode(byte val)
    {
        int offset = buffer.getLength();
        this.debugBreak(1);
        buffer.setLength(buffer.getLength() + 1);
        buffer.set(offset, val);
        return true;
    }
    
    public void encode(int val)
    {
        int offset = buffer.getLength();
        this.debugBreak(4);
        buffer.setLength(buffer.getLength() + 4);
        Unsafe.putLE32(buffer.getBuffer(), offset, val);
    }
    
    public void encode(float val)
    {
        int offset = buffer.getLength();
        this.debugBreak(4);
        buffer.setLength(buffer.getLength() + 4);
        Unsafe.putLE32(buffer.getBuffer(), offset, Unsafe.floatToUInt32(val));
    }
    
    public void encode(float[] val)
    {
        int offset = buffer.getLength();
        this.debugBreak(4 * val.length);
        buffer.setLength(buffer.getLength() + (4 * val.length));
        for (int i = 0; i < val.length; i++)
        {
            Unsafe.putLE32(buffer.getBuffer(), offset, Unsafe.floatToUInt32(val[i]));
            offset += 4;
        }
        
    }
    
    public void encode(IntSpan val)
    {
        int offset = buffer.getLength();
        this.debugBreak(4 * val.size());
        buffer.setLength(buffer.getLength() + (4 * val.size()));
        for (int i = 0; i < val.size(); i++)
        {
            Unsafe.putLE32(buffer.getBuffer(), offset, val.get(i));
            offset += 4;
        }
        
    }
    
    public void encode(IntList val)
    {
        this.encode(val.data, val.getCount());
    }
    
    public void encode(int[] val)
    {
        this.encode(val, val.length);
    }
    
    public void encode(int[] val, int len)
    {
        int offset = buffer.getLength();
        this.debugBreak(4 * len);
        buffer.setLength(buffer.getLength() + (4 * len));
        for (int i = 0; i < len; i++)
        {
            Unsafe.putLE32(buffer.getBuffer(), offset, val[i]);
            offset += 4;
        }
        
    }
    
    public void encode(byte[] buffer, int length)
    {
        int offset = this.buffer.getLength();
        this.debugBreak(length);
        this.buffer.setLength(this.buffer.getLength() + length);
        System.arraycopy(buffer, 0, this.buffer.getBuffer(), offset, length);
    }
    
    public void encode(ByteSpan buffer, int start, int length)
    {
        int offset = this.buffer.getLength();
        this.debugBreak(length);
        this.buffer.setLength(this.buffer.getLength() + length);
        buffer.slice(start, length).copyTo(ByteSpan.wrap(this.buffer.getBuffer()).slice(offset));
    }
    
    private void debugBreak(int len)
    {
        /*
            int debugOffset = 29;        
            int offset = this.buffer.Length;        
            if (debugOffset >= offset && debugOffset < offset + len)        
                Debugger.Break();        
            */    }
    
    public void clear()
    {
        
        buffer.clear();
        this.bitEncoderReservedBytes = 0L;
    }
    
    public void resize(int nbytes)
    {
        this.debugBreak(nbytes - buffer.getLength());
        buffer.setLength(nbytes);
    }
    
    /**
     *  Start encoding a bit sequence. A maximum size of the sequence needs to
     *  be known upfront.
     *  If encodeSize is true, the size of encoded bit sequence is stored before
     *  the sequence. Decoder can then use this size to skip over the bit sequence
     *  if needed.
     *  Returns false on error.
     *
     */
    public boolean startBitEncoding(int requiredBits, boolean encodeSize)
    {
        
        if (this.getBitEncoderActive())
            return false;
        // Bit encoding mode already active.
        if (requiredBits <= 0)
            return false;
        // Invalid size.
        this.encodeBitSequenceSize = encodeSize;
        int requiredBytes = (requiredBits + 7) / 8;
        this.bitEncoderReservedBytes = requiredBytes;
        int bufferStartSize = buffer.getLength();
        if (encodeSize)
        {
            // Reserve memory for storing the encoded bit sequence size. It will be
            // filled once the bit encoding ends.
            bufferStartSize += 8;
            //sizeof(long)
        }
        
        // Resize buffer to fit the maximum size of encoded bit data.
        this.debugBreak(requiredBytes);
        buffer.setLength(bufferStartSize + requiredBytes);
        BytePointer data = new BytePointer(this.getData(), bufferStartSize);
        this.bitEncoder = new BitEncoder(data);
        return true;
    }
    
    /**
     *  End the encoding of the bit sequence and return to the default byte-aligned
     *  encoding.
     *
     */
    public void endBitEncoding()
    {
        
        if (!this.getBitEncoderActive())
            return;
        long encodedBits = bitEncoder.getBits();
        long encodedBytes = (encodedBits + 7L) / 8L;
        // Encode size if needed.
        if (encodeBitSequenceSize)
        {
            int out_mem = (int)(this.getBytes() - (bitEncoderReservedBytes + 8L));
            EncoderBuffer var_size_buffer = new EncoderBuffer();
            Encoding.encodeVarint(encodedBytes, var_size_buffer);
            int size_len = var_size_buffer.getBytes();
            int dst = out_mem + size_len;
            int src = out_mem + 8;
            System.arraycopy(this.getData(), src, this.getData(), dst, (int)encodedBytes);
            // Store the size of the encoded data.
            System.arraycopy(var_size_buffer.buffer.getBuffer(), 0, this.getData(), out_mem, size_len);
            
            // We need to account for the difference between the preallocated and actual
            // storage needed for storing the encoded length. This will be used later to
            // compute the correct size of |buffer_|.
            bitEncoderReservedBytes += 8 - size_len;
        }
        
        
        // Resize the underlying buffer to match the number of encoded bits.
        buffer.setLength((int)(buffer.getLength() - bitEncoderReservedBytes + encodedBytes));
        this.bitEncoderReservedBytes = 0L;
    }
    
    /**
     *  Encode up to 32 bits into the buffer. Can be called only in between
     *  StartBitEncoding and EndBitEncoding. Otherwise returns false.
     *
     */
    public boolean encodeLeastSignificantBits32(int nbits, int value)
    {
        if (!this.getBitEncoderActive())
            return false;
        bitEncoder.putBits(value, nbits);
        return true;
    }
    
    public boolean getBitEncoderActive()
    {
        return bitEncoderReservedBytes > 0L;
    }
    
    void encode(IntSpan ints, int bytesOffset, int bytes)
    {
        int offset = this.buffer.getLength();
        this.buffer.setLength(this.buffer.getLength() + bytes);
        IntSpan dst = ByteSpan.wrap(buffer.getBuffer(), offset, bytes).asIntSpan();
        ints.slice(bytesOffset / 4, bytes / 4).copyTo(dst);
    }
    
    void encode(IntSpan ints, int bytes)
    {
        this.encode(ints, 0, bytes);
    }
    
    public BitEncoder getBitEncoder()
    {
        return bitEncoder;
    }
    
    public int getBytes()
    {
        return buffer.getLength();
    }
    
    public byte[] getData()
    {
        return buffer.getBuffer();
    }
    
    public EncoderBuffer()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            buffer = new DataBuffer();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
    public void encode2(int val)
    {
        this.encode(val);
    }
    
}
