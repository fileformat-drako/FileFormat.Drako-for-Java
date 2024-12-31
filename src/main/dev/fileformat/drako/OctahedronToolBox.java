package dev.fileformat.drako;
import dev.fileformat.drako.FloatSpan;
class OctahedronToolBox
{    
    int quantization_bits_;
    private int max_quantized_value_;
    private int max_value_;
    private int center_value_;
    public void setQuantizationBits(int q)
    {
        if (q < 2 || (q > 30))
            throw new IllegalArgumentException("Invalid quantization parameters");
        this.quantization_bits_ = q;
        this.max_quantized_value_ = (1 << quantization_bits_) - 1;
        this.max_value_ = max_quantized_value_ - 1;
        this.center_value_ = max_value_ / 2;
    }
    
    public boolean isInitialized()
    {
        return quantization_bits_ != -1;
    }
    
    // |s| and |t| are expected to be signed values.
    // 
    public boolean isInDiamond(int s, int t)
    {
        // Expect center already at origin.
        return Math.abs(s) + Math.abs(t) <= center_value_;
    }
    
    public void invertDiamond(IntVector v)
    {
        int sign_s = 0;
        int sign_t = 0;
        if (v.x >= 0 && (v.y >= 0))
        {
            sign_s = 1;
            sign_t = 1;
        }
        else if (v.x <= 0 && (v.y <= 0))
        {
            sign_s = -1;
            sign_t = -1;
        }
        else
        {
            sign_s = v.x > 0 ? 1 : -1;
            sign_t = v.y > 0 ? 1 : -1;
        }
        
        int corner_point_s = sign_s * center_value_;
        int corner_point_t = sign_t * center_value_;
        v.x = 2 * v.x - corner_point_s;
        v.y = 2 * v.y - corner_point_t;
        if (sign_s * sign_t >= 0)
        {
            int temp = v.x;
            v.x = -v.y;
            v.y = -temp;
        }
        else
        {
            int temp = v.x;
            v.x = v.y;
            v.y = temp;
        }
        
        
        v.x = (v.x + corner_point_s) / 2;
        v.y = (v.y + corner_point_t) / 2;
    }
    
    public void invertDiamond(int[] s, int[] t)
    {
        int sign_s = 0;
        int sign_t = 0;
        if (s[0] >= 0 && (t[0] >= 0))
        {
            sign_s = 1;
            sign_t = 1;
        }
        else if (s[0] <= 0 && (t[0] <= 0))
        {
            sign_s = -1;
            sign_t = -1;
        }
        else
        {
            sign_s = s[0] > 0 ? 1 : -1;
            sign_t = t[0] > 0 ? 1 : -1;
        }
        
        int corner_point_s = sign_s * center_value_;
        int corner_point_t = sign_t * center_value_;
        s[0] = 2 * s[0] - corner_point_s;
        t[0] = 2 * t[0] - corner_point_t;
        if (sign_s * sign_t >= 0)
        {
            int temp = s[0];
            s[0] = -t[0];
            t[0] = -temp;
        }
        else
        {
            int temp = s[0];
            s[0] = t[0];
            t[0] = temp;
        }
        
        
        s[0] = (s[0] + corner_point_s) / 2;
        t[0] = (t[0] + corner_point_t) / 2;
    }
    
    public void invertDirection(int[] s, int[] t)
    {
        s[0] *= -1;
        t[0] *= -1;
        this.invertDiamond(s, t);
    }
    
    // For correction values.
    // 
    public int modMax(int x)
    {
        if (x > center_value_)
            return x - max_quantized_value_;
        if (x < -center_value_)
            return x + max_quantized_value_;
        return x;
    }
    
    // For correction values.
    // 
    public int makePositive(int x)
    {
        if (x < 0)
            return x + max_quantized_value_;
        return x;
    }
    
    public int getQuantizationBits()
    {
        return quantization_bits_;
    }
    
    public int getMaxQuantizedValue()
    {
        return max_quantized_value_;
    }
    
    public int getMaxValue()
    {
        return max_value_;
    }
    
    public int getCenterValue()
    {
        return center_value_;
    }
    
