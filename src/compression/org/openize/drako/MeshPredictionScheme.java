package org.openize.drako;
/**
 *  Base class for all mesh prediction schemes that use the mesh connectivity
 *  data. |MeshDataT| can be any class that provides the same interface as the
 *  PredictionSchemeMeshData class.
 *
 */
abstract class MeshPredictionScheme extends PredictionScheme
{    
    protected MeshPredictionSchemeData meshData;
    protected MeshPredictionScheme(PointAttribute attribute, PredictionSchemeTransform transform, MeshPredictionSchemeData meshData)
    {
        super(attribute, transform);
        this.meshData = meshData;
    }
    
    public boolean getInitialized()
    {
        return true;
    }
    
    protected static void getParallelogramEntries(int ci, ICornerTable table, int[] vertexToDataMap, int[] oppEntry, int[] nextEntry, int[] prevEntry)
    {
        // One vertex of the input |table| correspond to exactly one attribute value
        // entry. The |table| can be either CornerTable for per-vertex attributes,
        // or MeshAttributeCornerTable for attributes with interior seams.
        oppEntry[0] = vertexToDataMap[table.vertex(ci)];
        nextEntry[0] = vertexToDataMap[table.vertex(table.next(ci))];
        prevEntry[0] = vertexToDataMap[table.vertex(table.previous(ci))];
    }
    
}
