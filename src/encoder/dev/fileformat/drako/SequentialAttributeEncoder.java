package dev.fileformat.drako;
/**
 *  A base class for encoding attribute values of a single attribute using a
 *  given sequence of point ids. The default implementation encodes all attribute
 *  values directly to the buffer but derived classes can perform any custom
 *  encoding (such as quantization) by overriding the EncodeValues() method.
 *
 */
class SequentialAttributeEncoder
{    
    private PointCloudEncoder encoder;
    protected PointAttribute attribute;
    PointAttribute portableAttribute;
    private int attributeId;
    /**
     *  List of attribute encoders that need to be encoded before this attribute.
     *  E.g. The parent attributes may be used to predict values used by this
     *  attribute encoder.
     *
     */
    private IntList parentAttributes;
    private boolean isParentEncoder;
    /**
     *  Method that can be used for custom initialization of an attribute encoder,
     *  such as creation of prediction schemes and initialization of attribute
     *  encoder dependencies.
     *  |encoder| is the parent PointCloudEncoder,
     *  |attributeId| is the id of the attribute that is being encoded by this
     *  encoder.
     *  This method is automatically called by the PointCloudEncoder after all
     *  attribute encoders are created and it should not be called explicitly from
     *  other places.
     *
     */
    public void initialize(PointCloudEncoder encoder, int attributeId)
        throws DrakoException
    {
        
        this.encoder = encoder;
        this.attribute = encoder.getPointCloud().attribute(attributeId);
        this.attributeId = attributeId;
    }
    
    /**
     *  Intialization for a specific attribute. This can be used mostly for
     *  standalone encoding of an attribute without an PointCloudEncoder.
     *
     */
    public void initializeStandalone(PointAttribute attribute)
    {
        
        this.attribute = attribute;
        this.attributeId = -1;
    }
    
    public void transformAttributeToPortableFormat(int[] point_ids)
    {
        // Default implementation doesn't transform the input data.
    }
    
    public void encodePortableAttribute(int[] point_ids, EncoderBuffer out_buffer)
        throws DrakoException
    {
        // Lossless encoding of the input values.
        this.encodeValues(point_ids, out_buffer);
    }
    
    public void encodeDataNeededByPortableTransform(EncoderBuffer out_buffer)
        throws DrakoException
    {
        // Default implementation doesn't transform the input data.
    }
    
    protected void setPredictionSchemeParentAttributes(PredictionScheme ps)
        throws DrakoException
    {
        for (int i = 0; i < ps.getNumParentAttributes(); ++i)
        {
            int att_id = encoder.getPointCloud().getNamedAttributeId(ps.getParentAttributeType(i));
            if (att_id == -1)
                throw DracoUtils.failed();
            // Requested attribute does not exist.
            ps.setParentAttribute(encoder.getPortableAttribute(att_id));
        }
        
    }
    
    /**
     *  Encode all attribute values in the order of the provided points.
     *  The actual implementation of the encoding is done in the EncodeValues()
     *  method.
     *
     */
    public boolean encode(int[] pointIds, EncoderBuffer outBuffer)
        throws DrakoException
    {
        this.encodeValues(pointIds, outBuffer);
        if (isParentEncoder && this.isLossyEncoder())
        {
            if (!this.prepareLossyAttributeData())
                return false;
        }
        
        return true;
    }
    
    public boolean isLossyEncoder()
    {
        return false;
    }
    
    public int getNumParentAttributes()
    {
        return parentAttributes.getCount();
    }
    
    public int getParentAttributeId(int i)
    {
        return parentAttributes.get(i);
    }
    
    /**
     *  Called when this attribute encoder becomes a parent encoder of another
     *  encoder.
     *
     */
    public void markParentAttribute()
    {
        
        this.isParentEncoder = true;
    }
    
    public int getUniqueId()
    {
        return SequentialAttributeEncoderType.GENERIC;
    }
    
    public PointAttribute getAttribute()
    {
        return attribute;
    }
    
    public int getAttributeId()
    {
        return attributeId;
    }
    
    public PointCloudEncoder getEncoder()
    {
        return encoder;
    }
    
    /**
     *  Should be used to initialize newly created prediction scheme.
     *  Returns false when the initialization failed (in which case the scheme
     *  cannot be used).
     *
     */
    protected void initPredictionScheme(PredictionScheme ps)
        throws DrakoException
    {
        
        for (int i = 0; i < ps.getNumParentAttributes(); ++i)
        {
            int attId = encoder.getPointCloud().getNamedAttributeId(ps.getParentAttributeType(i));
            if (attId == -1)
                throw DracoUtils.failed();
            // Requested attribute does not exist.
            parentAttributes.add(attId);
            encoder.markParentAttribute(attId);
        }
        
    }
    
    /**
     *  Encodes all attribute values in the specified order. Should be overriden
     *  for specialized  encoders.
     *
     */
    protected void encodeValues(int[] pointIds, EncoderBuffer outBuffer)
        throws DrakoException
    {
        int entrySize = attribute.getByteStride();
        byte[] valueData = new byte[entrySize];
        // Encode all attribute values in their native raw format.
        for (int i = 0; i < pointIds.length; ++i)
        {
            int entryId = attribute.mappedIndex(pointIds[i]);
            attribute.getValue(entryId, valueData);
            outBuffer.encode(valueData, entrySize);
        }
        
    }
    
    /**
     *  Method that can be used by lossy encoders to compute encoded lossy
     *  attribute data.
     *  If the return value is true, the caller can call either
     *  GetLossyAttributeData() or encodedLossyAttributeData() to get a new
     *  attribute that is filled with lossy version of the original data (i.e.,
     *  the same data that is going to be used by the decoder).
     *
     */
    protected boolean prepareLossyAttributeData()
    {
        return false;
    }
    
    protected boolean isParentEncoder()
    {
        return isParentEncoder;
    }
    
    protected static int selectPredictionMethod(int att_id, PointCloudEncoder encoder)
    {
        int speed = encoder.getOptions().getSpeed();
        if (speed >= 10)
            return PredictionSchemeMethod.DIFFERENCE;
        
        if (encoder.getGeometryType() == EncodedGeometryType.TRIANGULAR_MESH)
        {
            PointAttribute att = encoder.getPointCloud().attribute(att_id);
            if (att.getAttributeType() == AttributeType.TEX_COORD)
            {
                if (speed < 4)
                    return PredictionSchemeMethod.TEX_COORDS_PORTABLE;
            }
            
            
            if (att.getAttributeType() == AttributeType.NORMAL)
            {
                if (speed < 4)
                    return PredictionSchemeMethod.GEOMETRIC_NORMAL;
                return PredictionSchemeMethod.DIFFERENCE;
                // default
            }
            
            
            // Handle other attribute types.
            if (speed >= 8)
                return PredictionSchemeMethod.DIFFERENCE;
            if (speed >= 2 || (encoder.getPointCloud().getNumPoints() < 40))
                return PredictionSchemeMethod.PARALLELOGRAM;
            // Multi-parallelogram is used for speeds 0, 1.
            return PredictionSchemeMethod.CONSTRAINED_MULTI_PARALLELOGRAM;
        }
        
        // Default option is delta coding.
        return PredictionSchemeMethod.DIFFERENCE;
    }
    
    public SequentialAttributeEncoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            attributeId = -1;
            parentAttributes = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
