package org.openize.drako;
interface IBitDecoder
{    
    /**
     *  Sets |sourceBuffer| as the buffer to decode bits from.
     *  Returns false when the data is invalid.
     *
     */
    boolean startDecoding(DecoderBuffer sourceBuffer);
    
    int decodeLeastSignificantBits32(int nbits);
    
    boolean decodeNextBit();
    
    void endDecoding();
    
}
