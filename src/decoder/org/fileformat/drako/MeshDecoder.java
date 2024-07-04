package org.fileformat.drako;
/**
 *  Class that reconstructs a 3D mesh from input data that was encoded by
 *  MeshEncoder.
 *
 */
abstract class MeshDecoder extends PointCloudDecoder
{    
    private DracoMesh mesh;
    public MeshDecoder()
    {
        super(EncodedGeometryType.TRIANGULAR_MESH);
    }
    
    public DracoMesh getMesh()
    {
        return mesh;
    }
    
    /**
     *  The main entry point for mesh decoding.
     *
     * @param header 
     * @param inBuffer 
     * @param outMesh 
     * @param decodeData 
     */
    @Override
    public boolean decode(DracoHeader header, DecoderBuffer inBuffer, DracoPointCloud outMesh, boolean decodeData)
    {
        this.mesh = (DracoMesh)outMesh;
        return super.decode(header, inBuffer, outMesh, decodeData);
    }
    
    /**
     *  Returns the base connectivity of the decoded mesh (or nullptr if it is not
     *  initialized).
     *
     */
    public CornerTable getCornerTable()
    {
        return null;
    }
    
    /**
     *  Returns the attribute connectivity data or nullptr if it does not exist.
     *
     * @param attId 
     */
    public MeshAttributeCornerTable getAttributeCornerTable(int attId)
    {
        return null;
    }
    
    /**
     *  Returns the decoding data for a given attribute or nullptr when the data
     *  does not exist.
     *
     * @param attId 
     */
    public MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId)
    {
        return null;
    }
    
    @Override
    protected boolean decodeGeometryData()
    {
        if (mesh == null)
            return DracoUtils.failed();
        if (!this.decodeConnectivity())
            return DracoUtils.failed();
        return super.decodeGeometryData();
    }
    
    protected abstract boolean decodeConnectivity();
    
}
