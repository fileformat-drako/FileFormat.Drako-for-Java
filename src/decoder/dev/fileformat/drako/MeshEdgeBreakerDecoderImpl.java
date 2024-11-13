package dev.fileformat.drako;
import dev.fileformat.drako.AsposeUtils;
import dev.fileformat.drako.Struct;
import java.util.ArrayList;
import java.util.HashMap;
/**
 *  Implementation of the edgebreaker decoder that decodes data encoded with the
 *  MeshEdgeBreakerEncoderImpl class. The implementation of the decoder is based
 *  on the algorithm presented in Isenburg et al'02 "Spirale Reversi: Reverse
 *  decoding of the Edgebreaker encoding". Note that the encoding is still based
 *  on the standard edgebreaker method as presented in "3D Compression
 *  Made Simple: Edgebreaker on a Corner-Table" by Rossignac at al.'01.
 *  http://www.cc.gatech.edu/~jarek/papers/CornerTableSMI.pdf. One difference is
 *  caused by the properties of the spirale reversi algorithm that decodes the
 *  symbols from the last one to the first one. To make the decoding more
 *  efficient, we encode all symbols in the reverse order, therefore the decoder
 *  can process them one by one.
 *  The main advantage of the spirale reversi method is that the partially
 *  decoded mesh has valid connectivity data at any time during the decoding
 *  process (valid with respect to the decoded portion of the mesh). The standard
 *  Edgebreaker decoder used two passes (forward decoding + zipping) which not
 *  only prevented us from having a valid connectivity but it was also slower.
 *  The main benefit of having the valid connectivity is that we can use the
 *  known connectivity to predict encoded symbols that can improve the
 *  compression rate.
 *
 */
class MeshEdgeBreakerDecoderImpl implements IMeshEdgeBreakerDecoderImpl
{    
    /**
     *  Data for non-position attributes used by the decoder.
     *
     */
    static class AttributeData
    {
        /**
         *  Id of the attribute decoder that was used to decode this attribute data.
         *
         */
        int decoderId = -1;
        MeshAttributeCornerTable connectivityData;
        /**
         *  Flag that can mark the connectivityData invalid. In such case the base
         *  corner table of the mesh should be used instead.
         *
         */
        boolean isConnectivityUsed = true;
        MeshAttributeIndicesEncodingData encodingData = new MeshAttributeIndicesEncodingData();
        /**
         *  Opposite corners to attribute seam edges.
         *
         */
        IntList attributeSeamCorners = new IntList();
        
    }
    
    private MeshEdgeBreakerDecoder decoder;
    private CornerTable cornerTable;
    /**
     *  Stack used for storing corners that need to be traversed when decoding
     *  mesh vertices. New corner is added for each initial face and a split
     *  symbol, and one corner is removed when the end symbol is reached.
     *  Stored as member variable to prevent frequent memory reallocations when
     *  handling meshes with lots of disjoint components.  Originally, we used
     *  recursive functions to handle this behavior, but that can cause stack
     *  memory overflow when compressing huge meshes.
     *
     */
    private IntList cornerTraversalStack;
    /**
     *  Array stores the number of visited visited for each mesh traversal.
     *
     */
    private IntList vertexTraversalLength;
    /**
     *  List of decoded topology split events.
     *
     */
    private ArrayList<TopologySplitEventData> topologySplitData;
    /**
     *  List of decoded hole events.
     *
     */
    private ArrayList<HoleEventData> holeEventData;
    /**
     *  The number of processed hole events.
     *
     */
    private int numProcessedHoleEvents;
    /**
     *  Configuration of the initial face for each mesh component.
     *
     */
    private ArrayList<Boolean> initFaceConfigurations;
    /**
     *  Initial corner for each traversal.
     *
     */
    private IntList initCorners;
    /**
     *  Mapping between vertex ids assigned during connectivity decoding and vertex
     *  ids that were used during encoding.
     *
     */
    private IntList vertexIdMap;
    /**
     *  Id of the last processed input symbol.
     *
     */
    private int lastSymbolId;
    /**
     *  Id of the last decoded vertex.
     *
     */
    private int lastVertId;
    /**
     *  Id of the last decoded face.
     *
     */
    private int lastFaceId;
    /**
     *  Array for marking visited faces.
     *
     */
    private ArrayList<Boolean> visitedFaces;
    /**
     *  Array for marking visited vertices.
     *
     */
    private ArrayList<Boolean> visitedVerts;
    /**
     *  Array for marking vertices on open boundaries.
     *
     */
    private boolean[] isVertHole;
    /**
     *  The number of new vertices added by the encoder (because of non-manifold
     *  vertices on the input mesh).
     *  If there are no non-manifold edges/vertices on the input mesh, this should
     *  be 0.
     *
     */
    private int numNewVertices;
    /**
     *  For every newly added vertex, this array stores it's mapping to the
     *  parent vertex id of the encoded mesh.
     *
     */
    private HashMap<Integer, Integer> newToParentVertexMap;
    /**
     *  The number of vertices that were encoded (can be different from the number
     *  of vertices of the input mesh).
     *
     */
    private int numEncodedVertices;
    /**
     *  Array for storing the encoded corner ids in the order their associated
     *  vertices were decoded.
     *
     */
    private IntList processedCornerIds;
    /**
     *  Array storing corners in the order they were visited during the
     *  connectivity decoding (always storing the tip corner of each newly visited
     *  face).
     *
     */
    private IntList processedConnectivityCorners;
    private MeshAttributeIndicesEncodingData posEncodingData;
    /**
     *  Id of an attributes decoder that uses |pos_encoding_data_|.
     *
     */
    private int posDataDecoderId;
    private AttributeData[] attributeData;
    private ITraversalDecoder traversalDecoder;
    public MeshEdgeBreakerDecoderImpl(MeshEdgeBreakerDecoder decoder, ITraversalDecoder traversalDecoder)
    {
        this.$initFields$();
        this.init(decoder);
        this.traversalDecoder = traversalDecoder;
    }
    
    public boolean init(MeshEdgeBreakerDecoder decoder)
    {
        this.decoder = decoder;
        return true;
    }
    
    public MeshAttributeCornerTable getAttributeCornerTable(int attId)
    {
        
        for (int i = 0; i < attributeData.length; ++i)
        {
            AttributesDecoder dec = (decoder.getAttributesDecoders())[attributeData[i].decoderId];
            for (int j = 0; j < dec.getNumAttributes(); ++j)
            {
                if (dec.getAttributeId(j) == attId)
                {
                    if (attributeData[i].isConnectivityUsed)
                        return attributeData[i].connectivityData;
                    return null;
                }
                
            }
            
        }
        
        return null;
    }
    
