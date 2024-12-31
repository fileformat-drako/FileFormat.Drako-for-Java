package dev.fileformat.drako;
import java.util.ArrayList;
import java.util.Arrays;
/**
 *  Abstract base class for all point cloud and mesh encoders. It provides a
 *  basic funcionality that's shared between different encoders.
 *
 */
abstract class PointCloudEncoder
{    
    private DracoPointCloud pointCloud;
    private ArrayList<AttributesEncoder> attributesEncoders;
    /**
     *  Map between attribute id and encoder id.
     *
     */
    private int[] attributeToEncoderMap;
    /**
     *  Encoding order of individual attribute encoders (i.e., the order in which
     *  they are processed during encoding that may be different from the order
     *  in which they were created because of attribute dependencies.
     *
     */
    private int[] attributesEncoderIdsOrder;
    /**
     *  This buffer holds the final encoded data.
     *
     */
    protected EncoderBuffer buffer;
    protected DracoEncodeOptions options;
    public PointCloudEncoder()
    {
        this.$initFields$();
    }
    
    /**
     *  The main entry point that encodes provided point cloud.
     *
     * @throws DrakoException Raised when failed to encode the point cloud.
     */
    public void encode(DracoEncodeOptions options, EncoderBuffer outBuffer)
        throws DrakoException
    {
        this.options = options;
        this.buffer = outBuffer;
        
        // Cleanup from previous runs.
        attributesEncoders.clear();
        this.attributeToEncoderMap = null;
        this.attributesEncoderIdsOrder = null;
        
        if (pointCloud == null)
            throw DracoUtils.failed();
        this.initializeEncoder();
        this.encodeEncoderData();
        this.encodeGeometryData();
        this.encodePointAttributes();
    }
    
    public int getGeometryType()
    {
        return EncodedGeometryType.POINT_CLOUD;
    }
    
    /**
     *  Returns the unique identifier of the encoding method (such as Edgebreaker
     *  for mesh compression).
     *
     */
    public abstract int getEncodingMethod();
    
    public int getNumAttributesEncoders()
    {
        return attributesEncoders.size();
    }
    
    public AttributesEncoder attributesEncoder(int i)
    {
        return attributesEncoders.get(i);
    }
    
    /**
     *  Adds a new attribute encoder, returning its id.
     *
     */
    public int addAttributesEncoder(AttributesEncoder attEnc)
    {
        attributesEncoders.add(attEnc);
        return attributesEncoders.size() - 1;
    }
    
    /**
     *  Marks one attribute as a parent of another attribute. Must be called after
     *  all attribute encoders are created (usually in the
     *  AttributeEncoder::Initialize() method).
     *
     */
    public boolean markParentAttribute(int parentAttId)
    {
        
        if (parentAttId < 0 || (parentAttId >= pointCloud.getNumAttributes()))
            return false;
        int parentAttEncoderId = attributeToEncoderMap[parentAttId];
        if (!attributesEncoders.get(parentAttEncoderId).markParentAttribute(parentAttId))
            return false;
        return true;
    }
    
    public EncoderBuffer getBuffer()
    {
        return buffer;
    }
    
    public DracoEncodeOptions getOptions()
    {
        return options;
    }
    
    public DracoPointCloud getPointCloud()
    {
        return pointCloud;
    }
    
    public void setPointCloud(DracoPointCloud value)
    {
        this.pointCloud = value;
    }
    
    /**
     *  Can be implemented by derived classes to perform any custom initialization
     *  of the encoder. Called in the Encode() method.
     *
     */
    protected void initializeEncoder()
    {
    }
    
    /**
     *  Should be used to encode any encoder-specific data.
     *
     */
    protected void encodeEncoderData()
    {
    }
    
    /**
     *  Encodes any global geometry data (such as the number of points).
     *
     */
    protected void encodeGeometryData()
        throws DrakoException
    {
    }
    
