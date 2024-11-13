package dev.fileformat.drako;
interface IMeshEdgeBreakerDecoderImpl
{    
    boolean init(MeshEdgeBreakerDecoder decoder);
    
    MeshAttributeCornerTable getAttributeCornerTable(int attId);
    
    MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId);
    
    boolean createAttributesDecoder(int attDecoderId);
    
    boolean decodeConnectivity();
    
    boolean onAttributesDecoded();
    
    MeshEdgeBreakerDecoder getDecoder();
    
    CornerTable getCornerTable();
    
}
