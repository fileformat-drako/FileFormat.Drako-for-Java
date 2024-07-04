package org.fileformat.drako;
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
     */
    public boolean encode(DracoEncodeOptions options, EncoderBuffer outBuffer)
    {
        
        this.options = options;
        this.buffer = outBuffer;
        
        // Cleanup from previous runs.
        attributesEncoders.clear();
        this.attributeToEncoderMap = null;
        this.attributesEncoderIdsOrder = null;
        
        if (pointCloud == null)
            return false;
        if (!this.initializeEncoder())
            return false;
        if (!this.encodeEncoderData())
            return false;
        if (!this.encodeGeometryData())
            return false;
        if (!this.encodePointAttributes())
            return false;
        return true;
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
    protected boolean initializeEncoder()
    {
        return true;
    }
    
    /**
     *  Should be used to encode any encoder-specific data.
     *
     */
    protected boolean encodeEncoderData()
    {
        return true;
    }
    
    /**
     *  Encodes any global geometry data (such as the number of points).
     *
     */
    protected boolean encodeGeometryData()
    {
        return true;
    }
    
    /**
     *  encode all attribute values. The attribute encoders are sorted to resolve
     *  any attribute dependencies and all the encoded data is stored into the
     *  |buffer|.
     *  Returns false if the encoding failed.
     *
     */
    protected boolean encodePointAttributes()
    {
        
        if (!this.generateAttributesEncoders())
            return false;
        
        // Encode the number of attribute encoders.
        buffer.encode((byte)(attributesEncoders.size()));
        
        // Initialize all the encoders (this is used for example to init attribute
        // dependencies, no data is encoded in this step).
        for (AttributesEncoder attEnc : attributesEncoders)
        {
            if (!attEnc.initialize(this, pointCloud))
                return false;
        }
        
        
        // Rearrange attributes to respect dependencies between individual attributes.
        if (!this.rearrangeAttributesEncoders())
            return false;
        
        // Encode any data that is necessary to create the corresponding attribute
        // decoder.
        for (int i = 0; i < attributesEncoderIdsOrder.length; i++)
        {
            int attEncoderId = attributesEncoderIdsOrder[i];
            if (!this.encodeAttributesEncoderIdentifier(attEncoderId))
                return false;
        }
        
        
        // Also encode any attribute encoder data (such as the info about encoded
        // attributes).
        for (int i = 0; i < attributesEncoderIdsOrder.length; i++)
        {
            int attEncoderId = attributesEncoderIdsOrder[i];
            if (!attributesEncoders.get(attEncoderId).encodeAttributesEncoderData(buffer))
                return false;
        }
        
        
        // Lastly encode all the attributes using the provided attribute encoders.
        if (!this.encodeAllAttributes())
            return false;
        return true;
    }
    
    /**
     *  Generate attribute encoders that are going to be used for encoding
     *  point attribute data. Calls GenerateAttributesEncoder() for every attribute
     *  of the encoded PointCloud.
     *
     */
    protected boolean generateAttributesEncoders()
    {
        
        for (int i = 0; i < pointCloud.getNumAttributes(); ++i)
        {
            if (!this.generateAttributesEncoder(i))
                return false;
        }
        
        this.attributeToEncoderMap = new int[pointCloud.getNumAttributes()];
        for (int i = 0; i < attributesEncoders.size(); ++i)
        {
            for (int j = 0; j < attributesEncoders.get(i).getNumAttributes(); ++j)
            {
                attributeToEncoderMap[attributesEncoders.get(i).getAttributeId(j)] = i;
            }
            
        }
        
        return true;
    }
    
    /**
     *  Creates attribute encoder for a specific point attribute. This function
     *  needs to be implemented by the derived classes. The derived classes need
     *  to either 1. Create a new attribute encoder and add it using the
     *  AddAttributeEncoder method, or 2. add the attribute to an existing
     *  attribute encoder (using AttributesEncoder::AddAttributeId() method).
     *
     */
    protected abstract boolean generateAttributesEncoder(int attId);
    
    /**
     *  Encodes any data that is necessary to recreate a given attribute encoder.
     *  Note: this is called in order in which the attribute encoders are going to
     *  be encoded.
     *
     */
    protected boolean encodeAttributesEncoderIdentifier(int attEncoderId)
    {
        return true;
    }
    
    /**
     *  Encodes all the attribute data using the created attribute encoders.
     *
     */
    protected boolean encodeAllAttributes()
    {
        
        for (int i = 0; i < attributesEncoderIdsOrder.length; i++)
        {
            int attEncoderId = attributesEncoderIdsOrder[i];
            if (!attributesEncoders.get(attEncoderId).encodeAttributes(buffer))
                return false;
        }
        
        return true;
    }
    
    /**
     *  Rearranges attribute encoders and their attributes to reflect the
     *  underlying attribute dependencies. This ensures that the attributes are
     *  encoded in the correct order (parent attributes before their children).
     *
     */
    private boolean rearrangeAttributesEncoders()
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
                return false;
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
                    return false;
            }
            
            // Update the order of the attributes within the encoder.
            attributesEncoders.get(ae).setAttributeIds(attributeEncodingOrder);
        }
        
        return true;
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
