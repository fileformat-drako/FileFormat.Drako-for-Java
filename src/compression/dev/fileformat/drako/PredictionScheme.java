package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
abstract class PredictionScheme
{    
    protected PointAttribute attribute_;
    protected PredictionSchemeTransform transform_;
    protected PredictionScheme(PointAttribute attribute, PredictionSchemeTransform transform)
    {
        this.attribute_ = attribute;
        this.transform_ = transform;
    }
    
    /**
     *  Returns the encoded attribute.
     *
     * @return  Returns the encoded attribute.
     */
    public PointAttribute getAttribute()
    {
        return attribute_;
    }
    
    public abstract boolean getInitialized();
    
    /**
     *  Returns the number of parent attributes that are needed for the prediction.
     *
     * @return  Returns the number of parent attributes that are needed for the prediction.
     */
    public int getNumParentAttributes()
    {
        return 0;
    }
    
    public int getParentAttributeType(int i)
    {
        return AttributeType.INVALID;
    }
    
    // Sets the required parent attribute.
    // 
    public boolean setParentAttribute(PointAttribute att)
    {
        return false;
    }
    
    public boolean areCorrectionsPositive()
    {
        return transform_.areCorrectionsPositive();
    }
    
    public int getTransformType()
    {
        return transform_.getType();
    }
    
    public abstract int getPredictionMethod();
    
    public boolean encodePredictionData(EncoderBuffer buffer)
    {
        if (!transform_.encodeTransformData(buffer))
            return false;
        return true;
    }
    
    // Method that can be used to decode any prediction scheme specific data
    // from the input buffer.
    // 
    public boolean decodePredictionData(DecoderBuffer buffer)
    {
        if (!transform_.decodeTransformData(buffer))
            return false;
        return true;
    }
    
    public abstract boolean computeCorrectionValues(IntSpan in_data, IntSpan out_corr, int size, int num_components, int[] entry_to_point_id_map);
    
    // Reverts changes made by the prediction scheme during encoding.
    // 
    public abstract boolean computeOriginalValues(IntSpan in_corr, IntSpan out_data, int size, int num_components, int[] entry_to_point_id_map);
    
    public static PredictionScheme create(int method, PointAttribute att, PredictionSchemeTransform transform)
    {
        if (method == PredictionSchemeMethod.NONE)
            return null;
        return new PredictionSchemeDifference(att, transform);
    }
    
    public static PredictionScheme create(PointCloudEncoder encoder, int method, int attId, PredictionSchemeTransform transform)
    {
        PointAttribute attr = encoder.getPointCloud().attribute(attId);
        if (method == PredictionSchemeMethod.UNDEFINED)
        {
            method = encoder.getOptions().getPredictionMethod(encoder.getGeometryType(), attr);
        }
        
        if (method == PredictionSchemeMethod.NONE)
            return null;
        //No prediction is required when fastest speed is requested.
        if (encoder.getGeometryType() == EncodedGeometryType.TRIANGULAR_MESH)
        {
            MeshEncoder meshEncoder = (MeshEncoder)encoder;
            PredictionScheme ret = PredictionScheme.createMeshPredictionScheme(meshEncoder, method, attId, transform);
            if (ret != null)
                return ret;
            // Otherwise try to create another prediction scheme.
        }
        
        return new PredictionSchemeDifference(attr, transform);
    }
    
    static PredictionScheme createMeshPredictionScheme(MeshEncoder source, int method, int attId, PredictionSchemeTransform transform)
    {
        PointAttribute att = source.getPointCloud().attribute(attId);
        if (source.getGeometryType() == EncodedGeometryType.TRIANGULAR_MESH && (method == PredictionSchemeMethod.PARALLELOGRAM || (method == PredictionSchemeMethod.MULTI_PARALLELOGRAM) || (method == PredictionSchemeMethod.GEOMETRIC_NORMAL) || (method == PredictionSchemeMethod.TEX_COORDS_PORTABLE) || (method == PredictionSchemeMethod.TEX_COORDS_DEPRECATED)))
        {
            ICornerTable ct = source.getCornerTable();
            MeshAttributeIndicesEncodingData encodingData = source.getAttributeEncodingData(attId);
            if (ct == null || (encodingData == null))
                return null;
            ICornerTable attCt = source.getAttributeCornerTable(attId);
            MeshPredictionSchemeData md = new MeshPredictionSchemeData(source.getMesh(), attCt != null ? attCt : ct, encodingData.encodedAttributeValueIndexToCornerMap, encodingData.vertexToEncodedAttributeValueIndexMap);
            return PredictionScheme.createMeshPredictionSchemeInternal(method, att, transform, md);
        }
        
        
        return null;
    }
    
