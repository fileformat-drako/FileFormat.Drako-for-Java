package dev.fileformat.drako;
class PointCloudKdTreeEncoder extends PointCloudEncoder
{    
    @Override
    public int getEncodingMethod()
    {
        return DracoEncodingMethod.KD_TREE;
    }
    
    @Override
    protected void generateAttributesEncoder(int attId)
    {
        
        if (this.getNumAttributesEncoders() == 0)
        {
            // Create a new attribute encoder only for the first attribute.
            this.addAttributesEncoder(new KdTreeAttributesEncoder(attId));
            return;
        }
        
        
        // Add a new attribute to the attribute encoder.
        this.attributesEncoder(0).addAttributeId(attId);
    }
    
    @Override
    protected void encodeGeometryData()
    {
        int num_points = this.getPointCloud().getNumPoints();
        this.getBuffer().encode2(num_points);
    }
    
    
}
