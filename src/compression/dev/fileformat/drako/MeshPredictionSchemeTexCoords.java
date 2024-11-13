package dev.fileformat.drako;
import dev.fileformat.drako.Convert;
import dev.fileformat.drako.IntSpan;
import java.util.ArrayList;
class MeshPredictionSchemeTexCoords extends MeshPredictionScheme
{    
    private PointAttribute posAttribute;
    private int[] entryToPointIdMap;
    private int[] predictedValue;
    private int numComponents;
    /**
     *  Encoded / decoded array of UV flips.
     *
     */
    private ArrayList<Boolean> orientations;
    public MeshPredictionSchemeTexCoords(PointAttribute attribute, PredictionSchemeTransform transform_, MeshPredictionSchemeData meshData)
    {
        super(attribute, transform_, meshData);
        this.$initFields$();
    }
    
    public int getPredictionMethod()
    {
        return PredictionSchemeMethod.TEX_COORDS_DEPRECATED;
    }
    
    public int getNumParentAttributes()
    {
        return 1;
    }
    
    @Override
    public int getParentAttributeType(int i)
    {
        return AttributeType.POSITION;
    }
    
    @Override
    public boolean setParentAttribute(PointAttribute att)
    {
        if (att.getAttributeType() != AttributeType.POSITION)
            return false;
        // Invalid attribute type.
        if (att.getComponentsCount() != 3)
            return false;
        // Currently works only for 3 component positions.
        this.posAttribute = att;
        return true;
    }
    
    @Override
    public boolean computeCorrectionValues(IntSpan inData, IntSpan outCorr, int size, int numComponents, int[] entryToPointIdMap)
    {
        this.numComponents = numComponents;
        this.entryToPointIdMap = entryToPointIdMap;
        this.predictedValue = new int[numComponents];
        IntSpan predictedValueSpan = IntSpan.wrap(predictedValue);
        this.transform_.initializeEncoding(inData, numComponents);
        // We start processing from the end because this prediction uses data from
        // previous entries that could be overwritten when an entry is processed.
        for (int p = this.meshData.dataToCornerMap.getCount() - 1; p >= 0; --p)
        {
            int cornerId = this.meshData.dataToCornerMap.get(p);
            this.computePredictedValue(true, cornerId, inData, p);
            int dstOffset = p * numComponents;
            this.transform_.computeCorrection(inData, dstOffset, predictedValueSpan, 0, outCorr, 0, dstOffset);
        }
        
        return true;
    }
    
    @Override
    public boolean computeOriginalValues(IntSpan inCorr, IntSpan outData, int size, int numComponents, int[] entryToPointIdMap)
    {
        this.numComponents = numComponents;
        this.entryToPointIdMap = entryToPointIdMap;
        this.predictedValue = new int[numComponents];
        IntSpan predictedValueSpan = IntSpan.wrap(predictedValue);
        this.transform_.initializeDecoding(numComponents);
        int cornerMapSize = this.meshData.dataToCornerMap.getCount();
        for (int p = 0; p < cornerMapSize; ++p)
        {
            int cornerId = this.meshData.dataToCornerMap.get(p);
            this.computePredictedValue(false, cornerId, outData, p);
            int dstOffset = p * numComponents;
            this.transform_.computeOriginalValue(predictedValueSpan, 0, inCorr, dstOffset, outData, dstOffset);
        }
        
        return true;
    }
    
    @Override
    public boolean encodePredictionData(EncoderBuffer buffer)
    {
        int numOrientations = orientations.size();
        buffer.encode2(numOrientations);
        boolean lastOrientation = true;
        RAnsBitEncoder encoder = new RAnsBitEncoder();
        encoder.startEncoding();
        for (int i = 0; i < orientations.size(); i++)
        {
            boolean orientation = this.orientations.get(i);
            encoder.encodeBit(orientation == lastOrientation);
            lastOrientation = orientation;
        }
        
        encoder.endEncoding(buffer);
        return super.encodePredictionData(buffer);
    }
    
    @Override
    public boolean decodePredictionData(DecoderBuffer buffer)
    {
        int numOrientations = 0;
        final int[] ref0 = new int[1];
        if (!buffer.decode6(ref0))
        {
            numOrientations = ref0[0];
            return false;
        }
        else
        {
            numOrientations = ref0[0];
        }
        
        orientations.clear();
        orientations.addAll(Convert.asList(new boolean[numOrientations]));
        boolean lastOrientation = true;
        RAnsBitDecoder decoder = new RAnsBitDecoder();
        if (!decoder.startDecoding(buffer))
            return false;
        for (int i = 0; i < numOrientations; ++i)
        {
            if (!decoder.decodeNextBit())
            {
                lastOrientation = !lastOrientation;
            }
            
            orientations.set(i, lastOrientation);
        }
        
        decoder.endDecoding();
        return super.decodePredictionData(buffer);
    }
    
