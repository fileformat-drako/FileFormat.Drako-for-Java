package dev.fileformat.drako;
/**
 *  Class implements the edge breaker geometry compression method as described
 *  in "3D Compression Made Simple: Edgebreaker on a Corner-Table" by Rossignac
 *  at al.'01. http://www.cc.gatech.edu/~jarek/papers/CornerTableSMI.pdf
 *
 */
class MeshEdgeBreakerEncoder extends MeshEncoder
{    
    private IMeshEdgeBreakerEncoder impl;
    public CornerTable getCornerTable()
    {
        return impl.getCornerTable();
    }
    
    @Override
    public MeshAttributeCornerTable getAttributeCornerTable(int attId)
    {
        return impl.getAttributeCornerTable(attId);
    }
    
    @Override
    public MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId)
    {
        return impl.getAttributeEncodingData(attId);
    }
    
    @Override
    public int getEncodingMethod()
    {
        return DracoEncodingMethod.EDGE_BREAKER;
    }
    
    @Override
    protected void initializeEncoder()
    {
        this.impl = null;
        if (this.options.getCompressionLevel() == DracoCompressionLevel.OPTIMAL)
        {
            this.getBuffer().encode((byte)1);
            this.impl = new MeshEdgeBreakerEncoderImpl(new MeshEdgeBreakerTraversalPredictiveEncoder());
        }
        else
        {
            this.getBuffer().encode((byte)0);
            this.impl = new MeshEdgeBreakerEncoderImpl(new MeshEdgeBreakerTraversalEncoder());
        }
        
        impl.init(this);
    }
    
    @Override
    protected void encodeConnectivity()
        throws DrakoException
    {
        impl.encodeConnectivity();
    }
    
    @Override
    protected void generateAttributesEncoder(int attId)
        throws DrakoException
    {
        impl.generateAttributesEncoder(attId);
    }
    
    @Override
    protected void encodeAttributesEncoderIdentifier(int attEncoderId)
    {
        impl.encodeAttributesEncoderIdentifier(attEncoderId);
    }
    
    
}
