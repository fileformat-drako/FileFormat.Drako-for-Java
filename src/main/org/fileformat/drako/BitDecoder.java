package org.fileformat.drako;
final class BitDecoder
{    
    private BytePointer data = new BytePointer();
    private int dataEnd;
    private int bitOffset;
    public void copyFrom(BitDecoder bitDecoder)
    {
        this.data.copyFrom(bitDecoder.data);
        this.dataEnd = bitDecoder.dataEnd;
        this.bitOffset = bitDecoder.bitOffset;
    }
    
    public void load(BytePointer data, int count)
    {
        this.data.copyFrom(data);
        this.dataEnd = count;
        this.bitOffset = 0;
    }
    
    public int getBitsDecoded()
    {
        return bitOffset;
    }
    
    public void consume(int k)
    {
        bitOffset += k;
    }
    
    public int getBit()
    {
        int off = bitOffset;
        int byteOffset = off >> 3;
        int bitShift = (int)(off & 0x7);
        if (byteOffset < dataEnd)
        {
            int bit = (0xff & data.get(byteOffset)) >>> bitShift & 1;
            bitOffset++;
            return bit;
        }
        
        return 0;
    }
    
    public int peekBit(int offset)
    {
        int off = bitOffset + offset;
        int byteOffset = off >> 3;
        int bitShift = (int)(off & 0x7);
        if (byteOffset < dataEnd)
        {
            int bit = (0xff & data.get(byteOffset)) >>> bitShift & 1;
            return bit;
        }
        
        return 0;
    }
    
    public int getBits(int nbits)
    {
        int ret = 0;
        for (int bit = 0; bit < nbits; ++bit)
        {
            ret |= (int)(this.getBit() << bit);
        }
        
        return ret;
    }
    
    
}
