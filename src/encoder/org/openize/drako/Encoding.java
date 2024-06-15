package org.openize.drako;
import com.aspose.csporter.helpers.IntSpan;
class Encoding
{    
    static final int K_MAX_TAG_SYMBOL_BIT_LENGTH = 32;
    static final int K_MAX_RAW_ENCODING_BIT_LENGTH = 18;
    private static final int TAGGED = 0;
    private static final int RAW = 1;
    public static void convertSignedIntsToSymbols(IntSpan input, int num, IntSpan output)
    {
        // Convert the quantized values into a format more suitable for entropy
        // encoding.
        // Put the sign bit into LSB pos and shift the rest one bit left.
        for (int i = 0; i < num; ++i)
        {
            int val = input.get(i);
            boolean isNegative = val < 0;
            if (isNegative)
            {
                val = -val - 1;
                // Map -1 to 0, -2 to -1, etc..
            }
            
            // Map -1 to 0, -2 to -1, etc..
            val <<= 1;
            if (isNegative)
            {
                val |= 1;
            }
            
            output.put(i, val);
        }
        
    }
    
    /**
     *  Computes bit lengths of the input values. If numComponents > 1, the values
     *  are processed in "numComponents" sized chunks and the bit length is always
     *  computed for the largest value from the chunk.
     *
     */
    public static int[] computeBitLengths(IntSpan symbols, int numComponents, int[] outMaxValue)
    {
        int[] outBitLengths = new int[symbols.size() / numComponents];
        int p = 0;
        outMaxValue[0] = 0;
        // Maximum integer value across all components.
        for (int i = 0; i < symbols.size(); i += numComponents)
        {
            int maxComponentValue = symbols.get(i);
            for (int j = 1; j < numComponents; ++j)
            {
                if (maxComponentValue < symbols.get(i + j))
                {
                    maxComponentValue = symbols.get(i + j);
                }
                
            }
            
            int valueMsbPos = 0;
            if (maxComponentValue > 0)
            {
                valueMsbPos = DracoUtils.mostSignificantBit(maxComponentValue);
            }
            
            if (maxComponentValue > outMaxValue[0])
            {
                outMaxValue[0] = maxComponentValue;
            }
            
            outBitLengths[p++] = valueMsbPos + 1;
        }
        
        return outBitLengths;
    }
    
    static long computeShannonEntropy(IntSpan symbols, int num_symbols, int max_value, int[] out_num_unique_symbols)
    {
        int num_unique_symbols = 0;
        int[] symbol_frequencies = new int[max_value + 1];
        for (int i = 0; i < num_symbols; ++i)
        {
            ++symbol_frequencies[symbols.get(i)];
        }
        
        double total_bits = 0.0;
        double num_symbols_d = num_symbols;
        double log2 = Math.log(2);
        for (int i = 0; i < (max_value + 1); ++i)
        {
            if (symbol_frequencies[i] > 0)
            {
                ++num_unique_symbols;
                // Compute Shannon entropy for the symbol.
                // We don't want to use std::log2 here for Android build.
                total_bits += symbol_frequencies[i] * Math.log(1.0 * symbol_frequencies[i] / num_symbols_d) / log2;
            }
            
        }
        
        
        out_num_unique_symbols[0] = num_unique_symbols;
        // Entropy is always negative.
        return (long)-total_bits;
    }
    
    // Compute approximate frequency table size needed for storing the provided
    // symbols.
    // 
    static long approximateRAnsFrequencyTableBits(int max_value, int num_unique_symbols)
    {
        long table_zero_frequency_bits = 8 * (num_unique_symbols + ((max_value - num_unique_symbols) / 64));
        return 8 * num_unique_symbols + table_zero_frequency_bits;
    }
    
    static long approximateTaggedSchemeBits(IntSpan bit_lengths, int num_components)
    {
        long total_bit_length = 0L;
        final int[] ref0 = new int[1];
        for (int i = 0; i < bit_lengths.size(); ++i)
        {
            total_bit_length += (long)(bit_lengths.get(i));
        }
        
        int num_unique_symbols;
        long tag_bits = Encoding.computeShannonEntropy(bit_lengths, bit_lengths.size(), 32, ref0);
        num_unique_symbols = ref0[0];
        long tag_table_bits = Encoding.approximateRAnsFrequencyTableBits(num_unique_symbols, num_unique_symbols);
        return tag_bits + tag_table_bits + (total_bit_length * num_components);
    }
    
