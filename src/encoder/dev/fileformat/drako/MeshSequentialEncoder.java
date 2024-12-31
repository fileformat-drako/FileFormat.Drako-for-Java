package dev.fileformat.drako;
import dev.fileformat.drako.IntSpan;
/**
 *  Class that encodes mesh data using a simple binary representation of mesh's
 *  connectivity and geometry.
 *
 */
class MeshSequentialEncoder extends MeshEncoder
{    
    @Override
    public int getEncodingMethod()
    {
        return DracoEncodingMethod.SEQUENTIAL;
    }
    
    @Override
    protected void encodeConnectivity()
    {
        int numFaces = this.getMesh().getNumFaces();
        Encoding.encodeVarint(numFaces, this.getBuffer());
        Encoding.encodeVarint(this.getMesh().getNumPoints(), this.getBuffer());
        {
            // 1 = Encode indices directly.
            this.getBuffer().encode((byte)1);
            IntSpan face = IntSpan.wrap(new int[3]);
            if (this.getMesh().getNumPoints() < 256)
            {
                // Serialize indices as uint8T.
                for (int i = 0; i < numFaces; ++i)
                {
                    this.getMesh().readFace(i, face);
                    this.getBuffer().encode((byte)(face.get(0)));
                    this.getBuffer().encode((byte)(face.get(1)));
                    this.getBuffer().encode((byte)(face.get(2)));
                }
                
            }
            else if (this.getMesh().getNumPoints() < (1 << 16))
            {
                // Serialize indices as uint16T.
                for (int i = 0; i < numFaces; ++i)
                {
                    this.getMesh().readFace(i, face);
                    this.getBuffer().encode((short)(face.get(0)));
                    this.getBuffer().encode((short)(face.get(1)));
                    this.getBuffer().encode((short)(face.get(2)));
                }
                
            }
            else if (this.getMesh().getNumPoints() < (1 << 21))
            {
                // Serialize indices as varint.
                for (int i = 0; i < numFaces; ++i)
                {
                    this.getMesh().readFace(i, face);
                    Encoding.encodeVarint(face.get(0), this.buffer);
                    Encoding.encodeVarint(face.get(1), this.buffer);
                    Encoding.encodeVarint(face.get(2), this.buffer);
                }
                
            }
            else
            {
                // Serialize faces as uint (default).
                for (int i = 0; i < numFaces; ++i)
                {
                    this.getMesh().readFace(i, face);
                    this.getBuffer().encode(face);
                }
                
            }
            
        }
        
    }
    
    @Override
    protected void generateAttributesEncoder(int attId)
    {
        // Create only one attribute encoder that is going to encode all points in a
        // linear sequence.
        if (attId == 0)
        {
            // Create a new attribute encoder only for the first attribute.
            this.addAttributesEncoder(new SequentialAttributeEncodersController(new LinearSequencer(this.getPointCloud().getNumPoints()), attId));
        }
        else
        {
            // Reuse the existing attribute encoder for other attributes.
            this.attributesEncoder(0).addAttributeId(attId);
        }
        
    }
    
    private boolean compressAndEncodeIndices()
        throws DrakoException
    {
        int numFaces = this.getMesh().getNumFaces();
        IntSpan indicesBuffer = IntSpan.wrap(new int[3 * numFaces]);
        int lastIndexValue = 0;
        int p = 0;
        IntSpan face = IntSpan.wrap(new int[3]);
        for (int i = 0; i < numFaces; ++i)
        {
            this.getMesh().readFace(i, face);
            for (int j = 0; j < 3; ++j)
            {
                int indexValue = face.get(j);
                int indexDiff = indexValue - lastIndexValue;
                int encodedVal = Math.abs(indexDiff) << 1 | (indexDiff < 0 ? 1 : 0);
                indicesBuffer.put(p++, encodedVal);
                lastIndexValue = indexValue;
            }
            
        }
        
        Encoding.encodeSymbols(indicesBuffer, indicesBuffer.size(), 1, null, this.getBuffer());
        return true;
    }
    
    
}
