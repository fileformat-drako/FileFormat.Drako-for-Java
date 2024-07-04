package org.fileformat.drako;
interface ITraversalEncoder
{    
    void encodeSymbol(int sym);
    
    /**
     *  Called when a new corner is reached during the traversal. No-op for the
     *  default encoder.
     *
     */
    void newCornerReached(int corner);
    
    /**
     *  Called for every pair of connected and visited faces. |isSeam| specifies
     *  whether there is an attribute seam between the two faces.
     *
     */
    void encodeAttributeSeam(int attribute, boolean isSeam);
    
    /**
     *  Called before the traversal encoding is started.
     *
     */
    void start();
    
    /**
     *  Called when the traversal is finished.
     *
     */
    void done();
    
    /**
     *  Called when a traversal starts from a new initial face.
     *
     */
    void encodeStartFaceConfiguration(boolean interior);
    
    void init(IMeshEdgeBreakerEncoder encoder);
    
    EncoderBuffer getBuffer();
    
    int getNumEncodedSymbols();
    
}
