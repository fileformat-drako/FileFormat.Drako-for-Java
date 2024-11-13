package dev.fileformat.drako;
class MeshPredictionSchemeData
{    
    private DracoMesh mesh;
    private ICornerTable cornerTable;
    /**
     *  Mapping between vertices and their encoding order. I.e. when an attribute
     *  entry on a given vertex was encoded.
     *
     */
    int[] vertexToDataMap;
    /**
     *  Array that stores which corner was processed when a given attribute entry
     *  was encoded or decoded.
     *
     */
    IntList dataToCornerMap;
    public MeshPredictionSchemeData(DracoMesh mesh, ICornerTable table, IntList dataToCornerMap, int[] vertexToDataMap)
    {
        this.mesh = mesh;
        this.cornerTable = table;
        this.dataToCornerMap = dataToCornerMap;
        this.vertexToDataMap = vertexToDataMap;
    }
    
    public ICornerTable getCornerTable()
    {
        return cornerTable;
    }
    
}
