package org.fileformat.drako;
class DirectBitEncoder implements IBitEncoder
{    
    // Must be called before any Encode* function is called.
    // 
    @Override
    public void startEncoding()
    {
        this.clear();
    }
    
    // Encode one bit. If |bit| is true encode a 1, otherwise encode a 0.
    // 
    public void encodeBit(boolean bit)
    {
        if (bit)
        {
            local_bits_ |= 1 << (31 - num_local_bits_);
        }
        
        
        num_local_bits_++;
        if (num_local_bits_ == 32)
        {
            bits_.add(local_bits_);
            this.num_local_bits_ = 0;
            this.local_bits_ = 0;
        }
        
    }
    
    // Encode |nbits| of |value|, starting from the least significant bit.
    // |nbits| must be > 0 and <= 32.
    // 
    public void encodeLeastSignificantBits32(int nbits, int value)
    {
        int remaining = 32 - num_local_bits_;
        
        // Make sure there are no leading bits that should not be encoded and
        // start from here.
        value = value << (32 - nbits);
        if (nbits <= remaining)
        {
            value = value >>> num_local_bits_;
            this.local_bits_ = local_bits_ | value;
            num_local_bits_ += nbits;
            if (num_local_bits_ == 32)
            {
                bits_.add(local_bits_);
                this.local_bits_ = 0;
                this.num_local_bits_ = 0;
            }
            
        }
        else
        {
            value = value >>> (32 - nbits);
            this.num_local_bits_ = nbits - remaining;
            int value_l = value >>> num_local_bits_;
            this.local_bits_ = local_bits_ | value_l;
            bits_.add(local_bits_);
            this.local_bits_ = value << (32 - num_local_bits_);
        }
        
    }
    
    // Ends the bit encoding and stores the result into the target_buffer.
    // 
    public void endEncoding(EncoderBuffer target_buffer)
    {
        
        bits_.add(local_bits_);
        int size_in_byte = bits_.getCount() * 4;
        target_buffer.encode2(size_in_byte);
        target_buffer.encode(bits_);
        this.clear();
    }
    
    @Override
    public void clear()
    {
        
        bits_.clear();
        this.local_bits_ = 0;
        this.num_local_bits_ = 0;
    }
    
    private IntList bits_;
    int local_bits_;
    int num_local_bits_;
    public DirectBitEncoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            bits_ = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
