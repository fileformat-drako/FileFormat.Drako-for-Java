package org.openize.drako;
class BitEncoder
{    
    private BytePointer buffer = new BytePointer();
    private int bitOffset;
    public BitEncoder(BytePointer buffer)
    {
        this.buffer.copyFrom(buffer);
    }
    
    /**
     *  Write |nbits| of |data| into the bit buffer.
     *
     */
    public void putBits(int data, int nbits)
    {
        for (int bit = 0; bit < nbits; ++bit)
        {
            this.putBit((byte)(data >>> bit & 1));
        }
        
    }
    
    /**
     *  Return number of bits encoded so far.
     *
     */
    public int getBits()
    {
        return bitOffset;
    }
    
    private void putBit(byte value)
    {
        int byteSize = 8;
        int off = bitOffset;
        int byteOffset = off / byteSize;
        int bitShift = off % byteSize;
        
        // TODO(fgalligan): Check performance if we add a branch and only do one
        // memory write if bitShift is 7. Also try using a temporary variable to
        // hold the bits before writing to the buffer.
        buffer.set(byteOffset, (byte)(0xff & buffer.get(byteOffset) & ~(1 << bitShift) | ((0xff & value) << bitShift)));
        byte t = buffer.get(byteOffset);
        t = (byte)(0xff & t & ~(1 << bitShift));
        t = (byte)(0xff & t | ((0xff & value) << bitShift));
        buffer.set(byteOffset, t);
        bitOffset++;
    }
    
}
