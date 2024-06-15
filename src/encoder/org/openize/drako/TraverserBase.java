package org.openize.drako;
abstract class TraverserBase<TCornerTable extends ICornerTable> implements ICornerTableTraverser<TCornerTable>
{    
    protected TCornerTable corner_table_;
    protected boolean[] is_face_visited_;
    protected boolean[] is_vertex_visited_;
    protected MeshAttributeIndicesEncodingObserver<TCornerTable> traversal_observer_;
    public void init(TCornerTable corner_table, MeshAttributeIndicesEncodingObserver<TCornerTable> traversal_observer)
    {
        this.corner_table_ = corner_table;
        this.is_face_visited_ = new boolean[corner_table_.getNumFaces()];
        this.is_vertex_visited_ = new boolean[corner_table_.getNumVertices()];
        this.traversal_observer_ = traversal_observer;
    }
    
    @Override
    public TCornerTable getCornerTable()
    {
        return corner_table_;
    }
    
    protected boolean isFaceVisited(int face_id)
    {
        if (face_id == -1)
            return true;
        // Invalid faces are always considered as visited.
        return is_face_visited_[face_id];
    }
    
    // Returns true if the face containing the given corner was visited.
    // 
    protected boolean isCornerVisited(int corner_id)
    {
        if (corner_id == -1)
            return true;
        // Invalid faces are always considered as visited.
        return is_face_visited_[corner_id / 3];
    }
    
    protected void markFaceVisited(int face_id)
    {
        is_face_visited_[face_id] = true;
    }
    
    protected boolean isVertexVisited(int vert_id)
    {
        return is_vertex_visited_[vert_id];
    }
    
    protected void markVertexVisited(int vert_id)
    {
        is_vertex_visited_[vert_id] = true;
    }
    
    public abstract boolean traverseFromCorner(int cornerId);
    
    @Override
    public abstract void onTraversalStart();
    
    @Override
    public abstract void onTraversalEnd();
    
    
}