    public MeshAttributeIndicesEncodingData getAttributeEncodingData(int attId)
    {
        
        for (int i = 0; i < attributeData.length; ++i)
        {
            AttributesDecoder dec = (decoder.getAttributesDecoders())[attributeData[i].decoderId];
            for (int j = 0; j < dec.getNumAttributes(); ++j)
            {
                if (dec.getAttributeId(j) == attId)
                    return attributeData[i].encodingData;
            }
            
        }
        
        return posEncodingData;
    }
    
    public boolean createAttributesDecoder(int attDecoderId)
    {
        byte attDataId;
        final byte[] ref0 = new byte[1];
        final byte[] ref1 = new byte[1];
        final byte[] ref2 = new byte[1];
        if (!decoder.getBuffer().decode2(ref0))
        {
            attDataId = ref0[0];
            return DracoUtils.failed();
        }
        else
        {
            attDataId = ref0[0];
        }
        
        byte decoderType;
        if (!decoder.getBuffer().decode3(ref1))
        {
            decoderType = ref1[0];
            return DracoUtils.failed();
        }
        else
        {
            decoderType = ref1[0];
        }
        
        
        if (attDataId >= 0)
        {
            if (attDataId >= attributeData.length)
                return DracoUtils.failed();
            attributeData[0xff & attDataId].decoderId = attDecoderId;
        }
        else
        {
            if (posDataDecoderId >= 0)
                return DracoUtils.failed();
            this.posDataDecoderId = attDecoderId;
        }
        
        byte traversalMethod = MeshTraversalMethod.DEPTH_FIRST;
        if (decoder.getBitstreamVersion() >= 12)
        {
            byte encoded;
            if (!decoder.getBuffer().decode3(ref2))
            {
                encoded = ref2[0];
                return DracoUtils.failed();
            }
            else
            {
                encoded = ref2[0];
            }
            
            traversalMethod = encoded;
        }
        
        DracoMesh mesh = decoder.getMesh();
        PointsSequencer sequencer;
        
        if (decoderType == (byte)((byte)MeshAttributeElementType.VERTEX))
        {
            MeshAttributeIndicesEncodingData encodingData = null;
            if (attDataId < 0)
            {
                encodingData = posEncodingData;
            }
            else
            {
                encodingData = attributeData[0xff & attDataId].encodingData;
                // Mark the attribute connectivity data invalid to ensure it's not used
                // later on.
                attributeData[0xff & attDataId].isConnectivityUsed = false;
            }
            
            MeshTraversalSequencer<CornerTable> traversalSequencer = new MeshTraversalSequencer<CornerTable>(mesh, encodingData);
            MeshAttributeIndicesEncodingObserver<CornerTable> attObserver = new MeshAttributeIndicesEncodingObserver<CornerTable>(cornerTable, mesh, traversalSequencer, encodingData);
            ICornerTableTraverser<CornerTable> attTraverser;
            if (traversalMethod == MeshTraversalMethod.DEPTH_FIRST)
            {
                attTraverser = new EdgeBreakerTraverser<CornerTable>(attObserver);
            }
            else if (traversalMethod == MeshTraversalMethod.PREDICTION_DEGREE)
            {
                attTraverser = new PredictionDegreeTraverser(attObserver);
            }
            else
                return DracoUtils.failed();
            
            traversalSequencer.setTraverser(attTraverser);
            sequencer = traversalSequencer;
        }
        else
        {
            if (attDataId < 0)
                return DracoUtils.failed();
            // Attribute data must be specified.
            MeshTraversalSequencer<MeshAttributeCornerTable> traversalSequencer = new MeshTraversalSequencer<MeshAttributeCornerTable>(mesh, attributeData[0xff & attDataId].encodingData);
            MeshAttributeIndicesEncodingObserver<MeshAttributeCornerTable> attObserver = new MeshAttributeIndicesEncodingObserver<MeshAttributeCornerTable>(attributeData[0xff & attDataId].connectivityData, mesh, traversalSequencer, attributeData[0xff & attDataId].encodingData);
            EdgeBreakerTraverser<MeshAttributeCornerTable> attTraverser = new EdgeBreakerTraverser<MeshAttributeCornerTable>(attObserver);
            
            traversalSequencer.setTraverser(attTraverser);
            sequencer = traversalSequencer;
        }
        
        SequentialAttributeDecodersController attController = new SequentialAttributeDecodersController(sequencer);
        
        decoder.setAttributesDecoder(attDecoderId, attController);
        return true;
    }
    
