package org.openize.drako;
class FoldedBit32Decoder implements IBitDecoder
{    
    RAnsBitDecoder[] folded_number_decoders_;
    RAnsBitDecoder bit_decoder_;
    public FoldedBit32Decoder()
    {
        this.$initFields$();
        for (int i = 0; i < folded_number_decoders_.length; i++)
        {
            folded_number_decoders_[i] = new RAnsBitDecoder();
        }
        
        this.bit_decoder_ = new RAnsBitDecoder();
    }
    
    // Sets |source_buffer| as the buffer to decode bits from.
    // 
    public boolean startDecoding(DecoderBuffer source_buffer)
    {
        for (int i = 0; i < 32; i++)
        {
            if (!folded_number_decoders_[i].startDecoding(source_buffer))
                return false;
        }
        
        
        return bit_decoder_.startDecoding(source_buffer);
    }
    
    // Decode one bit. Returns true if the bit is a 1, otherwise false.
    // 
    @Override
    public boolean decodeNextBit()
    {
        return bit_decoder_.decodeNextBit();
    }
    
    // Decode the next |nbits| and return the sequence in |value|. |nbits| must be
    // > 0 and <= 32.
    // 
    public int decodeLeastSignificantBits32(int nbits)
    {
        int result = 0;
        for (int i = 0; i < nbits; ++i)
        {
            boolean bit = folded_number_decoders_[i].decodeNextBit();
            result = (result << 1) + (bit ? 1 : 0);
        }
        
        
        return result;
    }
    
    @Override
    public void endDecoding()
    {
        for (int i = 0; i < 32; i++)
        {
            folded_number_decoders_[i].endDecoding();
        }
        
        
        bit_decoder_.endDecoding();
    }
    
    public void clear()
    {
        for (int i = 0; i < 32; i++)
        {
            folded_number_decoders_[i].clear();
        }
        
        
        bit_decoder_.clear();
    }
    
    private void $initFields$()
    {
        try
        {
            folded_number_decoders_ = new RAnsBitDecoder[32];
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
