package org.openize.drako;
class MeshAttributeIndicesEncodingObserver<TCornerTable extends ICornerTable>
{    
    private TCornerTable attConnectivity;
    private MeshAttributeIndicesEncodingData encodingData;
    private DracoMesh mesh;
    private PointsSequencer sequencer;
    public MeshAttributeIndicesEncodingObserver(TCornerTable cornerTable, DracoMesh mesh, PointsSequencer sequencer, MeshAttributeIndicesEncodingData encodingData)
    {
        this.encodingData = encodingData;
        this.mesh = mesh;
        this.attConnectivity = cornerTable;
        this.sequencer = sequencer;
    }
    
    public TCornerTable getCornerTable()
    {
        return attConnectivity;
    }
    
    public void onNewFaceVisited(int face)
    {
    }
    
    public void onNewVertexVisited(int vertex, int corner)
    {
        int pointId = mesh.readCorner(corner);
        // Append the visited attribute to the encoding order.
        sequencer.addPointId(pointId);
        
        // Keep track of visited corners.
        encodingData.encodedAttributeValueIndexToCornerMap.add(corner);
        
        encodingData.vertexToEncodedAttributeValueIndexMap[vertex] = encodingData.numValues;
        
        encodingData.numValues++;
    }
    
}
