package dev.fileformat.drako;
/**
 *  Base class containing shared functionality used by both encoding and decoding
 *  canonicalized normal octahedron prediction scheme transforms. See the
 *  encoding transform for more details about the method.
 *
 */
class PredictionSchemeNormalOctahedronCanonicalizedTransformBase extends PredictionSchemeNormalOctahedronTransformBase
{    
    public int getType()
    {
        return PredictionSchemeTransformType.NORMAL_OCTAHEDRON_CANONICALIZED;
    }
    
    public PredictionSchemeNormalOctahedronCanonicalizedTransformBase()
    {
    }
    
    public PredictionSchemeNormalOctahedronCanonicalizedTransformBase(int max_quantized_value)
    {
        this.setMaxQuantizedValue(max_quantized_value);
    }
    
    protected int getRotationCount(IntVector pred)
    {
        return this.getRotationCount(pred.x, pred.y);
    }
    
    protected int getRotationCount(int sign_x, int sign_y)
    {
        int rotation_count = 0;
        if (sign_x == 0)
        {
            if (sign_y == 0)
            {
                rotation_count = 0;
            }
            else if (sign_y > 0)
            {
                rotation_count = 3;
            }
            else
            {
                rotation_count = 1;
            }
            
        }
        else if (sign_x > 0)
        {
            if (sign_y >= 0)
            {
                rotation_count = 2;
            }
            else
            {
                rotation_count = 1;
            }
            
        }
        else if (sign_y <= 0)
        {
            rotation_count = 0;
        }
        else
        {
            rotation_count = 3;
        }
        
        
        return rotation_count;
    }
    
    protected void rotatePoint(IntVector p, int rotation_count)
    {
        int s = p.x;
        int t = p.y;
        switch(rotation_count)
        {
            case 1:
            {
                p.x = t;
                p.y = -s;
                break;
                // return new int[]{p[1], -p[0]};
            }
            case 2:
            {
                p.x = -s;
                p.y = -t;
                break;
                // return new int[]{-p[0], -p[1]};
            }
            case 3:
            {
                p.x = -t;
                p.y = s;
                break;
                // return new int[]{-p[1], p[0]};
            }
            default:
            {
                break;
                //return p;
            }
        }
        
    }
    
    protected boolean isInBottomLeft(IntVector p)
    {
        return this.isInBottomLeft(p.x, p.y);
    }
    
    protected boolean isInBottomLeft(int s, int t)
    {
        if (s == 0 && (t == 0))
            return true;
        return s < 0 && (t <= 0);
    }
    
}
