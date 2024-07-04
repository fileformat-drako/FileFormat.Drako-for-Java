package org.fileformat.drako;
class MeshAttributeCornerTable extends ICornerTable
{    
    private static final int K_INVALID_VERTEX_INDEX = -1;
    private static final int K_INVALID_CORNER_INDEX = -1;
    private boolean[] isEdgeOnSeam;
    private boolean[] isVertexOnSeam;
    /**
     *  If this is set to true, it means that there are no attribute seams between
     *  two faces. This can be used to speed up some algorithms.
     *
     */
    private boolean noInteriorSeams = true;
    private int[] cornerToVertexMap;
    /**
     *  Map between vertices and their associated left most corners. A left most
     *  corner is a corner that is adjecent to a boundary or an attribute seam from
     *  right (i.e., SwingLeft from that corner will return an invalid corner). If
     *  no such corner exists for a given vertex, then any corner attached to the
     *  vertex can be used.
     *
     */
    private IntList vertexToLeftMostCornerMap;
    /**
     *  Map between vertex ids and attribute entry ids (i.e. the values stored in
     *  the attribute buffer). The attribute entry id can be retrieved using the
     *  VertexParent() method.
     *
     */
    private IntList vertexToAttributeEntryIdMap;
    private CornerTable cornerTable;
    public MeshAttributeCornerTable(CornerTable table)
    {
        this.$initFields$();
        
        this.isEdgeOnSeam = new boolean[table.getNumCorners()];
        this.isVertexOnSeam = new boolean[table.getNumVertices()];
        this.cornerToVertexMap = new int[table.getNumCorners()];
        for (int i = 0; i < cornerToVertexMap.length; i++)
        {
            cornerToVertexMap[i] = K_INVALID_CORNER_INDEX;
        }
        
        vertexToAttributeEntryIdMap.setCapacity(table.getNumVertices());
        vertexToLeftMostCornerMap.setCapacity(table.getNumVertices());
        this.cornerTable = table;
        this.noInteriorSeams = true;
    }
    
    public MeshAttributeCornerTable(DracoMesh mesh, CornerTable table, PointAttribute att)
    {
        this(table);
        
        // Find all necessary data for encoding attributes. For now we check which of
        // the mesh vertices is part of an attribute seam, because seams require
        // special handling.
        for (int c = 0; c < cornerTable.getNumCorners(); ++c)
        {
            int f = cornerTable.face(c);
            if (cornerTable.isDegenerated(f))
                continue;
            // Ignore corners on degenerated faces.
            int oppCorner = cornerTable.opposite(c);
            if (oppCorner < 0)
            {
                // Boundary. Mark it as seam edge.
                isEdgeOnSeam[c] = true;
                int v = cornerTable.vertex(cornerTable.next(c));
                isVertexOnSeam[v] = true;
                v = cornerTable.vertex(cornerTable.previous(c));
                isVertexOnSeam[v] = true;
                continue;
            }
            
            if (oppCorner < c)
                continue;
            // Opposite corner was already processed.
            int actC = c;
            int actSiblingC = oppCorner;
            for (int i = 0; i < 2; ++i)
            {
                // Get the sibling corners. I.e., the two corners attached to the same
                // vertex but divided by the seam edge.
                actC = cornerTable.next(actC);
                actSiblingC = cornerTable.previous(actSiblingC);
                int pointId = DracoUtils.cornerToPointId(actC, mesh);
                int siblingPointId = DracoUtils.cornerToPointId(actSiblingC, mesh);
                if (att.mappedIndex(pointId) != att.mappedIndex(siblingPointId))
                {
                    this.noInteriorSeams = false;
                    isEdgeOnSeam[c] = true;
                    isEdgeOnSeam[oppCorner] = true;
                    // Mark seam vertices.
                    isVertexOnSeam[cornerTable.vertex(cornerTable.next(c))] = true;
                    isVertexOnSeam[cornerTable.vertex(cornerTable.previous(c))] = true;
                    isVertexOnSeam[cornerTable.vertex(cornerTable.next(oppCorner))] = true;
                    isVertexOnSeam[cornerTable.vertex(cornerTable.previous(oppCorner))] = true;
                    break;
                }
                
            }
            
        }
        
        this.recomputeVertices(mesh, att);
    }
    
    public void addSeamEdge(int c)
    {
        isEdgeOnSeam[c] = true;
        // Mark seam vertices.
        isVertexOnSeam[cornerTable.vertex(cornerTable.next(c))] = true;
        isVertexOnSeam[cornerTable.vertex(cornerTable.previous(c))] = true;
        int oppCorner = cornerTable.opposite(c);
        if (oppCorner >= 0)
        {
            this.noInteriorSeams = false;
            isEdgeOnSeam[oppCorner] = true;
            isVertexOnSeam[cornerTable.vertex(cornerTable.next(oppCorner))] = true;
            isVertexOnSeam[cornerTable.vertex(cornerTable.previous(oppCorner))] = true;
        }
        
    }
    
