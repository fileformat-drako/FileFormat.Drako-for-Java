package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
class MeshSequentialDecoder extends MeshDecoder
{    
    @Override
    protected void decodeConnectivity()
        throws DrakoException
    {
        int numFaces;
        int numPoints;
        if (this.getBitstreamVersion() < 22)
        {
            numFaces = this.getBuffer().decodeU32();
            numPoints = this.getBuffer().decodeU32();
        }
        else
        {
            numFaces = Decoding.decodeVarintU32(this.buffer);
            numPoints = Decoding.decodeVarintU32(this.buffer);
        }
        
        byte connectivityMethod = this.getBuffer().decodeU8();
        if (connectivityMethod == 0)
        {
            this.decodeAndDecompressIndices(numFaces);
        }
        else if ((0xffffffffl & numPoints) < 256)
        {
            int[] face = new int[3];
            for (int i = 0; i < (0xffffffffl & numFaces); ++i)
            {
                for (int j = 0; j < 3; ++j)
                {
                    byte val = this.buffer.decodeU8();
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
                    short val = this.getBuffer().decodeU16();
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
                    int val = Decoding.decodeVarintU32(this.buffer);
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
                    int val = this.getBuffer().decodeI32();
                    face[j] = val;
                }
                
                this.getMesh().addFace(face);
            }
            
        }
        
        this.getPointCloud().setNumPoints(numPoints);
    }
    
    @Override
    protected void createAttributesDecoder(int attrDecoderId)
        throws DrakoException
    {
        
        // Always create the basic attribute decoder.
        this.setAttributesDecoder(attrDecoderId, new SequentialAttributeDecodersController(new LinearSequencer(this.getPointCloud().getNumPoints())));
    }
    
    /**
     *  Decodes face indices that were compressed with an entropy code.
     *  Returns false on error.
     *
     */
    void decodeAndDecompressIndices(int numFaces)
        throws DrakoException
    {
        IntSpan indicesBuffer = IntSpan.wrap(new int[numFaces * 3]);
        Decoding.decodeSymbols(numFaces * 3, 1, this.getBuffer(), indicesBuffer);
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
        
    }
    
    
}
