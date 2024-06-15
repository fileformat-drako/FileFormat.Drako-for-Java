package org.openize.drako;
interface IBitEncoder
{    
    void startEncoding();
    
    void encodeBit(boolean bit);
    
    void encodeLeastSignificantBits32(int nbits, int value);
    
    void endEncoding(EncoderBuffer targetBuffer);
    
    void clear();
    
}
