package dev.fileformat.drako;
import dev.fileformat.drako.Algorithms;
import java.util.Comparator;
/**
 *  A helper class for encoding symbols using the rANS algorithm (see ans.h).
 *  The class can be used to initialize and encode probability table needed by
 *  rANS, and to perform encoding of symbols into the provided EncoderBuffer.
 *
 */
class RAnsSymbolEncoder extends RAnsBitCodec
{    
    private int maxSymbols;
    private int ransPrecisionBits;
    private int ransPrecision;
    private RAnsBitCodec.RansSym[] probabilityTable;
    /**
     *  The number of symbols in the input alphabet.
     *
     */
    int numSymbols;
    /**
     *  Expected number of bits that is needed to encode the input.
     *
     */
    long numExpectedBits;
    RAnsEncoder ans;
    /**
     *  Initial offset of the encoder buffer before any ans data was encoded.
     *
     */
    long bufferOffset;
    public RAnsSymbolEncoder(int maxSymbolBitLength, long[] frequencies, EncoderBuffer buffer)
    {
        
        this.maxSymbols = 1 << maxSymbolBitLength;
        this.ransPrecisionBits = RAnsBitCodec.computeRAnsPrecisionFromMaxSymbolBitLength(maxSymbolBitLength);
        this.ransPrecision = 1 << ransPrecisionBits;
        
        this.ans = new RAnsEncoder(ransPrecisionBits);
        long totalFreq = 0L;
        int maxValidSymbol = 0;
        for (int i = 0; (0xffffffffl & i) < frequencies.length; ++i)
        {
            totalFreq += frequencies[i];
            if (frequencies[i] > 0L)
            {
                maxValidSymbol = i;
            }
            
        }
        
        int numSymbols = maxValidSymbol + 1;
        this.numSymbols = numSymbols;
        this.probabilityTable = (RAnsBitCodec.RansSym[])(MetaClasses.RansSym.newArray(numSymbols));
        double totalFreqD = totalFreq;
        double ransPrecisionD = ransPrecision;
        int totalRansProb = 0;
        for (int i = 0; i < (0xffffffffl & numSymbols); ++i)
        {
            long freq = frequencies[i];
            double prob = freq / totalFreqD;
            int ransProb = (int)(prob * ransPrecisionD + 0.5f);
            if (ransProb == 0 && (freq > 0L))
            {
                ransProb = 1;
            }
            
            probabilityTable[i].prob = ransProb;
            totalRansProb += ransProb;
        }
        
        // Because of rounding errors, the total precision may not be exactly accurate
        // and we may need to adjust the entries a little bit.
        if (totalRansProb != ransPrecision)
        {
            int[] sortedProbabilities = new int[numSymbols];
            for (int i = 0; i < (0xffffffffl & numSymbols); ++i)
            {
                sortedProbabilities[i] = i;
            }
            
            Algorithms.sort(sortedProbabilities, new Comparator<Integer>()
            {
                public int compare(Integer a, Integer b)
                {
                    return Long.compare(probabilityTable[a].prob & 0xFFFFFFFFL, probabilityTable[b].prob & 0xFFFFFFFFL);
                }
            });
            if (totalRansProb < ransPrecision)
            {
                // This happens rather infrequently, just add the extra needed precision
                // to the most frequent symbol.
                probabilityTable[sortedProbabilities.length - 1].prob += (int)(ransPrecision - totalRansProb);
            }
            else
            {
                int error = totalRansProb - ransPrecision;
                while (error > 0)
                {
                    double actTotalProbD = (double)totalRansProb;
                    double actRelErrorD = ransPrecisionD / actTotalProbD;
                    for (int j = (int)(numSymbols - 1); j > 0; --j)
                    {
                        int symbolId = sortedProbabilities[j];
                        if ((0xffffffffl & probabilityTable[symbolId].prob) <= 1)
                        {
                            if (j == (numSymbols - 1))
                                return;
                            // Most frequent symbol would be empty.
                            break;
                        }
                        
                        int newProb = (int)Math.floor(actRelErrorD * probabilityTable[symbolId].prob);
                        int fix = probabilityTable[symbolId].prob - newProb;
                        if (fix == 0)
                        {
                            fix = 1;
                        }
                        
                        if (fix >= probabilityTable[symbolId].prob)
                        {
                            fix = probabilityTable[symbolId].prob - 1;
                        }
                        
                        if (fix > error)
                        {
                            fix = error;
                        }
                        
                        probabilityTable[symbolId].prob -= fix;
                        totalRansProb -= fix;
                        error -= fix;
                        if (totalRansProb == ransPrecision)
                            break;
                    }
                    
                }
                
            }
            
        }
        
        int totalProb = 0;
        for (int i = 0; i < (0xffffffffl & numSymbols); ++i)
        {
            probabilityTable[i].cumProb = totalProb;
            totalProb += probabilityTable[i].prob;
        }
        
        if ((0xffffffffl & totalProb) != ransPrecision)
            throw new RuntimeException("Failed to initialize RAns symbol encoder");
        double numBits = 0.0;
        for (int i = 0; i < (0xffffffffl & numSymbols); ++i)
        {
            if (probabilityTable[i].prob == 0)
                continue;
            double normProb = probabilityTable[i].prob / ransPrecisionD;
            numBits += frequencies[i] * (Math.log(normProb) / Math.log(2));
        }
        
        this.numExpectedBits = (long)Math.ceil(-numBits);
        this.encodeTable(buffer);
    }
    
