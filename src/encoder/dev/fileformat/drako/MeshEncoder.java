package dev.fileformat.drako;
/**
 *  Abstract base class for all mesh encoders. It provides some basic
 *  funcionality that's shared between different encoders.
 *
 */
abstract class MeshEncoder extends PointCloudEncoder
{    
    private DracoMesh mesh;
    public int getGeometryType()
    {
        return EncodedGeometryType.TRIANGULAR_MESH;
    }
    
    /**
     *  Returns the base connectivity of the encoded mesh (or nullptr if it is not
     *  initialized).
     *
     * @return  Returns the base connectivity of the encoded mesh (or nullptr if it is not
 initialized).
     */
    public CornerTable getCornerTable()
    {
        return null;
    }
    
    /**
     *  Returns the attribute connectivity data or nullptr if it does not exist.
     *
     */
    public MeshAttributeCornerTable getAttributeCornerTable(int attId)
    {
        return null;
    }
    
    /**
     *  Returns the encoding data for a given attribute or nullptr when the data
     *  does not exist.
     *
     */
    public MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId)
    {
        return null;
    }
    
    public DracoMesh getMesh()
    {
        return mesh;
    }
    
    public void setMesh(DracoMesh value)
    {
        this.mesh = value;
        this.setPointCloud(value);
    }
    
    @Override
    protected boolean encodeGeometryData()
    {
        
        if (!this.encodeConnectivity())
            return false;
        return true;
    }
    
    /**
     *  Needs to be implemented by the derived classes.
     *
     */
    protected abstract boolean encodeConnectivity();
    
    
}
