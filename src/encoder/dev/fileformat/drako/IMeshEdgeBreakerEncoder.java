package dev.fileformat.drako;
interface IMeshEdgeBreakerEncoder
{    
    void init(MeshEdgeBreakerEncoder encoder);
    
    MeshAttributeCornerTable getAttributeCornerTable(int attId);
    
    MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId);
    
    void generateAttributesEncoder(int attId)
        throws DrakoException;
    
    void encodeAttributesEncoderIdentifier(int attEncoderId);
    
    void encodeConnectivity()
        throws DrakoException;
    
    /**
     *  Returns corner table of the encoded mesh.
     *
     * @return  Returns corner table of the encoded mesh.
     */
    CornerTable getCornerTable();
    
    MeshEdgeBreakerEncoder getEncoder();
    
}
