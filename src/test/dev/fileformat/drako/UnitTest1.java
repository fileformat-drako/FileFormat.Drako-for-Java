package dev.fileformat.drako;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;
public class UnitTest1
{    
    @Test
    public void decodeFromDrcFile()
        throws IOException, DrakoException
    {
        byte[] cube = Files.readAllBytes(Paths.get("TestData/cube.drc"));
        DracoPointCloud dm = Draco.decode(cube);
        DracoEncodeOptions opt = new DracoEncodeOptions();
        byte[] bytes = Draco.encode(dm, opt);
        DracoPointCloud dm2 = Draco.decode(bytes);
        Assert.assertNotNull(dm2);
        Assert.assertEquals(3, dm2.getNumAttributes());
        PointAttribute attr = dm2.getNamedAttribute(AttributeType.POSITION);
        Assert.assertNotNull(attr);
    }
    
    @Test
    public void encodeMeshToDrcWithImplicitUVNormalMapping()
        throws DrakoException
    {
        Vector3[] controlPoints = new Vector3[] {new Vector3(-5F, 0F, 5.0f), new Vector3(5F, 0F, 5.0f), new Vector3(5F, 10F, 5.0f), new Vector3(-5F, 10F, 5.0f), new Vector3(-5F, 0F, -5.0f), new Vector3(5F, 0F, -5.0f), new Vector3(5F, 10F, -5.0f), new Vector3(-5F, 10F, -5.0f)};
        Vector3[] normals = new Vector3[] {new Vector3(-1F, 0F, 0.0f), new Vector3(1F, 0F, 0.0f), new Vector3(0F, 1F, 0.0f), new Vector3(0F, 1F, 0.0f), new Vector3(0F, 0F, -1.0f), new Vector3(0F, 0F, -1.0f), new Vector3(0F, -1F, 0.0f), new Vector3(0F, -1F, 0.0f)};
        Vector2[] uv = new Vector2[] {new Vector2(-1F, 0F), new Vector2(1F, 0F), new Vector2(0F, 1F), new Vector2(0F, 1F), new Vector2(0F, 0F), new Vector2(0F, 0F), new Vector2(0F, -1F), new Vector2(0F, -1F)};
        int[] indices = new int[] {0, 1, 2, 0, 2, 3, 1, 5, 6, 1, 6, 2, 5, 4, 7, 5, 7, 6, 4, 0, 3, 4, 3, 7, 0, 4, 5, 0, 5, 1, 3, 2, 6, 3, 6, 7};
        DracoMesh mesh = new DracoMesh();
        PointAttribute attrPos = PointAttribute.wrap(AttributeType.POSITION, controlPoints);
        mesh.addAttribute(attrPos);
        PointAttribute attrNormal = PointAttribute.wrap(AttributeType.NORMAL, normals);
        mesh.addAttribute(attrNormal);
        PointAttribute attrUV = PointAttribute.wrap(AttributeType.TEX_COORD, uv);
        mesh.addAttribute(attrUV);
        
        //add triangle indices
        mesh.getIndices().addRange(indices);
        //number of the control points, it's required for the encoder to produce correct result.
        mesh.setNumPoints(8);
        DracoEncodeOptions opt = new DracoEncodeOptions();
        byte[] drcBytes = Draco.encode(mesh, opt);
        DracoMesh mesh2 = (DracoMesh)Draco.decode(drcBytes);
        Assert.assertNotNull(mesh2);
    }
    
    @Test
    public void encodeMeshWithExplicitUVNormalMapping()
        throws DrakoException
    {
        Vector3[] controlPoints = new Vector3[] {new Vector3(-5F, 0F, 5.0f), new Vector3(5F, 0F, 5.0f), new Vector3(5F, 10F, 5.0f), new Vector3(-5F, 10F, 5.0f), new Vector3(-5F, 0F, -5.0f), new Vector3(5F, 0F, -5.0f), new Vector3(5F, 10F, -5.0f), new Vector3(-5F, 10F, -5.0f)};
        Vector3[] normals = new Vector3[] {new Vector3(-1F, 0F, 0f)};
        Vector2[] uv = new Vector2[] {new Vector2(-1F, 0f)};
        int[] indices = new int[] {0, 1, 2, 0, 2, 3, 1, 5, 6, 1, 6, 2, 5, 4, 7, 5, 7, 6, 4, 0, 3, 4, 3, 7, 0, 4, 5, 0, 5, 1, 3, 2, 6, 3, 6, 7};
        DracoMesh mesh = new DracoMesh();
        PointAttribute attrPos = PointAttribute.wrap(AttributeType.POSITION, controlPoints);
        mesh.addAttribute(attrPos);
        PointAttribute attrNormal = PointAttribute.wrap(AttributeType.NORMAL, normals);
        PointAttribute attrUV = PointAttribute.wrap(AttributeType.TEX_COORD, uv);
        
        //These two attributes are not aware of the size of position data
        //need to explicit call this to allocate its internal memory before calling SetPointMapEntry
        attrUV.setExplicitMapping(controlPoints.length);
        attrNormal.setExplicitMapping(controlPoints.length);
        for (int i = 0; i < controlPoints.length; i++)
        {
            attrUV.setPointMapEntry(i, 0);
            attrNormal.setPointMapEntry(i, 0);
        }
        
        mesh.addAttribute(attrNormal);
        mesh.addAttribute(attrUV);
        
        //add triangle indices
        mesh.getIndices().addRange(indices);
        //number of the control points, it's required for the encoder to produce correct result.
        mesh.setNumPoints(8);
        DracoEncodeOptions opt = new DracoEncodeOptions();
        byte[] drcBytes = Draco.encode(mesh, opt);
        DracoMesh mesh2 = (DracoMesh)Draco.decode(drcBytes);
        Assert.assertNotNull(mesh2);
    }
    
    
}