    @Override
    public boolean decodeConnectivity()
    {
        final int[] ref3 = new int[1];
        final int[] ref4 = new int[1];
        final int[] ref5 = new int[1];
        final int[] ref6 = new int[1];
        final int[] ref7 = new int[1];
        final int[] ref8 = new int[1];
        final byte[] ref9 = new byte[1];
        final int[] ref10 = new int[1];
        final int[] ref11 = new int[1];
        final int[] ref12 = new int[1];
        final int[] ref13 = new int[1];
        final int[] ref14 = new int[1];
        final int[] ref15 = new int[1];
        final DecoderBuffer[] ref16 = new DecoderBuffer[1];
        this.numNewVertices = 0;
        newToParentVertexMap.clear();
        if (decoder.getBitstreamVersion() < 22)
        {
            int num_new_verts;
            if (decoder.getBitstreamVersion() < 20)
            {
                if (!decoder.getBuffer().decode5(ref3))
                {
                    num_new_verts = ref3[0];
                    return DracoUtils.failed();
                }
                else
                {
                    num_new_verts = ref3[0];
                }
                
            }
            else if (!Decoding.decodeVarint(ref4, decoder.getBuffer()))
            {
                num_new_verts = ref4[0];
                return DracoUtils.failed();
            }
            else
            {
                num_new_verts = ref4[0];
            }
            
            
            this.numNewVertices = num_new_verts;
        }
        
        int numEncodedVertices;
        if (decoder.getBitstreamVersion() < 20)
        {
            if (!decoder.getBuffer().decode5(ref5))
            {
                numEncodedVertices = ref5[0];
                return DracoUtils.failed();
            }
            else
            {
                numEncodedVertices = ref5[0];
            }
            
        }
        else if (!Decoding.decodeVarint(ref6, decoder.getBuffer()))
        {
            numEncodedVertices = ref6[0];
            return DracoUtils.failed();
        }
        else
        {
            numEncodedVertices = ref6[0];
        }
        
        
        this.numEncodedVertices = numEncodedVertices;
        int numFaces;
        
        if (decoder.getBitstreamVersion() < 20)
        {
            if (!decoder.getBuffer().decode5(ref7))
            {
                numFaces = ref7[0];
                return DracoUtils.failed();
            }
            else
            {
                numFaces = ref7[0];
            }
            
        }
        else if (!Decoding.decodeVarint(ref8, decoder.getBuffer()))
        {
            numFaces = ref8[0];
            return DracoUtils.failed();
        }
        else
        {
            numFaces = ref8[0];
        }
        
        
        if ((0xffffffffl & numFaces) > 805306367)//Upper limit of int32_t / 3
            return DracoUtils.failed();
        // Draco cannot handle this many faces.
        
        if ((0xffffffffl & numEncodedVertices) > (numFaces * 3))
            return DracoUtils.failed();
        byte numAttributeData;
        if (!decoder.getBuffer().decode3(ref9))
        {
            numAttributeData = ref9[0];
            return DracoUtils.failed();
        }
        else
        {
            numAttributeData = ref9[0];
        }
        
        int numEncodedSymbols;
        if (decoder.getBitstreamVersion() < 20)
        {
            if (!decoder.getBuffer().decode5(ref10))
            {
                numEncodedSymbols = ref10[0];
                return DracoUtils.failed();
            }
            else
            {
                numEncodedSymbols = ref10[0];
            }
            
        }
        else if (!Decoding.decodeVarint(ref11, decoder.getBuffer()))
        {
            numEncodedSymbols = ref11[0];
            return DracoUtils.failed();
        }
        else
        {
            numEncodedSymbols = ref11[0];
        }
        
        
        if ((0xffffffffl & numFaces) < (0xffffffffl & numEncodedSymbols))
            return DracoUtils.failed();
        int max_encoded_faces = numEncodedSymbols + (numEncodedSymbols / 3);
        if ((0xffffffffl & numFaces) > (0xffffffffl & max_encoded_faces))
            return DracoUtils.failed();
        int numEncodedSplitSymbols;
        if (decoder.getBitstreamVersion() < 20)
        {
            if (!decoder.getBuffer().decode5(ref12))
            {
                numEncodedSplitSymbols = ref12[0];
                return DracoUtils.failed();
            }
            else
            {
                numEncodedSplitSymbols = ref12[0];
            }
            
        }
        else if (!Decoding.decodeVarint(ref13, decoder.getBuffer()))
        {
            numEncodedSplitSymbols = ref13[0];
            return DracoUtils.failed();
        }
        else
        {
            numEncodedSplitSymbols = ref13[0];
        }
        
        
        if ((0xffffffffl & numEncodedSplitSymbols) > (0xffffffffl & numEncodedSymbols))
            return DracoUtils.failed();
        
        
        
        // Decode topology (connectivity).
        vertexTraversalLength.clear();
        this.cornerTable = new CornerTable();
        if (cornerTable == null)
            return DracoUtils.failed();
        processedCornerIds.clear();
        processedCornerIds.setCapacity(numFaces);
        processedConnectivityCorners.clear();
        processedConnectivityCorners.setCapacity(numFaces);
        topologySplitData.clear();
        holeEventData.clear();
        initFaceConfigurations.clear();
        initCorners.clear();
        
        this.numProcessedHoleEvents = 0;
        this.lastSymbolId = -1;
        
        this.lastFaceId = -1;
        this.lastVertId = -1;
        
        this.attributeData = new AttributeData[0xff & numAttributeData];
        
        
        if (!cornerTable.reset(numFaces, (int)(numEncodedVertices + numEncodedSplitSymbols)))
            return false;
        
        // Add one attribute data for each attribute decoder.
        for (int i = 0; i < attributeData.length; i++)
        {
            attributeData[i] = new AttributeData();
        }
        
        
        // Start with all vertices marked as holes (boundaries).
        // Only vertices decoded with TOPOLOGYC symbol (and the initial face) will
        // be marked as non hole vertices. We need to allocate the array larger
        // because split symbols can create extra vertices during the decoding
        // process (these extra vertices are then eliminated during deduplication).
        this.isVertHole = new boolean[numEncodedVertices + numEncodedSplitSymbols];
        for (int i = 0; i < isVertHole.length; i++)
        {
            isVertHole[i] = true;
        }
        
        int topologySplitDecodedBytes = -1;
        if (decoder.getBitstreamVersion() < 22)
        {
            int encodedConnectivitySize;
            if (decoder.getBitstreamVersion() < 20)
            {
                if (!decoder.getBuffer().decode5(ref14))
                {
                    encodedConnectivitySize = ref14[0];
                    return DracoUtils.failed();
                }
                else
                {
                    encodedConnectivitySize = ref14[0];
                }
                
            }
            else if (!Decoding.decodeVarint(ref15, decoder.getBuffer()))
            {
                encodedConnectivitySize = ref15[0];
                return DracoUtils.failed();
            }
            else
            {
                encodedConnectivitySize = ref15[0];
            }
            
            
            if (encodedConnectivitySize == 0 || ((0xffffffffl & encodedConnectivitySize) > decoder.getBuffer().getRemainingSize()))
                return DracoUtils.failed();
            DecoderBuffer eventBuffer = decoder.getBuffer().subBuffer(encodedConnectivitySize);// new DecoderBuffer(buf, decoder.Buffer. decoder.Buffer.DecodedSize + encodedConnectivitySize, decoder.Buffer.BufferSize - decoder.Buffer.DecodedSize - encodedConnectivitySize);
            
            
            // Decode hole and topology split events.
            topologySplitDecodedBytes = this.decodeHoleAndTopologySplitEvents(eventBuffer);
            if (topologySplitDecodedBytes == -1)
                return DracoUtils.failed();
        }
        else if (this.decodeHoleAndTopologySplitEvents(decoder.getBuffer()) == -1)
            return DracoUtils.failed();
        
        traversalDecoder.init(this);
        traversalDecoder.setNumEncodedVertices((int)(numEncodedVertices + numEncodedSplitSymbols));
        traversalDecoder.setNumAttributeData(0xff & numAttributeData);
        DecoderBuffer traversalEndBuffer;
        if (!traversalDecoder.start(ref16))
        {
            traversalEndBuffer = ref16[0];
            return DracoUtils.failed();
        }
        else
        {
            traversalEndBuffer = ref16[0];
        }
        
        int numConnectivityVerts = this.decodeConnectivity(numEncodedSymbols);
        if (numConnectivityVerts == -1)
            return DracoUtils.failed();
        
        // Set the main buffer to the end of the traversal.
        decoder.setBuffer(traversalEndBuffer.subBuffer(0));
        // .Initialize(traversalEndBuffer.GetBuffer(), traversalEndBuffer.  traversalEndBuffer.remainingCount);
        
        // Skip topology split data that was already decoded earlier.
        
        if (decoder.getBitstreamVersion() < 22)
        {
            // Skip topology split data that was already decoded earlier.
            decoder.getBuffer().advance(topologySplitDecodedBytes);
        }
        
        
        // Decode connectivity of non-position attributes.
        if (attributeData.length > 0)
        {
            if (decoder.getBitstreamVersion() < 21)
            {
                for (int ci = 0; ci < cornerTable.getNumCorners(); ci += 3)
                {
                    if (!this.decodeAttributeConnectivitiesOnFaceLegacy(ci))
                        return DracoUtils.failed();
                }
                
            }
            else
            {
                for (int ci = 0; ci < cornerTable.getNumCorners(); ci += 3)
                {
                    if (!this.decodeAttributeConnectivitiesOnFace(ci))
                        return DracoUtils.failed();
                }
                
            }
            
        }
        
        traversalDecoder.done();
        
        // Decode attribute connectivity.
        // Prepare data structure for decoding non-position attribute connectivites.
        for (int i = 0; i < attributeData.length; ++i)
        {
            attributeData[i].connectivityData = new MeshAttributeCornerTable(cornerTable);
            IntList corners = attributeData[i].attributeSeamCorners;
            for (int j = 0; j < corners.getCount(); j++)
            {
                int c = corners.get(j);
                attributeData[i].connectivityData.addSeamEdge(c);
            }
            
            // Recompute vertices from the newly added seam edges.
            attributeData[i].connectivityData.recomputeVertices(null, null);
        }
        
        
        //A3DUtils.Resize(posEncodingData.vertexToEncodedAttributeValueIndexMap, cornerTable.NumVertices);
        posEncodingData.vertexToEncodedAttributeValueIndexMap = new int[cornerTable.getNumVertices()];
        for (int i = 0; i < attributeData.length; ++i)
        {
            int attConnectivityVerts = attributeData[i].connectivityData.getNumVertices();
            if (attConnectivityVerts < cornerTable.getNumVertices())
            {
                attConnectivityVerts = cornerTable.getNumVertices();
            }
            
            //A3DUtils.Resize(attributeData[i].encodingData.vertexToEncodedAttributeValueIndexMap, attConnectivityVerts);
            attributeData[i].encodingData.vertexToEncodedAttributeValueIndexMap = new int[attConnectivityVerts];
        }
        
        if (!this.assignPointsToCorners())
            return DracoUtils.failed();
        return true;
    }
    
