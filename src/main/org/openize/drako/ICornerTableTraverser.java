package org.openize.drako;
interface ICornerTableTraverser<TCornerTable extends ICornerTable>
{    
    TCornerTable getCornerTable();
    
    // CornerTableTraversalProcessor<TCornerTable> TraversalProcessor{ get; }
    // 
    boolean traverseFromCorner(int cornerId);
    
    void onTraversalStart();
    
    void onTraversalEnd();
    
}