    static long approximateRawSchemeBits(IntSpan symbols, int num_symbols, int max_value, int[] out_num_unique_symbols)
    {
        int num_unique_symbols;
        final int[] ref1 = new int[1];
        long data_bits = Encoding.computeShannonEntropy(symbols, num_symbols, max_value, ref1);
        num_unique_symbols = ref1[0];
        long table_bits = Encoding.approximateRAnsFrequencyTableBits(max_value, num_unique_symbols);
        out_num_unique_symbols[0] = num_unique_symbols;
        return table_bits + data_bits;
    }
    
    public static boolean encodeSymbols(IntSpan symbols, int numValues, int numComponents, DracoEncodeOptions options, EncoderBuffer targetBuffer)
    {
        final int[] ref2 = new int[1];
        final int[] ref3 = new int[1];
        if (symbols.size() == 0)
            return true;
        if (numComponents == 0)
        {
            numComponents = 1;
        }
        
        int maxValue;
        int[] bitLengths = Encoding.computeBitLengths(symbols, numComponents, ref2);
        maxValue = ref2[0];
        long tagged_scheme_total_bits = Encoding.approximateTaggedSchemeBits(IntSpan.wrap(bitLengths), numComponents);
        int num_unique_symbols = 0;
        long raw_scheme_total_bits = Encoding.approximateRawSchemeBits(symbols, numValues, maxValue, ref3);
        num_unique_symbols = ref3[0];
        int max_value_bit_length = DracoUtils.mostSignificantBit(Math.max(1, maxValue)) + 1;
        int method;
        if (options != null && (options.getSymbolEncodingMethod() != null))
        {
            method = options.getSymbolEncodingMethod().intValue();
        }
        else if (tagged_scheme_total_bits < raw_scheme_total_bits || (max_value_bit_length > K_MAX_RAW_ENCODING_BIT_LENGTH))
        {
            method = TAGGED;
        }
        else
        {
            method = RAW;
        }
        
        
        // Use the tagged scheme.
        targetBuffer.encode((byte)method);
        if (method == TAGGED)
            return Encoding.encodeTaggedSymbols(symbols, numComponents, bitLengths, targetBuffer);
        
        if (method == RAW)
            return Encoding.encodeRawSymbols(symbols, numValues, maxValue, num_unique_symbols, options, targetBuffer);
        
        // Unknown method selected.
        return false;
    }
    
    static boolean encodeTaggedSymbols(IntSpan symbols, int numComponents, int[] bitLengths, EncoderBuffer targetBuffer)
    {
        long[] frequencies = new long[K_MAX_TAG_SYMBOL_BIT_LENGTH];
        
        // Compute the frequencies from input data.
        // Maximum integer value for the values across all components.
        for (int i = 0; i < bitLengths.length; ++i)
        {
            // Update the frequency of the associated entry id.
            ++frequencies[bitLengths[i]];
        }
        
        EncoderBuffer valueBuffer = new EncoderBuffer();
        int valueBits = K_MAX_TAG_SYMBOL_BIT_LENGTH * symbols.size();
        RAnsSymbolEncoder tagEncoder = new RAnsSymbolEncoder(5, frequencies, targetBuffer);
        
        // Start encoding bit tags.
        tagEncoder.startEncoding(targetBuffer);
        
        // Also start encoding the values.
        valueBuffer.startBitEncoding(valueBits, false);
        
        // Encoder needs the values to be encoded in the reverse order.
        for (int i = symbols.size() - numComponents; i >= 0; i -= numComponents)
        {
            int bitLength = bitLengths[i / numComponents];
            tagEncoder.encodeSymbol(bitLength);
            int j = symbols.size() - numComponents - i;
            int valueBitLength = bitLengths[j / numComponents];
            for (int c = 0; c < numComponents; ++c)
            {
                valueBuffer.encodeLeastSignificantBits32(valueBitLength, symbols.get(j + c));
            }
            
        }
        
        tagEncoder.endEncoding(targetBuffer);
        valueBuffer.endBitEncoding();
        
        // Append the values to the end of the target buffer.
        targetBuffer.encode(valueBuffer.getData(), valueBuffer.getBytes());
        return true;
    }
    
