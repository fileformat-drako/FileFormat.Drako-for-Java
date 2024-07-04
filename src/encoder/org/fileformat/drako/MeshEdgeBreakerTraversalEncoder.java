package org.fileformat.drako;
import java.util.ArrayList;
/**
 *  Default implementation of the edgebreaker traversal encoder. Face
 *  configurations are stored directly into the output buffer and the symbols
 *  are first collected and then encoded in the reverse order to make the
 *  decoding faster.
 *
 */
class MeshEdgeBreakerTraversalEncoder implements ITraversalEncoder
{    
    /**
     *  Bit-length of symbols in the EdgeBreakerTopologyBitPattern stored as a
     *  look up table for faster indexing.
     *
     */
    private static final int[] EDGE_BREAKER_TOPOLOGY_BIT_PATTERN_LENGTH = {1, 3, 0, 3, 0, 3, 0, 3};
    /**
     *  Buffers for storing encoded data.
     *
     */
    private RAnsBitEncoder start_face_encoder_;
    private EncoderBuffer traversalBuffer;
    IMeshEdgeBreakerEncoder encoderImpl;
    /**
     *  Symbols collected during the traversal.
     *
     */
    private ArrayList<Integer> symbols;
    /**
     *  Arithmetic encoder for encoding attribute seams.
     *  One context for each non-position attribute.
     *
     */
    private RAnsBitEncoder[] attributeConnectivityEncoders;
    public void init(IMeshEdgeBreakerEncoder encoder)
    {
        this.encoderImpl = encoder;
    }
    
    /**
     *  Called before the traversal encoding is started.
     *
     */
    @Override
    public void start()
    {
        DracoMesh mesh = encoderImpl.getEncoder().getMesh();
        // Allocate enough storage to store initial face configurations. This can
        // consume at most 1 bit per face if all faces are isolated.
        start_face_encoder_.startEncoding();
        if (mesh.getNumAttributes() > 1)
        {
            // Init and start arithemtic encoders for storing configuration types
            // of non-position attributes.
            this.attributeConnectivityEncoders = new RAnsBitEncoder[mesh.getNumAttributes() - 1];
            for (int i = 0; i < (mesh.getNumAttributes() - 1); ++i)
            {
                attributeConnectivityEncoders[i] = new RAnsBitEncoder();
                attributeConnectivityEncoders[i].startEncoding();
            }
            
        }
        
    }
    
    /**
     *  Called when a traversal starts from a new initial face.
     *
     */
    public void encodeStartFaceConfiguration(boolean interior)
    {
        start_face_encoder_.encodeBit(interior);
    }
    
    protected void encodeStartFaces()
    {
        start_face_encoder_.endEncoding(traversalBuffer);
    }
    
    protected void encodeTraversalSymbols()
    {
        // Bit encode the collected symbols.
        // Allocate enough storage for the bit encoder.
        // It's guaranteed that each face will need only up to 3 bits.
        traversalBuffer.startBitEncoding(encoderImpl.getEncoder().getMesh().getNumFaces() * 3, true);
        for (int i = symbols.size() - 1; i >= 0; --i)
        {
            traversalBuffer.encodeLeastSignificantBits32(EDGE_BREAKER_TOPOLOGY_BIT_PATTERN_LENGTH[(int)(symbols.get(i))], (int)(symbols.get(i)));
        }
        
        traversalBuffer.endBitEncoding();
    }
    
    protected void encodeAttributeSeams()
    {
        if (attributeConnectivityEncoders != null)
        {
            for (int i = 0; i < attributeConnectivityEncoders.length; ++i)
            {
                attributeConnectivityEncoders[i].endEncoding(traversalBuffer);
            }
            
        }
        
    }
    
    /**
     *  Called when a new corner is reached during the traversal. No-op for the
     *  default encoder.
     *
     */
    public void newCornerReached(int corner)
    {
    }
    
    /**
     *  Called whenever a new symbol is reached during the edgebreaker traversal.
     *
     */
    public void encodeSymbol(int symbol)
    {
        // Store the symbol. It will be encoded after all symbols are processed.
        symbols.add(symbol);
    }
    
    /**
     *  Called for every pair of connected and visited faces. |isSeam| specifies
     *  whether there is an attribute seam between the two faces.
     *
     */
    public void encodeAttributeSeam(int attribute, boolean isSeam)
    {
        attributeConnectivityEncoders[attribute].encodeBit(isSeam);
    }
    
    /**
     *  Called when the traversal is finished.
     *
     */
    @Override
    public void done()
    {
        this.encodeTraversalSymbols();
        this.encodeStartFaces();
        this.encodeAttributeSeams();
    }
    
    /**
     *  Returns the number of encoded symbols.
     *
     * @return  Returns the number of encoded symbols.
     */
    @Override
    public int getNumEncodedSymbols()
    {
        return symbols.size();
    }
    
    @Override
    public EncoderBuffer getBuffer()
    {
        return traversalBuffer;
    }
    
    protected EncoderBuffer getOutputBuffer()
    {
        return traversalBuffer;
    }
    
    public MeshEdgeBreakerTraversalEncoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            start_face_encoder_ = new RAnsBitEncoder();
            traversalBuffer = new EncoderBuffer();
            symbols = new ArrayList<Integer>();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
