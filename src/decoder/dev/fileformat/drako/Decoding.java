package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class Decoding
{    
    static void decodeSymbols(int numValues, int numComponents, DecoderBuffer srcBuffer, IntSpan outValues)
        throws DrakoException
    {
        if (numValues < 0)
            throw DracoUtils.failed();
        if (numValues == 0)
            return;
        byte scheme = srcBuffer.decodeU8();
        if (scheme == 0)
        {
            Decoding.decodeTaggedSymbols(numValues, numComponents, srcBuffer, outValues);
        }
        else if (scheme == 1)
        {
            Decoding.decodeRawSymbols(numValues, srcBuffer, outValues);
        }
        else
            throw DracoUtils.failed();
    }
    
    static boolean decodeTaggedSymbols(int numValues, int numComponents, DecoderBuffer srcBuffer, IntSpan outValues)
        throws DrakoException
    {
        RAnsSymbolDecoder tagDecoder = new RAnsSymbolDecoder(5);
        final int[] ref0 = new int[1];
        tagDecoder.create(srcBuffer);
        
        tagDecoder.startDecoding(srcBuffer);
        
        if (numValues > 0 && (tagDecoder.getNumSymbols() == 0))
            throw DracoUtils.failed();
        // Wrong number of symbols.
        long tmp = srcBuffer.startBitDecoding(false);
        int valueId = 0;
        for (int i = 0; i < numValues; i += numComponents)
        {
            int bitLength = tagDecoder.decodeSymbol();
            // Decode the actual value.
            for (int j = 0; j < numComponents; ++j)
            {
                int val;
                if (!srcBuffer.decodeLeastSignificantBits32(bitLength, ref0))
                {
                    val = ref0[0];
                    throw DracoUtils.failed();
                }
                else
                {
                    val = ref0[0];
                }
                
                outValues.put(valueId++, val);
            }
            
        }
        
        tagDecoder.endDecoding();
        srcBuffer.endBitDecoding();
        return true;
    }
    
    static void decodeRawSymbols(int numValues, DecoderBuffer srcBuffer, IntSpan outValues)
        throws DrakoException
    {
        byte maxBitLength = srcBuffer.decodeU8();
        RAnsSymbolDecoder decoder = new RAnsSymbolDecoder(0xff & maxBitLength);
        decoder.create(srcBuffer);
        
        if (numValues > 0 && (decoder.getNumSymbols() == 0))
            throw DracoUtils.failed();
        // Wrong number of symbols.
        
        decoder.startDecoding(srcBuffer);
        for (int i = 0; i < numValues; ++i)
        {
            int value = decoder.decodeSymbol();
            outValues.put(i, value);
        }
        
        decoder.endDecoding();
    }
    
    public static void convertSymbolsToSignedInts(IntSpan symbols, IntSpan result)
    {
        for (int i = 0; i < symbols.size(); ++i)
        {
            int val = symbols.get(i);
            boolean isNegative = (val & 1) != 0;//lowest bit is sign bit
            
            val >>= 1;
            int ret = val;
            if (isNegative)
            {
                ret = -ret - 1;
            }
            
            result.put(i, ret);
        }
        
    }
    
    /**
     *  Decodes a specified integer as varint. Note that the IntTypeT must be the
     *  same as the one used in the corresponding EncodeVarint() call.
     *
     * @param buffer 
     */
    public static int decodeVarintU32(DecoderBuffer buffer)
        throws DrakoException
    {
        byte in_ = buffer.decodeU8();
        int out_val;
        
        if ((0xff & in_ & (1 << 7)) != 0)
        {
            // Next byte is available, decode it first.
            out_val = Decoding.decodeVarintU32(buffer);
            // Append decoded info from this byte.
            out_val <<= 7;
            out_val |= (int)(0xff & in_ & ((1 << 7) - 1));
        }
        else
        {
            // Last byte reached
            out_val = 0xff & in_;
        }
        
        return out_val;
    }
    
    public static short decodeVarintU16(DecoderBuffer buffer)
        throws DrakoException
    {
        byte in_ = buffer.decodeU8();
        short out_val;
        
        if ((0xff & in_ & (1 << 7)) != 0)
        {
            // Next byte is available, decode it first.
            out_val = Decoding.decodeVarintU16(buffer);
            // Append decoded info from this byte.
            out_val <<= 7;
            out_val |= (short)(0xff & in_ & ((1 << 7) - 1));
        }
        else
        {
            // Last byte reached
            out_val = (short)(0xff & in_);
        }
        
        return out_val;
    }
    
    public static long decodeVarintU64(DecoderBuffer buffer)
        throws DrakoException
    {
        byte in_ = buffer.decodeU8();
        long out_val;
        
        if ((0xff & in_ & (1 << 7)) != 0)
        {
            // Next byte is available, decode it first.
            
            out_val = Decoding.decodeVarintU64(buffer);
            // Append decoded info from this byte.
            out_val <<= 7L;
            out_val |= 0xffffffffl & (int)(0xff & in_ & ((1 << 7) - 1));
        }
        else
        {
            // Last byte reached
            out_val = 0xff & in_;
        }
        
        return out_val;
    }
    
    
}
