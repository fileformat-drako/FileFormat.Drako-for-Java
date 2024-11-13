package dev.fileformat.drako;
/**
 *  List of all prediction methods currently supported by our framework.
 *
 */
final class PredictionSchemeMethod
{    
    /**
     *  Special value indicating that no prediction scheme was used.
     *
     */
    public static final int NONE = -2;
    /**
     *  Used when no specific prediction scheme is required.
     *
     */
    public static final int UNDEFINED = -1;
    public static final int DIFFERENCE = 0;
    public static final int PARALLELOGRAM = 1;
    public static final int MULTI_PARALLELOGRAM = 2;
    public static final int TEX_COORDS_DEPRECATED = 3;
    public static final int CONSTRAINED_MULTI_PARALLELOGRAM = 4;
    public static final int TEX_COORDS_PORTABLE = 5;
    public static final int GEOMETRIC_NORMAL = 6;
    
    
}
