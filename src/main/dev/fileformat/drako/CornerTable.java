package dev.fileformat.drako;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Int2D;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
import java.util.Arrays;
/**
 *  CornerTable is used to represent connectivity of triangular meshes.
 *  For every corner of all faces, the corner table stores the index of the
 *  opposite corner in the neighboring face (if it exists) as illustrated in the
 *  figure below (see corner |c| and it's opposite corner |o|).
 * 
 *      *
 *     /c\
 *    /   \
 *   /n   p\
 *  *-------*
 *   \     /
 *    \   /
 *     \o/
 *      *
 * 
 *  All corners are defined by unique CornerIndex and each triplet of corners
 *  that define a single face id always ordered consecutively as:
 *      { 3 * FaceIndex, 3 * FaceIndex + 1, 3 * FaceIndex +2 }.
 *  This representation of corners allows CornerTable to easily retrieve Next and
 *  Previous corners on any face (see corners |n| and |p| in the figure above).
 *  Using the Next, Previous, and Opposite corners then enables traversal of any
 *  2-manifold surface.
 *  If the CornerTable is constructed from a non-manifold surface, the input
 *  non-manifold edges and vertices are automatically split.
 *
 */
class CornerTable extends ICornerTable
{    
    static final class VertexEdgePair implements Struct<VertexEdgePair>, Serializable
    {
        public int sinkVert;
        public int edgeCorner;
        public VertexEdgePair(int sinkVert, int edgeCorner)
        {
            this.sinkVert = sinkVert;
            this.edgeCorner = edgeCorner;
        }
        
        public VertexEdgePair()
        {
        }
        
        private VertexEdgePair(VertexEdgePair other)
        {
            this.sinkVert = other.sinkVert;
            this.edgeCorner = other.edgeCorner;
        }
        
        @Override
        public VertexEdgePair clone()
        {
            return new VertexEdgePair(this);
        }
        
        @Override
        public void copyFrom(VertexEdgePair src)
        {
            if (src == null)
                return;
            this.sinkVert = src.sinkVert;
            this.edgeCorner = src.edgeCorner;
        }
        