    public static PredictionScheme create(PointCloudDecoder source, int method, int attId, PredictionSchemeTransform transform)
    {
        if (method == PredictionSchemeMethod.NONE)
            return null;
        if (source.getGeometryType() == EncodedGeometryType.TRIANGULAR_MESH)
            return PredictionScheme.createMeshPredictionScheme((MeshDecoder)source, method, attId, transform);
        PointAttribute att = source.getPointCloud().attribute(attId);
        return new PredictionSchemeDeltaDecoder(att, transform);
    }
    
    public static PredictionScheme createMeshPredictionScheme(MeshDecoder source, int method, int attId, PredictionSchemeTransform transform)
    {
        PointAttribute att = source.getPointCloud().attribute(attId);
        if (method == PredictionSchemeMethod.PARALLELOGRAM || (method == PredictionSchemeMethod.MULTI_PARALLELOGRAM) || (method == PredictionSchemeMethod.TEX_COORDS_DEPRECATED) || (method == PredictionSchemeMethod.TEX_COORDS_PORTABLE) || (method == PredictionSchemeMethod.GEOMETRIC_NORMAL))
        {
            CornerTable ct = source.getCornerTable();
            MeshAttributeIndicesEncodingData encodingData = source.getAttributeEncodingData(attId);
            if (ct == null || (encodingData == null))
                return null;
            MeshAttributeCornerTable attCt = source.getAttributeCornerTable(attId);
            if (attCt != null)
            {
                MeshPredictionSchemeData md = new MeshPredictionSchemeData(source.getMesh(), attCt, encodingData.encodedAttributeValueIndexToCornerMap, encodingData.vertexToEncodedAttributeValueIndexMap);
                PredictionScheme ret = PredictionScheme.createMeshPredictionSchemeInternal(method, att, transform, md);
                if (ret != null)
                    return ret;
            }
            else
            {
                MeshPredictionSchemeData md = new MeshPredictionSchemeData(source.getMesh(), ct, encodingData.encodedAttributeValueIndexToCornerMap, encodingData.vertexToEncodedAttributeValueIndexMap);
                PredictionScheme ret = PredictionScheme.createMeshPredictionSchemeInternal(method, att, transform, md);
                if (ret != null)
                    return ret;
            }
            
        }
        
        return null;
    }
    
    static PredictionScheme createMeshPredictionSchemeInternal(int method, PointAttribute attribute, PredictionSchemeTransform transform, MeshPredictionSchemeData meshData)
    {
        if (method == PredictionSchemeMethod.PARALLELOGRAM)
            return new MeshPredictionSchemeParallelogram(attribute, transform, meshData);else if (method == PredictionSchemeMethod.MULTI_PARALLELOGRAM)
            return new MeshPredictionSchemeMultiParallelogram(attribute, transform, meshData);else if (method == PredictionSchemeMethod.TEX_COORDS_DEPRECATED)
            return new MeshPredictionSchemeTexCoords(attribute, transform, meshData);else if (method == PredictionSchemeMethod.TEX_COORDS_PORTABLE)
            return new MeshPredictionSchemeTexCoordsPortableDecoder(attribute, transform, meshData);else if (method == PredictionSchemeMethod.GEOMETRIC_NORMAL)
            return new MeshPredictionSchemeGeometricNormal(attribute, transform, meshData);
        return null;
    }
    
}
