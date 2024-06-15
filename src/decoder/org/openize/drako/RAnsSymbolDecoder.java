package org.openize.drako;
class RAnsSymbolDecoder extends RAnsBitCodec
{    
    private int maxSymbolBitLength;
    private int maxSymbols;
    private int ransPrecision;
    private int[] probabilityTable;
    private int numSymbols;
    private RAnsDecoder ans;
    public RAnsSymbolDecoder(int maxSymbolBitLength)
    {
        this.maxSymbolBitLength = maxSymbolBitLength;
        this.maxSymbols = 1 << maxSymbolBitLength;
        int ransPrecisionBits = RAnsBitCodec.computeRAnsPrecisionFromMaxSymbolBitLength(maxSymbolBitLength);
        this.ransPrecision = 1 << ransPrecisionBits;
        this.ans = new RAnsDecoder(ransPrecisionBits);
    }
    
    public boolean create(DecoderBuffer buffer)
    {
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        final byte[] ref2 = new byte[1];
        final byte[] ref3 = new byte[1];
        if (buffer.getBitstreamVersion() == 0)
            return DracoUtils.failed();
        // Decode the number of alphabet symbols.
        if (buffer.getBitstreamVersion() < 20)
        {
            if (!buffer.decode6(ref0))
            {
                numSymbols = ref0[0];
                return DracoUtils.failed();
            }
            else
            {
                numSymbols = ref0[0];
            }
            
        }
        else
        {
            int n;
            if (!Decoding.decodeVarint(ref1, buffer))
            {
                n = ref1[0];
                return DracoUtils.failed();
            }
            else
            {
                n = ref1[0];
            }
            
            this.numSymbols = n;
        }
        
        this.probabilityTable = new int[numSymbols];
        if (numSymbols == 0)
            return true;
        // Decode the table.
        for (int i = 0; i < numSymbols; ++i)
        {
            int prob = 0;
            byte byteProb = 0;
            // Decode the first byte and extract the number of extra bytes we need to
            // get.
            if (!buffer.decode3(ref2))
            {
                byteProb = ref2[0];
                return DracoUtils.failed();
            }
            else
            {
                byteProb = ref2[0];
            }
            
            int token = 0xff & byteProb & 3;
            if (token == 3)
            {
                int offset = (0xff & byteProb) >>> 2;
                if (i + offset >= numSymbols)
                    return DracoUtils.failed();
                // Set zero probability for all symbols in the specified range.
                for (int j = 0; j < (offset + 1); ++j)
                {
                    probabilityTable[i + j] = 0;
                }
                
                
                i += offset;
            }
            else
            {
                int extraBytes = 0xff & byteProb & 3;
                prob = (int)((0xff & byteProb) >>> 2);
                for (int b = 0; b < extraBytes; ++b)
                {
                    byte eb;
                    if (!buffer.decode3(ref3))
                    {
                        eb = ref3[0];
                        return DracoUtils.failed();
                    }
                    else
                    {
                        eb = ref3[0];
                    }
                    
                    // Shift 8 bits for each extra byte and subtract 2 for the two first bits.
                    prob |= (int)(0xff & eb) << (8 * (b + 1) - 2);
                }
                
            }
            
            probabilityTable[i] = prob;
        }
        
        if (!ans.buildLookupTable(probabilityTable, numSymbols))
            return DracoUtils.failed();
        return true;
    }
    
    public boolean startDecoding(DecoderBuffer buffer)
    {
        long bytesEncoded;
        final long[] ref4 = new long[1];
        final long[] ref5 = new long[1];
        // Decode the number of bytes encoded by the encoder.
        if (buffer.getBitstreamVersion() < 20)
        {
            if (!buffer.decode(ref4))
            {
                bytesEncoded = ref4[0];
                return DracoUtils.failed();
            }
            else
            {
                bytesEncoded = ref4[0];
            }
            
        }
        else
        {
            long n;
            if (!Decoding.decodeVarint(ref5, buffer))
            {
                n = ref5[0];
                return DracoUtils.failed();
            }
            else
            {
                n = ref5[0];
            }
            
            bytesEncoded = n;
        }
        
        
        if (bytesEncoded > buffer.getRemainingSize())
            return DracoUtils.failed();
        BytePointer dataHead = BytePointer.add(buffer.getPointer(), buffer.getDecodedSize());
        // Advance the buffer past the rANS data.
        buffer.advance((int)bytesEncoded);
        if (ans.readInit(dataHead, (int)bytesEncoded) != 0)
            return DracoUtils.failed();
        return true;
    }
    
    public int getNumSymbols()
    {
        return numSymbols;
    }
    
    public int decodeSymbol()
    {
        return ans.read();
    }
    
    public void endDecoding()
    {
        ans.readEnd();
    }
    
}
