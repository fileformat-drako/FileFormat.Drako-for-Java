package dev.fileformat.drako;
abstract class MeshPredictionSchemeGeometricNormalPredictorBase
{    
    public PointAttribute pos_attribute_;
    public int[] entry_to_point_id_map_;
    protected MeshPredictionSchemeData mesh_data_;
    protected byte normal_prediction_mode_;
    public MeshPredictionSchemeGeometricNormalPredictorBase(MeshPredictionSchemeData meshData)
    {
        this.mesh_data_ = meshData;
    }
    
    public boolean isInitialized()
    {
        if (pos_attribute_ == null)
            return false;
        if (entry_to_point_id_map_ == null)
            return false;
        return true;
    }
    
    public abstract boolean setNormalPredictionMode(byte mode);
    
    public byte getNormalPredictionMode()
    {
        return normal_prediction_mode_;
    }
    
    protected LongVector3 getPositionForDataId(int data_id)
    {
        int point_id = entry_to_point_id_map_[data_id];
        int pos_val_id = pos_attribute_.mappedIndex(point_id);
        //long[] pos = new long[3];
        return pos_attribute_.convertValue(pos_val_id);
    }
    
    protected LongVector3 getPositionForCorner(int ci)
    {
        ICornerTable corner_table = mesh_data_.getCornerTable();
        int vert_id = corner_table.vertex(ci);
        int data_id = mesh_data_.vertexToDataMap[vert_id];
        return this.getPositionForDataId(data_id);
    }
    
    protected int[] getOctahedralCoordForDataId(int data_id, int[] data)
    {
        int data_offset = data_id * 2;
        return new int[] {data[data_offset], data[data_offset + 1]};
    }
    
    // Computes predicted octahedral coordinates on a given corner.
    // 
    public abstract void computePredictedValue(int corner_id, int[] prediction);
    
}
