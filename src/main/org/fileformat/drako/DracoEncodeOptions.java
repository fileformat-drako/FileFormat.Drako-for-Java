package org.fileformat.drako;
/**
 *  Save options for Google draco files
 *
 */
public class DracoEncodeOptions
{    
    /**
     *  Quantization bits for position, default value is 14
     *
     * @return  Quantization bits for position, default value is 14
     */
    public int getPositionBits()
    {
        return this.positionBits;
    }
    
    /**
     *  Quantization bits for position, default value is 14
     *
     * @param value New value
     */
    public void setPositionBits(int value)
    {
        this.positionBits = value;
    }
    
    private int positionBits;
    /**
     *  Quantization bits for texture coordinate, default value is 12
     *
     * @return  Quantization bits for texture coordinate, default value is 12
     */
    public int getTextureCoordinateBits()
    {
        return this.textureCoordinateBits;
    }
    
    /**
     *  Quantization bits for texture coordinate, default value is 12
     *
     * @param value New value
     */
    public void setTextureCoordinateBits(int value)
    {
        this.textureCoordinateBits = value;
    }
    
    private int textureCoordinateBits;
    /**
     *  Quantization bits for vertex color, default value is 10
     *
     * @return  Quantization bits for vertex color, default value is 10
     */
    public int getColorBits()
    {
        return this.colorBits;
    }
    
    /**
     *  Quantization bits for vertex color, default value is 10
     *
     * @param value New value
     */
    public void setColorBits(int value)
    {
        this.colorBits = value;
    }
    
    private int colorBits;
    /**
     *  Quantization bits for normal vectors, default value is 10
     *
     * @return  Quantization bits for normal vectors, default value is 10
     */
    public int getNormalBits()
    {
        return this.normalBits;
    }
    
    /**
     *  Quantization bits for normal vectors, default value is 10
     *
     * @param value New value
     */
    public void setNormalBits(int value)
    {
        this.normalBits = value;
    }
    
    private int normalBits;
    /**
     *  Compression level, default value is {@link org.fileformat.drako.DracoCompressionLevel#STANDARD}
     *
     * @return  Compression level, default value is {@link org.fileformat.drako.DracoCompressionLevel#STANDARD}
     */
    public DracoCompressionLevel getCompressionLevel()
    {
        return this.compressionLevel;
    }
    
    /**
     *  Compression level, default value is {@link org.fileformat.drako.DracoCompressionLevel#STANDARD}
     *
     * @param value New value
     */
    public void setCompressionLevel(DracoCompressionLevel value)
    {
        this.compressionLevel = value;
    }
    
    private DracoCompressionLevel compressionLevel = DracoCompressionLevel.NO_COMPRESSION;
    /**
     *  Apply {@link AssetInfo.UnitScaleFactor} to the mesh.
     *  Default value is false.
     *
     * @return  Apply {@link AssetInfo.UnitScaleFactor} to the mesh.
 Default value is false.
     */
    public boolean getApplyUnitScale()
    {
        return this.applyUnitScale;
    }
    
    /**
     *  Apply {@link AssetInfo.UnitScaleFactor} to the mesh.
     *  Default value is false.
     *
     * @param value New value
     */
    public void setApplyUnitScale(boolean value)
    {
        this.applyUnitScale = value;
    }
    
    private boolean applyUnitScale;
    /**
     *  Export the scene as point cloud, default value is false.
     *
     * @return  Export the scene as point cloud, default value is false.
     */
    public boolean getPointCloud()
    {
        return this.pointCloud;
    }
    
    /**
     *  Export the scene as point cloud, default value is false.
     *
     * @param value New value
     */
    public void setPointCloud(boolean value)
    {
        this.pointCloud = value;
    }
    
    private boolean pointCloud;
    boolean getSplitMeshOnSeams()
    {
        return this.splitMeshOnSeams;
    }
    
    void setSplitMeshOnSeams(boolean value)
    {
        this.splitMeshOnSeams = value;
    }
    
    private boolean splitMeshOnSeams;
    // value is defined in Encoding Tagged/Raw
    // 
    Integer getSymbolEncodingMethod()
    {
        return this.symbolEncodingMethod;
    }
    
    // value is defined in Encoding Tagged/Raw
    // 
    void setSymbolEncodingMethod(Integer value)
    {
        this.symbolEncodingMethod = value;
    }
    
    private Integer symbolEncodingMethod;
    // Compression Level/    Encoder/                 Predictive Scheme
    // NoCompression         Sequential               None
    // Fast                  Edgebreaker              Difference
    // Normal                EdgeBreaker              Parallelogram
    // Best                  Predictive Edgebreaker   MultiParallelogram
    // 
    boolean useBuiltinAttributeCompression = true;
    /**
     *  Construct a default configuration for saving draco files.
     *
     */
    public DracoEncodeOptions()
    {
        
        this.setPositionBits(11);
        this.setTextureCoordinateBits(12);
        this.setNormalBits(10);
        this.setColorBits(10);
        this.setCompressionLevel(DracoCompressionLevel.STANDARD);
    }
    
    int getPredictionMethod(int geometryType, PointAttribute attr)
    {
        /*
             *         
             *         
             **/        
        if (this.getCompressionLevel() == DracoCompressionLevel.NO_COMPRESSION)
            return PredictionSchemeMethod.NONE;
        //No prediction is required when fastest speed is requested.
        if (geometryType == EncodedGeometryType.TRIANGULAR_MESH)
        {
            if (attr.getAttributeType() == AttributeType.TEX_COORD)
            {
                if (this.getCompressionLevel() != DracoCompressionLevel.FAST && (this.getCompressionLevel() != DracoCompressionLevel.NO_COMPRESSION))
                    return PredictionSchemeMethod.TEX_COORDS_PORTABLE;
            }
            
            // Use speed setting to select the best encoding method.
            if (this.getCompressionLevel() == DracoCompressionLevel.FAST)
                return PredictionSchemeMethod.DIFFERENCE;
            if (this.getCompressionLevel() == DracoCompressionLevel.STANDARD)
                return PredictionSchemeMethod.PARALLELOGRAM;
            return PredictionSchemeMethod.MULTI_PARALLELOGRAM;
        }
        
        return PredictionSchemeMethod.UNDEFINED;
    }
    
    int getQuantizationBits(PointAttribute attribute)
    {
        switch(attribute.getAttributeType())
        {
            case AttributeType.COLOR:
                return this.getColorBits();
            case AttributeType.NORMAL:
                return this.getNormalBits();
            case AttributeType.POSITION:
                return this.getPositionBits();
            case AttributeType.TEX_COORD:
                return this.getTextureCoordinateBits();
            default:
            {
                throw new RuntimeException("Not supported quantization bits option for the specified attribute type");
            }
        }
        
    }
    
    int getAttributePredictionScheme(PointAttribute attribute)
    {
        return PredictionSchemeMethod.UNDEFINED;
    }
    
    int getSpeed()
    {
        return 3;
    }
    
    int getCompressionLevel2()
    {
        switch(this.getCompressionLevel())
        {
            case NO_COMPRESSION:
                return 2;
            case FAST:
                return 6;
            case STANDARD:
                return 7;
            case OPTIMAL:
                return 10;
        }
        
        
        return 7;
    }
    
}
