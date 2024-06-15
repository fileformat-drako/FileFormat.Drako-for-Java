package org.openize.drako;
/**
 *  Class providing the basic traversal funcionality needed by traversers (such
 *  as the EdgeBreakerTraverser, see edgebreakerTraverser.h). It is used to
 *  return the corner table that is used for the traversal, plus it provides a
 *  basic book-keeping of visited faces and vertices during the traversal.
 *
 */
class CornerTableTraversalProcessor<TCornerTable extends ICornerTable>
{    
    private TCornerTable cornerTable;
    private boolean[] isFaceVisited;
    private boolean[] isVertexVisited;
    public CornerTableTraversalProcessor(TCornerTable cornerTable)
    {
        //Contract.Assert(cornerTable != null);
        this.cornerTable = cornerTable;
        this.isFaceVisited = new boolean[cornerTable.getNumFaces()];
        this.resetVertexData();
    }
    
    public boolean isFaceVisited(int faceId)
    {
        if (faceId < 0)
            return true;
        // Invalid faces are always considered as visited.
        return isFaceVisited[faceId];
    }
    
    public void markFaceVisited(int faceId)
    {
        isFaceVisited[faceId] = true;
    }
    
    public boolean isVertexVisited(int vertId)
    {
        return isVertexVisited[vertId];
    }
    
    public void markVertexVisited(int vertId)
    {
        isVertexVisited[vertId] = true;
    }
    
    protected void resetVertexData()
    {
        this.initVertexData(cornerTable.getNumVertices());
    }
    
    protected void initVertexData(int numVerts)
    {
        this.isVertexVisited = new boolean[numVerts];
    }
    
    public TCornerTable getCornerTable()
    {
        return cornerTable;
    }
    
}
