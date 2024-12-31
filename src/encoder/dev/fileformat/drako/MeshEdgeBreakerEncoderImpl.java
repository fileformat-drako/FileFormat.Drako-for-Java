package dev.fileformat.drako;
import dev.fileformat.drako.AsposeUtils;
import dev.fileformat.drako.Int2D;
import dev.fileformat.drako.IntSpan;
import java.util.ArrayList;
import java.util.HashMap;
/**
 *  Class implementing the edgebreaker encoding as described in "3D Compression
 *  Made Simple: Edgebreaker on a Corner-Table" by Rossignac at al.'01.
 *  http://www.cc.gatech.edu/~jarek/papers/CornerTableSMI.pdf
 *
 */
class MeshEdgeBreakerEncoderImpl implements IMeshEdgeBreakerEncoder
{    
    /**
     *  Struct holding data used for encoding each non-position attribute.
     *  TODO(ostava): This should be probably renamed to something better.
     *
     */
    static class AttributeData
    {
        public int attributeIndex = -1;
        public MeshAttributeCornerTable connectivityData;
        /**
         *  Flag that can mark the connectivityData invalid. In such case the base
         *  corner table of the mesh should be used instead.
         *
         */
        public boolean isConnectivityUsed = true;
        /**
         *  Data about attribute encoding order.
         *
         */
        public MeshAttributeIndicesEncodingData encodingData = new MeshAttributeIndicesEncodingData();
        public byte traversalMethod;
        
    }
    
    /**
     *  The main encoder that own's this class.
     *
     */
    private MeshEdgeBreakerEncoder encoder;
    /**
     *  Mesh that's being encoded.
     *
     */
    private DracoMesh mesh;
    /**
     *  Corner table stores the mesh face connectivity data.
     *
     */
    private CornerTable cornerTable;
    /**
     *  Stack used for storing corners that need to be traversed when encoding
     *  the connectivity. New corner is added for each initial face and a split
     *  symbol, and one corner is removed when the end symbol is reached.
     *  Stored as member variable to prevent frequent memory reallocations when
     *  handling meshes with lots of disjoint components. Originally, we used
     *  recursive functions to handle this behavior, but that can cause stack
     *  memory overflow when compressing huge meshes.
     *
     */
    private IntList cornerTraversalStack;
    /**
     *  Array for marking visited faces.
     *
     */
    private ArrayList<Boolean> visitedFaces;
    /**
     *  Attribute data for position encoding.
     *
     */
    private MeshAttributeIndicesEncodingData posEncodingData;
    /**
     *  Array storing corners in the order they were visited during the
     *  connectivity encoding (always storing the tip corner of each newly visited
     *  face).
     *
     */
    private IntList processedConnectivityCorners;
    /**
     *  Array for storing visited vertex ids of all input vertices.
     *
     */
    private boolean[] visitedVertexIds;
    /**
     *  For each traversal, this array stores the number of visited vertices.
     *
     */
    private IntList vertexTraversalLength;
    /**
     *  Array for storing all topology split events encountered during the mesh
     *  traversal.
     *
     */
    private ArrayList<TopologySplitEventData> topologySplitEventData;
    /**
     *  Map between faceId and symbolId. Contains entries only for faces that
     *  were encoded with TOPOLOGYS symbol.
     *
     */
    private HashMap<Integer, Integer> faceToSplitSymbolMap;
    /**
     *  Array for marking holes that has been reached during the traversal.
     *
     */
    private ArrayList<Boolean> visitedHoles;
    /**
     *  Array for mapping vertices to hole ids. If a vertex is not on a hole, the
     *  stored value is -1.
     *
     */
    private int[] vertexHoleId;
    /**
     *  Array of hole events encountered during the traversal. There will be always
     *  exactly one hole event for each hole in the input mesh.
     *
     */
    private ArrayList<HoleEventData> holeEventData;
    /**
     *  Id of the last encoded symbol.
     *
     */
    private int lastEncodedSymbolId;
    /**
     *  The number of encoded split symbols.
     *
     */
    private int numSplitSymbols;
    private AttributeData[] attributeData;
    private byte posTraversalMethod;
    /**
     *  Array storing mapping between attribute encoder id and attribute data id.
     *
     */
    private IntList attributeEncoderToDataIdMap;
    private ITraversalEncoder traversalEncoder;
    /**
     *  Initializes data needed for encoding non-position attributes.
     *  Returns false on error.
     *
     */
    public void initAttributeData()
    {
        int numAttributes = mesh.getNumAttributes();
        // Ignore the position attribute. It's decoded separately.
        this.attributeData = new AttributeData[numAttributes - 1];
        for (int i = 0; i < attributeData.length; i++)
        {
            attributeData[i] = new AttributeData();
        }
        
        if (numAttributes == 1)
            return;
        int dataIndex = 0;
        for (int i = 0; i < numAttributes; ++i)
        {
            int attIndex = i;
            if (mesh.attribute(attIndex).getAttributeType() == AttributeType.POSITION)
                continue;
            PointAttribute att = mesh.attribute(attIndex);
            attributeData[dataIndex].attributeIndex = attIndex;
            attributeData[dataIndex].encodingData.encodedAttributeValueIndexToCornerMap.clear();
            
            attributeData[dataIndex].encodingData.encodedAttributeValueIndexToCornerMap.setCapacity(cornerTable.getNumCorners());
            DracoUtils.fill(attributeData[dataIndex].encodingData.vertexToEncodedAttributeValueIndexMap = new int[cornerTable.getNumCorners()], -1);
            
            attributeData[dataIndex].encodingData.numValues = 0;
            attributeData[dataIndex].connectivityData = new MeshAttributeCornerTable(mesh, cornerTable, att);
            ++dataIndex;
        }
        
    }
    