    private Vector3 getPositionForEntryId(int entryId)
    {
        int pointId = entryToPointIdMap[entryId];
        Vector3 pos = posAttribute.getValueAsVector3(posAttribute.mappedIndex(pointId));
        return pos;
    }
    
    private Vector2 getTexCoordForEntryId(int entryId, IntSpan data)
    {
        int dataOffset = entryId * numComponents;
        return new Vector2(data.get(dataOffset), data.get(dataOffset + 1));
    }
    
    private void computePredictedValue(boolean isEncoder, int cornerId, IntSpan data, int dataId)
    {
        int nextCornerId = this.meshData.getCornerTable().next(cornerId);
        int prevCornerId = this.meshData.getCornerTable().previous(cornerId);
        int nextDataId;
        int prevDataId;
        int nextVertId;
        int prevVertId;
        nextVertId = this.meshData.getCornerTable().vertex(nextCornerId);
        prevVertId = this.meshData.getCornerTable().vertex(prevCornerId);
        
        nextDataId = this.meshData.vertexToDataMap[nextVertId];
        prevDataId = this.meshData.vertexToDataMap[prevVertId];
        
        if (prevDataId < dataId && (nextDataId < dataId))
        {
            Vector2 nUv = this.getTexCoordForEntryId(nextDataId, data);
            Vector2 pUv = this.getTexCoordForEntryId(prevDataId, data);
            if (Vector2.op_eq(pUv, nUv))
            {
                // We cannot do a reliable prediction on degenerated UV triangles.
                predictedValue[0] = (int)pUv.x;
                predictedValue[1] = (int)pUv.y;
                return;
            }
            
            Vector3 tipPos = this.getPositionForEntryId(dataId);
            Vector3 nextPos = this.getPositionForEntryId(nextDataId);
            Vector3 prevPos = this.getPositionForEntryId(prevDataId);
            Vector3 pn = Vector3.sub(prevPos, nextPos);
            Vector3 cn = Vector3.sub(tipPos, nextPos);
            float pnNorm2Squared = Vector3.dot(pn, pn);
            float s = Vector3.dot(pn, cn) / pnNorm2Squared;
            float t = (float)Math.sqrt(Vector3.sub(cn, Vector3.mul(pn, s)).lengthSquared() / pnNorm2Squared);
            Vector2 pnUv = Vector2.sub(pUv, nUv);
            float pnus = pnUv.x * s + nUv.x;
            float pnut = pnUv.x * t;
            float pnvs = pnUv.y * s + nUv.y;
            float pnvt = pnUv.y * t;
            Vector2 predictedUv = new Vector2();
            if (isEncoder)
            {
                Vector2 predictedUv0 = new Vector2(pnus - pnvt, pnvs + pnut);
                Vector2 predictedUv1 = new Vector2(pnus + pnvt, pnvs - pnut);
                Vector2 cUv = this.getTexCoordForEntryId(dataId, data);
                if (Vector2.sub(cUv, predictedUv0).lengthSquared() < Vector2.sub(cUv, predictedUv1).lengthSquared())
                {
                    predictedUv.copyFrom(predictedUv0);
                    orientations.add(true);
                }
                else
                {
                    predictedUv.copyFrom(predictedUv1);
                    orientations.add(false);
                }
                
            }
            else
            {
                boolean orientation = orientations.get(orientations.size() - 1);
                orientations.remove(orientations.size() - 1);
                if (orientation)
                {
                    predictedUv.copyFrom(new Vector2(pnus - pnvt, pnvs + pnut));
                }
                else
                {
                    predictedUv.copyFrom(new Vector2(pnus + pnvt, pnvs - pnut));
                }
                
            }
            
            // Round the predicted value for integer types.
            predictedValue[0] = (int)Math.floor(predictedUv.x + 0.5f);
            predictedValue[1] = (int)Math.floor(predictedUv.y + 0.5f);
            return;
        }
        
        int dataOffset = 0;
        if (prevDataId < dataId)
        {
            // Use the value on the previous corner as the prediction.
            dataOffset = prevDataId * numComponents;
        }
        
        if (nextDataId < dataId)
        {
            // Use the value on the next corner as the prediction.
            dataOffset = nextDataId * numComponents;
        }
        else if (dataId > 0)
        {
            dataOffset = (dataId - 1) * numComponents;
        }
        else
        {
            // We are encoding the first value. Predict 0.
            for (int i = 0; i < numComponents; ++i)
            {
                predictedValue[i] = 0;
            }
            
            return;
        }
        
        for (int i = 0; i < numComponents; ++i)
        {
            predictedValue[i] = data.get(dataOffset + i);
        }
        
    }
    
    private void $initFields$()
    {
        try
        {
            orientations = new ArrayList<Boolean>();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