    @Override
    public boolean onAttributesDecoded()
    {
        return true;
    }
    
    @Override
    public MeshEdgeBreakerDecoder getDecoder()
    {
        return decoder;
    }
    
    @Override
    public CornerTable getCornerTable()
    {
        return cornerTable;
    }
    
    private boolean assignPointsToCorners()
    {
        // Map between the existing and deduplicated point ids.
        // Note that at this point we have one point id for each corner of the
        // mesh so there is cornerTable.numCorners() point ids.
        decoder.getMesh().setNumFaces(cornerTable.getNumFaces());
        int[] face = new int[3];
        
        if (attributeData.length == 0)
        {
            int numPoints = 0;
            int[] vertexToPointMap;
            vertexToPointMap = new int[cornerTable.getNumVertices()];
            DracoUtils.fill(vertexToPointMap, -1);
            // Add faces.
            for (int f = 0; f < decoder.getMesh().getNumFaces(); ++f)
            {
                for (int c = 0; c < 3; ++c)
                {
                    int vertId = cornerTable.vertex(3 * f + c);
                    int pointId = vertexToPointMap[vertId];
                    if (pointId == -1)
                    {
                        final int tmp17 = numPoints++;
                        vertexToPointMap[vertId] = tmp17;
                        pointId = tmp17;
                    }
                    
                    face[c] = pointId;
                }
                
                decoder.getMesh().setFace(f, face);
            }
            
            decoder.getPointCloud().setNumPoints(numPoints);
            return true;
        }
        
        IntList pointToCornerMap = new IntList();
        int[] cornerToPointMap = new int[cornerTable.getNumCorners()];// A3DUtils.NewArray<int>(cornerTable.NumCorners, 0);
        
        for (int v = 0; v < cornerTable.getNumVertices(); ++v)
        {
            int c = cornerTable.leftMostCorner(v);
            if (c < 0)
                continue;
            // Isolated vertex.
            int deduplicationFirstCorner = c;
            if (isVertHole[v])
            {
                // If the vertex is on a boundary, start deduplication from the left most
                // corner that is guaranteed to lie on the boundary.
                deduplicationFirstCorner = c;
            }
            else
            {
                // If we are not on the boundary we need to find the first seam (of any
                // attribute).
                for (int i = 0; i < attributeData.length; ++i)
                {
                    if (!attributeData[i].connectivityData.isCornerOnSeam(c))
                        continue;
                    // No seam for this attribute, ignore it.
                    int vertId = attributeData[i].connectivityData.vertex(c);
                    int actC = cornerTable.swingRight(c);
                    boolean seamFound = false;
                    while (actC != c)
                    {
                        if (attributeData[i].connectivityData.vertex(actC) != vertId)
                        {
                            // Attribute seam found. Stop.
                            deduplicationFirstCorner = actC;
                            seamFound = true;
                            break;
                        }
                        
                        actC = cornerTable.swingRight(actC);
                    }
                    
                    if (seamFound)
                        break;
                    // No reason to process other attributes if we found a seam.
                }
                
            }
            
            
            // Do a deduplication pass over the corners on the processed vertex.
            // At this point each corner corresponds to one point id and our goal is to
            // merge similar points into a single point id.
            // We do one one pass in a clocwise direction over the corners and we add
            // a new point id whenever one of the attributes change.
            c = deduplicationFirstCorner;
            // Create a new point.
            cornerToPointMap[c] = pointToCornerMap.getCount();
            pointToCornerMap.add(c);
            int prevC = c;
            c = cornerTable.swingRight(c);
            while (c >= 0 && (c != deduplicationFirstCorner))
            {
                boolean attributeSeam = false;
                for (int i = 0; i < attributeData.length; ++i)
                {
                    if (attributeData[i].connectivityData.vertex(c) != attributeData[i].connectivityData.vertex(prevC))
                    {
                        // Attribute index changed from the previous corner. We need to add a
                        // new point here.
                        attributeSeam = true;
                        break;
                    }
                    
                }
                
                if (attributeSeam)
                {
                    cornerToPointMap[c] = pointToCornerMap.getCount();
                    pointToCornerMap.add(c);
                }
                else
                {
                    cornerToPointMap[c] = cornerToPointMap[prevC];
                }
                
                prevC = c;
                c = cornerTable.swingRight(c);
            }
            
        }
        
        // Add faces.
        for (int f = 0; f < decoder.getMesh().getNumFaces(); ++f)
        {
            for (int c = 0; c < 3; ++c)
            {
                // Remap old points to the new ones.
                face[c] = cornerToPointMap[3 * f + c];
            }
            
            decoder.getMesh().setFace(f, face);
        }
        
        decoder.getPointCloud().setNumPoints(pointToCornerMap.getCount());
        return true;
    }
    
