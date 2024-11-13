package dev.fileformat.drako;
import dev.fileformat.drako.ByteSpan;
import dev.fileformat.drako.FloatSpan;
import dev.fileformat.drako.IntSpan;
class AttributeOctahedronTransform extends AttributeTransform
{    
    private int quantizationBits;
    public AttributeOctahedronTransform(int quantizationBits)
    {
        this.quantizationBits = quantizationBits;
    }
    
    @Override
    protected int getTransformedDataType(PointAttribute attribute)
    {
        return DataType.UINT32;
    }
    
    @Override
    protected int getTransformedNumComponents(PointAttribute attribute)
    {
        return 2;
    }
    
    @Override
    public void copyToAttributeTransformData(AttributeTransformData outData)
    {
        outData.transformType = AttributeTransformType.OCTAHEDRON_TRANSFORM;
        outData.appendValue(quantizationBits);
    }
    
    public boolean encodeParameters(EncoderBuffer encoder_buffer)
    {
        if (quantizationBits != -1)
        {
            encoder_buffer.encode((byte)quantizationBits);
            return true;
        }
        
        return DracoUtils.failed();
    }
    
    public PointAttribute generatePortableAttribute(PointAttribute attribute, int[] point_ids, int num_points)
    {
        int num_entries = point_ids.length;
        PointAttribute portable_attribute = this.initPortableAttribute(num_entries, 2, num_points, attribute, true);
        ByteSpan buffer = new ByteSpan(portable_attribute.getBuffer().getBuffer(), 0, num_entries * 2 * 4);
        IntSpan portable_attribute_data = buffer.asIntSpan();
        FloatSpan att_val = FloatSpan.wrap(new float[3]);
        int dst_index = 0;
        OctahedronToolBox converter = new OctahedronToolBox();
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        if (!converter.setQuantizationBits(quantizationBits))
            return null;
        for (int i = 0; i < point_ids.length; ++i)
        {
            int att_val_id = attribute.mappedIndex(point_ids[i]);
            attribute.getValue(att_val_id, att_val);
            int s;
            int t;
            converter.floatVectorToQuantizedOctahedralCoords(att_val, ref0, ref1);
            s = ref0[0];
            t = ref1[0];
            portable_attribute_data.put(dst_index++, s);
            portable_attribute_data.put(dst_index++, t);
        }
        
        return portable_attribute;
    }
    
}
