package dev.fileformat.drako;
import dev.fileformat.drako.ByteSpan;
import dev.fileformat.drako.IntSpan;
class SequentialIntegerAttributeDecoder extends SequentialAttributeDecoder
{    
    private PredictionScheme predictionScheme;
    @Override
    protected void decodeValues(int[] pointIds, DecoderBuffer inBuffer)
        throws DrakoException
    {
        int numValues = pointIds.length;
        byte predictionSchemeMethod = inBuffer.decodeI8();
        
        if (predictionSchemeMethod != (byte)PredictionSchemeMethod.NONE)
        {
            byte predictionTransformType = inBuffer.decodeI8();
            this.predictionScheme = this.createIntPredictionScheme((int)predictionSchemeMethod, (int)predictionTransformType);
        }
        
        
        if (predictionScheme != null)
        {
            this.initPredictionScheme(predictionScheme);
        }
        
        
        this.decodeIntegerValues(pointIds, inBuffer);
        
        if (this.getDecoder() != null && (this.getDecoder().getBitstreamVersion() < 20))
        {
            this.storeValues(numValues);
        }
        
    }
    
    protected PredictionScheme createIntPredictionScheme(int method, int transformType)
    {
        if (transformType != PredictionSchemeTransformType.WRAP)
            return null;
        // For now we support only wrap transform.
        return this.createPredictionSchemeForDecoder(method, this.getAttributeId(), this.getDecoder());
    }
    
    PredictionScheme createPredictionSchemeForDecoder(int method, int attId, PointCloudDecoder decoder)
    {
        return this.createPredictionSchemeForDecoder(method, attId, decoder, new PredictionSchemeWrapTransform());
    }
    
    /**
     *  Creates a prediction scheme for a given decoder and given prediction method.
     *  The prediction schemes are automatically initialized with decoder specific
     *  data if needed.
     *
     */
    PredictionScheme createPredictionSchemeForDecoder(int method, int attId, PointCloudDecoder decoder, PredictionSchemeTransform transform)
    {
        PointAttribute att = decoder.getPointCloud().attribute(attId);
        if (decoder.getGeometryType() == EncodedGeometryType.TRIANGULAR_MESH)
        {
            MeshDecoder meshDecoder = (MeshDecoder)decoder;
            PredictionScheme ret = PredictionScheme.create(meshDecoder, method, attId, transform);
            if (ret != null)
                return ret;
            // Otherwise try to create another prediction scheme.
        }
        
        return new PredictionSchemeDeltaDecoder(att, transform);
    }
    
    private void preparePortableAttribute(int num_entries, int num_components)
    {
        PointAttribute va = new PointAttribute();
        va.setAttributeType(this.attribute.getAttributeType());
        va.setComponentsCount(this.attribute.getComponentsCount());
        va.setDataType(DataType.INT32);
        va.setByteStride(num_components * DracoUtils.dataTypeLength(DataType.INT32));
        va.setIdentityMapping(true);
        va.reset(num_entries);
        this.setPortableAttribute(va);
    }
    
    protected int getNumValueComponents()
    {
        return this.attribute.getComponentsCount();
    }
    
    private IntSpan getValues(int numEntries)
    {
        int numComponents = this.getNumValueComponents();
        int numValues = numEntries * numComponents;
        if (numComponents <= 0)
            return null;
        this.preparePortableAttribute(numEntries, numComponents);
        if (this.getPortableAttribute().getNumUniqueEntries() == 0)
            return null;
        byte[] buf = this.getPortableAttribute().getBuffer().getBuffer();
        return ByteSpan.wrap(buf, 0, numValues * 4).asIntSpan();
    }
    
    public void decodeIntegerValues(int[] pointIds, DecoderBuffer inBuffer)
        throws DrakoException
    {
        int numComponents = this.getNumValueComponents();
        int numEntries = pointIds.length;
        int numValues = numEntries * numComponents;
        if (numComponents <= 0)
            throw DracoUtils.failed();
        IntSpan values = this.getValues(numEntries);
        if (values == null)
            throw DracoUtils.failed();
        byte compressed = inBuffer.decodeU8();
        if ((0xff & compressed) > 0)
        {
            // Decode compressed values.
            Decoding.decodeSymbols(numValues, numComponents, inBuffer, values);
        }
        else
        {
            byte numBytes = inBuffer.decodeU8();
            
            //if (numBytes == sizeof(int))
            //{
            //    if (!inBuffer.Decode(values, values.Count))
            //        return false;
            //}
            //else
            //{
            for (int i = 0; i < values.size(); ++i)
            {
                values.put(i, inBuffer.decodeI32());
            }
            
            //}
        }
        
        
        if (predictionScheme == null || !predictionScheme.areCorrectionsPositive())
        {
            // Convert the values back to the original signed format.
            Decoding.convertSymbolsToSignedInts(values, values);
        }
        
        
        // If the data was encoded with a prediction scheme, we must revert it.
        if (predictionScheme != null)
        {
            predictionScheme.decodePredictionData(inBuffer);
            predictionScheme.computeOriginalValues(values, values, values.size(), numComponents, pointIds);
        }
        
    }
    
    @Override
    public void transformAttributeToOriginalFormat(int[] pointIds)
        throws DrakoException
    {
        if (this.decoder != null && (this.decoder.getBitstreamVersion() < 20))
            return;
        this.storeValues(pointIds.length);
    }
    
    protected void storeValues(int numValues)
        throws DrakoException
    {
        switch(this.getAttribute().getDataType())
        {
            case DataType.UINT8:
            case DataType.INT8:
            {
                this.store8BitsValues(numValues);
                break;
            }
            case DataType.UINT16:
            case DataType.INT16:
            {
                this.store16BitsValues(numValues);
                break;
            }
            case DataType.UINT32:
            case DataType.INT32:
            {
                this.store32BitsValues(numValues);
                break;
            }
            default:
            {
                throw DracoUtils.failed();
            }
        }
        
    }
    
    /**
     *  Stores decoded values into the attribute with a data type AttributeTypeT.
     *
     */
    private void store8BitsValues(int numValues)
    {
        int vals = this.getAttribute().getComponentsCount() * numValues;
        IntSpan values = this.getValues(numValues);
        int outBytePos = 0;
        for (int i = 0; i < vals; ++i)
        {
            // Store the integer value into the attribute buffer.
            this.getAttribute().getBuffer().write(outBytePos, (byte)(values.get(i)));
            outBytePos++;
        }
        
    }
    
    private void store16BitsValues(int numValues)
    {
        int vals = this.getAttribute().getComponentsCount() * numValues;
        IntSpan values = this.getValues(numValues);
        int outBytePos = 0;
        for (int i = 0; i < vals; ++i)
        {
            // Store the integer value into the attribute buffer.
            this.getAttribute().getBuffer().write3(outBytePos, (short)(values.get(i)));
            outBytePos += 2;
        }
        
    }
    
    private void store32BitsValues(int numValues)
    {
        int vals = this.getAttribute().getComponentsCount() * numValues;
        IntSpan values = this.getValues(numValues);
        int outBytePos = 0;
        for (int i = 0; i < vals; ++i)
        {
            // Store the integer value into the attribute buffer.
            this.getAttribute().getBuffer().write(outBytePos, values.get(i));
            outBytePos += 4;
        }
        
    }
    
    
}
