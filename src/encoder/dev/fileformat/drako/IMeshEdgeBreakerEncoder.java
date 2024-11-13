package dev.fileformat.drako;
interface IMeshEdgeBreakerEncoder
{    
    boolean init(MeshEdgeBreakerEncoder encoder);
    
    MeshAttributeCornerTable getAttributeCornerTable(int attId);
    
    MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId);
    
    boolean generateAttributesEncoder(int attId);
    
    boolean encodeAttributesEncoderIdentifier(int attEncoderId);
    
    boolean encodeConnectivity();
    
    /**
     *  Returns corner table of the encoded mesh.
     *
     * @return  Returns corner table of the encoded mesh.
     */
    CornerTable getCornerTable();
    
    MeshEdgeBreakerEncoder getEncoder();
    
}