    private void assign(IntList list, int size, int val)
    {
        //Make list to the specified size and all content to specified value
        //remove unnecessary values
        while (list.getCount() > size)
        {
            list.removeAt(list.getCount() - 1);
        }
        
        //replace old values
        for (int i = 0; i < list.getCount(); i++)
        {
            list.set(i, val);
        }
        
        //extend to specified size
        while (list.getCount() < size)
        {
            list.add(val);
        }
        
    }
    
    private <T>void assign(ArrayList<T> list, int size, T val)
    {
        //Make list to the specified size and all content to specified value
        //remove unnecessary values
        while (list.size() > size)
        {
            list.remove(list.size() - 1);
        }
        
        //replace old values
        for (int i = 0; i < list.size(); i++)
        {
            list.set(i, val);
        }
        
        //extend to specified size
        while (list.size() < size)
        {
            list.add(val);
        }
        
    }
    
    /**
     *  Finds the configuration of the initial face that starts the traversal.
     *  Configurations are determined by location of holes around the init face
     *  and they are described in meshEdgebreakerShared.h.
     *  Returns true if the face configuration is interior and false if it is
     *  exterior.
     *
     */
    public boolean findInitFaceConfiguration(int faceId, int[] outCorner)
    {
        int cornerIndex = 3 * faceId;
        for (int i = 0; i < 3; ++i)
        {
            if (cornerTable.opposite(cornerIndex) == -1)
            {
                // If there is a boundary edge, the configuration is exterior and return
                // the int opposite to the first boundary edge.
                outCorner[0] = cornerIndex;
                return false;
            }
            
            if (vertexHoleId[cornerTable.vertex(cornerIndex)] != -1)
            {
                int rightCorner = cornerIndex;
                while (rightCorner >= 0)
                {
                    cornerIndex = rightCorner;
                    rightCorner = cornerTable.swingRight(rightCorner);
                }
                
                // |cornerIndex| now lies on a boundary edge and its previous corner is
                // guaranteed to be the opposite corner of the boundary edge.
                outCorner[0] = cornerTable.previous(cornerIndex);
                return false;
            }
            
            cornerIndex = cornerTable.next(cornerIndex);
        }
        
        // Else we have an interior configuration. Return the first corner id.
        outCorner[0] = cornerIndex;
        return true;
    }
    
