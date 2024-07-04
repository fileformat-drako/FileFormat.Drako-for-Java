package org.fileformat.drako;
class EdgeBreakerTraverser<TCornerTable extends ICornerTable> extends TraverserBase<TCornerTable>
{    
    // private CornerTableTraversalProcessor<TCornerTable> processor;
    // 
    private MeshAttributeIndicesEncodingObserver<TCornerTable> traversalObserver;
    private IntList cornerTraversalStack;
    public EdgeBreakerTraverser(TCornerTable corner_table, MeshAttributeIndicesEncodingObserver<TCornerTable> observer)
    {
        this.$initFields$();
        
        this.traversalObserver = observer;
        this.init(corner_table, observer);
    }
    
    public EdgeBreakerTraverser(MeshAttributeIndicesEncodingObserver<TCornerTable> traversalObserver)
    {
        this.$initFields$();
        this.corner_table_ = traversalObserver.getCornerTable();
        this.traversalObserver = traversalObserver;
        this.init(this.corner_table_, traversalObserver);
    }
    
    @Override
    public boolean traverseFromCorner(int cornerId)
    {
        cornerTraversalStack.clear();
        cornerTraversalStack.add(cornerId);
        int nextVert = this.corner_table_.vertex(this.corner_table_.next(cornerId));
        int prevVert = this.corner_table_.vertex(this.corner_table_.previous(cornerId));
        if (!this.isVertexVisited(nextVert))
        {
            this.markVertexVisited(nextVert);
            traversalObserver.onNewVertexVisited(nextVert, this.corner_table_.next(cornerId));
        }
        
        if (!this.isVertexVisited(prevVert))
        {
            this.markVertexVisited(prevVert);
            traversalObserver.onNewVertexVisited(prevVert, this.corner_table_.previous(cornerId));
        }
        
        
        // Start the actual traversal.
        while (cornerTraversalStack.getCount() > 0)
        {
            // Currently processed corner.
            cornerId = cornerTraversalStack.get(cornerTraversalStack.getCount() - 1);
            int faceId = cornerId / 3;
            // Make sure the face hasn't been visited yet.
            if (cornerId < 0 || this.isFaceVisited(faceId))
            {
                // This face has been already traversed.
                cornerTraversalStack.removeAt(cornerTraversalStack.getCount() - 1);
                continue;
            }
            
            while (true)
            {
                faceId = cornerId / 3;
                this.markFaceVisited(faceId);
                traversalObserver.onNewFaceVisited(faceId);
                int vertId = this.corner_table_.vertex(cornerId);
                boolean onBoundary = this.corner_table_.isOnBoundary(vertId);
                if (!this.isVertexVisited(vertId))
                {
                    this.markVertexVisited(vertId);
                    traversalObserver.onNewVertexVisited(vertId, cornerId);
                    if (!onBoundary)
                    {
                        cornerId = this.corner_table_.getRightCorner(cornerId);
                        continue;
                    }
                    
                }
                
                int rightCornerId = this.corner_table_.getRightCorner(cornerId);
                int leftCornerId = this.corner_table_.getLeftCorner(cornerId);
                int rightFaceId = rightCornerId < 0 ? -1 : rightCornerId / 3;
                int leftFaceId = leftCornerId < 0 ? -1 : leftCornerId / 3;
                if (this.isFaceVisited(rightFaceId))
                {
                    // Right face has been already visited.
                    if (this.isFaceVisited(leftFaceId))
                    {
                        // Both neighboring faces are visited. End reached.
                        cornerTraversalStack.removeAt(cornerTraversalStack.getCount() - 1);
                        break;
                        // Break from the while (true) loop.
                    }
                    else
                    {
                        // Go to the left face.
                        cornerId = leftCornerId;
                    }
                    
                }
                else if (this.isFaceVisited(leftFaceId))
                {
                    // Left face visited, go to the right one.
                    cornerId = rightCornerId;
                }
                else
                {
                    // Both neighboring faces are unvisited, we need to visit both of
                    // them.
                    
                    // Split the traversal.
                    // First make the top of the current corner stack point to the left
                    // face (this one will be processed second).
                    cornerTraversalStack.set(cornerTraversalStack.getCount() - 1, leftCornerId);
                    // Add a new corner to the top of the stack (right face needs to
                    // be traversed first).
                    cornerTraversalStack.add(rightCornerId);
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
            cornerTraversalStack = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