    int decodeHoleAndTopologySplitEvents(DecoderBuffer decoderBuffer)
    {
        int numTopologySplits;
        final int[] ref18 = new int[1];
        final int[] ref19 = new int[1];
        final int[] ref20 = new int[1];
        final int[] ref21 = new int[1];
        final byte[] ref22 = new byte[1];
        final int[] ref23 = new int[1];
        final int[] ref24 = new int[1];
        final long[] ref25 = new long[1];
        final int[] ref26 = new int[1];
        final int[] ref27 = new int[1];
        final int[] ref28 = new int[1];
        final int[] ref29 = new int[1];
        final int[] ref30 = new int[1];
        final int[] ref31 = new int[1];
        if (decoder.getBitstreamVersion() < 20)
        {
            if (!decoderBuffer.decode5(ref18))
            {
                numTopologySplits = ref18[0];
                return -1;
            }
            else
            {
                numTopologySplits = ref18[0];
            }
            
        }
        else if (!Decoding.decodeVarint(ref19, decoderBuffer))
        {
            numTopologySplits = ref19[0];
            return -1;
        }
        else
        {
            numTopologySplits = ref19[0];
        }
        
        
        if ((0xffffffffl & numTopologySplits) > 0)
        {
            if ((0xffffffffl & numTopologySplits) > cornerTable.getNumFaces())
                return -1;
            
            if (decoder.getBitstreamVersion() < 12)
            {
                for (int i = 0; i < (0xffffffffl & numTopologySplits); ++i)
                {
                    TopologySplitEventData eventData = new TopologySplitEventData();
                    if (!decoderBuffer.decode6(ref20))
                    {
                        eventData.splitSymbolId = ref20[0];
                        return -1;
                    }
                    else
                    {
                        eventData.splitSymbolId = ref20[0];
                    }
                    
                    if (!decoderBuffer.decode6(ref21))
                    {
                        eventData.sourceSymbolId = ref21[0];
                        return -1;
                    }
                    else
                    {
                        eventData.sourceSymbolId = ref21[0];
                    }
                    
                    byte edgeData;
                    if (!decoderBuffer.decode3(ref22))
                    {
                        edgeData = ref22[0];
                        return -1;
                    }
                    else
                    {
                        edgeData = ref22[0];
                    }
                    
                    eventData.sourceEdge = (byte)(0xff & edgeData & 1);
                    eventData.splitEdge = (byte)((0xff & edgeData) >>> 1 & 1);
                    topologySplitData.add(eventData);
                }
                
            }
            else
            {
                int last_source_symbol_id = 0;
                for (int i = 0; i < (0xffffffffl & numTopologySplits); ++i)
                {
                    TopologySplitEventData event_data = new TopologySplitEventData();
                    int delta;
                    Decoding.decodeVarint(ref23, decoderBuffer);
                    delta = ref23[0];
                    event_data.sourceSymbolId = (int)(delta + last_source_symbol_id);
                    Decoding.decodeVarint(ref24, decoderBuffer);
                    delta = ref24[0];
                    if ((0xffffffffl & delta) > event_data.sourceSymbolId)
                        return -1;
                    event_data.splitSymbolId = event_data.sourceSymbolId - delta;
                    last_source_symbol_id = event_data.sourceSymbolId;
                    topologySplitData.add(event_data);
                }
                
                long tmp;
                decoderBuffer.startBitDecoding(false, ref25);
                tmp = ref25[0];
                for (int i = 0; i < (0xffffffffl & numTopologySplits); ++i)
                {
                    int edge_data;
                    if (decoder.getBitstreamVersion() < 22)
                    {
                        decoderBuffer.decodeLeastSignificantBits32(2, ref26);
                        edge_data = ref26[0];
                    }
                    else
                    {
                        decoderBuffer.decodeLeastSignificantBits32(1, ref27);
                        edge_data = ref27[0];
                    }
                    
                    
                    topologySplitData.get(i).sourceEdge = (byte)(edge_data & 1);
                }
                
                
                decoderBuffer.endBitDecoding();
            }
            
        }
        
        int numHoleEvents = 0;
        if (decoder.getBitstreamVersion() < 20)
        {
            if (!decoderBuffer.decode5(ref28))
            {
                numHoleEvents = ref28[0];
                return -1;
            }
            else
            {
                numHoleEvents = ref28[0];
            }
            
        }
        else if (decoder.getBitstreamVersion() < 21)
        {
            if (!Decoding.decodeVarint(ref29, decoderBuffer))
            {
                numHoleEvents = ref29[0];
                return -1;
            }
            else
            {
                numHoleEvents = ref29[0];
            }
            
        }
        
        
        if ((0xffffffffl & numHoleEvents) > 0)
        {
            if (decoder.getBitstreamVersion() < 12)
            {
                for (int i = 0; (0xffffffffl & i) < (0xffffffffl & numHoleEvents); ++i)
                {
                    HoleEventData eventData = new HoleEventData();
                    if (!decoderBuffer.decode6(ref30))
                    {
                        eventData.symbolId = ref30[0];
                        return -1;
                    }
                    else
                    {
                        eventData.symbolId = ref30[0];
                    }
                    
                    holeEventData.add(Struct.byVal(eventData));
                }
                
            }
            else
            {
                int last_symbol_id = 0;
                for (int i = 0; i < (0xffffffffl & numHoleEvents); ++i)
                {
                    HoleEventData event_data = new HoleEventData();
                    int delta;
                    Decoding.decodeVarint(ref31, decoderBuffer);
                    delta = ref31[0];
                    event_data.symbolId = (int)(delta + last_symbol_id);
                    last_symbol_id = event_data.symbolId;
                    holeEventData.add(Struct.byVal(event_data));
                }
                
            }
            
        }
        
        
        return decoderBuffer.getDecodedSize();
    }
    
