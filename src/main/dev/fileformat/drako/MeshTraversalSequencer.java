package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class MeshTraversalSequencer<TCornerTable extends ICornerTable> extends PointsSequencer
{    
    private ICornerTableTraverser<TCornerTable> traverser;
    private DracoMesh mesh;
    private MeshAttributeIndicesEncodingData encodingData;
    private IntList cornerOrder;
    public MeshTraversalSequencer(DracoMesh mesh, MeshAttributeIndicesEncodingData encodingData)
    {
        this.mesh = mesh;
        this.encodingData = encodingData;
    }
    
    public void setTraverser(ICornerTableTraverser<TCornerTable> t)
    {
        this.traverser = t;
    }
    
    /**
     *  Function that can be used to set an order in which the mesh corners should
     *  be processed. This is an optional flag used usually only by the encoder
     *  to match the same corner order that is going to be used by the decoder.
     *  Note that |cornerOrder| should contain only one corner per face (it can
     *  have all corners but only the first encountered corner for each face is
     *  going to be used to start a traversal). If the corner order is not set, the
     *  corners are processed sequentially based on their ids.
     *
     */
    public void setCornerOrder(IntList cornerOrder)
    {
        this.cornerOrder = cornerOrder;
    }
    
    @Override
    public void updatePointToAttributeIndexMapping(PointAttribute attribute)
        throws DrakoException
    {
        TCornerTable cornerTable = traverser.getCornerTable();
        attribute.setExplicitMapping(mesh.getNumPoints());
        int numFaces = mesh.getNumFaces();
        int numPoints = mesh.getNumPoints();
        IntSpan face = IntSpan.wrap(new int[3]);
        for (int f = 0; f < numFaces; ++f)
        {
            mesh.readFace(f, face);
            for (int p = 0; p < 3; ++p)
            {
                int pointId = face.get(p);
                int vertId = cornerTable.vertex(3 * f + p);
                int attEntryId = encodingData.vertexToEncodedAttributeValueIndexMap[vertId];
                if (attEntryId >= numPoints)
                    throw DracoUtils.failed();
                attribute.setPointMapEntry(pointId, attEntryId);
            }
            
        }
        
    }
    
    @Override
    protected void generateSequenceInternal()
        throws DrakoException
    {
        traverser.onTraversalStart();
        if (cornerOrder != null)
        {
            for (int i = 0; i < cornerOrder.getCount(); ++i)
            {
                this.processCorner(cornerOrder.get(i));
            }
            
        }
        else
        {
            int num_faces = traverser.getCornerTable().getNumFaces();
            for (int i = 0; i < num_faces; ++i)
            {
                this.processCorner(3 * i);
            }
            
        }
        
        traverser.onTraversalEnd();
    }
    
    private void processCorner(int cornerId)
        throws DrakoException
    {
        traverser.traverseFromCorner(cornerId);
    }
    
}