    private void encodeTable(EncoderBuffer buffer)
    {
        Encoding.encodeVarint2(numSymbols, buffer);
        // Use varint encoding for the probabilities (first two bits represent the
        // number of bytes used - 1).
        for (int i = 0; (0xffffffffl & i) < (0xffffffffl & numSymbols); ++i)
        {
            int prob = probabilityTable[i].prob;
            int numExtraBytes = 0;
            if ((0xffffffffl & prob) >= (1 << 6))
            {
                numExtraBytes++;
                if ((0xffffffffl & prob) >= (1 << 14))
                {
                    numExtraBytes++;
                    if ((0xffffffffl & prob) >= (1 << 22))
                    {
                        numExtraBytes++;
                    }
                    
                }
                
            }
            
            
            if (prob == 0)
            {
                int offset = 0;
                for (; (0xffffffffl & offset) < ((1 << 6) - 1); ++offset)
                {
                    int next_prob = probabilityTable[i + offset + 1].prob;
                    if ((0xffffffffl & next_prob) > 0)
                        break;
                }
                
                
                buffer.encode((byte)(offset << 2 | 3));
                i += offset;
            }
            else
            {
                // Encode the first byte (including the number of extra bytes).
                buffer.encode((byte)(prob << 2 | (int)(numExtraBytes & 3)));
                // Encode the extra bytes.
                for (int b = 0; b < numExtraBytes; ++b)
                {
                    buffer.encode((byte)(prob >>> (8 * (b + 1) - 2)));
                }
                
            }
            
        }
        
    }
    
    public void startEncoding(EncoderBuffer buffer)
    {
        long requiredBits = 2 * numExpectedBits + 32L;
        
        this.bufferOffset = (long)(buffer.getBytes());
        long requiredBytes = (requiredBits + 7L) / 8L;
        buffer.resize((int)bufferOffset + (int)requiredBytes + 8);
        byte[] data = buffer.getData();
        // Offset the encoding by sizeof(bufferOffset). We will use this memory to
        // store the number of encoded bytes.
        ans.reset(new BytePointer(data, (int)bufferOffset));
    }
    
    public void encodeSymbol(int symbol)
    {
        ans.write(probabilityTable[symbol]);
    }
    
    public void endEncoding(EncoderBuffer buffer)
    {
        int src = (int)bufferOffset;
        long bytes_written = (long)(ans.writeEnd());
        EncoderBuffer var_size_buffer = new EncoderBuffer();
        Encoding.encodeVarint(bytes_written, var_size_buffer);
        int size_len = var_size_buffer.getBytes();
        int dst = src + size_len;
        System.arraycopy(buffer.getData(), src, buffer.getData(), dst, (int)bytes_written);
        
        // Store the size of the encoded data.
        //memcpy(src, var_size_buffer.data(), size_len);
        System.arraycopy(var_size_buffer.getData(), 0, buffer.getData(), src, size_len);
        
        // Resize the buffer to match the number of encoded bytes.
        buffer.resize((int)bufferOffset + (int)bytes_written + size_len);
    }
    
}
