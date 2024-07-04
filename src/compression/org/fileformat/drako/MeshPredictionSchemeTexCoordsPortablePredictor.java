package org.fileformat.drako;
import com.aspose.csporter.helpers.IntSpan;
import java.util.Arrays;
class MeshPredictionSchemeTexCoordsPortablePredictor
{    
    private static final int K_NUM_COMPONENTS = 2;
    public PointAttribute pos_attribute_;
    public int[] entry_to_point_id_map_;
    public int[] predicted_value_;
    // Encoded / decoded array of UV flips.
    // TODO(ostava): We should remove this and replace this with in-place encoding
    // and decoding to avoid unnecessary copy.
    // 
    private boolean[] orientations_;
    private int orientationCount = 0;
    MeshPredictionSchemeData mesh_data_;
    public MeshPredictionSchemeTexCoordsPortablePredictor(MeshPredictionSchemeData md)
    {
        this.$initFields$();
        this.pos_attribute_ = null;
        this.entry_to_point_id_map_ = null;
        this.mesh_data_ = md;
    }
    
    boolean isInitialized()
    {
        return pos_attribute_ != null;
    }
    
    LongVector3 getPositionForEntryId(int entry_id)
    {
        int point_id = entry_to_point_id_map_[entry_id];
        LongVector3 pos = pos_attribute_.convertValue(pos_attribute_.mappedIndex(point_id));
        return pos;
    }
    
    LongVector3 getTexCoordForEntryId(int entry_id, IntSpan data)
    {
        int data_offset = entry_id * 2;
        return new LongVector3(data.get(data_offset), data.get(data_offset + 1));
    }
    
