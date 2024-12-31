package dev.fileformat.drako;
import dev.fileformat.drako.AsposeUtils;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
final class VertexCornersIterator implements Struct<VertexCornersIterator>, Serializable
{    
    // Create the iterator from the provided corner table and the central vertex.
    // 
    public static VertexCornersIterator fromVertex(ICornerTable table, int vert_id)
    {
        VertexCornersIterator ret = new VertexCornersIterator();
        ret.corner_table_ = table;
        ret.start_corner_ = table.leftMostCorner(vert_id);
        ret.corner_ = ret.start_corner_;
        ret.left_traversal_ = true;
        return ret;
    }
    
    public static VertexCornersIterator fromCorner(ICornerTable table, int corner_id)
    {
        VertexCornersIterator ret = new VertexCornersIterator();
        ret.corner_table_ = table;
        ret.start_corner_ = corner_id;
        ret.corner_ = ret.start_corner_;
        ret.left_traversal_ = true;
        return ret;
    }
    
    public int getCorner()
    {
        return corner_;
    }
    
    // Returns true when all ring vertices have been visited.
    // 
    public boolean getEnd()
    {
        return corner_ == CornerTable.K_INVALID_CORNER_INDEX;
    }
    
    // Proceeds to the next corner if possible.
    // 
    public void next()
    {
        if (left_traversal_)
        {
            this.corner_ = corner_table_.swingLeft(corner_);
            if (corner_ == CornerTable.K_INVALID_CORNER_INDEX)
            {
                // Open boundary reached.
                this.corner_ = corner_table_.swingRight(start_corner_);
                this.left_traversal_ = false;
            }
            else if (corner_ == start_corner_)
            {
                // End reached.
                this.corner_ = CornerTable.K_INVALID_CORNER_INDEX;
            }
            
        }
        else
        {
            // Go to the right until we reach a boundary there (no explicit check
            // is needed in this case).
            this.corner_ = corner_table_.swingRight(corner_);
        }
        
    }
    
    private ICornerTable corner_table_;
    // The first processed corner.
    // 
    private int start_corner_;
    // The last processed corner.
    // 
    private int corner_;
    // Traversal direction.
    // 
    private boolean left_traversal_;
    public VertexCornersIterator()
    {
    }
    
    private VertexCornersIterator(VertexCornersIterator other)
    {
        this.corner_table_ = other.corner_table_;
        this.start_corner_ = other.start_corner_;
        this.corner_ = other.corner_;
        this.left_traversal_ = other.left_traversal_;
    }
    
    @Override
    public VertexCornersIterator clone()
    {
        return new VertexCornersIterator(this);
    }
    
    @Override
    public void copyFrom(VertexCornersIterator src)
    {
        if (src == null)
            return;
        this.corner_table_ = src.corner_table_;
        this.start_corner_ = src.start_corner_;
        this.corner_ = src.corner_;
        this.left_traversal_ = src.left_traversal_;
    }
    
    static final long serialVersionUID = -1504785517L;
    @Override
    public int hashCode()
    {
        HashBuilder builder = new HashBuilder();
        builder.hash(this.corner_table_);
        builder.hash(this.start_corner_);
        builder.hash(this.corner_);
        builder.hash(this.left_traversal_);
        return builder.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof VertexCornersIterator))
            return false;
        VertexCornersIterator rhs = (VertexCornersIterator)obj;
        if (!AsposeUtils.equals(this.corner_table_, rhs.corner_table_))
            return false;
        if (this.start_corner_ != rhs.start_corner_)
            return false;
        if (this.corner_ != rhs.corner_)
            return false;
        if (this.left_traversal_ != rhs.left_traversal_)
            return false;
        return true;
    }
    
}
