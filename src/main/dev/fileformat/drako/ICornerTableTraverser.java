package dev.fileformat.drako;
interface ICornerTableTraverser<TCornerTable extends ICornerTable>
{    
    TCornerTable getCornerTable();
    
    // CornerTableTraversalProcessor<TCornerTable> TraversalProcessor{ get; }
    // 
    void traverseFromCorner(int cornerId)
        throws DrakoException;
    
    void onTraversalStart();
    
    void onTraversalEnd();
    
}