    private int decodeConnectivity(int numSymbols)
    {
        IntList activeCornerStack = new IntList();
        HashMap<Integer, Integer> topologySplitActiveCorners = new HashMap<Integer, Integer>();
        boolean removeInvalidVertices = attributeData.length == 0;
        IntList invalidVertices = new IntList();
        int maxNumVertices = isVertHole.length;
        int numFaces = 0;
        final Integer[] ref32 = new Integer[1];
        final byte[] ref34 = new byte[1];
        final int[] ref35 = new int[1];
        for (int symbolId = 0; symbolId < numSymbols; ++symbolId)
        {
            int face = numFaces++;
            boolean checkTopologySplit = false;
            int symbol = traversalDecoder.decodeSymbol();
            if (symbol == EdgeBreakerTopologyBitPattern.C)
            {
                // Create a new face between two edges on the open boundary.
                // The first edge is opposite to the corner "a" from the image below.
                // The other edge is opposite to the corner "b" that can be reached
                // through a CCW traversal around the vertex "v".
                // One new active boundary edge is created, opposite to the new corner
                // "x".
                //
                //     *-------*
                //    / \     / \
                //   /   \   /   \
                //  /     \ /     \
                // *-------v-------*
                //  \b    /x\    a/
                //   \   /   \   /
                //    \ /  C  \ /
                //     *.......*
                // Find the corner "b" from the corner "a" which is the corner on the
                // top of the active stack.
                if (activeCornerStack.getCount() == 0)
                    return -1;
                int cornerA = activeCornerStack.get(activeCornerStack.getCount() - 1);
                int vertexX = cornerTable.vertex(cornerTable.next(cornerA));
                int cornerB = cornerTable.next(cornerTable.leftMostCorner(vertexX));
                int corner = 3 * face;
                // Update opposite corner mappings.
                this.setOppositeCorners(cornerA, corner + 1);
                this.setOppositeCorners(cornerB, corner + 2);
                
                // Update vertex mapping.
                cornerTable.mapCornerToVertex(corner, vertexX);
                cornerTable.mapCornerToVertex(corner + 1, cornerTable.vertex(cornerTable.next(cornerB)));
                int vert_a_prev = cornerTable.vertex(cornerTable.previous(cornerA));
                cornerTable.mapCornerToVertex(corner + 2, vert_a_prev);
                cornerTable.setLeftMostCorner(vert_a_prev, corner + 2);
                // Mark the vertex |x| as interior.
                isVertHole[vertexX] = false;
                // Update the corner on the active stack.
                activeCornerStack.set(activeCornerStack.getCount() - 1, corner);
            }
            else if (symbol == EdgeBreakerTopologyBitPattern.R || (symbol == EdgeBreakerTopologyBitPattern.L))
            {
                
                // Create a new face extending from the open boundary edge opposite to the
                // corner "a" from the image below. Two new boundary edges are created
                // opposite to corners "r" and "l". New active corner is set to either "r"
                // or "l" depending on the decoded symbol. One new vertex is created
                // at the opposite corner to corner "a".
                //     *-------*
                //    /a\     / \
                //   /   \   /   \
                //  /     \ /     \
                // *-------v-------*
                //  .l   r.
                //   .   .
                //    . .
                //     *
                if (activeCornerStack.getCount() == 0)
                    return -1;
                int corner_a = activeCornerStack.get(activeCornerStack.getCount() - 1);
                int corner = 3 * face;
                int opp_corner;
                int corner_l;
                int corner_r;
                if (symbol == EdgeBreakerTopologyBitPattern.R)
                {
                    // "r" is the new first corner.
                    opp_corner = corner + 2;
                    corner_l = corner + 1;
                    corner_r = corner;
                }
                else
                {
                    // "l" is the new first corner.
                    opp_corner = corner + 1;
                    corner_l = corner;
                    corner_r = corner + 2;
                }
                
                
                this.setOppositeCorners(opp_corner, corner_a);
                int new_vert_index = cornerTable.addNewVertex();
                
                if (cornerTable.getNumVertices() > maxNumVertices)
                    return -1;
                // Unexpected number of decoded vertices.
                
                cornerTable.mapCornerToVertex(opp_corner, new_vert_index);
                cornerTable.setLeftMostCorner(new_vert_index, opp_corner);
                int vertex_r = cornerTable.vertex(cornerTable.previous(corner_a));
                cornerTable.mapCornerToVertex(corner_r, vertex_r);
                // Update left-most corner on the vertex on the |corner_r|.
                cornerTable.setLeftMostCorner(vertex_r, corner_r);
                
                cornerTable.mapCornerToVertex(corner_l, cornerTable.vertex(cornerTable.next(corner_a)));
                activeCornerStack.set(activeCornerStack.getCount() - 1, corner);
                checkTopologySplit = true;
            }
            else if (symbol == EdgeBreakerTopologyBitPattern.S)
            {
                // Create a new face that merges two last active edges from the active
                // stack. No new vertex is created, but two vertices at corners "p" and
                // "n" need to be merged into a single vertex.
                //
                // *-------v-------*
                //  \a   p/x\n   b/
                //   \   /   \   /
                //    \ /  S  \ /
                //     *.......*
                //
                if (activeCornerStack.getCount() == 0)
                    return -1;
                int corner_b = activeCornerStack.get(activeCornerStack.getCount() - 1);
                activeCornerStack.removeAt(activeCornerStack.getCount() - 1);
                int tmp;
                if (AsposeUtils.tryGetValue(topologySplitActiveCorners, symbolId, ref32))
                {
                    tmp = ref32[0] == null ? 0 : ref32[0];
                    // Topology split event. Move the retrieved edge to the stack.
                    activeCornerStack.add(tmp);
                }
                else
                {
                    tmp = ref32[0] == null ? 0 : ref32[0];
                }
                
                
                if (activeCornerStack.getCount() == 0)
                    return -1;
                int corner_a = activeCornerStack.get(activeCornerStack.getCount() - 1);
                final boolean tmp33 = cornerTable.opposite(corner_a) != CornerTable.K_INVALID_CORNER_INDEX;
                
                if (tmp33 || (cornerTable.opposite(corner_b) != CornerTable.K_INVALID_CORNER_INDEX))
                    return -1;
                int corner = 3 * face;
                // Update the opposite corner mapping.
                this.setOppositeCorners(corner_a, corner + 2);
                this.setOppositeCorners(corner_b, corner + 1);
                int vertex_p = cornerTable.vertex(cornerTable.previous(corner_a));
                cornerTable.mapCornerToVertex(corner, vertex_p);
                cornerTable.mapCornerToVertex(corner + 1, cornerTable.vertex(cornerTable.next(corner_a)));
                int vert_b_prev = cornerTable.vertex(cornerTable.previous(corner_b));
                cornerTable.mapCornerToVertex(corner + 2, vert_b_prev);
                cornerTable.setLeftMostCorner(vert_b_prev, corner + 2);
                int corner_n = cornerTable.next(corner_b);
                int vertex_n = cornerTable.vertex(corner_n);
                traversalDecoder.mergeVertices(vertex_p, vertex_n);
                // Update the left most corner on the newly merged vertex.
                cornerTable.setLeftMostCorner(vertex_p, cornerTable.leftMostCorner(vertex_n));
                
                // Also update the vertex id at corner "n" and all corners that are
                // connected to it in the CCW direction.
                while (corner_n != CornerTable.K_INVALID_CORNER_INDEX)
                {
                    cornerTable.mapCornerToVertex(corner_n, vertex_p);
                    corner_n = cornerTable.swingLeft(corner_n);
                }
                
                
                // Make sure the old vertex n is now mapped to an invalid corner (make it
                // isolated).
                cornerTable.makeVertexIsolated(vertex_n);
                if (removeInvalidVertices)
                {
                    invalidVertices.add(vertex_n);
                }
                
                activeCornerStack.set(activeCornerStack.getCount() - 1, corner);
            }
            else if (symbol == EdgeBreakerTopologyBitPattern.E)
            {
                int corner = 3 * face;
                int firstVertIdx = cornerTable.addNewVertex();
                // Create three new vertices at the corners of the new face.
                cornerTable.mapCornerToVertex(corner, firstVertIdx);
                cornerTable.mapCornerToVertex(corner + 1, cornerTable.addNewVertex());
                cornerTable.mapCornerToVertex(corner + 2, cornerTable.addNewVertex());
                
                if (cornerTable.getNumVertices() > maxNumVertices)
                    return -1;
                // Unexpected number of decoded vertices.
                
                cornerTable.setLeftMostCorner(firstVertIdx, corner);
                cornerTable.setLeftMostCorner(firstVertIdx + 1, corner + 1);
                cornerTable.setLeftMostCorner(firstVertIdx + 2, corner + 2);
                // Add the tip corner to the active stack.
                activeCornerStack.add(corner);
                checkTopologySplit = true;
            }
            else
                return -1;
            
            // Inform the traversal decoder that a new corner has been reached.
            traversalDecoder.newActiveCornerReached(activeCornerStack.get(activeCornerStack.getCount() - 1));
            
            if (checkTopologySplit)
            {
                int encoder_symbol_id = numSymbols - symbolId - 1;
                byte split_edge;
                int encoderSplitSymbolId;
                while (this.isTopologySplit(encoder_symbol_id, ref34, ref35))
                {
                    split_edge = ref34[0];
                    encoderSplitSymbolId = ref35[0];
                    if (encoderSplitSymbolId < 0)
                        return -1;
                    // Wrong split symbol id.
                    int act_top_corner = activeCornerStack.getBack();
                    int new_active_corner;
                    if (split_edge == EdgeFaceName.RIGHT_FACE_EDGE)
                    {
                        new_active_corner = cornerTable.next(act_top_corner);
                    }
                    else
                    {
                        new_active_corner = cornerTable.previous(act_top_corner);
                    }
                    
                    int decoderSplitSymbolId = numSymbols - encoderSplitSymbolId - 1;
                    topologySplitActiveCorners.put(decoderSplitSymbolId, new_active_corner);
                }
                
                split_edge = ref34[0];
                encoderSplitSymbolId = ref35[0];
            }
            
        }
        
        
        if (cornerTable.getNumVertices() > maxNumVertices)
            return -1;
        // Unexpected number of decoded vertices.
        // Decode start faces and connect them to the faces from the active stack.
        while (activeCornerStack.getCount() > 0)
        {
            int corner = activeCornerStack.getBack();
            activeCornerStack.popBack();
            boolean interior_face = traversalDecoder.decodeStartFaceConfiguration();
            if (interior_face)
            {
                // The start face is interior, we need to find three corners that are
                // opposite to it. The first opposite corner "a" is the corner from the
                // top of the active corner stack and the remaining two corners "b" and
                // "c" are then the next corners from the left-most corners of vertices
                // "n" and "x" respectively.
                //
                //           *-------*
                //          / \     / \
                //         /   \   /   \
                //        /     \ /     \
                //       *-------p-------*
                //      / \a    . .    c/ \
                //     /   \   .   .   /   \
                //    /     \ .  I  . /     \
                //   *-------n.......x------*
                //    \     / \     / \     /
                //     \   /   \   /   \   /
                //      \ /     \b/     \ /
                //       *-------*-------*
                //
                
                if (numFaces >= cornerTable.getNumFaces())
                    return -1;
                int corner_a = corner;
                int vert_n = cornerTable.vertex(cornerTable.next(corner_a));
                int corner_b = cornerTable.next(cornerTable.leftMostCorner(vert_n));
                int vert_x = cornerTable.vertex(cornerTable.next(corner_b));
                int corner_c = cornerTable.next(cornerTable.leftMostCorner(vert_x));
                int vert_p = cornerTable.vertex(cornerTable.next(corner_c));
                int face = numFaces++;
                int new_corner = 3 * face;
                this.setOppositeCorners(new_corner, corner);
                this.setOppositeCorners(new_corner + 1, corner_b);
                this.setOppositeCorners(new_corner + 2, corner_c);
                
                // Map new corners to existing vertices.
                cornerTable.mapCornerToVertex(new_corner, vert_x);
                cornerTable.mapCornerToVertex(new_corner + 1, vert_p);
                cornerTable.mapCornerToVertex(new_corner + 2, vert_n);
                
                // Mark all three vertices as interior.
                for (int ci = 0; ci < 3; ++ci)
                {
                    isVertHole[cornerTable.vertex(new_corner + ci)] = false;
                }
                
                
                initFaceConfigurations.add(true);
                initCorners.add(new_corner);
            }
            else
            {
                // The initial face wasn't interior and the traversal had to start from
                // an open boundary. In this case no new face is added, but we need to
                // keep record about the first opposite corner to this boundary.
                initFaceConfigurations.add(false);
                initCorners.add(corner);
            }
            
        }
        
        
        if (numFaces != cornerTable.getNumFaces())
            return -1;
        // Unexpected number of decoded faces.
        int num_vertices = cornerTable.getNumVertices();
        // If any vertex was marked as isolated, we want to remove it from the corner
        // table to ensure that all vertices in range <0, num_vertices> are valid.
        for (int i = 0; i < invalidVertices.getCount(); i++)
        {
            int invalidVert = invalidVertices.get(i);
            int srcVert = num_vertices - 1;
            while (cornerTable.leftMostCorner(srcVert) == CornerTable.K_INVALID_CORNER_INDEX)
            {
                // The last vertex is invalid, proceed to the previous one.
                srcVert = --num_vertices - 1;
            }
            
            
            if (srcVert < invalidVert)
                continue;
            // No need to swap anything.
            VertexCornersIterator vcit = VertexCornersIterator.fromVertex(cornerTable, srcVert);
            for (; !vcit.getEnd(); vcit.next())
            {
                int cid = vcit.getCorner();
                cornerTable.mapCornerToVertex(cid, invalidVert);
            }
            
            
            cornerTable.setLeftMostCorner(invalidVert, cornerTable.leftMostCorner(srcVert));
            
            // Make the |src_vert| invalid.
            cornerTable.makeVertexIsolated(srcVert);
            isVertHole[invalidVert] = isVertHole[srcVert];
            isVertHole[srcVert] = false;
            
            // The last vertex is now invalid.
            num_vertices--;
        }
        
        return num_vertices;
    }
    
