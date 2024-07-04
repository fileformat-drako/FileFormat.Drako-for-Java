package org.fileformat.drako;
import com.aspose.csporter.helpers.IntSpan;
final class Decoding
{    
    static boolean decodeSymbols(int numValues, int numComponents, DecoderBuffer srcBuffer, IntSpan outValues)
    {
        final byte[] ref0 = new byte[1];
        if (numValues < 0)
            return DracoUtils.failed();
        if (numValues == 0)
            return true;
        byte scheme;
        if (!srcBuffer.decode3(ref0))
        {
            scheme = ref0[0];
            return DracoUtils.failed();
        }
        else
        {
            scheme = ref0[0];
        }
        
        if (scheme == 0)
            return Decoding.decodeTaggedSymbols(numValues, numComponents, srcBuffer, outValues);else if (scheme == 1)
            return Decoding.decodeRawSymbols(numValues, srcBuffer, outValues);
        return DracoUtils.failed();
    }
    
    static boolean decodeTaggedSymbols(int numValues, int numComponents, DecoderBuffer srcBuffer, IntSpan outValues)
    {
        RAnsSymbolDecoder tagDecoder = new RAnsSymbolDecoder(5);
        final long[] ref1 = new long[1];
        final int[] ref2 = new int[1];
        if (!tagDecoder.create(srcBuffer))
            return DracoUtils.failed();
        
        if (!tagDecoder.startDecoding(srcBuffer))
            return DracoUtils.failed();
        
        if (numValues > 0 && (tagDecoder.getNumSymbols() == 0))
            return DracoUtils.failed();
        // Wrong number of symbols.
        long tmp;
        srcBuffer.startBitDecoding(false, ref1);
        tmp = ref1[0];
        int valueId = 0;
        for (int i = 0; i < numValues; i += numComponents)
        {
            int bitLength = tagDecoder.decodeSymbol();
            // Decode the actual value.
            for (int j = 0; j < numComponents; ++j)
            {
                int val;
                if (!srcBuffer.decodeLeastSignificantBits32(bitLength, ref2))
                {
                    val = ref2[0];
                    return DracoUtils.failed();
                }
                else
                {
                    val = ref2[0];
                }
                
                outValues.put(valueId++, val);
            }
            
        }
        
        tagDecoder.endDecoding();
        srcBuffer.endBitDecoding();
        return true;
    }
    
    static boolean decodeRawSymbols(int numValues, DecoderBuffer srcBuffer, IntSpan outValues)
    {
        byte maxBitLength;
        final byte[] ref3 = new byte[1];
        if (!srcBuffer.decode3(ref3))
        {
            maxBitLength = ref3[0];
            return DracoUtils.failed();
        }
        else
        {
            maxBitLength = ref3[0];
        }
        
        RAnsSymbolDecoder decoder = new RAnsSymbolDecoder(0xff & maxBitLength);
        if (!decoder.create(srcBuffer))
            return DracoUtils.failed();
        
        if (numValues > 0 && (decoder.getNumSymbols() == 0))
            return DracoUtils.failed();
        // Wrong number of symbols.
        
        if (!decoder.startDecoding(srcBuffer))
            return DracoUtils.failed();
        for (int i = 0; i < numValues; ++i)
        {
            int value = decoder.decodeSymbol();
            outValues.put(i, value);
        }
        
        decoder.endDecoding();
        return true;
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
     * @param out_val 
     * @param buffer 
     */
    public static boolean decodeVarint(int[] out_val, DecoderBuffer buffer)
    {
        byte in_;
        final byte[] ref4 = new byte[1];
        if (!buffer.decode3(ref4))
        {
            in_ = ref4[0];
            out_val[0] = 0;
            return DracoUtils.failed();
        }
        else
        {
            in_ = ref4[0];
        }
        
        
        if ((0xff & in_ & (1 << 7)) != 0)
        {
            // Next byte is available, decode it first.
            if (!Decoding.decodeVarint(out_val, buffer))
                return DracoUtils.failed();
            // Append decoded info from this byte.
            out_val[0] <<= 7;
            out_val[0] |= (int)(0xff & in_ & ((1 << 7) - 1));
        }
        else
        {
            // Last byte reached
            out_val[0] = 0xff & in_;
        }
        
        return true;
    }
    
    public static boolean decodeVarint(short[] out_val, DecoderBuffer buffer)
    {
        byte in_;
        final byte[] ref5 = new byte[1];
        if (!buffer.decode3(ref5))
        {
            in_ = ref5[0];
            out_val[0] = 0;
            return DracoUtils.failed();
        }
        else
        {
            in_ = ref5[0];
        }
        
        
        if ((0xff & in_ & (1 << 7)) != 0)
        {
            // Next byte is available, decode it first.
            if (!Decoding.decodeVarint(out_val, buffer))
                return DracoUtils.failed();
            // Append decoded info from this byte.
            out_val[0] <<= 7;
            out_val[0] |= (short)(0xff & in_ & ((1 << 7) - 1));
        }
        else
        {
            // Last byte reached
            out_val[0] = (short)(0xff & in_);
        }
        
        return true;
    }
    
    public static boolean decodeVarint(long[] out_val, DecoderBuffer buffer)
    {
        byte in_;
        final byte[] ref6 = new byte[1];
        if (!buffer.decode3(ref6))
        {
            in_ = ref6[0];
            out_val[0] = 0L;
            return DracoUtils.failed();
        }
        else
        {
            in_ = ref6[0];
        }
        
        
        if ((0xff & in_ & (1 << 7)) != 0)
        {
            // Next byte is available, decode it first.
            if (!Decoding.decodeVarint(out_val, buffer))
                return DracoUtils.failed();
            // Append decoded info from this byte.
            out_val[0] <<= 7L;
            out_val[0] |= 0xffffffffl & (int)(0xff & in_ & ((1 << 7) - 1));
        }
        else
        {
            // Last byte reached
            out_val[0] = 0xff & in_;
        }
        
        return true;
    }
    
    
}
