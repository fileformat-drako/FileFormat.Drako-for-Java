package dev.fileformat.drako;
class PointCloudKdTreeDecoder extends PointCloudDecoder
{    
    public PointCloudKdTreeDecoder()
    {
        super(EncodedGeometryType.POINT_CLOUD);
    }
    
    @Override
    protected void decodeGeometryData()
        throws DrakoException
    {
        int num_points = this.buffer.decodeI32();
        if (num_points < 0)
            throw DracoUtils.failed();
        this.getPointCloud().setNumPoints(num_points);
    }
    
    @Override
    protected void createAttributesDecoder(int attrDecoderId)
        throws DrakoException
    {
        // Always create the basic attribute decoder.
        this.setAttributesDecoder(attrDecoderId, new KdTreeAttributesDecoder());
    }
    
}
