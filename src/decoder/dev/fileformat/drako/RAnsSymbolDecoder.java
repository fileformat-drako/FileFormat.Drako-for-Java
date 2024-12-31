package dev.fileformat.drako;
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
    
    public void create(DecoderBuffer buffer)
        throws DrakoException
    {
        if (buffer.getBitstreamVersion() == 0)
            throw DracoUtils.failed();
        // Decode the number of alphabet symbols.
        if (buffer.getBitstreamVersion() < 20)
        {
            this.numSymbols = buffer.decodeI32();
        }
        else
        {
            int n = Decoding.decodeVarintU32(buffer);
            this.numSymbols = n;
        }
        
        this.probabilityTable = new int[numSymbols];
        if (numSymbols == 0)
            return;
        // Decode the table.
        for (int i = 0; i < numSymbols; ++i)
        {
            int prob = 0;
            byte byteProb = buffer.decodeU8();
            int token = 0xff & byteProb & 3;
            if (token == 3)
            {
                int offset = (0xff & byteProb) >>> 2;
                if (i + offset >= numSymbols)
                    throw DracoUtils.failed();
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
                    byte eb = buffer.decodeU8();
                    // Shift 8 bits for each extra byte and subtract 2 for the two first bits.
                    prob |= (int)(0xff & eb) << (8 * (b + 1) - 2);
                }
                
            }
            
            probabilityTable[i] = prob;
        }
        
        if (!ans.buildLookupTable(probabilityTable, numSymbols))
            throw DracoUtils.failed();
    }
    
    public void startDecoding(DecoderBuffer buffer)
        throws DrakoException
    {
        long bytesEncoded;
        // Decode the number of bytes encoded by the encoder.
        if (buffer.getBitstreamVersion() < 20)
        {
            bytesEncoded = buffer.decodeI64();
        }
        else
        {
            long n = Decoding.decodeVarintU64(buffer);
            bytesEncoded = n;
        }
        
        
        if (bytesEncoded > buffer.getRemainingSize())
            throw DracoUtils.failed();
        BytePointer dataHead = BytePointer.add(buffer.getPointer(), buffer.getDecodedSize());
        // Advance the buffer past the rANS data.
        buffer.advance((int)bytesEncoded);
        if (ans.readInit(dataHead, (int)bytesEncoded) != 0)
            throw DracoUtils.failed();
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
