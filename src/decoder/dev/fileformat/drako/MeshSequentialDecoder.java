package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class MeshSequentialDecoder extends MeshDecoder
{    
    @Override
    protected boolean decodeConnectivity()
    {
        int numFaces;
        int numPoints;
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        final int[] ref2 = new int[1];
        final int[] ref3 = new int[1];
        final byte[] ref4 = new byte[1];
        final byte[] ref5 = new byte[1];
        final short[] ref6 = new short[1];
        final int[] ref7 = new int[1];
        final int[] ref8 = new int[1];
        if (this.getBitstreamVersion() < 22)
        {
            if (!this.getBuffer().decode5(ref0))
            {
                numFaces = ref0[0];
                return DracoUtils.failed();
            }
            else
            {
                numFaces = ref0[0];
            }
            
            if (!this.getBuffer().decode5(ref1))
            {
                numPoints = ref1[0];
                return DracoUtils.failed();
            }
            else
            {
                numPoints = ref1[0];
            }
            
        }
        else
        {
            if (!Decoding.decodeVarint(ref2, this.buffer))
            {
                numFaces = ref2[0];
                return false;
            }
            else
            {
                numFaces = ref2[0];
            }
            
            if (!Decoding.decodeVarint(ref3, this.buffer))
            {
                numPoints = ref3[0];
                return false;
            }
            else
            {
                numPoints = ref3[0];
            }
            
        }
        
        byte connectivityMethod;
        if (!this.getBuffer().decode3(ref4))
        {
            connectivityMethod = ref4[0];
            return DracoUtils.failed();
        }
        else
        {
            connectivityMethod = ref4[0];
        }
        
        if (connectivityMethod == 0)
        {
            if (!this.decodeAndDecompressIndices(numFaces))
                return DracoUtils.failed();
        }
        else if ((0xffffffffl & numPoints) < 256)
        {
            int[] face = new int[3];
            for (int i = 0; i < (0xffffffffl & numFaces); ++i)
            {
                for (int j = 0; j < 3; ++j)
                {
                    byte val;
                    if (!this.getBuffer().decode3(ref5))
                    {
                        val = ref5[0];
                        return DracoUtils.failed();
                    }
                    else
                    {
                        val = ref5[0];
                    }
                    
                    face[j] = 0xff & val;
                }
                
                this.getMesh().addFace(face);
            }
            
        }
        else if ((0xffffffffl & numPoints) < (1 << 16))
        {
            int[] face = new int[3];
            for (int i = 0; i < (0xffffffffl & numFaces); ++i)
            {
                for (int j = 0; j < 3; ++j)
                {
                    short val;
                    if (!this.getBuffer().decode(ref6))
                    {
                        val = ref6[0];
                        return DracoUtils.failed();
                    }
                    else
                    {
                        val = ref6[0];
                    }
                    
                    face[j] = 0xffff & val;
                }
                
                this.getMesh().addFace(face);
            }
            
        }
        else if (this.getMesh().getNumPoints() < (1 << 21) && (this.getBitstreamVersion() >= 22))
        {
            int[] face = new int[3];
            for (int i = 0; i < (0xffffffffl & numFaces); ++i)
            {
                for (int j = 0; j < 3; ++j)
                {
                    int val;
                    if (!Decoding.decodeVarint(ref7, this.buffer))
                    {
                        val = ref7[0];
                        return false;
                    }
                    else
                    {
                        val = ref7[0];
                    }
                    
                    face[j] = val;
                }
                
                this.getMesh().addFace(face);
            }
            
        }
        else
        {
            int[] face = new int[3];
            for (int i = 0; i < (0xffffffffl & numFaces); ++i)
            {
                for (int j = 0; j < 3; ++j)
                {
                    int val;
                    if (!this.getBuffer().decode6(ref8))
                    {
                        val = ref8[0];
                        return DracoUtils.failed();
                    }
                    else
                    {
                        val = ref8[0];
                    }
                    
                    face[j] = val;
                }
                
                this.getMesh().addFace(face);
            }
            
        }
        
        this.getPointCloud().setNumPoints(numPoints);
        return true;
    }
    
    @Override
    protected boolean createAttributesDecoder(int attrDecoderId)
    {
        
        // Always create the basic attribute decoder.
        this.setAttributesDecoder(attrDecoderId, new SequentialAttributeDecodersController(new LinearSequencer(this.getPointCloud().getNumPoints())));
        return true;
    }
    
    /**
     *  Decodes face indices that were compressed with an entropy code.
     *  Returns false on error.
     *
     */
    boolean decodeAndDecompressIndices(int numFaces)
    {
        IntSpan indicesBuffer = IntSpan.wrap(new int[numFaces * 3]);
        if (!Decoding.decodeSymbols(numFaces * 3, 1, this.getBuffer(), indicesBuffer))
            return DracoUtils.failed();
        int lastIndexValue = 0;
        int vertexIndex = 0;
        int[] face = new int[3];
        for (int i = 0; i < numFaces; ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                int encodedVal = indicesBuffer.get(vertexIndex++);
                int indexDiff = encodedVal >> 1;
                if ((encodedVal & 1) != 0)
                {
                    indexDiff = -indexDiff;
                }
                
                int indexValue = indexDiff + lastIndexValue;
                face[j] = indexValue;
                lastIndexValue = indexValue;
            }
            
            this.getMesh().addFace(face);
        }
        
        return true;
    }
    
    
}
