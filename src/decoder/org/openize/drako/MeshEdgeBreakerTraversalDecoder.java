package org.openize.drako;
class MeshEdgeBreakerTraversalDecoder implements ITraversalDecoder
{    
    /**
     *  Buffer that contains the encoded data.
     *
     */
    protected DecoderBuffer buffer;
    protected DecoderBuffer symbol_buffer_;
    protected DecoderBuffer startFaceBuffer;
    protected RAnsBitDecoder startFaceDecoder;
    protected RAnsBitDecoder[] attributeConnectivityDecoders;
    protected int numAttributeData;
    protected IMeshEdgeBreakerDecoderImpl decoderImpl;
    /**
     *  Returns true if there is an attribute seam for the next processed pair
     *  of visited faces.
     *  |attribute| is used to mark the id of the non-position attribute (in range
     *  of &lt; 0, numAttributes - 1&gt;).
     *
     */
    public boolean decodeAttributeSeam(int attribute)
    {
        return attributeConnectivityDecoders[attribute].decodeNextBit();
    }
    
    protected int getBitstreamVersion()
    {
        return decoderImpl.getDecoder().getBitstreamVersion();
    }
    
    public void init(IMeshEdgeBreakerDecoderImpl decoder)
    {
        this.decoderImpl = decoder;
        this.buffer = decoder.getDecoder().getBuffer().subBuffer(0);
    }
    
    public void setNumEncodedVertices(int numVertices)
    {
    }
    
    public void setNumAttributeData(int numData)
    {
        this.numAttributeData = numData;
    }
    
    public boolean start(DecoderBuffer[] outBuffer)
    {
        outBuffer[0] = null;
        if (!this.decodeTraversalSymbols())
            return false;
        if (!this.decodeStartFaces())
            return false;
        if (!this.decodeAttributeSeams())
            return false;
        outBuffer[0] = buffer;
        return true;
    }
    
    @Override
    public void done()
    {
        if (symbol_buffer_.getBitDecoderActive())
        {
            symbol_buffer_.endBitDecoding();
        }
        
        if (this.getBitstreamVersion() < 22)
        {
            startFaceBuffer.endBitDecoding();
        }
        else
        {
            startFaceDecoder.endDecoding();
        }
        
    }
    
    @Override
    public int decodeSymbol()
    {
        int s;
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        symbol_buffer_.decodeLeastSignificantBits32(1, ref0);
        s = ref0[0];
        int symbol = s;
        if (symbol == EdgeBreakerTopologyBitPattern.C)
            return symbol;
        int symbolSuffix;
        symbol_buffer_.decodeLeastSignificantBits32(2, ref1);
        symbolSuffix = ref1[0];
        s |= symbolSuffix << 1;
        return s;
    }
    
    public void mergeVertices(int dest, int source)
    {
    }
    
    public void newActiveCornerReached(int corner)
    {
    }
    
    @Override
    public boolean decodeStartFaceConfiguration()
    {
        final int[] ref2 = new int[1];
        if (this.getBitstreamVersion() < 22)
        {
            int face_configuration;
            startFaceBuffer.decodeLeastSignificantBits32(1, ref2);
            face_configuration = ref2[0];
            return (0xffffffffl & face_configuration) != 0;
        }
        else
        {
            boolean ret = startFaceDecoder.decodeNextBit();
            return ret;
        }
        
    }
    
    protected boolean decodeTraversalSymbols()
    {
        long traversalSize;
        final long[] ref3 = new long[1];
        this.symbol_buffer_ = buffer.clone();
        if (!symbol_buffer_.startBitDecoding(true, ref3))
        {
            traversalSize = ref3[0];
            return DracoUtils.failed();
        }
        else
        {
            traversalSize = ref3[0];
        }
        
        this.buffer = symbol_buffer_.clone();
        if (traversalSize > buffer.getRemainingSize())
            return DracoUtils.failed();
        buffer.advance((int)traversalSize);
        return true;
    }
    
    protected boolean decodeStartFaces()
    {
        final long[] ref4 = new long[1];
        // Create a decoder that is set to the end of the encoded traversal data.
        if (this.getBitstreamVersion() < 22)
        {
            this.startFaceBuffer = buffer.clone();
            long traversalSize;
            if (!startFaceBuffer.startBitDecoding(true, ref4))
            {
                traversalSize = ref4[0];
                return DracoUtils.failed();
            }
            else
            {
                traversalSize = ref4[0];
            }
            
            this.buffer = startFaceBuffer.clone();
            if (traversalSize > buffer.getRemainingSize())
                return DracoUtils.failed();
            buffer.advance((int)traversalSize);
            return true;
        }
        
        return startFaceDecoder.startDecoding(buffer);
    }
    
    protected boolean decodeAttributeSeams()
    {
        // Prepare attribute decoding.
        if (numAttributeData > 0)
        {
            this.attributeConnectivityDecoders = new RAnsBitDecoder[numAttributeData];
            for (int i = 0; i < numAttributeData; ++i)
            {
                attributeConnectivityDecoders[i] = new RAnsBitDecoder();
                if (!attributeConnectivityDecoders[i].startDecoding(buffer))
                    return DracoUtils.failed();
            }
            
        }
        
        
        return true;
    }
    
    public MeshEdgeBreakerTraversalDecoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            symbol_buffer_ = new DecoderBuffer();
            startFaceDecoder = new RAnsBitDecoder();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
