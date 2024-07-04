package org.fileformat.drako;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;
public class UnitTest1
{    
    @Test
    public void decodeFromDrcFile()
        throws IOException
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
    public void encodeMeshToDrc()
    {
        Vector3[] controlPoints = new Vector3[] {new Vector3(-5F, 0F, 5.0f), new Vector3(5F, 0F, 5.0f), new Vector3(5F, 10F, 5.0f), new Vector3(-5F, 10F, 5.0f), new Vector3(-5F, 0F, -5.0f), new Vector3(5F, 0F, -5.0f), new Vector3(5F, 10F, -5.0f), new Vector3(-5F, 10F, -5.0f)};
        int[] indices = new int[] {0, 1, 2, 0, 2, 3, 1, 5, 6, 1, 6, 2, 5, 4, 7, 5, 7, 6, 4, 0, 3, 4, 3, 7, 0, 4, 5, 0, 5, 1, 3, 2, 6, 3, 6, 7};
        DracoMesh mesh = new DracoMesh();
        PointAttribute attrPos = PointAttribute.wrap(AttributeType.POSITION, controlPoints);
        mesh.addAttribute(attrPos);
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
