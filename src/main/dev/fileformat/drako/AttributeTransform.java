package dev.fileformat.drako;
abstract class AttributeTransform
{    
    // Copy parameter values into the provided AttributeTransformData instance.
    // 
    public abstract void copyToAttributeTransformData(AttributeTransformData outData);
    
    public void transferToAttribute(PointAttribute attribute)
    {
        AttributeTransformData transform_data = new AttributeTransformData();
        this.copyToAttributeTransformData(transform_data);
        attribute.setAttributeTransformData(transform_data);
    }
    
    protected PointAttribute initPortableAttribute(int num_entries, int num_components, int num_points, PointAttribute attribute, boolean is_unsigned)
    {
        int dt = is_unsigned ? DataType.UINT32 : DataType.INT32;
        PointAttribute portable_attribute = new PointAttribute();
        portable_attribute.setAttributeType(attribute.getAttributeType());
        portable_attribute.setComponentsCount(num_components);
        portable_attribute.setDataType(dt);
        portable_attribute.setByteStride(num_components * DracoUtils.dataTypeLength(dt));
        portable_attribute.reset(num_entries);
        if (num_points > 0)
        {
            portable_attribute.setExplicitMapping(num_points);
        }
        else
        {
            portable_attribute.setIdentityMapping(true);
        }
        
        
        return portable_attribute;
    }
    
    public PointAttribute initTransformedAttribute(PointAttribute src_attribute, int num_entries)
    {
        int num_components = this.getTransformedNumComponents(src_attribute);
        int dt = this.getTransformedDataType(src_attribute);
        PointAttribute transformed_attribute = new PointAttribute();
        transformed_attribute.setAttributeType(src_attribute.getAttributeType());
        transformed_attribute.setComponentsCount(num_components);
        transformed_attribute.setDataType(dt);
        transformed_attribute.setNormalized(false);
        transformed_attribute.setByteStride(num_components * DracoUtils.dataTypeLength(dt));
        
        transformed_attribute.reset(num_entries);
        transformed_attribute.setIdentityMapping(true);
        
        transformed_attribute.setUniqueId(src_attribute.getUniqueId());
        return transformed_attribute;
    }
    
    protected abstract int getTransformedDataType(PointAttribute attribute);
    
    protected abstract int getTransformedNumComponents(PointAttribute attribute);
    
    
}
