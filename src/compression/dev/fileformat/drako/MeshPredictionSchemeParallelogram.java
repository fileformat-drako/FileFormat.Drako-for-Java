package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
/**
 *  Parallelogram prediction predicts an attribute value V from three vertices
 *  on the opposite face to the predicted vertex. The values on the three
 *  vertices are used to construct a parallelogram V' = O - A - B, where O is the
 *  value on the oppoiste vertex, and A, B are values on the shared vertices:
 *      V
 *     / \
 *    /   \
 *   /     \
 *  A-------B
 *   \     /
 *    \   /
 *     \ /
 *      O
 *
 */
class MeshPredictionSchemeParallelogram extends MeshPredictionScheme
{    
    public MeshPredictionSchemeParallelogram(PointAttribute attribute, PredictionSchemeTransform transform_, MeshPredictionSchemeData meshData)
    {
        super(attribute, transform_, meshData);
    }
    
    public int getPredictionMethod()
    {
        return PredictionSchemeMethod.PARALLELOGRAM;
    }
    
    @Override
    public boolean computeCorrectionValues(IntSpan inData, IntSpan outCorr, int size, int numComponents, int[] entryToPointIdMap)
    {
        this.transform_.initializeEncoding(inData, numComponents);
        IntSpan predVals = IntSpan.wrap(new int[numComponents]);
        ICornerTable table = this.meshData.getCornerTable();
        int[] vertexToDataMap = this.meshData.vertexToDataMap;
        for (int p = this.meshData.dataToCornerMap.getCount() - 1; p > 0; --p)
        {
            int cornerId = this.meshData.dataToCornerMap.get(p);
            int dst_offset = p * numComponents;
            if (!MeshPredictionSchemeParallelogram.computeParallelogramPrediction(p, cornerId, table, vertexToDataMap, inData, numComponents, predVals))
            {
                int src_offset = (p - 1) * numComponents;
                this.transform_.computeCorrection(inData, dst_offset, inData, src_offset, outCorr, dst_offset, 0);
            }
            else
            {
                // Apply the parallelogram prediction.
                this.transform_.computeCorrection(inData, dst_offset, predVals, 0, outCorr, dst_offset, 0);
            }
            
            /*
                // Initialize the vertex ids to "p" which ensures that if the opposite            
                // corner does not exist we will not use the vertices to predict the            
                // encoded value.            
                int vertOpp = p, vertNext = p, vertPrev = p;            
                int oppCorner = table.Opposite(cornerId);            
                if (oppCorner >= 0)            
                {            
                    // Get vertices on the opposite face.            
                    GetParallelogramEntries(oppCorner, table, vertexToDataMap, ref vertOpp, ref vertNext,            
                        ref vertPrev);            
                }            
                int dstOffset = p * numComponents;            
                if (vertOpp >= p || vertNext >= p || vertPrev >= p)            
                {            
                    // Some of the vertices are not valid (not encoded yet).            
                    // We use the last encoded point as a reference.            
                    int srcOffset = (p - 1) * numComponents;            
                    transform_.ComputeCorrection(inData, dstOffset, inData, srcOffset, outCorr, dstOffset, 0);            
                }            
                else            
                {            
                    // Apply the parallelogram prediction.            
                    int vOppOff = vertOpp * numComponents;            
                    int vNextOff = vertNext * numComponents;            
                    int vPrevOff = vertPrev * numComponents;            
                    for (int c = 0; c < numComponents; ++c)            
                    {            
                        predVals[c] = (inData[vNextOff + c] + inData[vPrevOff + c]) -            
                                       inData[vOppOff + c];            
                    }            
                    transform_.ComputeCorrection(inData, dstOffset, predVals, 0,            
                        outCorr, dstOffset, 0);            
                }            
                */            
        }
        
        // First element is always fixed because it cannot be predicted.
        for (int i = 0; i < numComponents; ++i)
        {
            predVals.put(i, 0);
        }
        
        this.transform_.computeCorrection(inData, predVals, outCorr, 0);
        return true;
    }
    
    @Override
    public boolean computeOriginalValues(IntSpan inCorr, IntSpan outData, int size, int numComponents, int[] entryToPointIdMap)
    {
        this.transform_.initializeDecoding(numComponents);
        ICornerTable table = this.meshData.getCornerTable();
        int[] vertexToDataMap = this.meshData.vertexToDataMap;
        IntSpan predVals = IntSpan.wrap(new int[numComponents]);
        
        // Restore the first value.
        this.transform_.computeOriginalValue(predVals, inCorr, outData);
        int cornerMapSize = this.meshData.dataToCornerMap.getCount();
        for (int p = 1; p < cornerMapSize; ++p)
        {
            int corner_id = this.meshData.dataToCornerMap.get(p);
            int dst_offset = p * numComponents;
            if (!MeshPredictionSchemeParallelogram.computeParallelogramPrediction(p, corner_id, table, vertexToDataMap, outData, numComponents, predVals))
            {
                int src_offset = (p - 1) * numComponents;
                this.transform_.computeOriginalValue(outData, src_offset, inCorr, dst_offset, outData, dst_offset);
            }
            else
            {
                // Apply the parallelogram prediction.
                this.transform_.computeOriginalValue(predVals, 0, inCorr, dst_offset, outData, dst_offset);
            }
            
        }
        
        return true;
    }
    
    // Computes parallelogram prediction for a given corner and data entry id.
    // The prediction is stored in |out_prediction|.
    // Function returns false when the prediction couldn't be computed, e.g. because
    // not all entry points were available.
    // 
    public static boolean computeParallelogramPrediction(int data_entry_id, int ci, ICornerTable table, int[] vertex_to_data_map, IntSpan in_data, int num_components, IntSpan out_prediction)
    {
        int oci = table.opposite(ci);
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        final int[] ref2 = new int[1];
        if (oci == CornerTable.K_INVALID_CORNER_INDEX)
            return false;
        int vert_opp = 0;
        int vert_next = 0;
        int vert_prev = 0;
        ref0[0] = vert_opp;
        ref1[0] = vert_next;
        ref2[0] = vert_prev;
        MeshPredictionScheme.getParallelogramEntries(oci, table, vertex_to_data_map, ref0, ref1, ref2);
        vert_opp = ref0[0];
        vert_next = ref1[0];
        vert_prev = ref2[0];
        if (vert_opp < data_entry_id && (vert_next < data_entry_id) && (vert_prev < data_entry_id))
        {
            int v_opp_off = vert_opp * num_components;
            int v_next_off = vert_next * num_components;
            int v_prev_off = vert_prev * num_components;
            for (int c = 0; c < num_components; ++c)
            {
                out_prediction.put(c, in_data.get(v_next_off + c) + in_data.get(v_prev_off + c) - in_data.get(v_opp_off + c));
            }
            
            
            return true;
        }
        
        
        return false;
        // Not all data is available for prediction
    }
    
}
