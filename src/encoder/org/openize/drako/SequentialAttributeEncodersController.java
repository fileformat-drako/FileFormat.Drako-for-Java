package org.openize.drako;
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
    public boolean initialize(PointCloudEncoder encoder, DracoPointCloud pc)
    {
        if (!super.initialize(encoder, pc))
            return false;
        if (!this.createSequentialEncoders())
            return false;
        // Initialize all value encoders.
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            int attId = this.getAttributeId(i);
            if (!sequentialEncoders[i].initialize(encoder, attId))
                return false;
        }
        
        return true;
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
    protected boolean createSequentialEncoders()
    {
        this.sequentialEncoders = new SequentialAttributeEncoder[this.getNumAttributes()];
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            sequentialEncoders[i] = this.createSequentialEncoder(i);
            if (sequentialEncoders[i] == null)
                return false;
        }
        
        return true;
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
    public boolean encodeAttributesEncoderData(EncoderBuffer outBuffer)
    {
        if (!super.encodeAttributesEncoderData(outBuffer))
            return false;
        // Encode a unique id of every sequential encoder.
        for (int i = 0; i < sequentialEncoders.length; ++i)
        {
            outBuffer.encode((byte)(sequentialEncoders[i].getUniqueId()));
        }
        
        return true;
    }
    
    @Override
    public boolean encodeAttributes(EncoderBuffer outBuffer)
    {
        final int[][] ref0 = new int[1][];
        if (sequencer == null || !sequencer.generateSequence(ref0))
        {
            pointIds = ref0[0];
            return false;
        }
        else
        {
            pointIds = ref0[0];
        }
        
        return super.encodeAttributes(outBuffer);
    }
    
    @Override
    protected boolean transformAttributesToPortableFormat()
    {
        for (int i = 0; i < sequentialEncoders.length; ++i)
        {
            if (!sequentialEncoders[i].transformAttributeToPortableFormat(pointIds))
                return false;
        }
        
        return true;
    }
    
    @Override
    protected boolean encodePortableAttributes(EncoderBuffer out_buffer)
    {
        for (int i = 0; i < sequentialEncoders.length; ++i)
        {
            if (!sequentialEncoders[i].encodePortableAttribute(pointIds, out_buffer))
                return false;
        }
        
        return true;
    }
    
    @Override
    protected boolean encodeDataNeededByPortableTransforms(EncoderBuffer out_buffer)
    {
        for (int i = 0; i < sequentialEncoders.length; ++i)
        {
            if (!sequentialEncoders[i].encodeDataNeededByPortableTransform(out_buffer))
                return false;
        }
        
        return true;
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
