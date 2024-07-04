package org.fileformat.drako;
class DirectBitDecoder implements IBitDecoder
{    
    int[] bits_;
    int pos_;
    int num_used_bits_;
    public void clear()
    {
        this.bits_ = null;
        this.num_used_bits_ = 0;
        this.pos_ = 0;
    }
    
    // Sets |source_buffer| as the buffer to decode bits from.
    // 
    public boolean startDecoding(DecoderBuffer source_buffer)
    {
        final int[] ref0 = new int[1];
        
        this.clear();
        int size_in_bytes;
        if (!source_buffer.decode6(ref0))
        {
            size_in_bytes = ref0[0];
            return false;
        }
        else
        {
            size_in_bytes = ref0[0];
        }
        
        
        // Check that size_in_bytes is > 0 and a multiple of 4 as the encoder always
        // encodes 32 bit elements.
        if (size_in_bytes == 0 || ((size_in_bytes & 0x3) != 0))
            return false;
        if (size_in_bytes > source_buffer.getRemainingSize())
            return false;
        int num_32bit_elements = size_in_bytes / 4;
        this.bits_ = new int[num_32bit_elements];
        if (!source_buffer.decode(bits_))
            return false;
        this.pos_ = 0;
        this.num_used_bits_ = 0;
        return true;
    }
    
    // Decode one bit. Returns true if the bit is a 1, otherwise false.
    // 
    @Override
    public boolean decodeNextBit()
    {
        int selector = 1 << (31 - num_used_bits_);
        if (pos_ == bits_.length)
            return false;
        boolean bit = (bits_[pos_] & selector) != 0;
        ++num_used_bits_;
        if (num_used_bits_ == 32)
        {
            ++pos_;
            this.num_used_bits_ = 0;
        }
        
        
        return bit;
    }
    
    // Decode the next |nbits| and return the sequence in |value|. |nbits| must be
    // > 0 and <= 32.
    // 
    public int decodeLeastSignificantBits32(int nbits)
    {
        int remaining = 32 - num_used_bits_;
        int value = 0;
        if (nbits <= remaining)
        {
            if (pos_ == bits_.length)
                return 0;
            
            value = (int)(bits_[pos_] << num_used_bits_ >>> (32 - nbits));
            num_used_bits_ += nbits;
            if (num_used_bits_ == 32)
            {
                ++pos_;
                this.num_used_bits_ = 0;
            }
            
        }
        else
        {
            if (pos_ + 1 == bits_.length)
                return 0;
            int value_l = bits_[pos_] << num_used_bits_;
            this.num_used_bits_ = nbits - remaining;
            ++pos_;
            int value_r = bits_[pos_] >>> (32 - num_used_bits_);
            value = value_l >>> (32 - num_used_bits_ - remaining) | value_r;
        }
        
        
        return value;
    }
    
    @Override
    public void endDecoding()
    {
    }
    
    
}
