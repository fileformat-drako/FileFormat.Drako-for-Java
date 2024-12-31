package dev.fileformat.drako;
/**
 *  A basic implementation of an attribute encoder that can be used to encode
 *  an arbitrary set of attributes. The encoder creates a sequential attribute
 *  encoder for each encoded attribute (see sequentialAttributeEncoder.h) and
 *  then it encodes all attribute values in an order defined by a point sequence
 *  generated in the GeneratePointSequence() method. The default implementation
 *  generates a linear sequence of all points, but derived classes can generate
 *  any custom sequence.
 *
 */
class SequentialAttributeEncodersController extends AttributesEncoder
{    
    private int attId;
    private PointsSequencer sequencer;
    private SequentialAttributeEncoder[] sequentialEncoders;
    private int[] pointIds;
    public SequentialAttributeEncodersController(PointsSequencer sequencer, int attId)
    {
        super(attId);
        this.sequencer = sequencer;
        this.attId = attId;
    }
    
    @Override
    public void initialize(PointCloudEncoder encoder, DracoPointCloud pc)
        throws DrakoException
    {
        super.initialize(encoder, pc);
        this.createSequentialEncoders();
        // Initialize all value encoders.
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            int attId = this.getAttributeId(i);
            sequentialEncoders[i].initialize(encoder, attId);
        }
        
    }
    
    @Override
    public boolean markParentAttribute(int pointAttributeId)
    {
        int loc_id = this.getLocalIdForPointAttribute(pointAttributeId);
        if (loc_id < 0)
            return false;
        sequentialEncoders[loc_id].markParentAttribute();
        return true;
    }
    
    /**
     *  Creates all sequential encoders (one for each attribute associated with the
     *  encoder).
     *
     */
    protected void createSequentialEncoders()
        throws DrakoException
    {
        this.sequentialEncoders = new SequentialAttributeEncoder[this.getNumAttributes()];
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            sequentialEncoders[i] = this.createSequentialEncoder(i);
            if (sequentialEncoders[i] == null)
                throw DracoUtils.failed();
        }
        
    }
    
    /**
     *  Create a sequential encoder for a given attribute based on the attribute
     *  type
     *  and the provided encoder options.
     *
     */
    protected SequentialAttributeEncoder createSequentialEncoder(int i)
    {
        int attId = this.getAttributeId(i);
        PointAttribute att = this.getEncoder().getPointCloud().attribute(attId);
        
        switch(att.getDataType())
        {
            case DataType.UINT8:
            case DataType.INT8:
            case DataType.UINT16:
            case DataType.INT16:
            case DataType.UINT32:
            case DataType.INT32:
                return new SequentialIntegerAttributeEncoder();
            case DataType.FLOAT32:
            {
                {
                    int quantBits = 0;
                    DracoEncodeOptions opts = this.getEncoder().getOptions();
                    if (att.getAttributeType() == AttributeType.NORMAL)
                    {
                        quantBits = opts.getNormalBits();
                    }
                    
                    switch(att.getAttributeType())
                    {
                        case AttributeType.NORMAL:
                        {
                            quantBits = opts.getNormalBits();
                            break;
                        }
                        case AttributeType.COLOR:
                        {
                            quantBits = opts.getColorBits();
                            break;
                        }
                        case AttributeType.POSITION:
                        {
                            quantBits = opts.getPositionBits();
                            break;
                        }
                        case AttributeType.TEX_COORD:
                        {
                            quantBits = opts.getTextureCoordinateBits();
                            break;
                        }
                    }
                    
                    if (quantBits > 0)
                    {
                        if (att.getAttributeType() == AttributeType.NORMAL)
                            return new SequentialNormalAttributeEncoder();else
                            return new SequentialQuantizationAttributeEncoder();
                    }
                    
                    break;
                }
                
            }
        }
        
        // Return the default attribute encoder.
        return new SequentialAttributeEncoder();
    }
    
    @Override
    public void encodeAttributesEncoderData(EncoderBuffer outBuffer)
    {
        super.encodeAttributesEncoderData(outBuffer);
        // Encode a unique id of every sequential encoder.
        for (int i = 0; i < sequentialEncoders.length; ++i)
        {
            outBuffer.encode((byte)(sequentialEncoders[i].getUniqueId()));
        }
        
    }
    
    @Override
    public void encodeAttributes(EncoderBuffer outBuffer)
        throws DrakoException
    {
        if (sequencer == null)
            throw DracoUtils.failed();
        this.pointIds = sequencer.generateSequence();
        super.encodeAttributes(outBuffer);
    }
    
    @Override
    protected void transformAttributesToPortableFormat()
    {
        for (int i = 0; i < sequentialEncoders.length; ++i)
        {
            sequentialEncoders[i].transformAttributeToPortableFormat(pointIds);
        }
        
    }
    
    @Override
    protected void encodePortableAttributes(EncoderBuffer out_buffer)
        throws DrakoException
    {
        for (int i = 0; i < sequentialEncoders.length; ++i)
        {
            sequentialEncoders[i].encodePortableAttribute(pointIds, out_buffer);
        }
        
    }
    
    @Override
    protected void encodeDataNeededByPortableTransforms(EncoderBuffer out_buffer)
        throws DrakoException
    {
        for (int i = 0; i < sequentialEncoders.length; ++i)
        {
            sequentialEncoders[i].encodeDataNeededByPortableTransform(out_buffer);
        }
        
    }
    
    @Override
    public PointAttribute getPortableAttribute(int parentAttId)
    {
        int loc_id = this.getLocalIdForPointAttribute(parentAttId);
        if (loc_id < 0)
            return null;
        return sequentialEncoders[loc_id].portableAttribute;
    }
    
    @Override
    public byte getUniqueId()
    {
        return (byte)((byte)AttributeEncoderType.BASIC);
    }
    
}
