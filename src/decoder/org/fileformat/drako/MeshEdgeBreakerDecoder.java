package org.fileformat.drako;
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
    protected boolean initializeDecoder()
    {
        byte traversalDecoderType;
        final byte[] ref0 = new byte[1];
        if (!this.buffer.decode3(ref0))
        {
            traversalDecoderType = ref0[0];
            return DracoUtils.failed();
        }
        else
        {
            traversalDecoderType = ref0[0];
        }
        
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
            return DracoUtils.failed();
        return true;
    }
    
    @Override
    protected boolean createAttributesDecoder(int attDecoderId)
    {
        return impl.createAttributesDecoder(attDecoderId);
    }
    
    @Override
    protected boolean decodeConnectivity()
    {
        return impl.decodeConnectivity();
    }
    
    @Override
    protected boolean onAttributesDecoded()
    {
        return impl.onAttributesDecoded();
    }
    
    
}
