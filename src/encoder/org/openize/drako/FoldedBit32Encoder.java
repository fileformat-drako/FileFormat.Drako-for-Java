package org.openize.drako;
class FoldedBit32Encoder implements IBitEncoder
{    
    public FoldedBit32Encoder()
    {
        this.folded_number_encoders_ = new RAnsBitEncoder[32];
        for (int i = 0; i < folded_number_encoders_.length; i++)
        {
            folded_number_encoders_[i] = new RAnsBitEncoder();
        }
        
        this.bit_encoder_ = new RAnsBitEncoder();
    }
    
    // Must be called before any Encode* function is called.
    // 
    @Override
    public void startEncoding()
    {
        for (int i = 0; i < 32; i++)
        {
            folded_number_encoders_[i].startEncoding();
        }
        
        
        bit_encoder_.startEncoding();
    }
    
    // Encode one bit. If |bit| is true encode a 1, otherwise encode a 0.
    // 
    public void encodeBit(boolean bit)
    {
        bit_encoder_.encodeBit(bit);
    }
    
    // Encode |nbits| of |value|, starting from the least significant bit.
    // |nbits| must be > 0 and <= 32.
    // 
    public void encodeLeastSignificantBits32(int nbits, int value)
    {
        int selector = 1 << (nbits - 1);
        for (int i = 0; i < nbits; i++)
        {
            boolean bit = (value & selector) != 0;
            folded_number_encoders_[i].encodeBit(bit);
            selector = selector >> 1;
        }
        
    }
    
    // Ends the bit encoding and stores the result into the target_buffer.
    // 
    public void endEncoding(EncoderBuffer target_buffer)
    {
        for (int i = 0; i < 32; i++)
        {
            folded_number_encoders_[i].endEncoding(target_buffer);
        }
        
        
        bit_encoder_.endEncoding(target_buffer);
    }
    
    @Override
    public void clear()
    {
        for (int i = 0; i < 32; i++)
        {
            folded_number_encoders_[i].clear();
        }
        
        
        bit_encoder_.clear();
    }
    
    private RAnsBitEncoder[] folded_number_encoders_;
    private RAnsBitEncoder bit_encoder_;
}