    /**
     *  Recomputes vertices using the newly added seam edges (needs to be called
     *  whenever the seam edges are updated).
     *  |mesh| and |att| can be null, in which case mapping between vertices and
     *  attribute value ids is set to identity.
     *
     */
    public void recomputeVertices(DracoMesh mesh, PointAttribute att)
    {
        
        if (mesh != null && (att != null))
        {
            this.recomputeVerticesInternal(true, mesh, att);
        }
        else
        {
            this.recomputeVerticesInternal(false, null, null);
        }
        
    }
    
    public void recomputeVerticesInternal(boolean initVertexToAttributeEntryMap, DracoMesh mesh, PointAttribute att)
    {
        int numNewVertices = 0;
        for (int v = 0; v < cornerTable.getNumVertices(); ++v)
        {
            int c = cornerTable.leftMostCorner(v);
            if (c < 0)
                continue;
            // Isolated vertex?
            int firstVertId = numNewVertices++;
            if (initVertexToAttributeEntryMap)
            {
                int pointId = DracoUtils.cornerToPointId(c, mesh);
                vertexToAttributeEntryIdMap.add(att.mappedIndex(pointId));
            }
            else
            {
                // Identity mapping
                vertexToAttributeEntryIdMap.add(firstVertId);
            }
            
            int firstC = c;
            int actC;
            // Check if the vertex is on a seam edge, if it is we need to find the first
            // attribute entry on the seam edge when traversing in the ccw direction.
            if (isVertexOnSeam[v])
            {
                // Try to swing left on the modified corner table. We need to get the
                // first corner that defines an attribute seam.
                actC = this.swingLeft(firstC);
                while (actC >= 0)
                {
                    firstC = actC;
                    actC = this.swingLeft(actC);
                }
                
            }
            
            cornerToVertexMap[firstC] = firstVertId;
            vertexToLeftMostCornerMap.add(firstC);
            actC = cornerTable.swingRight(firstC);
            while (actC >= 0 && (actC != firstC))
            {
                if (this.isCornerOppositeToSeamEdge(cornerTable.next(actC)))
                {
                    firstVertId = numNewVertices++;
                    if (initVertexToAttributeEntryMap)
                    {
                        int pointId = DracoUtils.cornerToPointId(actC, mesh);
                        vertexToAttributeEntryIdMap.add(att.mappedIndex(pointId));
                    }
                    else
                    {
                        // Identity mapping.
                        vertexToAttributeEntryIdMap.add(firstVertId);
                    }
                    
                    vertexToLeftMostCornerMap.add(actC);
                }
                
                cornerToVertexMap[actC] = firstVertId;
                actC = cornerTable.swingRight(actC);
            }
            
        }
        
    }
    
    public boolean isCornerOppositeToSeamEdge(int corner)
    {
        return isEdgeOnSeam[corner];
    }
    
    @Override
    public int opposite(int corner)
    {
        if (this.isCornerOppositeToSeamEdge(corner))
            return K_INVALID_CORNER_INDEX;
        return cornerTable.opposite(corner);
    }
    
    /**
     *  Returns true when a corner is attached to any attribute seam.
     *
     */
    public boolean isCornerOnSeam(int corner)
    {
        return isVertexOnSeam[cornerTable.vertex(corner)];
    }
    
    /**
     *  Similar to CornerTable::GetLeftCorner and CornerTable::GetRightCorner, but
     *  does not go over seam edges.
     *
     */
    @Override
    public int getLeftCorner(int corner)
    {
        return this.opposite(this.previous(corner));
    }
    
    @Override
    public int getRightCorner(int corner)
    {
        return this.opposite(this.next(corner));
    }
    
    /**
     *  Similar to CornerTable::SwingRight, but it does not go over seam edges.
     *
     * @param corner 
     */
    @Override
    public int swingRight(int corner)
    {
        return this.previous(this.opposite(this.previous(corner)));
    }
    
    /**
     *  Similar to CornerTable.SwingLeft, but it does not go over seam edges.
     *
     * @param corner 
     */
    @Override
    public int swingLeft(int corner)
    {
        return this.next(this.opposite(this.next(corner)));
    }
    
    public int getNumVertices()
    {
        return vertexToAttributeEntryIdMap.getCount();
    }
    
    public int getNumFaces()
    {
        return cornerTable.getNumFaces();
    }
    
    @Override
    public int vertex(int corner)
    {
        return cornerToVertexMap[corner];
    }
    
    // Returns the attribute entry id associated to the given vertex.
    // 
    public int vertexParent(int vert)
    {
        return vertexToAttributeEntryIdMap.get(vert);
    }
    
    @Override
    public int leftMostCorner(int v)
    {
        return vertexToLeftMostCornerMap.get(v);
    }
    
    @Override
    public boolean isOnBoundary(int vert)
    {
        int corner = this.leftMostCorner(vert);
        if (corner < 0)
            return true;
        return this.isCornerOnSeam(corner);
    }
    
    public boolean getNoInteriorSeams()
    {
        return noInteriorSeams;
    }
    
    CornerTable getCornerTable()
    {
        return cornerTable;
    }
    
    private void $initFields$()
    {
        try
        {
            vertexToLeftMostCornerMap = new IntList();
            vertexToAttributeEntryIdMap = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