        static final long serialVersionUID = -478043180L;
        @Override
        public int hashCode()
        {
            HashBuilder builder = new HashBuilder();
            builder.hash(this.sinkVert);
            builder.hash(this.edgeCorner);
            return builder.hashCode();
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof VertexEdgePair))
                return false;
            VertexEdgePair rhs = (VertexEdgePair)obj;
            if (this.sinkVert != rhs.sinkVert)
                return false;
            if (this.edgeCorner != rhs.edgeCorner)
                return false;
            return true;
        }
        
    }
    
    public static final int K_INVALID_FACE_INDEX = -1;
    public static final int K_INVALID_CORNER_INDEX = -1;
    public static final int K_INVALID_VERTEX_INDEX = -1;
    private int[] oppositeCorners;
    private IntList vertexCorners;
    private int[] cornerToVertexMap;
    private int numOriginalVertices;
    private int numDegeneratedFaces;
    private int numIsolatedVertices;
    private IntList nonManifoldVertexParents;
    private ValenceCache valenceCache;
    public CornerTable()
    {
        this.$initFields$();
        this.valenceCache = new ValenceCache(this);
    }
    
    public void initialize(Int2D faces)
    {
        final int[] ref0 = new int[1];
        
        valenceCache.clearValenceCache();
        valenceCache.clearValenceCacheInaccurate();
        int numFaces = faces.getLength(0);
        this.cornerToVertexMap = new int[numFaces * 3];
        for (int fi = 0; fi < numFaces; ++fi)
        {
            for (int i = 0; i < 3; ++i)
            {
                int corner = this.firstCorner(fi);
                cornerToVertexMap[corner + i] = faces.get(fi, i);
            }
            
        }
        
        int numVertices = -1;
        this.computeOppositeCorners(ref0);
        numVertices = ref0[0];
        this.computeVertexCorners(numVertices);
    }
    
    public int[] allCorners(int face)
    {
        int ci = face * 3;
        return new int[] {ci, ci + 1, ci + 2};
    }
    
    @Override
    public int opposite(int corner)
    {
        if (corner < 0)
            return corner;
        return oppositeCorners[corner];
    }
    
    @Override
    public int vertex(int corner)
    {
        if (corner < 0)
            return K_INVALID_VERTEX_INDEX;
        return cornerToVertexMap[corner];
    }
    
    public int face(int corner)
    {
        if (corner < 0)
            return K_INVALID_FACE_INDEX;
        return corner / 3;
    }
    
    public int firstCorner(int face)
    {
        if (face < 0)
            return K_INVALID_CORNER_INDEX;
        return face * 3;
    }
    
    /**
     *      *-------*
     *     / \     / \
     *    /   \   /   \
     *   /   sl\c/sr   \
     *  *-------v-------*
     *  Returns the corner on the adjacent face on the right that maps to
     *  the same vertex as the given corner (sr in the above diagram).
     *
     */
    @Override
    public int swingRight(int corner)
    {
        return this.previous(this.opposite(this.previous(corner)));
    }
    
    /**
     *  Returns the number of new vertices that were created as a result of
     *  spliting of non-manifold vertices of the input geometry.
     *
     * @return  Returns the number of new vertices that were created as a result of
 spliting of non-manifold vertices of the input geometry.
     */
    public int getNumNewVertices()
    {
        return this.getNumVertices() - numOriginalVertices;
    }
    
    public int getNumOriginalVertices()
    {
        return numOriginalVertices;
    }
    
    /**
     *  Returns the number of faces with duplicated vertex indices.
     *
     * @return  Returns the number of faces with duplicated vertex indices.
     */
    public int getNumDegeneratedFaces()
    {
        return numDegeneratedFaces;
    }
    
    /**
     *  Returns the number of isolated vertices (vertices that have
     *  vertexCorners mapping set to kInvalidCornerIndex.
     *
     * @return  Returns the number of isolated vertices (vertices that have
 vertexCorners mapping set to kInvalidCornerIndex.
     */
    public int getNumIsolatedVertices()
    {
        return numIsolatedVertices;
    }
    
    public int getNumVertices()
    {
        return vertexCorners.getCount();
    }
    
    public int getNumCorners()
    {
        return cornerToVertexMap.length;
    }
    
    public int getNumFaces()
    {
        return cornerToVertexMap.length / 3;
    }
    
    /**
     *  Returns the corner on the left face that maps to the same vertex as the
     *  given corner (sl in the above diagram).
     *
     */
    @Override
    public int swingLeft(int corner)
    {
        return this.next(this.opposite(this.next(corner)));
    }
    
    private void computeOppositeCorners(int[] numVertices)
    {
        this.oppositeCorners = new int[this.getNumCorners()];
        for (int i = 0; i < oppositeCorners.length; i++)
        {
            oppositeCorners[i] = K_INVALID_CORNER_INDEX;
        }
        
        IntList numCornersOnVertices = new IntList();
        numCornersOnVertices.setCapacity(this.getNumCorners());
        for (int c = 0; c < this.getNumCorners(); ++c)
        {
            int v1 = this.vertex(c);
            if (v1 >= numCornersOnVertices.getCount())
            {
                numCornersOnVertices.resize(v1 + 1, 0);
            }
            
            // For each corner there is always exactly one outgoing half-edge attached
            // to its vertex.
            numCornersOnVertices.set(v1, numCornersOnVertices.get(v1) + 1);
        }
        
        VertexEdgePair[] vertexEdges = (VertexEdgePair[])(MetaClasses.VertexEdgePair.newArray(this.getNumCorners()));
        for (int i = 0; i < vertexEdges.length; i++)
        {
            vertexEdges[i] = new VertexEdgePair(-1, -1);
        }
        
        int[] vertexOffset = new int[numCornersOnVertices.getCount()];
        int offset = 0;
        for (int i = 0; i < numCornersOnVertices.getCount(); ++i)
        {
            vertexOffset[i] = offset;
            offset += numCornersOnVertices.get(i);
        }
        
        
        // Now go over the all half-edges (using their opposite corners) and either
        // insert them to the |vertexEdge| array or connect them with existing
        // half-edges.
        for (int c = 0; c < this.getNumCorners(); ++c)
        {
            int sourceV = this.vertex(this.next(c));
            int sinkV = this.vertex(this.previous(c));
            int faceIndex = this.face(c);
            if (c == this.firstCorner(faceIndex))
            {
                int v0 = this.vertex(c);
                if (v0 == sourceV || (v0 == sinkV) || (sourceV == sinkV))
                {
                    ++numDegeneratedFaces;
                    c += 2;
                    // Ignore the next two corners of the same face.
                    continue;
                }
                
            }
            
            int oppositeC = -1;
            int numCornersOnVert = numCornersOnVertices.get(sinkV);
            // Where to look for the first half-edge on the sink vertex.
            offset = vertexOffset[sinkV];
            for (int i = 0; i < numCornersOnVert; ++i, ++offset)
            {
                int otherV = vertexEdges[offset].sinkVert;
                if (otherV < 0)
                    break;
                // No matching half-edge found on the sink vertex.
                if (otherV == sourceV)
                {
                    // A matching half-edge was found on the sink vertex. Mark the
                    // half-edge's opposite corner.
                    oppositeC = vertexEdges[offset].edgeCorner;
                    // Remove the half-edge from the sink vertex. We remap all subsequent
                    // half-edges one slot down.
                    // TODO(ostava): This can be optimized a little bit, by remaping only
                    // the half-edge on the last valid slot into the deleted half-edge's
                    // slot.
                    for (int j = i + 1; j < numCornersOnVert; ++j, ++offset)
                    {
                        vertexEdges[offset] = Struct.byVal(vertexEdges[offset + 1]);
                        if (vertexEdges[offset].sinkVert < 0)
                            break;
                        // Unused half-edge reached.
                    }
                    
                    // Mark the last entry as unused.
                    vertexEdges[offset].sinkVert = -1;
                    break;
                }
                
            }
            
            if (oppositeC < 0)
            {
                int numCornersOnSourceVert = numCornersOnVertices.get(sourceV);
                offset = vertexOffset[sourceV];
                for (int i = 0; i < numCornersOnSourceVert; ++i, ++offset)
                {
                    // Find the first unused half-edge slot on the source vertex.
                    if (vertexEdges[offset].sinkVert < 0)
                    {
                        vertexEdges[offset].sinkVert = sinkV;
                        vertexEdges[offset].edgeCorner = c;
                        break;
                    }
                    
                }
                
            }
            else
            {
                // Opposite corner found.
                oppositeCorners[c] = oppositeC;
                oppositeCorners[oppositeC] = c;
            }
            
        }
        
        numVertices[0] = numCornersOnVertices.getCount();
    }
    
    void computeVertexCorners(int numVertices)
    {
        this.numOriginalVertices = numVertices;
        vertexCorners.resize(numVertices, K_INVALID_CORNER_INDEX);
        boolean[] visitedVertices = new boolean[numVertices];
        int numVisitedVertices = numVertices;
        boolean[] visitedCorners = new boolean[this.getNumCorners()];
        
        for (int f = 0; f < this.getNumFaces(); ++f)
        {
            int firstFaceCorner = this.firstCorner(f);
            // Check whether the face is degenerated. If so ignore it.
            if (this.isDegenerated(f))
                continue;
            
            for (int k = 0; k < 3; ++k)
            {
                int c = firstFaceCorner + k;
                if (visitedCorners[c])
                    continue;
                int v = cornerToVertexMap[c];
                boolean isNonManifoldVertex = false;
                if (visitedVertices[v])
                {
                    // A visited vertex of an unvisited corner found. Must be a non-manifold
                    // vertex.
                    // Create a new vertex for it.
                    vertexCorners.add(K_INVALID_CORNER_INDEX);
                    nonManifoldVertexParents.add(v);
                    if (numVisitedVertices >= visitedVertices.length)
                    {
                        //resize 
                        visitedVertices = visitedVertices == null ? new boolean[visitedVertices.length * 2] : Arrays.copyOf(visitedVertices, visitedVertices.length * 2);
                    }
                    
                    visitedVertices[numVisitedVertices++] = false;
                    v = numVertices++;
                    isNonManifoldVertex = true;
                }
                
                // Mark the vertex as visited.
                visitedVertices[v] = true;
                int actC = c;
                while (actC != K_INVALID_CORNER_INDEX)
                {
                    visitedCorners[actC] = true;
                    // Vertex will eventually point to the left most corner.
                    vertexCorners.set(v, actC);
                    if (isNonManifoldVertex)
                    {
                        // Update vertex index in the corresponding face.
                        cornerToVertexMap[actC] = v;
                    }
                    
                    actC = this.swingLeft(actC);
                    if (actC == c)
                        break;
                    // Full circle reached.
                }
                
                if (actC == K_INVALID_CORNER_INDEX)
                {
                    // If we have reached an open boundary we need to swing right from the
                    // initial corner to mark all corners in the opposite direction.
                    actC = this.swingRight(c);
                    while (actC != K_INVALID_CORNER_INDEX)
                    {
                        visitedCorners[actC] = true;
                        if (isNonManifoldVertex)
                        {
                            int actF = this.face(actC);
                            cornerToVertexMap[actC] = v;
                        }
                        
                        actC = this.swingRight(actC);
                    }
                    
                }
                
            }
            
        }
        
        
        // Count the number of isolated (unprocessed) vertices.
        this.numIsolatedVertices = 0;
        for (int i = 0; i < numVisitedVertices; i++)
        {
            if (!visitedVertices[i])
            {
                ++numIsolatedVertices;
            }
            
        }
        
    }
    
    public boolean isDegenerated(int face)
    {
        if (face == K_INVALID_FACE_INDEX)
            return true;
        int firstFaceCorner = this.firstCorner(face);
        int v0 = this.vertex(firstFaceCorner);
        int v1 = this.vertex(this.next(firstFaceCorner));
        int v2 = this.vertex(this.previous(firstFaceCorner));
        if (v0 == v1 || (v0 == v2) || (v1 == v2))
            return true;
        return false;
    }
    
    /**
     *  Returns the left-most corner of a single vertex 1-ring. If a vertex is not
     *  on a boundary (in which case it has a full 1-ring), this function returns
     *  any of the corners mapped to the given vertex.
     *
     * @param v 
     */
    @Override
    public int leftMostCorner(int v)
    {
        return vertexCorners.get(v);
    }
    
    /**
     *  Returns true if the specified vertex is on a boundary.
     *
     * @param vert 
     */
    @Override
    public boolean isOnBoundary(int vert)
    {
        int corner = this.leftMostCorner(vert);
        if (this.swingLeft(corner) < 0)
            return true;
        return false;
    }
    
    /**
     *  Get opposite corners on the left and right faces respecitively (see image
     *  below, where L and R are the left and right corners of a corner X.
     * 
     *  *-------*-------*
     *   \L    /X\    R/
     *    \   /   \   /
     *     \ /     \ /
     *      *-------*
     *
     * @param cornerId 
     */
    @Override
    public int getLeftCorner(int cornerId)
    {
        if (cornerId < 0)
            return K_INVALID_CORNER_INDEX;
        return this.opposite(this.previous(cornerId));
    }
    
    @Override
    public int getRightCorner(int cornerId)
    {
        if (cornerId < 0)
            return K_INVALID_CORNER_INDEX;
        return this.opposite(this.next(cornerId));
    }
    
    /**
     *  Methods that modify an existing corner table.
     *  Sets the opposite corner mapping between two corners. Caller must ensure
     *  that the indices are valid.
     *
     * @param cornerId 
     * @param oppCornerId 
     */
    public void setOppositeCorner(int cornerId, int oppCornerId)
    {
        oppositeCorners[cornerId] = oppCornerId;
    }
    
    /**
     *  Updates mapping betweeh a corner and a vertex.
     *
     * @param cornerId 
     * @param vertId 
     */
    public void mapCornerToVertex(int cornerId, int vertId)
    {
        if (vertId >= 0)
        {
            if (vertexCorners.getCount() <= vertId)
            {
                vertexCorners.resize(vertId + 1);
            }
            
            cornerToVertexMap[cornerId] = vertId;
        }
        
    }
    
    /**
     *  Sets a new left most corner for a given vertex.
     *
     * @param vert 
     * @param corner 
     */
    public void setLeftMostCorner(int vert, int corner)
    {
        if (vert != K_INVALID_VERTEX_INDEX)
        {
            vertexCorners.set(vert, corner);
        }
        
    }
    
    /**
     *  Makes a vertex isolated (not attached to any corner).
     *
     * @param vert 
     */
    public void makeVertexIsolated(int vert)
    {
        vertexCorners.set(vert, K_INVALID_CORNER_INDEX);
    }
    
    /**
     *  Returns true if a vertex is not attached to any face.
     *
     * @param v 
     */
    public boolean isVertexIsolated(int v)
    {
        return this.leftMostCorner(v) < 0;
    }
    
    /**
     *  Updates the vertex to corner map on a specified vertex. This should be
     *  called in cases where the mapping may be invalid (e.g. when the corner
     *  table was constructed manually).
     *
     */
    public void updateVertexToCornerMap(int vert)
    {
        int firstC = vertexCorners.get(vert);
        if (firstC < 0)
            return;
        // Isolated vertex.
        int actC = this.swingLeft(firstC);
        int c = firstC;
        while (actC >= 0 && (actC != firstC))
        {
            c = actC;
            actC = this.swingLeft(actC);
        }
        
        if (actC != firstC)
        {
            vertexCorners.set(vert, c);
        }
        
    }
    
    // Resets the corner table to the given number of invalid faces.
    // 
    public void reset(int numFaces, int numVertices)
    {
        if (numFaces < 0 || (numVertices < 0))
            throw new IllegalArgumentException();
        if (numFaces > (Integer.MAX_VALUE / 3))
            throw new IllegalArgumentException();
        this.cornerToVertexMap = new int[numFaces * 3];
        this.oppositeCorners = new int[numFaces * 3];
        for (int i = 0; i < cornerToVertexMap.length; i++)
        {
            cornerToVertexMap[i] = -1;
            oppositeCorners[i] = -1;
        }
        
        vertexCorners.setCapacity(numVertices);
        valenceCache.clearValenceCache();
        valenceCache.clearValenceCacheInaccurate();
    }
    
    public int confidentVertex(int corner)
    {
        //DRACO_DCHECK_GE(corner.value(), 0);
        //DRACO_DCHECK_LT(corner.value(), num_corners());
        return cornerToVertexMap[corner];
    }
    
    public int valence(int v)
    {
        if (v == K_INVALID_VERTEX_INDEX)
            return -1;
        int valence = 0;
        int startCorner = this.leftMostCorner(v);
        int corner = startCorner;
        boolean leftTraversal = true;
        
        while (corner >= 0)
        {
            valence++;
            
            if (leftTraversal)
            {
                corner = this.swingLeft(corner);
                if (corner < 0)
                {
                    // Open boundary reached.
                    corner = startCorner;
                    leftTraversal = false;
                }
                else if (corner == startCorner)
                {
                    // End reached.
                    corner = K_INVALID_CORNER_INDEX;
                }
                
            }
            else
            {
                // Go to the right until we reach a boundary there (no explicit check
                // is needed in this case).
                corner = this.swingRight(corner);
            }
            
        }
        
        return valence;
    }
    
    public int addNewVertex()
    {
        vertexCorners.add(K_INVALID_CORNER_INDEX);
        return vertexCorners.getCount() - 1;
    }
    
    private void $initFields$()
    {
        try
        {
            vertexCorners = new IntList();
            nonManifoldVertexParents = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
