package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class MeshEdgeBreakerTraversalValenceDecoder extends MeshEdgeBreakerTraversalDecoder
{    
    private static final int[] EDGE_BREAKER_SYMBOL_TO_TOPOLOGY_ID = {EdgeBreakerTopologyBitPattern.C, EdgeBreakerTopologyBitPattern.S, EdgeBreakerTopologyBitPattern.L, EdgeBreakerTopologyBitPattern.R, EdgeBreakerTopologyBitPattern.E};
    CornerTable cornerTable;
    int numVertices = 0;
    private int[] vertexValences;
    private int lastSymbol;
    int activeContext;
    int minValence = 2;
    int maxValence = 7;
    private int[][] contextSymbols;
    // Points to the active symbol in each context.
    // 
    private int[] contextCounters;
    @Override
    public void init(IMeshEdgeBreakerDecoderImpl decoder)
    {
        super.init(decoder);
        this.cornerTable = decoder.getCornerTable();
    }
    
    @Override
    public void setNumEncodedVertices(int numVertices)
    {
        this.numVertices = numVertices;
    }
    
    @Override
    public DecoderBuffer start()
        throws DrakoException
    {
        DecoderBuffer outBuffer = null;
        if (this.getBitstreamVersion() < 22)
        {
            super.decodeTraversalSymbols();
        }
        
        
        super.decodeStartFaces();
        super.decodeAttributeSeams();
        outBuffer = this.buffer.clone();
        
        if (this.getBitstreamVersion() < 22)
        {
            int numSplitSymbols;
            if (this.getBitstreamVersion() < 20)
            {
                numSplitSymbols = outBuffer.decodeI32();
            }
            else
            {
                numSplitSymbols = Decoding.decodeVarintU32(outBuffer);
            }
            
            
            if (numSplitSymbols >= numVertices)
                throw DracoUtils.failed();
            byte mode = outBuffer.decodeU8();
            if (mode == 0)// Edgebreaker valence mode 2-7
            {
                this.minValence = 2;
                this.maxValence = 7;
            }
            else
                throw DracoUtils.failed();
        }
        else
        {
            this.minValence = 2;
            this.maxValence = 7;
        }
        
        
        if (numVertices < 0)
            throw DracoUtils.failed();
        // Set the valences of all initial vertices to 0.
        this.vertexValences = new int[numVertices];
        int numUniqueValences = maxValence - minValence + 1;
        
        // Decode all symbols for all contexts.
        this.contextSymbols = new int[numUniqueValences][];
        this.contextCounters = new int[contextSymbols.length];
        for (int i = 0; i < contextSymbols.length; ++i)
        {
            int numSymbols = Decoding.decodeVarintU32(outBuffer);
            if ((0xffffffffl & numSymbols) > 0)
            {
                
                contextSymbols[i] = new int[numSymbols];
                Decoding.decodeSymbols(numSymbols, 1, outBuffer, IntSpan.wrap(contextSymbols[i]));
                // All symbols are going to be processed from the back.
                contextCounters[i] = numSymbols;
            }
            
        }
        
        
        return outBuffer;
    }
    
    @Override
    public int decodeSymbol()
    {
        // First check if we have a valid context.
        if (activeContext != -1)
        {
            int contextCounter = --contextCounters[activeContext];
            if (contextCounter < 0)
                return EdgeBreakerTopologyBitPattern.INVALID;
            int symbol_id = (contextSymbols[activeContext])[contextCounter];
            this.lastSymbol = EDGE_BREAKER_SYMBOL_TO_TOPOLOGY_ID[symbol_id];
        }
        else if (this.getBitstreamVersion() < 22)
        {
            // We don't have a predicted symbol or the symbol was mis-predicted.
            // Decode it directly.
            this.lastSymbol = super.decodeSymbol();
        }
        else
        {
            // The first symbol must be E.
            this.lastSymbol = EdgeBreakerTopologyBitPattern.E;
        }
        
        
        return lastSymbol;
    }
    
    @Override
    public void newActiveCornerReached(int corner)
    {
        int next = cornerTable.next(corner);
        int prev = cornerTable.previous(corner);
        // Update valences.
        switch(lastSymbol)
        {
            case EdgeBreakerTopologyBitPattern.S:
            case EdgeBreakerTopologyBitPattern.C:
            {
                vertexValences[cornerTable.vertex(next)] += 1;
                vertexValences[cornerTable.vertex(prev)] += 1;
                break;
            }
            case EdgeBreakerTopologyBitPattern.R:
            {
                vertexValences[cornerTable.vertex(corner)] += 1;
                vertexValences[cornerTable.vertex(next)] += 1;
                vertexValences[cornerTable.vertex(prev)] += 2;
                break;
            }
            case EdgeBreakerTopologyBitPattern.L:
            {
                vertexValences[cornerTable.vertex(corner)] += 1;
                vertexValences[cornerTable.vertex(next)] += 2;
                vertexValences[cornerTable.vertex(prev)] += 1;
                break;
            }
            case EdgeBreakerTopologyBitPattern.E:
            {
                vertexValences[cornerTable.vertex(corner)] += 2;
                vertexValences[cornerTable.vertex(next)] += 2;
                vertexValences[cornerTable.vertex(prev)] += 2;
                break;
            }
            default:
            {
                break;
            }
        }
        
        int activeValence = vertexValences[cornerTable.vertex(next)];
        int clampedValence;
        if (activeValence < minValence)
        {
            clampedValence = minValence;
        }
        else if (activeValence > maxValence)
        {
            clampedValence = maxValence;
        }
        else
        {
            clampedValence = activeValence;
        }
        
        
        this.activeContext = clampedValence - minValence;
    }
    
    @Override
    public void mergeVertices(int dest, int source)
    {
        // Update valences on the merged vertices.
        vertexValences[dest] += vertexValences[source];
    }
    
    public MeshEdgeBreakerTraversalValenceDecoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            lastSymbol = EdgeBreakerTopologyBitPattern.INVALID;
            activeContext = -1;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
