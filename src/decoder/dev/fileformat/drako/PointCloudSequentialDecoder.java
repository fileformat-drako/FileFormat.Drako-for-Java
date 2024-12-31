package dev.fileformat.drako;
class PointCloudSequentialDecoder extends PointCloudDecoder
{    
    public PointCloudSequentialDecoder()
    {
        super(EncodedGeometryType.POINT_CLOUD);
    }
    
    @Override
    protected void decodeGeometryData()
        throws DrakoException
    {
        int num_points = this.buffer.decodeI32();
        this.getPointCloud().setNumPoints(num_points);
    }
    
    @Override
    protected void createAttributesDecoder(int attrDecoderId)
        throws DrakoException
    {
        // Always create the basic attribute decoder.
        this.setAttributesDecoder(attrDecoderId, new SequentialAttributeDecodersController(new LinearSequencer(this.getPointCloud().getNumPoints())));
    }
    
}