    /**
     *  Encodes the connectivity between vertices.
     *
     */
    public void encodeConnectivityFromCorner(int cornerId)
    {
        
        cornerTraversalStack.clear();
        cornerTraversalStack.add(cornerId);
        int numFaces = mesh.getNumFaces();
        while (cornerTraversalStack.getCount() > 0)
        {
            // Currently processed corner.
            cornerId = cornerTraversalStack.get(cornerTraversalStack.getCount() - 1);
            // Make sure the face hasn't been visited yet.
            if (cornerId < 0 || visitedFaces.get(cornerTable.face(cornerId)))
            {
                // This face has been already traversed.
                cornerTraversalStack.removeAt(cornerTraversalStack.getCount() - 1);
                continue;
            }
            
            int numVisitedFaces = 0;
            while (numVisitedFaces < numFaces)
            {
                // Mark the current face as visited.
                ++numVisitedFaces;
                ++lastEncodedSymbolId;
                int faceId = cornerTable.face(cornerId);
                visitedFaces.set(faceId, true);
                processedConnectivityCorners.add(cornerId);
                traversalEncoder.newCornerReached(cornerId);
                int vertId = cornerTable.vertex(cornerId);
                boolean onBoundary = vertexHoleId[vertId] != -1;
                if (!this.isVertexVisited(vertId))
                {
                    // A new unvisited vertex has been reached. We need to store its
                    // position difference using next,prev, and opposite vertices.
                    visitedVertexIds[vertId] = true;
                    if (!onBoundary)
                    {
                        // If the vertex is on boundary it must correspond to an unvisited
                        // hole and it will be encoded with TOPOLOGYS symbol later).
                        traversalEncoder.encodeSymbol(EdgeBreakerTopologyBitPattern.C);
                        // Move to the right triangle.
                        cornerId = this.getRightCorner(cornerId);
                        continue;
                    }
                    
                }
                
                int rightCornerId = this.getRightCorner(cornerId);
                int leftCornerId = this.getLeftCorner(cornerId);
                int rightFaceId = cornerTable.face(rightCornerId);
                int leftFaceId = cornerTable.face(leftCornerId);
                if (this.isRightFaceVisited(cornerId))
                {
                    // Right face has been already visited.
                    // Check whether there is a topology split event.
                    if (rightFaceId != -1)
                    {
                        this.checkAndStoreTopologySplitEvent(lastEncodedSymbolId, faceId, EdgeFaceName.RIGHT_FACE_EDGE, rightFaceId);
                    }
                    
                    if (this.isLeftFaceVisited(cornerId))
                    {
                        // Both neighboring faces are visited. End reached.
                        // Check whether there is a topology split event on the left face.
                        if (leftFaceId != -1)
                        {
                            this.checkAndStoreTopologySplitEvent(lastEncodedSymbolId, faceId, EdgeFaceName.LEFT_FACE_EDGE, leftFaceId);
                        }
                        
                        traversalEncoder.encodeSymbol(EdgeBreakerTopologyBitPattern.E);
                        cornerTraversalStack.removeAt(cornerTraversalStack.getCount() - 1);
                        break;
                        // Break from the while (numVisitedFaces < numFaces) loop.
                    }
                    else
                    {
                        traversalEncoder.encodeSymbol(EdgeBreakerTopologyBitPattern.R);
                        // Go to the left face.
                        cornerId = leftCornerId;
                    }
                    
                }
                else if (this.isLeftFaceVisited(cornerId))
                {
                    // Check whether there is a topology split event on the left face.
                    if (leftFaceId != -1)
                    {
                        this.checkAndStoreTopologySplitEvent(lastEncodedSymbolId, faceId, EdgeFaceName.LEFT_FACE_EDGE, leftFaceId);
                    }
                    
                    traversalEncoder.encodeSymbol(EdgeBreakerTopologyBitPattern.L);
                    // Left face visited, go to the right one.
                    cornerId = rightCornerId;
                }
                else
                {
                    traversalEncoder.encodeSymbol(EdgeBreakerTopologyBitPattern.S);
                    ++numSplitSymbols;
                    // Both neighboring faces are unvisited, we need to visit both of
                    // them.
                    if (onBoundary)
                    {
                        int holeId = vertexHoleId[vertId];
                        if (!visitedHoles.get(holeId))
                        {
                            this.encodeHole(cornerId, false);
                            holeEventData.add(new HoleEventData(lastEncodedSymbolId));
                        }
                        
                    }
                    
                    faceToSplitSymbolMap.put(faceId, lastEncodedSymbolId);
                    // Split the traversal.
                    // First make the top of the current corner stack point to the left
                    // face (this one will be processed second).
                    cornerTraversalStack.set(cornerTraversalStack.getCount() - 1, leftCornerId);
                    // Add a new corner to the top of the stack (right face needs to
                    // be traversed first).
                    cornerTraversalStack.add(rightCornerId);
                    // Break from the while (numVisitedFaces < numFaces) loop.
                    break;
                }
                
            }
            
        }
        
        // All corners have been processed.
    }
    
    /**
     *  Encodes all vertices of a hole starting at startCornerId.
     *  The vertex associated with the first corner is encoded only if
     *  |encodeFirstVertex| is true.
     *  Returns the number of encoded hole vertices.
     *
     */
    public int encodeHole(int startCornerId, boolean encodeFirstVertex)
    {
        int cornerId = startCornerId;
        cornerId = cornerTable.previous(cornerId);
        while (cornerTable.opposite(cornerId) != -1)
        {
            cornerId = cornerTable.opposite(cornerId);
            cornerId = cornerTable.next(cornerId);
        }
        
        int startVertexId = cornerTable.vertex(startCornerId);
        int numEncodedHoleVerts = 0;
        if (encodeFirstVertex)
        {
            visitedVertexIds[startVertexId] = true;
            ++numEncodedHoleVerts;
        }
        
        
        // cornerId is now opposite to the boundary edge.
        // Mark the hole as visited.
        visitedHoles.set(vertexHoleId[startVertexId], true);
        int startVertId = cornerTable.vertex(cornerTable.next(cornerId));
        int actVertexId = cornerTable.vertex(cornerTable.previous(cornerId));
        while (actVertexId != startVertexId)
        {
            // Encode the end vertex of the boundary edge.
            
            startVertId = actVertexId;
            
            // Mark the vertex as visited.
            visitedVertexIds[actVertexId] = true;
            ++numEncodedHoleVerts;
            cornerId = cornerTable.next(cornerId);
            // Look for the next attached open boundary edge.
            while (cornerTable.opposite(cornerId) != -1)
            {
                cornerId = cornerTable.opposite(cornerId);
                cornerId = cornerTable.next(cornerId);
            }
            
            actVertexId = cornerTable.vertex(cornerTable.previous(cornerId));
        }
        
        return numEncodedHoleVerts;
    }
    
