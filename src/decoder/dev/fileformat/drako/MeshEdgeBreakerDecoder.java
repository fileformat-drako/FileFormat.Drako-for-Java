package dev.fileformat.drako;
/**
 *  Class for decoding data encoded by MeshEdgeBreakerEncoder.
 *
 */
class MeshEdgeBreakerDecoder extends MeshDecoder
{    
    private IMeshEdgeBreakerDecoderImpl impl;
    @Override
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
    protected void initializeDecoder()
        throws DrakoException
    {
        byte traversalDecoderType = this.buffer.decodeU8();
        this.impl = null;
        if (traversalDecoderType == 0)
        {
            this.impl = new MeshEdgeBreakerDecoderImpl(this, new MeshEdgeBreakerTraversalDecoder());
        }
        else if (traversalDecoderType == 1)
        {
            this.impl = new MeshEdgeBreakerDecoderImpl(this, new MeshEdgeBreakerTraversalPredictiveDecoder());
        }
        else if (traversalDecoderType == 2)
        {
            this.impl = new MeshEdgeBreakerDecoderImpl(this, new MeshEdgeBreakerTraversalValenceDecoder());
        }
        else
            throw DracoUtils.failed();
    }
    
    @Override
    protected void createAttributesDecoder(int attDecoderId)
        throws DrakoException
    {
        impl.createAttributesDecoder(attDecoderId);
    }
    
    @Override
    protected void decodeConnectivity()
        throws DrakoException
    {
        impl.decodeConnectivity();
    }
    
    @Override
    protected void onAttributesDecoded()
    {
        impl.onAttributesDecoded();
    }
    
    
}
