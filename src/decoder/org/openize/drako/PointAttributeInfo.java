package org.openize.drako;
class PointAttributeInfo
{    
    public PointAttribute attribute;
    public int offset_dimensionality;
    public int data_type;
    public int data_size;
    public int num_components;
    public PointAttributeInfo(PointAttribute target_att, int total_dimensionality, int data_type, int data_size, int num_components)
    {
        this.attribute = target_att;
        this.offset_dimensionality = total_dimensionality;
        this.data_type = data_type;
        this.data_size = data_size;
        this.num_components = num_components;
    }
    
}
