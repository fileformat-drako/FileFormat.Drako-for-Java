package org.fileformat.drako;
import com.aspose.csporter.helpers.AsposeUtils;
import com.aspose.csporter.helpers.Struct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
/**
 *  DracoPointCloud is a collection of n-dimensional points that are described by a
 *  set of PointAttributes that can represent data such as positions or colors
 *  of individual points (see pointAttribute.h).
 *
 */
public class DracoPointCloud
{    
    static final class VertexIndex implements Comparable<VertexIndex>, Struct<VertexIndex>, Serializable
    {
        private DracoPointCloud cloud;
        private int p;
        public VertexIndex(DracoPointCloud cloud, int p)
        {
            this.cloud = cloud;
            this.p = p;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof VertexIndex))
                return false;
            int ret = this.compareTo((VertexIndex)obj);
            return ret == 0;
        }
        
        public int compareTo(VertexIndex other)
        {
            for (int i = 0; i < cloud.attributes.size(); ++i)
            {
                PointAttribute attr = cloud.attributes.get(i);
                int id0 = attr.mappedIndex(p);
                int id1 = attr.mappedIndex(other.p);
                if (id0 < id1)
                    return -1;
                if (id0 > id1)
                    return 1;
            }
            
            return 0;
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("p: "));
            for (int i = 0; i < cloud.attributes.size(); ++i)
            {
                PointAttribute attr = cloud.attributes.get(i);
                int id0 = attr.mappedIndex(p);
                if (i > 0)
                {
                    sb.append("/");
                }
                
                sb.append(id0);
            }
            
            return sb.toString();
        }
        
        public VertexIndex()
        {
        }
        
        private VertexIndex(VertexIndex other)
        {
            this.cloud = other.cloud;
            this.p = other.p;
        }
        
        @Override
        public VertexIndex clone()
        {
            return new VertexIndex(this);
        }
        
        @Override
        public void copyFrom(VertexIndex src)
        {
            if (src == null)
                return;
            this.cloud = src.cloud;
            this.p = src.p;
        }
        
        static final long serialVersionUID = -636171031L;
        @Override
        public int hashCode()
        {
            int hash = 0;
            for (int i = 0; i < cloud.attributes.size(); ++i)
            {
                int attId = cloud.attributes.get(i).mappedIndex(p);
                hash = attId << 2 ^ (hash << 1);
            }
            
            return hash;
        }
        
    }
    
    /**
     *  Attributes describing the point cloud.
     *
     */
    private ArrayList<PointAttribute> attributes;
    private ArrayList<GeometryMetadata> metadatas;
    /**
     *  Ids of named attributes of the given type.
     *
     */
    private IntList[] namedAttributeIndex;
    /**
     *  The number of n-dimensional points. All point attribute values are stored
     *  in corresponding PointAttribute instances in the |attributes| array.
     *
     */
    private int numPoints;
    /**
     *  Returns the number of named attributes of a given type.
     *
     */
    public int numNamedAttributes(int type)
    {
        int idx = type;
        if (idx < 0 || (idx >= namedAttributeIndex.length))
            return 0;
        return namedAttributeIndex[idx].getCount();
    }
    
    /**
     *  Returns attribute id of the first named attribute with a given type or -1
     *  when the attribute is not used by the point cloud.
     *
     */
    public int getNamedAttributeId(int type)
    {
        return this.getNamedAttributeId(type, 0);
    }
    
    /**
     *  Returns the id of the i-th named attribute of a given type.
     *
     */
    public int getNamedAttributeId(int type, int i)
    {
        int idx = type;
        if (idx < 0 || (idx >= namedAttributeIndex.length))
            return -1;
        IntList attrs = namedAttributeIndex[idx];
        if (attrs == null)
            return -1;
        if (i < 0 || (i >= attrs.getCount()))
            return -1;
        return attrs.get(i);
    }
    
    /**
     *  Returns the i-th named attribute of a given type.
     *
     */
    public PointAttribute getNamedAttribute(int type, int i)
    {
        int id = this.getNamedAttributeId(type, i);
        if (id == -1)
            return null;
        return attributes.get(id);
    }
    
    /**
     *  Returns the i-th named attribute of a given type.
     *
     */
    public PointAttribute getNamedAttribute(int type)
    {
        return this.getNamedAttribute(type, 0);
    }
    
    /**
     *  Returns the named attribute of a given custom id.
     *
     */
    public PointAttribute getNamedAttributeByCustomId(int type, short customId)
    {
        int idx = type;
        for (int attId = 0; attId < namedAttributeIndex[idx].getCount(); ++attId)
        {
            if (attributes.get(namedAttributeIndex[idx].get(attId)).getUniqueId() == customId)
                return attributes.get(namedAttributeIndex[idx].get(attId));
        }
        
        return null;
    }
    
    public int getNumAttributes()
    {
        return attributes.size();
    }
    
    public PointAttribute attribute(int attId)
    {
        return attributes.get(attId);
    }
    
    /**
     *  Adds a new attribute to the point cloud.
     *  Returns the attribute id.
     *
     * @param pa 
     */
    public int addAttribute(PointAttribute pa)
    {
        attributes.add(pa);
        IntList attrs = namedAttributeIndex[pa.getAttributeType()];
        if (attrs == null)
        {
            final IntList tmp0 = new IntList();
            namedAttributeIndex[pa.getAttributeType()] = tmp0;
            attrs = tmp0;
        }
        
        int ret = attributes.size() - 1;
        attrs.add(ret);
        pa.setUniqueId((short)ret);
        return ret;
    }
    
    /**
     *  Creates and adds a new attribute to the point cloud. The attribute has
     *  properties derived from the provided GeometryAttribute |att|.
     *  If |identityMapping| is set to true, the attribute will use identity
     *  mapping between point indices and attribute value indices (i.e., each point
     *  has a unique attribute value).
     *  If |identityMapping| is false, the mapping between point indices and
     *  attribute value indices is set to explicit, and it needs to be initialized
     *  manually using the PointAttribute::SetPointMapEntry() method.
     *  |numAttributeValues| can be used to specify the number of attribute
     *  values that are going to be stored in the newly created attribute.
     *  Returns attribute id of the newly created attribute.
     *
     */
    public int addAttribute(GeometryAttribute att, boolean identityMapping, int numAttributeValues)
    {
        int type = att.getAttributeType();
        if (type == AttributeType.INVALID)
            return -1;
        int attId = this.addAttribute((PointAttribute)att);
        PointAttribute pa = this.attribute(attId);
        // Initialize point cloud specific attribute data.
        pa.setIdentityMapping(identityMapping);
        if (!identityMapping)
        {
            // First create mapping between indices.
            pa.setExplicitMapping(numPoints);
        }
        else
        {
            pa.resize(numPoints);
        }
        
        if (numAttributeValues > 0)
        {
            pa.reset(numAttributeValues);
        }
        
        return attId;
    }
    
    /**
     *  Deduplicates all attribute values (all attribute entries with the same
     *  value are merged into a single entry).
     *
     */
    public boolean deduplicateAttributeValues()
    {
        
        if (numPoints == 0)
            return DracoUtils.failed();
        // Unexcpected attribute size.
        // Deduplicate all attributes.
        for (int i = 0; i < attributes.size(); i++)
        {
            PointAttribute attr = attributes.get(i);
            attr.deduplicateValues();
        }
        
        return true;
    }
    
    /**
     *  Removes duplicate point ids (two point ids are duplicate when all of their
     *  attributes are mapped to the same entry ids).
     *
     */
    public void deduplicatePointIds()
    {
        HashMap<VertexIndex, Integer> uniquePointMap = new HashMap<VertexIndex, Integer>();
        int numUniquePoints = 0;
        int[] indexMap = new int[numPoints];
        IntList uniquePoints = new IntList();
        final Integer[] ref1 = new Integer[1];
        // Go through all vertices and find their duplicates.
        for (int i = 0; i < numPoints; ++i)
        {
            int tmp;
            VertexIndex p = new VertexIndex(this, i);
            if (AsposeUtils.tryGetValue(uniquePointMap, p, ref1))
            {
                tmp = ref1[0] == null ? 0 : ref1[0];
                indexMap[i] = tmp;
            }
            else
            {
                tmp = ref1[0] == null ? 0 : ref1[0];
                uniquePointMap.put(Struct.byVal(p), numUniquePoints);
                indexMap[i] = numUniquePoints++;
                uniquePoints.add(i);
            }
            
        }
        
        if (numUniquePoints == numPoints)
            return;
        // All vertices are already unique.
        
        this.applyPointIdDeduplication(indexMap, uniquePoints);
        this.setNumPoints(numUniquePoints);
    }
    
    /**
     *  Gets the number of n-dimensional points stored within the point cloud.
     *
     * @return   the number of n-dimensional points stored within the point cloud.
     */
    public int getNumPoints()
    {
        return numPoints;
    }
    
    /**
     *  Sets the number of n-dimensional points stored within the point cloud.
     *
     * @param value New value
     */
    public void setNumPoints(int value)
    {
        this.numPoints = value;
    }
    
    ArrayList<GeometryMetadata> getMetadatas()
    {
        return metadatas;
    }
    
    /**
     *  Applies id mapping of deduplicated points (called by DeduplicatePointIds).
     *
     */
    void applyPointIdDeduplication(int[] idMap, IntList uniquePointIds)
    {
        int numUniquePoints = 0;
        for (int i = 0; i < uniquePointIds.getCount(); i++)
        {
            int newPointId = idMap[uniquePointIds.get(i)];
            if (newPointId >= numUniquePoints)
            {
                // New unique vertex reached. Copy attribute indices to the proper
                // position.
                for (int a = 0; a < attributes.size(); ++a)
                {
                    PointAttribute attr = attributes.get(a);
                    attr.setPointMapEntry(newPointId, attr.mappedIndex(uniquePointIds.get(i)));
                }
                
                numUniquePoints = newPointId + 1;
            }
            
        }
        
        for (int a = 0; a < attributes.size(); ++a)
        {
            attributes.get(a).setExplicitMapping(numUniquePoints);
        }
        
    }
    
    PointAttribute getAttributeByUniqueId(int uniqueId)
    {
        for (int i = 0; i < attributes.size(); ++i)
        {
            PointAttribute attr = attributes.get(i);
            if (attr.getUniqueId() == uniqueId)
                return attr;
        }
        
        return null;
    }
    
    public DracoPointCloud()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            attributes = new ArrayList<PointAttribute>();
            metadatas = new ArrayList<GeometryMetadata>();
            namedAttributeIndex = new IntList[AttributeType.NAMED_ATTRIBUTES_COUNT];
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