    private void setOppositeCorners(int corner0, int corner1)
    {
        cornerTable.setOppositeCorner(corner0, corner1);
        cornerTable.setOppositeCorner(corner1, corner0);
    }
    
    /**
     *  Returns true if the current symbol was part of a topolgy split event. This
     *  means that the current face was connected to the left edge of a face
     *  encoded with the TOPOLOGYS symbol. |outSymbolEdge| can be used to
     *  identify which edge of the source symbol was connected to the TOPOLOGYS
     *  symbol.
     *
     */
    private boolean isTopologySplit(int encoderSymbolId, byte[] outFaceEdge, int[] outEncoderSplitSymbolId)
    {
        outFaceEdge[0] = EdgeFaceName.LEFT_FACE_EDGE;
        outEncoderSplitSymbolId[0] = 0;
        if (topologySplitData.isEmpty())
            return false;
        TopologySplitEventData back = topologySplitData.get(topologySplitData.size() - 1);
        if (back.sourceSymbolId > encoderSymbolId)
        {
            // Something is wrong; if the desired source symbol is greater than the
            // current encoderSymbolId, we missed it, or the input was tampered
            // (|encoderSymbolId| keeps decreasing).
            // Return invalid symbol id to notify the decoder that there was an
            // error.
            outEncoderSplitSymbolId[0] = -1;
            return true;
        }
        
        if (back.sourceSymbolId != encoderSymbolId)
            return false;
        outFaceEdge[0] = back.sourceEdge;
        outEncoderSplitSymbolId[0] = back.splitSymbolId;
        // Remove the latest split event.
        topologySplitData.remove(topologySplitData.size() - 1);
        return true;
    }
    
