package org.fileformat.drako;
interface ICornerTableTraverser<TCornerTable extends ICornerTable>
{    
    TCornerTable getCornerTable();
    
    // CornerTableTraversalProcessor<TCornerTable> TraversalProcessor{ get; }
    // 
    boolean traverseFromCorner(int cornerId);
    
    void onTraversalStart();
    
    void onTraversalEnd();
    
}