    public int getRightCorner(int cornerId)
    {
        int nextCornerId = cornerTable.next(cornerId);
        return cornerTable.opposite(nextCornerId);
    }
    
    public int getLeftCorner(int cornerId)
    {
        int prevCornerId = cornerTable.previous(cornerId);
        return cornerTable.opposite(prevCornerId);
    }
    
    public boolean isRightFaceVisited(int cornerId)
    {
        int nextCornerId = cornerTable.next(cornerId);
        int oppCornerId = cornerTable.opposite(nextCornerId);
        if (oppCornerId != -1)
            return visitedFaces.get(cornerTable.face(oppCornerId));
        // Else we are on a boundary.
        return true;
    }
    
    public boolean isLeftFaceVisited(int cornerId)
    {
        int prevCornerId = cornerTable.previous(cornerId);
        int oppCornerId = cornerTable.opposite(prevCornerId);
        if (oppCornerId != -1)
            return visitedFaces.get(cornerTable.face(oppCornerId));
        // Else we are on a boundary.
        return true;
    }
    
    public boolean isVertexVisited(int vertId)
    {
        return visitedVertexIds[vertId];
    }
    
    /**
     *  Finds and stores data about all holes in the input mesh.
     *
     */
    public boolean findHoles()
    {
        int numCorners = cornerTable.getNumCorners();
        // Go over all corners and detect non-visited open boundaries
        for (int i = 0; i < numCorners; ++i)
        {
            if (cornerTable.isDegenerated(cornerTable.face(i)))
                continue;
            // Don't process corners assigned to degenerated faces.
            if (cornerTable.opposite(i) == -1)
            {
                int boundaryVertId = cornerTable.vertex(cornerTable.next(i));
                if (vertexHoleId[boundaryVertId] != -1)
                    continue;
                int boundaryId = visitedHoles.size();
                visitedHoles.add(false);
                int cornerId = i;
                while (vertexHoleId[boundaryVertId] == -1)
                {
                    // Mark the first vertex on the open boundary.
                    vertexHoleId[boundaryVertId] = boundaryId;
                    cornerId = cornerTable.next(cornerId);
                    // Look for the next attached open boundary edge.
                    while (cornerTable.opposite(cornerId) != -1)
                    {
                        cornerId = cornerTable.opposite(cornerId);
                        cornerId = cornerTable.next(cornerId);
                    }
                    
                    // Id of the next vertex in the vertex on the hole.
                    boundaryVertId = cornerTable.vertex(cornerTable.next(cornerId));
                }
                
            }
            
        }
        
        return true;
    }
    
    /**
     *  For faces encoded with symbol TOPOLOGYS (split), this method returns
     *  the encoded symbol id or -1 if the face wasn't encoded by a split symbol.
     *
     */
    public int getSplitSymbolIdOnFace(int faceId)
    {
        int ret;
        final Integer[] ref0 = new Integer[1];
        if (AsposeUtils.tryGetValue(faceToSplitSymbolMap, faceId, ref0))
        {
            ret = ref0[0] == null ? 0 : ref0[0];
            return ret;
        }
        else
        {
            ret = ref0[0] == null ? 0 : ref0[0];
        }
        
        return -1;
    }
    
    /**
     *  Checks whether there is a topology split event on a neighboring face and
     *  stores the event data if necessary. For more info about topology split
     *  events, see description of TopologySplitEventData in
     *  meshEdgebreakerShared.h.
     *
     */
    public void checkAndStoreTopologySplitEvent(int srcSymbolId, int srcFaceId, byte srcEdge, int neighborFaceId)
    {
        int symbolId = this.getSplitSymbolIdOnFace(neighborFaceId);
        if (symbolId == -1)
            return;
        // Not a split symbol, no topology split event could happen.
        TopologySplitEventData eventData = new TopologySplitEventData();
        
        eventData.splitSymbolId = symbolId;
        // It's always the left edge for true split symbols (as the right edge is
        // traversed first).
        eventData.splitEdge = EdgeFaceName.LEFT_FACE_EDGE;
        
        eventData.sourceSymbolId = srcSymbolId;
        eventData.sourceEdge = srcEdge;
        topologySplitEventData.add(eventData);
    }
    