    /**
     *  encode all attribute values. The attribute encoders are sorted to resolve
     *  any attribute dependencies and all the encoded data is stored into the
     *  |buffer|.
     *  Returns false if the encoding failed.
     *
     */
    protected void encodePointAttributes()
        throws DrakoException
    {
        
        this.generateAttributesEncoders();
        
        // Encode the number of attribute encoders.
        buffer.encode((byte)(attributesEncoders.size()));
        
        // Initialize all the encoders (this is used for example to init attribute
        // dependencies, no data is encoded in this step).
        for (AttributesEncoder attEnc : attributesEncoders)
        {
            attEnc.initialize(this, pointCloud);
        }
        
        
        // Rearrange attributes to respect dependencies between individual attributes.
        this.rearrangeAttributesEncoders();
        
        // Encode any data that is necessary to create the corresponding attribute
        // decoder.
        for (int i = 0; i < attributesEncoderIdsOrder.length; i++)
        {
            int attEncoderId = attributesEncoderIdsOrder[i];
            this.encodeAttributesEncoderIdentifier(attEncoderId);
        }
        
        
        // Also encode any attribute encoder data (such as the info about encoded
        // attributes).
        for (int i = 0; i < attributesEncoderIdsOrder.length; i++)
        {
            int attEncoderId = attributesEncoderIdsOrder[i];
            attributesEncoders.get(attEncoderId).encodeAttributesEncoderData(buffer);
        }
        
        
        // Lastly encode all the attributes using the provided attribute encoders.
        this.encodeAllAttributes();
    }
    
    /**
     *  Generate attribute encoders that are going to be used for encoding
     *  point attribute data. Calls GenerateAttributesEncoder() for every attribute
     *  of the encoded PointCloud.
     *
     * @throws DrakoException throws when failed to generate encoders
     */
    protected void generateAttributesEncoders()
        throws DrakoException
    {
        
        for (int i = 0; i < pointCloud.getNumAttributes(); ++i)
        {
            this.generateAttributesEncoder(i);
        }
        
        this.attributeToEncoderMap = new int[pointCloud.getNumAttributes()];
        for (int i = 0; i < attributesEncoders.size(); ++i)
        {
            for (int j = 0; j < attributesEncoders.get(i).getNumAttributes(); ++j)
            {
                attributeToEncoderMap[attributesEncoders.get(i).getAttributeId(j)] = i;
            }
            
        }
        
    }
    
    /**
     *  Creates attribute encoder for a specific point attribute. This function
     *  needs to be implemented by the derived classes. The derived classes need
     *  to either 1. Create a new attribute encoder and add it using the
     *  AddAttributeEncoder method, or 2. add the attribute to an existing
     *  attribute encoder (using AttributesEncoder::AddAttributeId() method).
     *
     */
    protected abstract void generateAttributesEncoder(int attId)
        throws DrakoException;
    
    /**
     *  Encodes any data that is necessary to recreate a given attribute encoder.
     *  Note: this is called in order in which the attribute encoders are going to
     *  be encoded.
     *
     */
    protected void encodeAttributesEncoderIdentifier(int attEncoderId)
    {
    }
    
    /**
     *  Encodes all the attribute data using the created attribute encoders.
     *
     */
    protected void encodeAllAttributes()
        throws DrakoException
    {
        
        for (int i = 0; i < attributesEncoderIdsOrder.length; i++)
        {
            int attEncoderId = attributesEncoderIdsOrder[i];
            attributesEncoders.get(attEncoderId).encodeAttributes(buffer);
        }
        
    }
    
