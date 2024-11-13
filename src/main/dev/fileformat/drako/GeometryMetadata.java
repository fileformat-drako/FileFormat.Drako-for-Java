package dev.fileformat.drako;
import java.util.HashMap;
/**
 *  Metadata for geometries.
 *
 */
public class GeometryMetadata extends Metadata
{    
    /**
     *  Meta data for attributes.
     *
     */
    public HashMap<Integer, Metadata> attributeMetadata;
    public GeometryMetadata()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            attributeMetadata = new HashMap<Integer, Metadata>();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