    static boolean encodeRawSymbols(IntSpan symbols, int num_values, int max_entry_value, int num_unique_symbols, DracoEncodeOptions options, EncoderBuffer target_buffer)
    {
        int symbol_bits = 0;
        if (num_unique_symbols > 0)
        {
            symbol_bits = DracoUtils.mostSignificantBit(num_unique_symbols);
        }
        
        int unique_symbols_bit_length = symbol_bits + 1;
        // Currently, we don't support encoding of more than 2^18 unique symbols.
        if (unique_symbols_bit_length > K_MAX_RAW_ENCODING_BIT_LENGTH)
            return false;
        int compression_level = options.getCompressionLevel2();
        
        // Adjust the bit_length based on compression level. Lower compression levels
        // will use fewer bits while higher compression levels use more bits. Note
        // that this is going to work for all valid bit_lengths because the actual
        // number of bits allocated for rANS encoding is hard coded as:
        // std::max(12, 3 * bit_length / 2) , therefore there will be always a
        // sufficient number of bits available for all symbols.
        // See ComputeRAnsPrecisionFromUniqueSymbolsBitLength() for the formula.
        // This hardcoded equation cannot be changed without changing the bitstream.
        if (compression_level < 4)
        {
            unique_symbols_bit_length -= 2;
        }
        else if (compression_level < 6)
        {
            unique_symbols_bit_length -= 1;
        }
        else if (compression_level > 9)
        {
            unique_symbols_bit_length += 2;
        }
        else if (compression_level > 7)
        {
            unique_symbols_bit_length += 1;
        }
        
        
        // Clamp the bit_length to a valid range.
        unique_symbols_bit_length = Math.min(Math.max(1, unique_symbols_bit_length), K_MAX_RAW_ENCODING_BIT_LENGTH);
        target_buffer.encode((byte)unique_symbols_bit_length);
        // Use appropriate symbol encoder based on the maximum symbol bit length.
        
        return Encoding.encodeRawSymbolsInternal(unique_symbols_bit_length, symbols, num_values, max_entry_value, target_buffer);
    }
    
    static boolean encodeRawSymbolsInternal(int unique_symbols_bit_length, IntSpan symbols, int num_values, int max_entry_value, EncoderBuffer target_buffer)
    {
        long[] frequencies = new long[max_entry_value + 1];
        for (int i = 0; i < num_values; ++i)
        {
            ++frequencies[symbols.get(i)];
        }
        
        RAnsSymbolEncoder encoder = new RAnsSymbolEncoder(unique_symbols_bit_length, frequencies, target_buffer);
        encoder.startEncoding(target_buffer);
        final boolean needsReverseEncoding = true;
        if (needsReverseEncoding)
        {
            for (int i = num_values - 1; i >= 0; --i)
            {
                encoder.encodeSymbol(symbols.get(i));
            }
            
        }
        
        
        encoder.endEncoding(target_buffer);
        return true;
    }
    
    static boolean encodeRawSymbols(IntSpan symbols, int maxValue, EncoderBuffer targetBuffer)
    {
        int maxEntryValue = maxValue;
        int maxValueBits = 0;
        if (maxEntryValue > 0)
        {
            maxValueBits = DracoUtils.mostSignificantBit(maxValue);
        }
        
        int maxValueBitLength = maxValueBits + 1;
        // Currently, we don't support encoding of values larger than 2^20.
        if (maxValueBitLength > K_MAX_RAW_ENCODING_BIT_LENGTH)
            return false;
        targetBuffer.encode((byte)maxValueBitLength);
        long[] frequencies = new long[maxEntryValue + 1];
        for (int i = 0; i < symbols.size(); ++i)
        {
            ++frequencies[symbols.get(i)];
        }
        
        RAnsSymbolEncoder encoder = new RAnsSymbolEncoder(maxValueBitLength, frequencies, targetBuffer);
        
        encoder.startEncoding(targetBuffer);
        // Encode all values.
        for (int i = symbols.size() - 1; i >= 0; --i)
        {
            encoder.encodeSymbol(symbols.get(i));
        }
        
        encoder.endEncoding(targetBuffer);
        return true;
    }
    
    public static boolean encodeVarint(long val, EncoderBuffer buffer)
    {
        byte out_ = 0;
        out_ |= (byte)(val & ((1 << 7) - 1));
        if (val >= (1 << 7))
        {
            out_ |= (byte)(1 << 7);
            if (!buffer.encode(out_))
                return false;
            if (!Encoding.encodeVarint(val >>> 7, buffer))
                return false;
            return true;
        }
        
        
        if (!buffer.encode(out_))
            return false;
        return true;
    }
    
    public static boolean encodeVarint(int val, EncoderBuffer buffer)
    {
        return Encoding.encodeVarint2(val, buffer);
    }
    
    
    public static boolean encodeVarint2(int val, EncoderBuffer buffer)
    {
        byte out_ = 0;
        out_ |= (byte)(val & ((1 << 7) - 1));
        if ((0xffffffffl & val) >= (1 << 7))
        {
            out_ |= (byte)(1 << 7);
            if (!buffer.encode(out_))
                return false;
            if (!Encoding.encodeVarint(val >>> 7, buffer))
                return false;
            return true;
        }
        
        
        if (!buffer.encode(out_))
            return false;
        return true;
    }
    
}