    /**
     *  Decodes all non-position attribute connectivities on the currently
     *  processed face.
     *
     */
    private boolean decodeAttributeConnectivitiesOnFaceLegacy(int corner)
    {
        // Three corners of the face.
        this.decodeAttributeConnectivitiesOnFaceLegacyImpl(corner);
        this.decodeAttributeConnectivitiesOnFaceLegacyImpl(cornerTable.next(corner));
        this.decodeAttributeConnectivitiesOnFaceLegacyImpl(cornerTable.previous(corner));
        
        return true;
    }
    
    private void decodeAttributeConnectivitiesOnFaceLegacyImpl(int corner)
    {
        {
            int oppCorner = cornerTable.opposite(corner);
            if (oppCorner < 0)
            {
                // Don't decode attribute seams on boundary edges (every boundary edge
                // is automatically an attribute seam).
                for (int i = 0; i < attributeData.length; ++i)
                {
                    attributeData[i].attributeSeamCorners.add(corner);
                }
                
                return;
            }
            
            
            for (int i = 0; i < attributeData.length; ++i)
            {
                boolean isSeam = traversalDecoder.decodeAttributeSeam(i);
                if (isSeam)
                {
                    attributeData[i].attributeSeamCorners.add(corner);
                }
                
            }
            
        }
        
    }
    
    private boolean decodeAttributeConnectivitiesOnFace(int corner)
    {
        int src_face_id = cornerTable.face(corner);
        this.decodeAttributeConnectivitiesOnFace(corner, src_face_id);
        this.decodeAttributeConnectivitiesOnFace(cornerTable.next(corner), src_face_id);
        this.decodeAttributeConnectivitiesOnFace(cornerTable.previous(corner), src_face_id);
        return true;
    }
    
    private void decodeAttributeConnectivitiesOnFace(int corner, int src_face_id)
    {
        int opp_corner = cornerTable.opposite(corner);
        if (opp_corner == CornerTable.K_INVALID_CORNER_INDEX)
        {
            // Don't decode attribute seams on boundary edges (every boundary edge
            // is automatically an attribute seam).
            for (int i = 0; i < attributeData.length; ++i)
            {
                attributeData[i].attributeSeamCorners.add(corner);
            }
            
            
            return;
        }
        
        int opp_face_id = cornerTable.face(opp_corner);
        // Don't decode edges when the opposite face has been already processed.
        if (opp_face_id < src_face_id)
            return;
        
        for (int i = 0; i < attributeData.length; ++i)
        {
            boolean is_seam = traversalDecoder.decodeAttributeSeam(i);
            if (is_seam)
            {
                attributeData[i].attributeSeamCorners.add(corner);
            }
            
        }
        
    }
    
    private void $initFields$()
    {
        try
        {
            cornerTraversalStack = new IntList();
            vertexTraversalLength = new IntList();
            topologySplitData = new ArrayList<TopologySplitEventData>();
            holeEventData = new ArrayList<HoleEventData>();
            initFaceConfigurations = new ArrayList<Boolean>();
            initCorners = new IntList();
            vertexIdMap = new IntList();
            lastSymbolId = -1;
            lastVertId = -1;
            lastFaceId = -1;
            visitedFaces = new ArrayList<Boolean>();
            visitedVerts = new ArrayList<Boolean>();
            newToParentVertexMap = new HashMap<Integer, Integer>();
            processedCornerIds = new IntList();
            processedConnectivityCorners = new IntList();
            posEncodingData = new MeshAttributeIndicesEncodingData();
            posDataDecoderId = -1;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
