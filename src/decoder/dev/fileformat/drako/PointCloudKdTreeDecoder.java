package dev.fileformat.drako;
class PointCloudKdTreeDecoder extends PointCloudDecoder
{    
    public PointCloudKdTreeDecoder()
    {
        super(EncodedGeometryType.POINT_CLOUD);
    }
    
    @Override
    protected boolean decodeGeometryData()
    {
        int num_points;
        final int[] ref0 = new int[1];
        if (!this.getBuffer().decode6(ref0))
        {
            num_points = ref0[0];
            return false;
        }
        else
        {
            num_points = ref0[0];
        }
        
        if (num_points < 0)
            return false;
        this.getPointCloud().setNumPoints(num_points);
        return true;
    }
    
    @Override
    protected boolean createAttributesDecoder(int attrDecoderId)
    {
        // Always create the basic attribute decoder.
        return this.setAttributesDecoder(attrDecoderId, new KdTreeAttributesDecoder());
    }
    
}
