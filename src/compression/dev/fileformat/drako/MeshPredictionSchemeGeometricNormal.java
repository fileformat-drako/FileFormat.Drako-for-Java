package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class MeshPredictionSchemeGeometricNormal extends MeshPredictionScheme
{    
    MeshPredictionSchemeGeometricNormalPredictorArea predictor_;
    RAnsBitDecoder flip_normal_bit_decoder_;
    OctahedronToolBox octahedron_tool_box_;
    public MeshPredictionSchemeGeometricNormal(PointAttribute attribute, PredictionSchemeTransform transform, MeshPredictionSchemeData meshData)
    {
        super(attribute, transform, meshData);
        this.$initFields$();
        this.predictor_ = new MeshPredictionSchemeGeometricNormalPredictorArea(meshData);
        this.octahedron_tool_box_ = new OctahedronToolBox();
        this.flip_normal_bit_decoder_ = new RAnsBitDecoder();
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
            return DracoUtils.failed();
        // Invalid attribute type.
        if (att.getComponentsCount() != 3)
            return DracoUtils.failed();
        // Currently works only for 3 component positions.
        predictor_.pos_attribute_ = att;
        return true;
    }
    
    public boolean getInitialized()
    {
        
        if (!predictor_.isInitialized())
            return DracoUtils.failed();
        //if (!meshData.Initialized)
        //    return DracoUtils.Failed();
        if (!octahedron_tool_box_.isInitialized())
            return DracoUtils.failed();
        return true;
    }
    
    @Override
    public boolean computeOriginalValues(IntSpan inCorr, IntSpan outData, int size, int numComponents, int[] entryToPointIdMap)
    {
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        octahedron_tool_box_.setQuantizationBits(((PredictionSchemeNormalOctahedronTransformBase)this.transform_).getQuantizationBits());
        predictor_.entry_to_point_id_map_ = entryToPointIdMap;
        int corner_map_size = this.meshData.dataToCornerMap.getCount();
        int[] pred_normal_3d = new int[3];
        IntSpan pred_normal_oct = IntSpan.wrap(new int[2]);
        
        for (int data_id = 0; data_id < corner_map_size; ++data_id)
        {
            int corner_id = this.meshData.dataToCornerMap.get(data_id);
            predictor_.computePredictedValue(corner_id, pred_normal_3d);
            
            // Compute predicted octahedral coordinates.
            octahedron_tool_box_.canonicalizeIntegerVector(pred_normal_3d);
            if (flip_normal_bit_decoder_.decodeNextBit())
            {
                pred_normal_3d[0] = -pred_normal_3d[0];
                pred_normal_3d[1] = -pred_normal_3d[1];
                pred_normal_3d[2] = -pred_normal_3d[2];
            }
            
            int s;
            int t;
            octahedron_tool_box_.integerVectorToQuantizedOctahedralCoords(pred_normal_3d, ref0, ref1);
            s = ref0[0];
            t = ref1[0];
            pred_normal_oct.put(0, s);
            pred_normal_oct.put(1, t);
            int data_offset = data_id * 2;
            this.transform_.computeOriginalValue(pred_normal_oct, 0, inCorr, data_offset, outData, data_offset);
        }
        
        
        flip_normal_bit_decoder_.endDecoding();
        return true;
    }
    
    @Override
    public boolean decodePredictionData(DecoderBuffer buffer)
    {
        final byte[] ref2 = new byte[1];
        if (!this.transform_.decodeTransformData(buffer))
            return DracoUtils.failed();
        
        if (buffer.getBitstreamVersion() < 22)
        {
            byte prediction_mode;
            buffer.decode3(ref2);
            prediction_mode = ref2[0];
            
            if (!predictor_.setNormalPredictionMode(prediction_mode))
                return DracoUtils.failed();
        }
        
        
        // Init normal flips.
        if (!flip_normal_bit_decoder_.startDecoding(buffer))
            return DracoUtils.failed();
        
        return true;
    }
    
    public int getPredictionMethod()
    {
        return PredictionSchemeMethod.GEOMETRIC_NORMAL;
    }
    
    private RAnsBitEncoder flip_normal_bit_encoder_;
    @Override
    public boolean computeCorrectionValues(IntSpan in_data, IntSpan out_corr, int size, int num_components, int[] entry_to_point_id_map)
    {
        final int[] ref3 = new int[1];
        final int[] ref4 = new int[1];
        final int[] ref5 = new int[1];
        final int[] ref6 = new int[1];
        
        octahedron_tool_box_.setQuantizationBits(((PredictionSchemeNormalOctahedronTransformBase)this.transform_).getQuantizationBits());
        predictor_.entry_to_point_id_map_ = entry_to_point_id_map;
        
        flip_normal_bit_encoder_.startEncoding();
        int corner_map_size = this.meshData.dataToCornerMap.getCount();
        int[] pred_normal_3d = new int[3];
        IntSpan pos_pred_normal_oct = IntSpan.wrap(new int[2]);
        IntSpan neg_pred_normal_oct = IntSpan.wrap(new int[2]);
        IntSpan pos_correction = IntSpan.wrap(new int[2]);
        IntSpan neg_correction = IntSpan.wrap(new int[2]);
        for (int data_id = 0; data_id < corner_map_size; ++data_id)
        {
            int corner_id = this.meshData.dataToCornerMap.get(data_id);
            predictor_.computePredictedValue(corner_id, pred_normal_3d);
            
            // Compute predicted octahedral coordinates.
            octahedron_tool_box_.canonicalizeIntegerVector(pred_normal_3d);
            int s;
            int t;
            octahedron_tool_box_.integerVectorToQuantizedOctahedralCoords(pred_normal_3d, ref3, ref4);
            s = ref3[0];
            t = ref4[0];
            pos_pred_normal_oct.put(0, s);
            pos_pred_normal_oct.put(1, t);
            pred_normal_3d[0] = -pred_normal_3d[0];
            pred_normal_3d[1] = -pred_normal_3d[1];
            pred_normal_3d[2] = -pred_normal_3d[2];
            octahedron_tool_box_.integerVectorToQuantizedOctahedralCoords(pred_normal_3d, ref5, ref6);
            s = ref5[0];
            t = ref6[0];
            neg_pred_normal_oct.put(0, s);
            neg_pred_normal_oct.put(1, t);
            int data_offset = data_id * 2;
            
            this.transform_.computeCorrection(in_data.slice(data_offset), pos_pred_normal_oct, pos_correction, 0);
            this.transform_.computeCorrection(in_data.slice(data_offset), neg_pred_normal_oct, neg_correction, 0);
            pos_correction.put(0, octahedron_tool_box_.modMax(pos_correction.get(0)));
            pos_correction.put(1, octahedron_tool_box_.modMax(pos_correction.get(1)));
            neg_correction.put(0, octahedron_tool_box_.modMax(neg_correction.get(0)));
            neg_correction.put(1, octahedron_tool_box_.modMax(neg_correction.get(1)));
            if (DracoUtils.absSum(pos_correction) < DracoUtils.absSum(neg_correction))
            {
                flip_normal_bit_encoder_.encodeBit(false);
                out_corr.put(data_offset, octahedron_tool_box_.makePositive(pos_correction.get(0)));
                out_corr.put(data_offset + 1, octahedron_tool_box_.makePositive(pos_correction.get(1)));
            }
            else
            {
                flip_normal_bit_encoder_.encodeBit(true);
                out_corr.put(data_offset, octahedron_tool_box_.makePositive(neg_correction.get(0)));
                out_corr.put(data_offset + 1, octahedron_tool_box_.makePositive(neg_correction.get(1)));
            }
            
        }
        
        
        return true;
    }
    
    @Override
    public boolean encodePredictionData(EncoderBuffer buffer)
    {
        
        if (!this.transform_.encodeTransformData(buffer))
            return false;
        
        // Encode normal flips.
        flip_normal_bit_encoder_.endEncoding(buffer);
        return true;
    }
    
    private void $initFields$()
    {
        try
        {
            flip_normal_bit_encoder_ = new RAnsBitEncoder();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
