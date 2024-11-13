package dev.fileformat.drako;
import java.util.ArrayList;
/**
 *  Encoder that tries to predict the edgebreaker traversal symbols based on the
 *  vertex valences of the unencoded portion of the mesh. The current prediction
 *  scheme assumes that each vertex has valence 6 which can be used to predict
 *  the symbol preceeding the one that is currently encoded. Predictions are
 *  encoded using an arithmetic coding which can lead to less than 1 bit per
 *  triangle encoding for highly regular meshes.
 *
 */
class MeshEdgeBreakerTraversalPredictiveEncoder extends MeshEdgeBreakerTraversalEncoder
{    
    private CornerTable cornerTable;
    private IntList vertexValences;
    private ArrayList<Boolean> predictions;
    /**
     *  Previously encoded symbol.
     *
     */
    private Integer prevSymbol;
    /**
     *  The total number of encoded split symbols.
     *
     */
    private int numSplitSymbols;
    private int lastCorner;
    /**
     *  Explicitly count the number of encoded symbols.
     *
     */
    private int numSymbols;
    private int computePredictedSymbol(int pivot)
    {
        int valence = vertexValences.get(pivot);
        if (valence < 0)
            return EdgeBreakerTopologyBitPattern.INVALID;
        if (valence < 6)
            return EdgeBreakerTopologyBitPattern.R;
        return EdgeBreakerTopologyBitPattern.C;
    }
    
    @Override
    public void encodeSymbol(int symbol)
    {
        ++numSymbols;
        Integer predictedSymbol = null;
        int next = cornerTable.next(lastCorner);
        int prev = cornerTable.previous(lastCorner);
        switch(symbol)
        {
            case EdgeBreakerTopologyBitPattern.C:
            {
                // Compute prediction.
                predictedSymbol = this.computePredictedSymbol(cornerTable.vertex(next));
                vertexValences.set(cornerTable.vertex(next), vertexValences.get(cornerTable.vertex(next)) - 1);
                vertexValences.set(cornerTable.vertex(prev), vertexValences.get(cornerTable.vertex(prev)) - 1);
                break;
            }
            case EdgeBreakerTopologyBitPattern.S:
            {
                // Update velences.
                vertexValences.set(cornerTable.vertex(next), vertexValences.get(cornerTable.vertex(next)) - 1);
                vertexValences.set(cornerTable.vertex(prev), vertexValences.get(cornerTable.vertex(prev)) - 1);
                // Whenever we reach a split symbol, mark its tip vertex as invalid by
                // setting the valence to a negative value. Any prediction that will
                // use this vertex will then cause a misprediction. This is currently
                // necessary because the decodding works in the reverse direction and
                // the decoder doesn't know about these vertices until the split
                // symbol is decoded at which point two vertices are merged into one.
                // This can be most likely solved on the encoder side by spliting the
                // tip vertex into two, but since split symbols are relatively rare,
                // it's probably not worth doing it.
                vertexValences.set(cornerTable.vertex(lastCorner), -1);
                ++numSplitSymbols;
                break;
            }
            case EdgeBreakerTopologyBitPattern.R:
            {
                // Compute prediction.
                predictedSymbol = this.computePredictedSymbol(cornerTable.vertex(next));
                // Update valences.
                vertexValences.set(cornerTable.vertex(lastCorner), vertexValences.get(cornerTable.vertex(lastCorner)) - 1);
                vertexValences.set(cornerTable.vertex(next), vertexValences.get(cornerTable.vertex(next)) - 1);
                vertexValences.set(cornerTable.vertex(prev), vertexValences.get(cornerTable.vertex(prev)) - 2);
                break;
            }
            case EdgeBreakerTopologyBitPattern.L:
            {
                vertexValences.set(cornerTable.vertex(lastCorner), vertexValences.get(cornerTable.vertex(lastCorner)) - 1);
                vertexValences.set(cornerTable.vertex(next), vertexValences.get(cornerTable.vertex(next)) - 2);
                vertexValences.set(cornerTable.vertex(prev), vertexValences.get(cornerTable.vertex(prev)) - 1);
                break;
            }
            case EdgeBreakerTopologyBitPattern.E:
            {
                vertexValences.set(cornerTable.vertex(lastCorner), vertexValences.get(cornerTable.vertex(lastCorner)) - 2);
                vertexValences.set(cornerTable.vertex(next), vertexValences.get(cornerTable.vertex(next)) - 2);
                vertexValences.set(cornerTable.vertex(prev), vertexValences.get(cornerTable.vertex(prev)) - 2);
                break;
            }
            default:
            {
                break;
            }
        }
        
        boolean storePrevSymbol = true;
        if (predictedSymbol != null)
        {
            if (prevSymbol != null)
            {
                if (predictedSymbol.intValue() == prevSymbol)
                {
                    predictions.add(true);
                    storePrevSymbol = false;
                }
                else
                {
                    predictions.add(false);
                }
                
            }
            
        }
        
        if (storePrevSymbol && (prevSymbol != null))
        {
            super.encodeSymbol(prevSymbol.intValue());
        }
        
        this.prevSymbol = symbol;
    }
    
    @Override
    public void newCornerReached(int corner)
    {
        this.lastCorner = corner;
    }
    
    @Override
    public void done()
    {
        // We still need to store the last encoded symbol.
        if (prevSymbol != null)
        {
            super.encodeSymbol(prevSymbol.intValue());
        }
        
        // Store the init face configurations and the explicitly encoded symbols.
        super.done();
        // Encode the number of split symbols.
        this.getOutputBuffer().encode2(numSplitSymbols);
        RAnsBitEncoder predictionEncoder = new RAnsBitEncoder();
        predictionEncoder.startEncoding();
        for (int i = predictions.size() - 1; i >= 0; --i)
        {
            predictionEncoder.encodeBit(predictions.get(i));
        }
        
        predictionEncoder.endEncoding(this.getOutputBuffer());
    }
    
    @Override
    public void init(IMeshEdgeBreakerEncoder encoder)
    {
        super.init(encoder);
        this.cornerTable = encoder.getCornerTable();
        // Initialize valences of all vertices.
        vertexValences.resize(cornerTable.getNumVertices(), 0);
        for (int i = 0; i < vertexValences.getCount(); ++i)
        {
            vertexValences.set(i, cornerTable.valence(i));
        }
        
    }
    
    public int getNumEncodedSymbols()
    {
        return numSymbols;
    }
    
    public MeshEdgeBreakerTraversalPredictiveEncoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            vertexValences = new IntList();
            predictions = new ArrayList<Boolean>();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
