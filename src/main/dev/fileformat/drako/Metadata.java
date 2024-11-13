package dev.fileformat.drako;
import java.util.HashMap;
/**
 *  Draco metadata
 *
 */
public class Metadata
{    
    /**
     *  Entries of the metadata
     *
     */
    public HashMap<String, byte[]> entries;
    /**
     *  Named sub metadata
     *
     */
    public HashMap<String, Metadata> subMetadata;
    public Metadata()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            entries = new HashMap<String, byte[]>();
            subMetadata = new HashMap<String, Metadata>();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
