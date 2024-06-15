package org.openize.drako;
abstract class ICornerTable
{    
    public abstract int getNumFaces();
    
    public abstract int getNumVertices();
    
    public static int localIndex(int corner)
    {
        return corner % 3;
    }
    
    public int next(int corner)
    {
        if (corner < 0)
            return corner;
        return ICornerTable.localIndex(++corner) != 0 ? corner : corner - 3;
    }
    
    public int previous(int corner)
    {
        if (corner < 0)
            return corner;
        return ICornerTable.localIndex(corner) != 0 ? corner - 1 : corner + 2;
    }
    
    public abstract int vertex(int corner);
    
    public abstract boolean isOnBoundary(int vert);
    
    public abstract int opposite(int corner);
    
    public abstract int getRightCorner(int cornerId);
    
    public abstract int getLeftCorner(int cornerId);
    
    public abstract int leftMostCorner(int v);
    
    public abstract int swingRight(int corner);
    
    public abstract int swingLeft(int corner);
    
    
}
