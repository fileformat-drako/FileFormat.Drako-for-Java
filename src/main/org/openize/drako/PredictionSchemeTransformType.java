package org.openize.drako;
/**
 *  List of all prediction scheme transforms used by our framework.
 *
 */
final class PredictionSchemeTransformType
{    
    public static final int NONE = -1;
    /**
     *  Basic delta transform where the prediction is computed as difference the
     *  predicted and original value.
     *
     */
    public static final int DELTA = 0;
    /**
     *  An improved delta transform where all computed delta values are wrapped
     *  around a fixed interval which lowers the entropy.
     *
     */
    public static final int WRAP = 1;
    /**
     *  Specialized transform for normal coordinates using inverted tiles.
     *
     */
    public static final int NORMAL_OCTAHEDRON = 2;
    /**
     *  Specialized transform for normal coordinates using canonicalized inverted tiles.
     *
     */
    public static final int NORMAL_OCTAHEDRON_CANONICALIZED = 3;
    
    
}
