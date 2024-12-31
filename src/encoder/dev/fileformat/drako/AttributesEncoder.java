package dev.fileformat.drako;
/**
 *  Base class for encoding one or more attributes of a PointCloud (or other
 *  geometry). This base class provides only the basic interface that is used
 *  by the PointCloudEncoder. The actual encoding must be implemented in derived
 *  classes using the EncodeAttributes() method.
 *
 */
abstract class AttributesEncoder
{    
    /**
     *  List of attribute ids that need to be encoded with this encoder.
     *
     */
    private int[] pointAttributeIds;
    /**
     *  Map between point attribute id and the local id (i.e., the inverse of the
     *  |pointAttributeIds|.
     *
     */
    private int[] pointAttributeToLocalIdMap;
    PointCloudEncoder pointCloudEncoder;
    DracoPointCloud pointCloud;
    public AttributesEncoder()
    {
    }
    
    /**
     *  Constructs an attribute encoder assosciated with a given point attribute.
     *
     */
    public AttributesEncoder(int pointAttribId)
    {
        this.pointAttributeIds = new int[] {pointAttribId};
        this.pointAttributeToLocalIdMap = new int[] {0};
    }
    
    /**
     *  Called after all attribute encoders are created. It can be used to perform
     *  any custom initialization, including setting up attribute dependencies.
     *  Note: no data should be encoded in this function, because the decoder may
     *  process encoders in a different order from the decoder.
     *
     */
    public void initialize(PointCloudEncoder encoder, DracoPointCloud pc)
        throws DrakoException
    {
        
        this.pointCloudEncoder = encoder;
        this.pointCloud = pc;
    }
    
    /**
     *  Encodes data needed by the target attribute decoder.
     *
     */
    public void encodeAttributesEncoderData(EncoderBuffer outBuffer)
    {
        
        // Encode data about all attributes.
        Encoding.encodeVarint2(this.getNumAttributes(), outBuffer);
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            int attId = pointAttributeIds[i];
            PointAttribute pa = pointCloud.attribute(attId);
            outBuffer.encode((byte)(pa.getAttributeType()));
            outBuffer.encode((byte)(pa.getDataType()));
            outBuffer.encode((byte)(pa.getComponentsCount()));
            outBuffer.encode((byte)(pa.getNormalized() ? 1 : 0));
            Encoding.encodeVarint(0xffff & pa.getUniqueId(), outBuffer);
        }
        
    }
    
    /**
     *  Returns a unique identifier of the given encoder type, that is used during
     *  decoding to ruct the corresponding attribute decoder.
     *
     */
    public abstract byte getUniqueId();
    
    /**
     *  Encode attribute data to the target buffer. Needs to be implmented by the
     *  derived classes.
     *
     */
    public void encodeAttributes(EncoderBuffer out_buffer)
        throws DrakoException
    {
        
        this.transformAttributesToPortableFormat();
        this.encodePortableAttributes(out_buffer);
        // Encode data needed by portable transforms after the attribute is encoded.
        // This corresponds to the order in which the data is going to be decoded by
        // the decoder.
        this.encodeDataNeededByPortableTransforms(out_buffer);
    }
    
    // Transforms the input attribute data into a form that should be losslessly
    // encoded (transform itself can be lossy).
    // 
    protected void transformAttributesToPortableFormat()
        throws DrakoException
    {
    }
    
    // Losslessly encodes data of all portable attributes.
    // Precondition: All attributes must have been transformed into portable
    // format at this point (see TransformAttributesToPortableFormat() method).
    // 
    protected abstract void encodePortableAttributes(EncoderBuffer out_buffer)
        throws DrakoException;
    
    // Encodes any data needed to revert the transform to portable format for each
    // attribute (e.g. data needed for dequantization of quantized values).
    // 
    protected void encodeDataNeededByPortableTransforms(EncoderBuffer out_buffer)
        throws DrakoException
    {
    }
    
    /**
     *  Returns the number of attributes that need to be encoded before the
     *  specified attribute is encoded.
     *  Note that the attribute is specified by its point attribute id.
     *
     */
    public int numParentAttributes(int pointAttributeId)
    {
        return 0;
    }
    
    public int getParentAttributeId(int pointAttributeId, int parentI)
    {
        return -1;
    }
    
    /**
     *  Marks a given attribute as a parent of another attribute.
     *
     */
    public boolean markParentAttribute(int pointAttributeId)
    {
        return false;
    }
    
    public void addAttributeId(int id)
    {
        int[] ids = pointAttributeIds;
        if (ids == null || (ids.length == 0))
        {
            ids = new int[] {id};
        }
        else
        {
            ids = new int[pointAttributeIds.length + 1];
            System.arraycopy(pointAttributeIds, 0, ids, 0, pointAttributeIds.length);
            ids[ids.length - 1] = id;
        }
        
        this.setAttributeIds(ids);
    }
    
    /**
     *  Sets new attribute point ids (replacing the existing ones).
     *
     */
    public void setAttributeIds(int[] pointAttributeIds)
    {
        this.pointAttributeIds = new int[pointAttributeIds.length];
        this.pointAttributeToLocalIdMap = new int[pointAttributeIds.length];
        for (int i = 0; i < pointAttributeIds.length; i++)
        {
            this.pointAttributeIds[i] = pointAttributeIds[i];
            pointAttributeToLocalIdMap[i] = this.pointAttributeIds.length - 1;
        }
        
    }
    
    public int getAttributeId(int i)
    {
        return pointAttributeIds[i];
    }
    
    public int getNumAttributes()
    {
        return pointAttributeIds == null ? 0 : pointAttributeIds.length;
    }
    
    public PointCloudEncoder getEncoder()
    {
        return pointCloudEncoder;
    }
    
    protected int getLocalIdForPointAttribute(int pointAttributeId)
    {
        if (pointAttributeToLocalIdMap == null)
            return -1;
        int idMapSize = pointAttributeToLocalIdMap.length;
        if (pointAttributeId >= idMapSize)
            return -1;
        return pointAttributeToLocalIdMap[pointAttributeId];
    }
    
    public PointAttribute getPortableAttribute(int parentAttId)
    {
        return null;
    }
    
}