    /**
     *  Rearranges attribute encoders and their attributes to reflect the
     *  underlying attribute dependencies. This ensures that the attributes are
     *  encoded in the correct order (parent attributes before their children).
     *
     */
    private void rearrangeAttributesEncoders()
        throws DrakoException
    {
        
        // Find the encoding order of the attribute encoders that is determined by
        // the parent dependencies between individual encoders. Instead of traversing
        // a graph we encode the attributes in multiple iterations where encoding of
        // attributes that depend on other attributes may get posponed until the
        // parent attributes are processed.
        // This is simpler to implement than graph traversal and it automatically
        // detects any cycles in the dependency graph.
        // TODO(ostava): Current implementation needs to encode all attributes of a
        // single encoder to be encoded in a single "chunk", therefore we need to sort
        // attribute encoders before we sort individual attributes. This requirement
        // can be lifted for encoders that can encode individual attributes separately
        // but it will require changes in the current API.
        attributesEncoderIdsOrder = attributesEncoderIdsOrder == null ? new int[attributesEncoders.size()] : Arrays.copyOf(attributesEncoderIdsOrder, attributesEncoders.size());
        boolean[] isEncoderProcessed = new boolean[attributesEncoders.size()];
        int numProcessedEncoders = 0;
        while (numProcessedEncoders < attributesEncoders.size())
        {
            boolean encoderProcessed = false;
            for (int i = 0; i < attributesEncoders.size(); ++i)
            {
                if (isEncoderProcessed[i])
                    continue;
                // Encoder already processed.
                boolean canBeProcessed = true;
                for (int p = 0; p < attributesEncoders.get(i).getNumAttributes(); ++p)
                {
                    int attId = attributesEncoders.get(i).getAttributeId(p);
                    for (int ap = 0; ap < attributesEncoders.get(i).numParentAttributes(attId); ++ap)
                    {
                        int parentAttId = attributesEncoders.get(i).getParentAttributeId(attId, ap);
                        int parentEncoderId = attributeToEncoderMap[parentAttId];
                        if (parentAttId != i && !isEncoderProcessed[parentEncoderId])
                        {
                            canBeProcessed = false;
                            break;
                        }
                        
                    }
                    
                }
                
                if (!canBeProcessed)
                    continue;
                // Try to process the encoder in the next iteration.
                // Encoder can be processed. Update the encoding order.
                attributesEncoderIdsOrder[numProcessedEncoders++] = i;
                isEncoderProcessed[i] = true;
                encoderProcessed = true;
            }
            
            if (!encoderProcessed && (numProcessedEncoders < attributesEncoders.size()))
                throw DracoUtils.failed();
        }
        
        int[] attributeEncodingOrder = null;
        boolean[] isAttributeProcessed = new boolean[pointCloud.getNumAttributes()];
        int numProcessedAttributes;
        for (int aeOrder = 0; aeOrder < attributesEncoders.size(); ++aeOrder)
        {
            int ae = attributesEncoderIdsOrder[aeOrder];
            int numEncoderAttributes = attributesEncoders.get(ae).getNumAttributes();
            if (numEncoderAttributes < 2)
                continue;
            // No need to resolve dependencies for a single attribute.
            numProcessedAttributes = 0;
            attributeEncodingOrder = attributeEncodingOrder == null ? new int[numEncoderAttributes] : Arrays.copyOf(attributeEncodingOrder, numEncoderAttributes);
            while (numProcessedAttributes < numEncoderAttributes)
            {
                boolean attributeProcessed = false;
                for (int i = 0; i < numEncoderAttributes; ++i)
                {
                    int attId = attributesEncoders.get(ae).getAttributeId(i);
                    if (isAttributeProcessed[i])
                        continue;
                    // Attribute already processed.
                    boolean canBeProcessed = true;
                    for (int p = 0; p < attributesEncoders.get(ae).numParentAttributes(attId); ++p)
                    {
                        int parentAttId = attributesEncoders.get(ae).getParentAttributeId(attId, p);
                        if (!isAttributeProcessed[parentAttId])
                        {
                            canBeProcessed = false;
                            break;
                        }
                        
                    }
                    
                    if (!canBeProcessed)
                        continue;
                    // Try to process the attribute in the next iteration.
                    // Attribute can be processed. Update the encoding order.
                    attributeEncodingOrder[numProcessedAttributes++] = i;
                    isAttributeProcessed[i] = true;
                    attributeProcessed = true;
                }
                
                if (!attributeProcessed && (numProcessedAttributes < numEncoderAttributes))
                    throw DracoUtils.failed();
            }
            
            // Update the order of the attributes within the encoder.
            attributesEncoders.get(ae).setAttributeIds(attributeEncodingOrder);
        }
        
    }
    
    public PointAttribute getPortableAttribute(int parent_att_id)
    {
        if (parent_att_id < 0 || (parent_att_id >= pointCloud.getNumAttributes()))
            return null;
        int parent_att_encoder_id = attributeToEncoderMap[parent_att_id];
        return attributesEncoders.get(parent_att_encoder_id).getPortableAttribute(parent_att_id);
    }
    
    private void $initFields$()
    {
        try
        {
            attributesEncoders = new ArrayList<AttributesEncoder>();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