    // Normalize |vec| such that its abs sum is equal to the center value;
    // 
    public void canonicalizeIntegerVector(int[] vec)
    {
        long abs_sum = Math.abs(vec[0]) + Math.abs(vec[1]) + Math.abs(vec[2]);
        
        if (abs_sum == 0L)
        {
            vec[0] = center_value_;
            // vec[1] == v[2] == 0
        }
        else
        {
            vec[0] = (int)((long)(vec[0]) * center_value_ / abs_sum);
            vec[1] = (int)((long)(vec[1]) * center_value_ / abs_sum);
            if (vec[2] >= 0)
            {
                vec[2] = center_value_ - Math.abs(vec[0]) - Math.abs(vec[1]);
            }
            else
            {
                vec[2] = -(center_value_ - Math.abs(vec[0]) - Math.abs(vec[1]));
            }
            
        }
        
    }
    
    // Converts an integer vector to octahedral coordinates.
    // Precondition: |int_vec| abs sum must equal center value.
    // 
    public void integerVectorToQuantizedOctahedralCoords(int[] int_vec, int[] out_s, int[] out_t)
    {
        int s;
        int t;
        if (int_vec[0] >= 0)
        {
            // Right hemisphere.
            s = int_vec[1] + center_value_;
            t = int_vec[2] + center_value_;
        }
        else
        {
            // Left hemisphere.
            if (int_vec[1] < 0)
            {
                s = Math.abs(int_vec[2]);
            }
            else
            {
                s = max_value_ - Math.abs(int_vec[2]);
            }
            
            
            if (int_vec[2] < 0)
            {
                t = Math.abs(int_vec[1]);
            }
            else
            {
                t = max_value_ - Math.abs(int_vec[1]);
            }
            
        }
        
        this.canonicalizeOctahedralCoords(s, t, out_s, out_t);
    }
    
    // Convert all edge points in the top left and bottom right quadrants to
    // their corresponding position in the bottom left and top right quadrants.
    // Convert all corner edge points to the top right corner.
    // 
    private void canonicalizeOctahedralCoords(int s, int t, int[] out_s, int[] out_t)
    {
        if (s == 0 && (t == 0) || (s == 0 && (t == max_value_)) || (s == max_value_ && (t == 0)))
        {
            s = max_value_;
            t = max_value_;
        }
        else if (s == 0 && (t > center_value_))
        {
            t = center_value_ - (t - center_value_);
        }
        else if (s == max_value_ && (t < center_value_))
        {
            t = center_value_ + (center_value_ - t);
        }
        else if (t == max_value_ && (s < center_value_))
        {
            s = center_value_ + (center_value_ - s);
        }
        else if (t == 0 && (s > center_value_))
        {
            s = center_value_ - (s - center_value_);
        }
        
        
        out_s[0] = s;
        out_t[0] = t;
    }
    
    public void floatVectorToQuantizedOctahedralCoords(FloatSpan vector, int[] out_s, int[] out_t)
    {
        double abs_sum = Math.abs(vector.get(0)) + Math.abs(vector.get(1)) + Math.abs(vector.get(2));
        double[] scaled_vector = new double[3];
        if (abs_sum > 1e-6)
        {
            double scale = 1.0 / abs_sum;
            scaled_vector[0] = vector.get(0) * scale;
            scaled_vector[1] = vector.get(1) * scale;
            scaled_vector[2] = vector.get(2) * scale;
        }
        else
        {
            scaled_vector[0] = 1.0;
            scaled_vector[1] = 0.0;
            scaled_vector[2] = 0.0;
        }
        
        int[] int_vec = new int[3];
        int_vec[0] = (int)Math.floor(scaled_vector[0] * center_value_ + 0.5);
        int_vec[1] = (int)Math.floor(scaled_vector[1] * center_value_ + 0.5);
        // Make sure the sum is exactly the center value.
        int_vec[2] = center_value_ - Math.abs(int_vec[0]) - Math.abs(int_vec[1]);
        if (int_vec[2] < 0)
        {
            // If the sum of first two coordinates is too large, we need to decrease
            // the length of one of the coordinates.
            if (int_vec[1] > 0)
            {
                int_vec[1] += int_vec[2];
            }
            else
            {
                int_vec[1] -= int_vec[2];
            }
            
            
            int_vec[2] = 0;
        }
        
        
        // Take care of the sign.
        if (scaled_vector[2] < 0.0)
        {
            int_vec[2] *= -1;
        }
        
        this.integerVectorToQuantizedOctahedralCoords(int_vec, out_s, out_t);
    }
    
    public OctahedronToolBox()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            quantization_bits_ = -1;
            max_quantized_value_ = -1;
            max_value_ = -1;
            center_value_ = -1;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
