package dev.fileformat.drako;
class DepthFirstTraverser extends TraverserBase<CornerTable>
{    
    private IntList corner_traversal_stack_;
    public DepthFirstTraverser(CornerTable cornerTable, MeshAttributeIndicesEncodingObserver<CornerTable> traversal_observer)
    {
        this.$initFields$();
        this.init(cornerTable, traversal_observer);
    }
    
    @Override
    public boolean traverseFromCorner(int corner_id)
    {
        
        if (this.isCornerVisited(corner_id))
            return true;
        // Already traversed.
        
        corner_traversal_stack_.clear();
        corner_traversal_stack_.add(corner_id);
        int next_vert = this.corner_table_.vertex(this.corner_table_.next(corner_id));
        int prev_vert = this.corner_table_.vertex(this.corner_table_.previous(corner_id));
        if (next_vert == -1 || (prev_vert == -1))
            return false;
        if (!this.isVertexVisited(next_vert))
        {
            this.markVertexVisited(next_vert);
            this.traversal_observer_.onNewVertexVisited(next_vert, this.corner_table_.next(corner_id));
        }
        
        if (!this.isVertexVisited(prev_vert))
        {
            this.markVertexVisited(prev_vert);
            this.traversal_observer_.onNewVertexVisited(prev_vert, this.corner_table_.previous(corner_id));
        }
        
        
        // Start the actual traversal.
        while (corner_traversal_stack_.getCount() > 0)
        {
            // Currently processed corner.
            corner_id = corner_traversal_stack_.get(corner_traversal_stack_.getCount() - 1);
            int face_id = corner_id / 3;
            // Make sure the face hasn't been visited yet.
            if (corner_id == -1 || this.isFaceVisited(face_id))
            {
                // This face has been already traversed.
                corner_traversal_stack_.removeAt(corner_traversal_stack_.getCount() - 1);
                continue;
            }
            
            while (true)
            {
                this.markFaceVisited(face_id);
                this.traversal_observer_.onNewFaceVisited(face_id);
                int vert_id = this.corner_table_.vertex(corner_id);
                if (vert_id == -1)
                    return false;
                if (!this.isVertexVisited(vert_id))
                {
                    boolean on_boundary = this.corner_table_.isOnBoundary(vert_id);
                    this.markVertexVisited(vert_id);
                    this.traversal_observer_.onNewVertexVisited(vert_id, corner_id);
                    if (!on_boundary)
                    {
                        corner_id = this.corner_table_.getRightCorner(corner_id);
                        face_id = corner_id / 3;
                        continue;
                    }
                    
                }
                
                int right_corner_id = this.corner_table_.getRightCorner(corner_id);
                int left_corner_id = this.corner_table_.getLeftCorner(corner_id);
                int right_face_id = right_corner_id == -1 ? -1 : right_corner_id / 3;
                int left_face_id = left_corner_id == -1 ? -1 : left_corner_id / 3;
                if (this.isFaceVisited(right_face_id))
                {
                    // Right face has been already visited.
                    if (this.isFaceVisited(left_face_id))
                    {
                        // Both neighboring faces are visited. End reached.
                        corner_traversal_stack_.removeAt(corner_traversal_stack_.getCount() - 1);
                        break;
                        // Break from the while (true) loop.
                    }
                    else
                    {
                        // Go to the left face.
                        corner_id = left_corner_id;
                        face_id = left_face_id;
                    }
                    
                }
                else if (this.isFaceVisited(left_face_id))
                {
                    // Left face visited, go to the right one.
                    corner_id = right_corner_id;
                    face_id = right_face_id;
                }
                else
                {
                    // Both neighboring faces are unvisited, we need to visit both of
                    // them.
                    
                    // Split the traversal.
                    // First make the top of the current corner stack point to the left
                    // face (this one will be processed second).
                    corner_traversal_stack_.set(corner_traversal_stack_.getCount() - 1, left_corner_id);
                    // Add a new corner to the top of the stack (right face needs to
                    // be traversed first).
                    corner_traversal_stack_.add(right_corner_id);
                    // Break from the while (true) loop.
                    break;
                }
                
            }
            
        }
        
        return true;
    }
    
    @Override
    public void onTraversalStart()
    {
    }
    
    @Override
    public void onTraversalEnd()
    {
    }
    
    private void $initFields$()
    {
        try
        {
            corner_traversal_stack_ = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
