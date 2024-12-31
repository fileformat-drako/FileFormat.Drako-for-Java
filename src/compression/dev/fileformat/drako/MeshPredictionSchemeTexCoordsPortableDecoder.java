package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class MeshPredictionSchemeTexCoordsPortableDecoder extends MeshPredictionScheme
{    
    MeshPredictionSchemeTexCoordsPortablePredictor predictor_;
    public MeshPredictionSchemeTexCoordsPortableDecoder(PointAttribute attribute, PredictionSchemeTransform transform, MeshPredictionSchemeData meshData)
    {
        super(attribute, transform, meshData);
        this.predictor_ = new MeshPredictionSchemeTexCoordsPortablePredictor(meshData);
    }
    
    @Override
    public void computeCorrectionValues(IntSpan in_data, IntSpan out_corr, int size, int num_components, int[] entry_to_point_id_map)
    {
        predictor_.entry_to_point_id_map_ = entry_to_point_id_map;
        this.transform_.initializeEncoding(in_data, num_components);
        // We start processing from the end because this prediction uses data from
        // previous entries that could be overwritten when an entry is processed.
        for (int p = this.meshData.dataToCornerMap.getCount() - 1; p >= 0; --p)
        {
            int corner_id = this.meshData.dataToCornerMap.get(p);
            predictor_.computePredictedValue(true, corner_id, in_data, p);
            int dst_offset = p * num_components;
            this.transform_.computeCorrection(in_data, dst_offset, IntSpan.wrap(predictor_.predicted_value_), 0, out_corr, dst_offset, 0);
        }
        
    }
    
    @Override
    public void encodePredictionData(EncoderBuffer buffer)
    {
        int num_orientations = predictor_.num_orientations();
        buffer.encode2(num_orientations);
        boolean last_orientation = true;
        RAnsBitEncoder encoder = new RAnsBitEncoder();
        encoder.startEncoding();
        for (int i = 0; i < num_orientations; ++i)
        {
            boolean orientation = predictor_.orientation(i);
            encoder.encodeBit(orientation == last_orientation);
            last_orientation = orientation;
        }
        
        
        encoder.endEncoding(buffer);
        super.encodePredictionData(buffer);
    }
    
    @Override
    public void setParentAttribute(PointAttribute att)
        throws DrakoException
    {
        if (att == null || (att.getAttributeType() != AttributeType.POSITION))
            throw DracoUtils.failed();
        // Invalid attribute type.
        if (att.getComponentsCount() != 3)
            throw DracoUtils.failed();
        // Currently works only for 3 component positions.
        predictor_.pos_attribute_ = att;
    }
    
    @Override
    public void computeOriginalValues(IntSpan inCorr, IntSpan outData, int size, int numComponents, int[] entryToPointIdMap)
        throws DrakoException
    {
        predictor_.entry_to_point_id_map_ = entryToPointIdMap;
        this.transform_.initializeDecoding(numComponents);
        int corner_map_size = this.meshData.dataToCornerMap.getCount();
        for (int p = 0; p < corner_map_size; ++p)
        {
            int corner_id = this.meshData.dataToCornerMap.get(p);
            if (!predictor_.computePredictedValue(false, corner_id, outData, p))
                throw DracoUtils.failed();
            int dst_offset = p * numComponents;
            this.transform_.computeOriginalValue(IntSpan.wrap(predictor_.predicted_value_), 0, inCorr, dst_offset, outData, dst_offset);
        }
        
    }
    
    @Override
    public void decodePredictionData(DecoderBuffer buffer)
        throws DrakoException
    {
        int num_orientations = buffer.decodeI32();
        if (num_orientations < 0)
            throw DracoUtils.failed();
        predictor_.resizeOrientations(num_orientations);
        boolean last_orientation = true;
        RAnsBitDecoder decoder = new RAnsBitDecoder();
        decoder.startDecoding(buffer);
        for (int i = 0; i < num_orientations; ++i)
        {
            if (!decoder.decodeNextBit())
            {
                last_orientation = !last_orientation;
            }
            
            predictor_.set_orientation(i, last_orientation);
        }
        
        decoder.endDecoding();
        super.decodePredictionData(buffer);
    }
    
    @Override
    public int getParentAttributeType(int i)
    {
        return AttributeType.POSITION;
    }
    
    public int getNumParentAttributes()
    {
        return 1;
    }
    
    public int getPredictionMethod()
    {
        return PredictionSchemeMethod.TEX_COORDS_PORTABLE;
    }
    
}
