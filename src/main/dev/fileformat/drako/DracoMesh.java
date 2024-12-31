package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
import java.util.ArrayList;
public class DracoMesh extends DracoPointCloud
{    
    static class AttributeData
    {
        public AttributeData()
        {
            this.elementType = MeshAttributeElementType.CORNER;
        }
        
        int elementType;
    }
    
    /**
     *  Mesh specific per-attribute data.
     *
     */
    private ArrayList<AttributeData> attributeData;
    private IntList faces;
    public IntList getIndices()
    {
        return faces;
    }
    
    public void setCorner(int corner, int value)
    {
        faces.set(corner, value);
        //var tmp = faces[corner / 3];
        //tmp[corner % 3] = value;
    }
    
    public int readCorner(int corner)
    {
        return faces.get(corner);
        //var tmp = faces[corner / 3];
        //return tmp[corner % 3];
    }
    
    public void readFace(int faceId, int[] face)
    {
        int ptr = faceId * 3;
        face[0] = faces.get(ptr++);
        face[1] = faces.get(ptr++);
        face[2] = faces.get(ptr++);
    }
    
    public void readFace(int faceId, IntSpan face)
    {
        int ptr = faceId * 3;
        face.put(0, faces.get(ptr++));
        face.put(1, faces.get(ptr++));
        face.put(2, faces.get(ptr++));
    }
    
    public void addFace(int[] face)
    {
        faces.add(face[0]);
        faces.add(face[1]);
        faces.add(face[2]);
    }
    
    @Override
    public int addAttribute(PointAttribute pa)
    {
        AttributeData ad = new AttributeData();
        attributeData.add(ad);
        return super.addAttribute(pa);
    }
    
    public void setFace(int faceId, int[] face)
    {
        if (faceId >= this.getNumFaces())
        {
            faces.resize((faceId + 1) * 3);
        }
        
        int p = faceId * 3;
        int[] data = faces.data;
        if (p + 2 < data.length)
        {
            data[p + 0] = face[0];
            data[p + 1] = face[1];
            data[p + 2] = face[2];
        }
        
        
        //if(faceId >= NumFaces)
        //    A3DUtils.Resize(NumFaces * 3, faceId + 1);
        //faces[faceId] = face;
    }
    
    public int getNumFaces()
    {
        return faces.getCount() / 3;
    }
    
    public void setNumFaces(int value)
    {
        faces.resize(value * 3);
        //A3DUtils.Resize(faces, value);
    }
    
    @Override
    void applyPointIdDeduplication(int[] idMap, IntList uniquePointIds)
    {
        super.applyPointIdDeduplication(idMap, uniquePointIds);
        int p = 0;
        for (int f = 0; f < this.getNumFaces(); ++f)
        {
            for (int c = 0; c < 3; ++c)
            {
                faces.set(p, idMap[faces.get(p)]);
                p++;
            }
            
        }
        
    }
    
    private static MeshDecoder createMeshDecoder(int method)
    {
        if (method == DracoEncodingMethod.SEQUENTIAL)
            return new MeshSequentialDecoder();
        if (method == DracoEncodingMethod.EDGE_BREAKER)
            return new MeshEdgeBreakerDecoder();
        return null;
    }
    
    private static PointCloudDecoder createPointCloudDecoder(int method)
    {
        if (method == DracoEncodingMethod.SEQUENTIAL)
            return new PointCloudSequentialDecoder();
        if (method == DracoEncodingMethod.KD_TREE)
            return new PointCloudKdTreeDecoder();
        return null;
    }
    
    static DracoPointCloud decode(DecoderBuffer buffer, boolean decodeData)
        throws DrakoException
    {
        DracoHeader header = DracoHeader.parse(buffer);
        if (header == null)
            return null;
        if (header.encoderType == EncodedGeometryType.TRIANGULAR_MESH)
            return DracoMesh.decodeMesh(buffer, header, decodeData);else if (header.encoderType == EncodedGeometryType.POINT_CLOUD)
            return DracoMesh.decodePointCloud(buffer, header, decodeData);
        return null;
    }
    
    static DracoPointCloud decode(DecoderBuffer buffer)
        throws DrakoException
    {
        return DracoMesh.decode(buffer, true);
    }
    
    private static DracoPointCloud decodePointCloud(DecoderBuffer buffer, DracoHeader header, boolean decodeData)
    {
        buffer.setBitstreamVersion(header.version);
        PointCloudDecoder decoder = DracoMesh.createPointCloudDecoder(header.method);
        if (decoder == null)
            return null;
        try
        {
            DracoPointCloud ret = new DracoPointCloud();
            decoder.decode(header, buffer, ret, decodeData);
            return ret;
        }
        catch(Exception $e)
        {
            return null;
        }
        
    }
    
    private static DracoMesh decodeMesh(DecoderBuffer buffer, DracoHeader header, boolean decodeData)
    {
        buffer.setBitstreamVersion(header.version);
        MeshDecoder decoder = DracoMesh.createMeshDecoder(header.method);
        if (decoder == null)
            return null;
        try
        {
            DracoMesh ret = new DracoMesh();
            decoder.decode(header, buffer, ret, decodeData);
            return ret;
        }
        catch(Exception $e)
        {
            return null;
        }
        
    }
    
    public int getAttributeElementType(int attId)
    {
        return attributeData.get(attId).elementType;
    }
    
    public DracoMesh()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            attributeData = new ArrayList<AttributeData>();
            faces = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
