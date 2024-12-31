package dev.fileformat.drako;
interface IMeshEdgeBreakerDecoderImpl
{    
    void init(MeshEdgeBreakerDecoder decoder);
    
    MeshAttributeCornerTable getAttributeCornerTable(int attId);
    
    MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId);
    
    void createAttributesDecoder(int attDecoderId)
        throws DrakoException;
    
    void decodeConnectivity()
        throws DrakoException;
    
    void onAttributesDecoded();
    
    MeshEdgeBreakerDecoder getDecoder();
    
    CornerTable getCornerTable();
    
}
