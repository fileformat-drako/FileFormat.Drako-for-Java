package dev.fileformat.drako;
import dev.fileformat.drako.ByteSpan;
import dev.fileformat.drako.IntSpan;
class SequentialIntegerAttributeDecoder extends SequentialAttributeDecoder
{    
    private PredictionScheme predictionScheme;
    @Override
    protected boolean decodeValues(int[] pointIds, DecoderBuffer inBuffer)
    {
        int numValues = pointIds.length;
        byte predictionSchemeMethod;
        final byte[] ref0 = new byte[1];
        final byte[] ref1 = new byte[1];
        inBuffer.decode2(ref0);
        predictionSchemeMethod = ref0[0];
        
        if (predictionSchemeMethod != (byte)PredictionSchemeMethod.NONE)
        {
            byte predictionTransformType;
            inBuffer.decode2(ref1);
            predictionTransformType = ref1[0];
            this.predictionScheme = this.createIntPredictionScheme((int)predictionSchemeMethod, (int)predictionTransformType);
        }
        
        
        if (predictionScheme != null)
        {
            if (!this.initPredictionScheme(predictionScheme))
                return DracoUtils.failed();
        }
        
        
        if (!this.decodeIntegerValues(pointIds, inBuffer))
            return DracoUtils.failed();
        
        if (this.getDecoder() != null && (this.getDecoder().getBitstreamVersion() < 20))
        {
            if (!this.storeValues(numValues))
                return DracoUtils.failed();
        }
        
        
        return true;
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
    
    public boolean decodeIntegerValues(int[] pointIds, DecoderBuffer inBuffer)
    {
        int numComponents = this.getNumValueComponents();
        int numEntries = pointIds.length;
        int numValues = numEntries * numComponents;
        final byte[] ref2 = new byte[1];
        final byte[] ref3 = new byte[1];
        final int[] ref4 = new int[1];
        if (numComponents <= 0)
            return DracoUtils.failed();
        IntSpan values = this.getValues(numEntries);
        if (values == null)
            return DracoUtils.failed();
        byte compressed;
        if (!inBuffer.decode3(ref2))
        {
            compressed = ref2[0];
            return DracoUtils.failed();
        }
        else
        {
            compressed = ref2[0];
        }
        
        if ((0xff & compressed) > 0)
        {
            // Decode compressed values.
            if (!Decoding.decodeSymbols(numValues, numComponents, inBuffer, values))
                return DracoUtils.failed();
        }
        else
        {
            byte numBytes;
            if (!inBuffer.decode3(ref3))
            {
                numBytes = ref3[0];
                return DracoUtils.failed();
            }
            else
            {
                numBytes = ref3[0];
            }
            
            
            //if (numBytes == sizeof(int))
            //{
            //    if (!inBuffer.Decode(values, values.Count))
            //        return false;
            //}
            //else
            //{
            for (int i = 0; i < values.size(); ++i)
            {
                int tmp;
                inBuffer.decode6(ref4);
                tmp = ref4[0];
                values.put(i, tmp);
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
            if (!predictionScheme.decodePredictionData(inBuffer))
                return DracoUtils.failed();
            
            if (!predictionScheme.computeOriginalValues(values, values, values.size(), numComponents, pointIds))
                return DracoUtils.failed();
        }
        
        return true;
    }
    
    @Override
    public boolean transformAttributeToOriginalFormat(int[] pointIds)
    {
        if (this.decoder != null && (this.decoder.getBitstreamVersion() < 20))
            return true;
        return this.storeValues(pointIds.length);
    }
    
    protected boolean storeValues(int numValues)
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
                return DracoUtils.failed();
        }
        
        return true;
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
