package org.fileformat.drako;
class PointCloudKdTreeEncoder extends PointCloudEncoder
{    
    @Override
    public int getEncodingMethod()
    {
        return DracoEncodingMethod.KD_TREE;
    }
    
    @Override
    protected boolean generateAttributesEncoder(int attId)
    {
        
        if (this.getNumAttributesEncoders() == 0)
        {
            // Create a new attribute encoder only for the first attribute.
            this.addAttributesEncoder(new KdTreeAttributesEncoder(attId));
            return true;
        }
        
        
        // Add a new attribute to the attribute encoder.
        this.attributesEncoder(0).addAttributeId(attId);
        return true;
        
    }
    
    @Override
    protected boolean encodeGeometryData()
    {
        int num_points = this.getPointCloud().getNumPoints();
        this.getBuffer().encode2(num_points);
        return true;
    }
    
    
}
