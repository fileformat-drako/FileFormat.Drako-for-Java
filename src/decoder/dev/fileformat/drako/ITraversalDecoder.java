package dev.fileformat.drako;
interface ITraversalDecoder
{    
    /**
     *  Returns true if there is an attribute seam for the next processed pair
     *  of visited faces.
     *  |attribute| is used to mark the id of the non-position attribute (in range
     *  of &lt;0, numAttributes - 1&gt;).
     *
     */
    boolean decodeAttributeSeam(int attribute);
    
    void init(IMeshEdgeBreakerDecoderImpl decoder);
    
    /**
     *  Used to tell the decoder what is the number of expected decoded vertices.
     *  Ignored by default.
     *
     */
    void setNumEncodedVertices(int numVertices);
    
    /**
     *  Set the number of non-position attribute data for which we need to decode
     *  the connectivity.
     *
     */
    void setNumAttributeData(int numData);
    
    /**
     *  Called before the traversal decoding is started.
     *  Returns a buffer decoder that points to data that was encoded after the
     *  traversal.
     *
     */
    boolean start(DecoderBuffer[] outBuffer);
    
    void done();
    
    /**
     *  Returns the next edgebreaker symbol that was reached during the traversal.
     *
     */
    int decodeSymbol();
    
    /**
     *  Called whenever |source| vertex is about to be merged into the |dest|
     *  vertex.
     *
     */
    void mergeVertices(int dest, int source);
    
    /**
     *  Called whenever a new active corner is set in the decoder.
     *
     */
    void newActiveCornerReached(int corner);
    
    /**
     *  Returns the configuration of a new initial face.
     *
     */
    boolean decodeStartFaceConfiguration();
    
}
