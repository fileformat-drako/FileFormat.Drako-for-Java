package dev.fileformat.drako;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
/**
 *  Hole event is used to store info about the first symbol that reached a
 *  vertex of so far unvisited hole. This can happen only on either the initial
 *  face or during a regular traversal when TOPOLOGYS is encountered.
 *
 */
final class HoleEventData implements Struct<HoleEventData>, Serializable
{    
    int symbolId;
    public HoleEventData(int symId)
    {
        this.symbolId = symId;
    }
    
    public HoleEventData()
    {
    }
    
    private HoleEventData(HoleEventData other)
    {
        this.symbolId = other.symbolId;
    }
    
    @Override
    public HoleEventData clone()
    {
        return new HoleEventData(this);
    }
    
    @Override
    public void copyFrom(HoleEventData src)
    {
        if (src == null)
            return;
        this.symbolId = src.symbolId;
    }
    
    static final long serialVersionUID = 1860329541L;
    @Override
    public int hashCode()
    {
        HashBuilder builder = new HashBuilder();
        builder.hash(this.symbolId);
        return builder.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof HoleEventData))
            return false;
        HoleEventData rhs = (HoleEventData)obj;
        if (this.symbolId != rhs.symbolId)
            return false;
        return true;
    }
    
}