    /**
     *  Encodes connectivity of all attributes on a newly traversed face.
     *
     */
    public void encodeAttributeConnectivitiesOnFace(int corner)
    {
        int[] corners = {corner, cornerTable.next(corner), cornerTable.previous(corner)};
        int src_face_id = cornerTable.face(corner);
        visitedFaces.set(src_face_id, true);
        
        for (int c = 0; c < 3; ++c)
        {
            int oppCorner = cornerTable.opposite(corners[c]);
            if (oppCorner < 0)
                continue;
            // Don't encode attribute seams on boundary edges.
            int opp_face_id = cornerTable.face(oppCorner);
            if (visitedFaces.get(opp_face_id))
                continue;
            
            for (int i = 0; i < attributeData.length; ++i)
            {
                if (attributeData[i].connectivityData.isCornerOppositeToSeamEdge(corners[c]))
                {
                    traversalEncoder.encodeAttributeSeam(i, true);
                }
                else
                {
                    traversalEncoder.encodeAttributeSeam(i, false);
                }
                
            }
            
        }
        
    }
    
    /**
     *  This function is used to to assign correct encoding order of attributes
     *  to unprocessed corners. The encoding order is equal to the order in which
     *  the attributes are going to be processed by the decoder and it is necessary
     *  for proper prediction of attribute values.
     * public bool AssignPositionEncodingOrderToAllCorners();
     *  This function is used to generate encoding order for all non-position
     *  attributes.
     *  Returns false when one or more attributes failed to be processed.
     * public bool GenerateEncodingOrderForAttributes();
     *
     */
    public MeshEdgeBreakerEncoderImpl(ITraversalEncoder encoder)
    {
        this.$initFields$();
        this.lastEncodedSymbolId = -1;
        this.traversalEncoder = encoder;
    }
    
    public void init(MeshEdgeBreakerEncoder encoder)
    {
        
        this.encoder = encoder;
        this.mesh = encoder.getMesh();
        attributeEncoderToDataIdMap.clear();
    }
    
    public MeshAttributeCornerTable getAttributeCornerTable(int attId)
    {
        
        for (int i = 0; i < attributeData.length; ++i)
        {
            if (attributeData[i].attributeIndex == attId)
            {
                if (attributeData[i].isConnectivityUsed)
                    return attributeData[i].connectivityData;
                return null;
            }
            
        }
        
        return null;
    }
    