    // Computes predicted UV coordinates on a given corner. The coordinates are
    // stored in |predicted_value_| member.
    // 
    public boolean computePredictedValue(boolean is_encoder_t, int corner_id, IntSpan data, int data_id)
    {
        int next_corner_id = mesh_data_.getCornerTable().next(corner_id);
        int prev_corner_id = mesh_data_.getCornerTable().previous(corner_id);
        int next_data_id;
        int prev_data_id;
        int next_vert_id;
        int prev_vert_id;
        next_vert_id = mesh_data_.getCornerTable().vertex(next_corner_id);
        prev_vert_id = mesh_data_.getCornerTable().vertex(prev_corner_id);
        
        next_data_id = mesh_data_.vertexToDataMap[next_vert_id];
        prev_data_id = mesh_data_.vertexToDataMap[prev_vert_id];
        
        if (prev_data_id < data_id && (next_data_id < data_id))
        {
            LongVector3 n_uv = this.getTexCoordForEntryId(next_data_id, data);
            LongVector3 p_uv = this.getTexCoordForEntryId(prev_data_id, data);
            if (DracoUtils.vecEquals(p_uv, n_uv))
            {
                // We cannot do a reliable prediction on degenerated UV triangles.
                predicted_value_[0] = (int)p_uv.x;
                predicted_value_[1] = (int)p_uv.y;
                return true;
            }
            
            LongVector3 tip_pos = this.getPositionForEntryId(data_id);
            LongVector3 next_pos = this.getPositionForEntryId(next_data_id);
            LongVector3 prev_pos = this.getPositionForEntryId(prev_data_id);
            LongVector3 pn = DracoUtils.sub(prev_pos, next_pos);
            int pn_norm2_squared = DracoUtils.squaredNorm(pn);
            if ((0xffffffffl & pn_norm2_squared) != 0)
            {
                LongVector3 cn = DracoUtils.sub(tip_pos, next_pos);
                long cn_dot_pn = DracoUtils.dot(pn, cn);
                LongVector3 pn_uv = DracoUtils.sub(p_uv, n_uv);
                LongVector3 x_uv = DracoUtils.add(DracoUtils.mul(n_uv, 0xffffffffl & pn_norm2_squared), DracoUtils.mul(pn_uv, cn_dot_pn));
                LongVector3 x_pos = DracoUtils.add(next_pos, DracoUtils.div(DracoUtils.mul(pn, cn_dot_pn), 0xffffffffl & pn_norm2_squared));
                long cx_norm2_squared = 0xffffffffl & DracoUtils.squaredNorm(DracoUtils.sub(tip_pos, x_pos));
                LongVector3 cx_uv = new LongVector3(pn_uv.y, -pn_uv.x);// Rotated PN_UV.
                
                int norm_squared = (int)DracoUtils.intSqrt((long)(cx_norm2_squared * (0xffffffffl & pn_norm2_squared)));
                // Final cx_uv in the scaled coordinate space.
                cx_uv.copyFrom(DracoUtils.mul(cx_uv, 0xffffffffl & norm_squared));
                LongVector3 predicted_uv = new LongVector3();
                if (is_encoder_t)
                {
                    LongVector3 predicted_uv_0 = DracoUtils.div(DracoUtils.add(x_uv, cx_uv), 0xffffffffl & pn_norm2_squared);
                    LongVector3 predicted_uv_1 = DracoUtils.div(DracoUtils.sub(x_uv, cx_uv), 0xffffffffl & pn_norm2_squared);
                    LongVector3 c_uv = this.getTexCoordForEntryId(data_id, data);
                    if (orientationCount == orientations_.length)
                    {
                        orientations_ = orientations_ == null ? new boolean[orientations_.length + (orientations_.length >> 1)] : Arrays.copyOf(orientations_, orientations_.length + (orientations_.length >> 1));
                    }
                    
                    if ((0xffffffffl & DracoUtils.squaredNorm(DracoUtils.sub(c_uv, predicted_uv_0))) < (0xffffffffl & DracoUtils.squaredNorm(DracoUtils.sub(c_uv, predicted_uv_1))))
                    {
                        predicted_uv.copyFrom(predicted_uv_0);
                        orientations_[orientationCount++] = true;
                    }
                    else
                    {
                        predicted_uv.copyFrom(predicted_uv_1);
                        orientations_[orientationCount++] = false;
                    }
                    
                }
                else
                {
                    // When decoding the data, we already know which orientation to use.
                    if (orientationCount == 0)
                        return false;
                    boolean orientation = orientations_[orientationCount - 1];
                    orientationCount--;
                    if (orientation)
                    {
                        predicted_uv.copyFrom(DracoUtils.div(DracoUtils.add(x_uv, cx_uv), 0xffffffffl & pn_norm2_squared));
                    }
                    else
                    {
                        predicted_uv.copyFrom(DracoUtils.div(DracoUtils.sub(x_uv, cx_uv), 0xffffffffl & pn_norm2_squared));
                    }
                    
                }
                
                
                predicted_value_[0] = (int)predicted_uv.x;
                predicted_value_[1] = (int)predicted_uv.y;
                return true;
            }
            
        }
        
        int data_offset = 0;
        if (prev_data_id < data_id)
        {
            // Use the value on the previous corner as the prediction.
            data_offset = prev_data_id * K_NUM_COMPONENTS;
        }
        
        
        if (next_data_id < data_id)
        {
            // Use the value on the next corner as the prediction.
            data_offset = next_data_id * K_NUM_COMPONENTS;
        }
        else if (data_id > 0)
        {
            data_offset = (data_id - 1) * K_NUM_COMPONENTS;
        }
        else
        {
            // We are encoding the first value. Predict 0.
            for (int i = 0; i < K_NUM_COMPONENTS; ++i)
            {
                predicted_value_[i] = 0;
            }
            
            
            return true;
        }
        
        
        for (int i = 0; i < K_NUM_COMPONENTS; ++i)
        {
            predicted_value_[i] = data.get(data_offset + i);
        }
        
        
        return true;
    }
    
    public boolean orientation(int i)
    {
        return orientations_[i];
    }
    
    public void set_orientation(int i, boolean v)
    {
        orientations_[i] = v;
    }
    
    public int num_orientations()
    {
        return orientationCount;
    }
    
    public void resizeOrientations(int num_orientations)
    {
        orientations_ = orientations_ == null ? new boolean[num_orientations] : Arrays.copyOf(orientations_, num_orientations);
        this.orientationCount = num_orientations;
    }
    
    private void $initFields$()
    {
        try
        {
            predicted_value_ = new int[2];
            orientations_ = new boolean[10];
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
