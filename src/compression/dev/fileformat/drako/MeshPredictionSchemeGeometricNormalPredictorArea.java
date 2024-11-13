package dev.fileformat.drako;
class MeshPredictionSchemeGeometricNormalPredictorArea extends MeshPredictionSchemeGeometricNormalPredictorBase
{    
    public MeshPredictionSchemeGeometricNormalPredictorArea(MeshPredictionSchemeData meshData)
    {
        super(meshData);
        this.setNormalPredictionMode(NormalPredictionMode.TRIANGLE_AREA);
    }
    
    // Computes predicted octahedral coordinates on a given corner.
    // 
    @Override
    public void computePredictedValue(int corner_id, int[] prediction)
    {
        ICornerTable corner_table = this.mesh_data_.getCornerTable();
        VertexCornersIterator cit = VertexCornersIterator.fromCorner(corner_table, corner_id);
        LongVector3 pos_cent = this.getPositionForCorner(corner_id);
        LongVector3 normal = new LongVector3();
        int c_next;
        int c_prev;
        while (!cit.getEnd())
        {
            // Getting corners.
            if (this.normal_prediction_mode_ == NormalPredictionMode.ONE_TRIANGLE)
            {
                c_next = corner_table.next(corner_id);
                c_prev = corner_table.previous(corner_id);
            }
            else
            {
                c_next = corner_table.next(cit.getCorner());
                c_prev = corner_table.previous(cit.getCorner());
            }
            
            LongVector3 pos_next = this.getPositionForCorner(c_next);
            LongVector3 pos_prev = this.getPositionForCorner(c_prev);
            LongVector3 delta_next = DracoUtils.sub(pos_next, pos_cent);
            LongVector3 delta_prev = DracoUtils.sub(pos_prev, pos_cent);
            LongVector3 cross = DracoUtils.crossProduct(delta_next, delta_prev);
            normal.copyFrom(DracoUtils.add(normal, cross));
            cit.next();
        }
        
        long upper_bound = 1 << 29;
        if (this.normal_prediction_mode_ == NormalPredictionMode.ONE_TRIANGLE)
        {
            int abs_sum = (int)DracoUtils.absSum(normal);
            if (abs_sum > upper_bound)
            {
                long quotient = abs_sum / upper_bound;
                normal.copyFrom(DracoUtils.div(normal, quotient));
            }
            
        }
        else
        {
            long abs_sum = DracoUtils.absSum(normal);
            if (abs_sum > upper_bound)
            {
                long quotient = abs_sum / upper_bound;
                normal.copyFrom(DracoUtils.div(normal, quotient));
            }
            
        }
        
        
        prediction[0] = (int)normal.x;
        prediction[1] = (int)normal.y;
        prediction[2] = (int)normal.z;
    }
    
    @Override
    public boolean setNormalPredictionMode(byte mode)
    {
        if (mode == NormalPredictionMode.ONE_TRIANGLE)
        {
            this.normal_prediction_mode_ = mode;
            return true;
        }
        else if (mode == NormalPredictionMode.TRIANGLE_AREA)
        {
            this.normal_prediction_mode_ = mode;
            return true;
        }
        
        
        return false;
    }
    
}
