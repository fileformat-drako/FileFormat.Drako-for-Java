package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class MeshPredictionSchemeMultiParallelogram extends MeshPredictionScheme
{    
    public MeshPredictionSchemeMultiParallelogram(PointAttribute attribute, PredictionSchemeTransform transform, MeshPredictionSchemeData meshData)
    {
        super(attribute, transform, meshData);
    }
    
    public int getPredictionMethod()
    {
        return PredictionSchemeMethod.MULTI_PARALLELOGRAM;
    }
    
    @Override
    public boolean computeCorrectionValues(IntSpan inData, IntSpan outCorr, int size, int numComponents, int[] entryToPointIdMap)
    {
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        final int[] ref2 = new int[1];
        this.transform_.initializeEncoding(inData, numComponents);
        ICornerTable table = this.meshData.getCornerTable();
        int[] vertexToDataMap = this.meshData.vertexToDataMap;
        IntSpan predVals = IntSpan.wrap(new int[numComponents]);
        
        // We start processing from the end because this prediction uses data from
        // previous entries that could be overwritten when an entry is processed.
        for (int p = this.meshData.dataToCornerMap.getCount() - 1; p > 0; --p)
        {
            int startCornerId = this.meshData.dataToCornerMap.get(p);
            int cornerId = startCornerId;
            int numParallelograms = 0;
            for (int i = 0; i < numComponents; ++i)
            {
                predVals.put(i, 0);
            }
            
            while (cornerId >= 0)
            {
                int vertOpp = p;
                int vertNext = p;
                int vertPrev = p;
                int oppCorner = table.opposite(cornerId);
                if (oppCorner >= 0)
                {
                    ref0[0] = vertOpp;
                    ref1[0] = vertNext;
                    ref2[0] = vertPrev;
                    MeshPredictionScheme.getParallelogramEntries(oppCorner, table, vertexToDataMap, ref0, ref1, ref2);
                    vertOpp = ref0[0];
                    vertNext = ref1[0];
                    vertPrev = ref2[0];
                }
                
                if (vertOpp < p && (vertNext < p) && (vertPrev < p))
                {
                    int vOppOff = vertOpp * numComponents;
                    int vNextOff = vertNext * numComponents;
                    int vPrevOff = vertPrev * numComponents;
                    for (int c = 0; c < numComponents; ++c)
                    {
                        predVals.put(c, predVals.get(c) + (inData.get(vNextOff + c) + inData.get(vPrevOff + c) - inData.get(vOppOff + c)));
                    }
                    
                    ++numParallelograms;
                }
                
                
                // Proceed to the next corner attached to the vertex.
                // TODO(ostava): This will not go around the whole neighborhood on
                // vertices on a mesh boundary. We need to SwingLeft from the start vertex
                // again to get the full coverage.
                cornerId = table.swingRight(cornerId);
                if (cornerId == startCornerId)
                {
                    cornerId = -1;
                }
                
            }
            
            int dstOffset = p * numComponents;
            if (numParallelograms == 0)
            {
                int srcOffset = (p - 1) * numComponents;
                this.transform_.computeCorrection(inData, dstOffset, inData, srcOffset, outCorr, 0, dstOffset);
            }
            else
            {
                // Compute the correction from the predicted value.
                for (int c = 0; c < numComponents; ++c)
                {
                    predVals.put(c, predVals.get(c) / numParallelograms);
                }
                
                this.transform_.computeCorrection(inData, dstOffset, predVals, 0, outCorr, 0, dstOffset);
            }
            
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
        IntSpan predVals = IntSpan.wrap(new int[numComponents]);
        IntSpan parallelogramPredVals = IntSpan.wrap(new int[numComponents]);
        
        this.transform_.computeOriginalValue(predVals, inCorr, outData);
        ICornerTable table = this.meshData.getCornerTable();
        int[] vertexToDataMap = this.meshData.vertexToDataMap;
        int cornerMapSize = this.meshData.dataToCornerMap.getCount();
        for (int p = 1; p < cornerMapSize; ++p)
        {
            int startCornerId = this.meshData.dataToCornerMap.get(p);
            int cornerId = startCornerId;
            int numParallelograms = 0;
            for (int i = 0; i < numComponents; ++i)
            {
                predVals.put(i, 0);
            }
            
            while (cornerId != CornerTable.K_INVALID_CORNER_INDEX)
            {
                if (MeshPredictionSchemeParallelogram.computeParallelogramPrediction(p, cornerId, table, vertexToDataMap, outData, numComponents, parallelogramPredVals))
                {
                    for (int c = 0; c < numComponents; ++c)
                    {
                        predVals.put(c, predVals.get(c) + parallelogramPredVals.get(c));
                    }
                    
                    ++numParallelograms;
                }
                
                
                // Proceed to the next corner attached to the vertex.
                cornerId = table.swingRight(cornerId);
                if (cornerId == startCornerId)
                {
                    cornerId = CornerTable.K_INVALID_CORNER_INDEX;
                }
                
            }
            
            int dstOffset = p * numComponents;
            if (numParallelograms == 0)
            {
                int srcOffset = (p - 1) * numComponents;
                this.transform_.computeOriginalValue(outData, srcOffset, inCorr, dstOffset, outData, dstOffset);
            }
            else
            {
                // Compute the correction from the predicted value.
                for (int c = 0; c < numComponents; ++c)
                {
                    predVals.put(c, predVals.get(c) / numParallelograms);
                }
                
                this.transform_.computeOriginalValue(predVals, 0, inCorr, dstOffset, outData, dstOffset);
            }
            
        }
        
        return true;
    }
    
}
