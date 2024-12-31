package dev.fileformat.drako;
interface IBitDecoder
{    
    /**
     *  Sets |sourceBuffer| as the buffer to decode bits from.
     *  Returns false when the data is invalid.
     *
     */
    void startDecoding(DecoderBuffer sourceBuffer)
        throws DrakoException;
    
    int decodeLeastSignificantBits32(int nbits);
    
    boolean decodeNextBit();
    
    void endDecoding();
    
}
