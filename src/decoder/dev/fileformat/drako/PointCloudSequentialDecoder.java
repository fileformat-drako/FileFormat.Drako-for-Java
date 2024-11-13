package dev.fileformat.drako;
class PointCloudSequentialDecoder extends PointCloudDecoder
{    
    public PointCloudSequentialDecoder()
    {
        super(EncodedGeometryType.POINT_CLOUD);
    }
    
    @Override
    protected boolean decodeGeometryData()
    {
        int num_points;
        final int[] ref0 = new int[1];
        if (!this.buffer.decode6(ref0))
        {
            num_points = ref0[0];
            return false;
        }
        else
        {
            num_points = ref0[0];
        }
        
        this.getPointCloud().setNumPoints(num_points);
        return true;
    }
    
    @Override
    protected boolean createAttributesDecoder(int attrDecoderId)
    {
        // Always create the basic attribute decoder.
        return this.setAttributesDecoder(attrDecoderId, new SequentialAttributeDecodersController(new LinearSequencer(this.getPointCloud().getNumPoints())));
    }
    
}