    public MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId)
    {
        for (int i = 0; i < attributeData.length; ++i)
        {
            if (attributeData[i].attributeIndex == attId)
                return attributeData[i].encodingData;
        }
        
        return posEncodingData;
    }
    
    public void generateAttributesEncoder(int attId)
        throws DrakoException
    {
        int elementType = this.getEncoder().getMesh().getAttributeElementType(attId);
        PointAttribute att = this.getEncoder().getPointCloud().attribute(attId);
        int attDataId = -1;
        for (int i = 0; i < attributeData.length; ++i)
        {
            if (attributeData[i].attributeIndex == attId)
            {
                attDataId = i;
                break;
            }
            
        }
        
        byte traversalMethod = MeshTraversalMethod.DEPTH_FIRST;
        PointsSequencer sequencer = null;
        if (att.getAttributeType() == AttributeType.POSITION || (elementType == MeshAttributeElementType.VERTEX) || (elementType == MeshAttributeElementType.CORNER && attributeData[attDataId].connectivityData.getNoInteriorSeams()))
        {
            MeshAttributeIndicesEncodingData encodingData;
            if (att.getAttributeType() == AttributeType.POSITION)
            {
                encodingData = posEncodingData;
            }
            else
            {
                encodingData = attributeData[attDataId].encodingData;
                attributeData[attDataId].isConnectivityUsed = false;
            }
            
            int speed = encoder.getOptions().getSpeed();
            if (speed == 0 && (att.getAttributeType() == AttributeType.POSITION))
            {
                traversalMethod = MeshTraversalMethod.PREDICTION_DEGREE;
                if (mesh.getNumAttributes() > 1)
                {
                    // Make sure we don't use the prediction degree traversal when we encode
                    // multiple attributes using the same connectivity.
                    // TODO(ostava): We should investigate this and see if the prediction
                    // degree can be actually used efficiently for non-position attributes.
                    traversalMethod = MeshTraversalMethod.DEPTH_FIRST;
                }
                
            }
            
            MeshTraversalSequencer<CornerTable> traversalSequencer = new MeshTraversalSequencer<CornerTable>(mesh, encodingData);
            MeshAttributeIndicesEncodingObserver<CornerTable> attObserver = new MeshAttributeIndicesEncodingObserver<CornerTable>(cornerTable, mesh, traversalSequencer, encodingData);
            ICornerTableTraverser<CornerTable> attTraverser = null;
            if (traversalMethod == MeshTraversalMethod.PREDICTION_DEGREE)
            {
                //typedef MeshAttributeIndicesEncodingObserver<CornerTable> AttObserver;
                //typedef MaxPredictionDegreeTraverser<CornerTable, AttObserver> AttTraverser;
                attTraverser = new EdgeBreakerTraverser<CornerTable>(cornerTable, attObserver);
            }
            else if (traversalMethod == MeshTraversalMethod.DEPTH_FIRST)
            {
                //typedef MeshAttributeIndicesEncodingObserver<CornerTable> AttObserver;
                //typedef DepthFirstTraverser<CornerTable, AttObserver> AttTraverser;
                //sequencer = CreateVertexTraversalSequencer<AttTraverser>(encoding_data);
                attTraverser = new DepthFirstTraverser(cornerTable, attObserver);
            }
            
            
            traversalSequencer.setCornerOrder(processedConnectivityCorners);
            traversalSequencer.setTraverser(attTraverser);
            sequencer = traversalSequencer;
        }
        else
        {
            MeshTraversalSequencer<MeshAttributeCornerTable> traversalSequencer = new MeshTraversalSequencer<MeshAttributeCornerTable>(mesh, attributeData[attDataId].encodingData);
            MeshAttributeIndicesEncodingObserver<MeshAttributeCornerTable> attObserver = new MeshAttributeIndicesEncodingObserver<MeshAttributeCornerTable>(attributeData[attDataId].connectivityData, mesh, traversalSequencer, attributeData[attDataId].encodingData);
            EdgeBreakerTraverser<MeshAttributeCornerTable> attTraverser = new EdgeBreakerTraverser<MeshAttributeCornerTable>(attributeData[attDataId].connectivityData, attObserver);
            
            traversalSequencer.setCornerOrder(processedConnectivityCorners);
            traversalSequencer.setTraverser(attTraverser);
            sequencer = traversalSequencer;
        }
        
        
        if (sequencer == null)
            throw DracoUtils.failed();
        
        if (attDataId == -1)
        {
            this.posTraversalMethod = traversalMethod;
        }
        else
        {
            attributeData[attDataId].traversalMethod = traversalMethod;
        }
        
        SequentialAttributeEncodersController attController = new SequentialAttributeEncodersController(sequencer, attId);
        
        // Update the mapping between the encoder id and the attribute data id.
        // This will be used by the decoder to select the approperiate attribute
        // decoder and the correct connectivity.
        attributeEncoderToDataIdMap.add(attDataId);
        this.getEncoder().addAttributesEncoder(attController);
    }
    
    public void encodeAttributesEncoderIdentifier(int attEncoderId)
    {
        int attDataId = attributeEncoderToDataIdMap.get(attEncoderId);
        byte traversalMethod;
        encoder.getBuffer().encode((byte)attDataId);
        int elementType = MeshAttributeElementType.VERTEX;
        
        if (attDataId >= 0)
        {
            int attId = attributeData[attDataId].attributeIndex;
            elementType = this.getEncoder().getMesh().getAttributeElementType(attId);
            traversalMethod = attributeData[attDataId].traversalMethod;
        }
        else
        {
            traversalMethod = posTraversalMethod;
        }
        
        if (elementType == MeshAttributeElementType.VERTEX || (elementType == MeshAttributeElementType.CORNER && attributeData[attDataId].connectivityData.getNoInteriorSeams()))
        {
            // Per-vertex encoder.
            encoder.getBuffer().encode((byte)((byte)MeshAttributeElementType.VERTEX));
        }
        else
        {
            // Per-corner encoder.
            encoder.getBuffer().encode((byte)((byte)MeshAttributeElementType.CORNER));
        }
        
        encoder.getBuffer().encode(traversalMethod);
    }
    
    private CornerTable createCornerTableFromPositionAttribute(DracoMesh mesh)
    {
        PointAttribute att = mesh.getNamedAttribute(AttributeType.POSITION);
        if (att == null)
            return null;
        Int2D faces = new Int2D(mesh.getNumFaces(), 3);
        IntSpan face = IntSpan.wrap(new int[3]);
        for (int i = 0; i < mesh.getNumFaces(); ++i)
        {
            mesh.readFace(i, face);
            for (int j = 0; j < 3; ++j)
            {
                // Map general vertex indices to position indices.
                faces.set(i, j, att.mappedIndex(face.get(j)));
            }
            
        }
        
        CornerTable ret = new CornerTable();
        ret.initialize(faces);
        return ret;
    }
    
    private CornerTable createCornerTableFromAllAttributes(DracoMesh mesh)
    {
        Int2D faces = new Int2D(mesh.getNumFaces(), 3);
        IntSpan face = IntSpan.wrap(new int[3]);
        for (int i = 0; i < mesh.getNumFaces(); ++i)
        {
            mesh.readFace(i, face);
            // Each face is identified by point indices that automatically split the
            // mesh along attribute seams.
            for (int j = 0; j < 3; ++j)
            {
                faces.set(i, j, face.get(j));
            }
            
        }
        
        CornerTable ret = new CornerTable();
        ret.initialize(faces);
        return ret;
    }
    
    @Override
    public void encodeConnectivity()
        throws DrakoException
    {
        final int[] ref1 = new int[1];
        
        // To encode the mesh, we need face connectivity data stored in a corner
        // table. To compute the connectivity we must use indices associated with
        // POSITION attribute, because they define which edges can be connected
        // together.
        
        if (encoder.getOptions().getSplitMeshOnSeams())
        {
            this.cornerTable = this.createCornerTableFromAllAttributes(mesh);
        }
        else
        {
            this.cornerTable = this.createCornerTableFromPositionAttribute(mesh);
        }
        
        if (cornerTable == null || (cornerTable.getNumFaces() == cornerTable.getNumDegeneratedFaces()))
            throw DracoUtils.failed();
        
        traversalEncoder.init(this);
        int numVerticesToBeEncoded = cornerTable.getNumVertices() - cornerTable.getNumIsolatedVertices();
        Encoding.encodeVarint2(numVerticesToBeEncoded, this.getEncoder().getBuffer());
        int numFaces = cornerTable.getNumFaces() - cornerTable.getNumDegeneratedFaces();
        Encoding.encodeVarint2(numFaces, this.getEncoder().getBuffer());
        this.assign(visitedFaces, mesh.getNumFaces(), false);
        DracoUtils.fill(posEncodingData.vertexToEncodedAttributeValueIndexMap = new int[cornerTable.getNumVertices()], -1);
        posEncodingData.encodedAttributeValueIndexToCornerMap.clear();
        posEncodingData.encodedAttributeValueIndexToCornerMap.setCapacity(cornerTable.getNumFaces() * 3);
        //Assign(visitedVertexIds, cornerTable.NumVertices, false);
        this.visitedVertexIds = new boolean[cornerTable.getNumVertices()];
        vertexTraversalLength.clear();
        this.lastEncodedSymbolId = -1;
        this.numSplitSymbols = 0;
        topologySplitEventData.clear();
        faceToSplitSymbolMap.clear();
        visitedHoles.clear();
        //Assign(vertexHoleId, cornerTable.NumVertices, -1);
        DracoUtils.fill(this.vertexHoleId = new int[cornerTable.getNumVertices()], -1);
        holeEventData.clear();
        processedConnectivityCorners.clear();
        processedConnectivityCorners.setCapacity(cornerTable.getNumFaces());
        posEncodingData.numValues = 0;
        
        if (!this.findHoles())
            throw DracoUtils.failed();
        
        this.initAttributeData();
        byte numAttributeData = (byte)attributeData.length;
        encoder.getBuffer().encode(numAttributeData);
        int numCorners = cornerTable.getNumCorners();
        
        traversalEncoder.start();
        IntList initFaceConnectivityCorners = new IntList();
        // Traverse the surface starting from each unvisited corner.
        for (int cId = 0; cId < numCorners; ++cId)
        {
            int cornerIndex = cId;
            int faceId = cornerTable.face(cornerIndex);
            if (visitedFaces.get(faceId))
                continue;
            // Face has been already processed.
            if (cornerTable.isDegenerated(faceId))
                continue;
            // Ignore degenerated faces.
            int startCorner;
            boolean interiorConfig = this.findInitFaceConfiguration(faceId, ref1);
            startCorner = ref1[0];
            traversalEncoder.encodeStartFaceConfiguration(interiorConfig);
            
            if (interiorConfig)
            {
                // Select the correct vertex on the face as the root.
                cornerIndex = startCorner;
                int vertId = cornerTable.vertex(cornerIndex);
                int nextVertId = cornerTable.vertex(cornerTable.next(cornerIndex));
                int prevVertId = cornerTable.vertex(cornerTable.previous(cornerIndex));
                
                visitedVertexIds[vertId] = true;
                visitedVertexIds[nextVertId] = true;
                visitedVertexIds[prevVertId] = true;
                // New traversal started. Initiate it's length with the first vertex.
                vertexTraversalLength.add(1);
                
                // Mark the face as visited.
                visitedFaces.set(faceId, true);
                // Start compressing from the opposite face of the "next" corner. This way
                // the first encoded corner corresponds to the tip corner of the regular
                // edgebreaker traversal (essentially the initial face can be then viewed
                // as a TOPOLOGYC face).
                initFaceConnectivityCorners.add(cornerTable.next(cornerIndex));
                int oppId = cornerTable.opposite(cornerTable.next(cornerIndex));
                int oppFaceId = cornerTable.face(oppId);
                if (oppFaceId != -1 && !visitedFaces.get(oppFaceId))
                {
                    this.encodeConnectivityFromCorner(oppId);
                }
                
            }
            else
            {
                // Bounary configuration. We start on a boundary rather than on a face.
                // First encode the hole that's opposite to the startCorner.
                this.encodeHole(cornerTable.next(startCorner), true);
                // Start processing the face opposite to the boundary edge (the face
                // containing the startCorner).
                this.encodeConnectivityFromCorner(startCorner);
            }
            
        }
        
        // Reverse the order of connectivity corners to match the order in which
        // they are going to be decoded.
        processedConnectivityCorners.reverse();
        // Append the init face connectivity corners (which are processed in order by
        // the decoder after the regular corners.
        processedConnectivityCorners.addRange(initFaceConnectivityCorners);
        // Emcode connectivity for all non-position attributes.
        if (attributeData.length > 0)
        {
            // Use the same order of corner that will be used by the decoder.
            for (int i = 0; i < visitedFaces.size(); i++)
            {
                visitedFaces.set(i, false);
            }
            
            for (int i = 0; i < processedConnectivityCorners.getCount(); i++)
            {
                int ci = processedConnectivityCorners.get(i);
                this.encodeAttributeConnectivitiesOnFace(ci);
            }
            
        }
        
        traversalEncoder.done();
        
        // Encode the number of symbols.
        Encoding.encodeVarint2(traversalEncoder.getNumEncodedSymbols(), encoder.getBuffer());
        
        // Encode the number of split symbols.
        Encoding.encodeVarint2(numSplitSymbols, encoder.getBuffer());
        
        // Append the traversal buffer.
        
        this.encodeSplitData();
        
        encoder.getBuffer().encode(traversalEncoder.getBuffer().getData(), traversalEncoder.getBuffer().getBytes());
    }
    
    void encodeSplitData()
    {
        int numEvents = topologySplitEventData.size();
        Encoding.encodeVarint2(numEvents, encoder.getBuffer());
        if (numEvents > 0)
        {
            int last_source_symbol_id = 0;// Used for delta coding.
            
            for (int i = 0; i < numEvents; ++i)
            {
                TopologySplitEventData event_data = topologySplitEventData.get(i);
                // Encode source symbol id as delta from the previous source symbol id.
                // Source symbol ids are always stored in increasing order so the delta is
                // going to be positive.
                Encoding.encodeVarint2((int)(event_data.sourceSymbolId - last_source_symbol_id), encoder.getBuffer());
                // Encode split symbol id as delta from the current source symbol id.
                // Split symbol id is always smaller than source symbol id so the below
                // delta is going to be positive.
                Encoding.encodeVarint2((int)(event_data.sourceSymbolId - event_data.splitSymbolId), encoder.getBuffer());
                last_source_symbol_id = event_data.sourceSymbolId;
            }
            
            
            encoder.getBuffer().startBitEncoding(numEvents, false);
            for (int i = 0; i < numEvents; ++i)
            {
                TopologySplitEventData event_data = topologySplitEventData.get(i);
                encoder.getBuffer().encodeLeastSignificantBits32(1, 0xff & event_data.sourceEdge);
            }
            
            
            encoder.getBuffer().endBitEncoding();
        }
        
    }
    
    @Override
    public CornerTable getCornerTable()
    {
        return cornerTable;
    }
    
    @Override
    public MeshEdgeBreakerEncoder getEncoder()
    {
        return encoder;
    }
    
    private void $initFields$()
    {
        try
        {
            cornerTraversalStack = new IntList();
            visitedFaces = new ArrayList<Boolean>();
            posEncodingData = new MeshAttributeIndicesEncodingData();
            processedConnectivityCorners = new IntList();
            vertexTraversalLength = new IntList();
            topologySplitEventData = new ArrayList<TopologySplitEventData>();
            faceToSplitSymbolMap = new HashMap<Integer, Integer>();
            visitedHoles = new ArrayList<Boolean>();
            holeEventData = new ArrayList<HoleEventData>();
            attributeData = null;
            attributeEncoderToDataIdMap = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
    private void assign(ArrayList<Boolean> list, int size, boolean val)
    {
        while (list.size() > size)
        {
            list.remove(list.size() - 1);
        }
        
        for (int i = 0; i < list.size(); i++)
        {
            list.set(i, val);
        }
        
        while (list.size() < size)
        {
            list.add(val);
        }
        
    }
    
}
