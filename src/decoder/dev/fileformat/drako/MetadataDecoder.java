package dev.fileformat.drako;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
class MetadataDecoder
{    
    static class MetadataTuple
    {
        public Metadata parent;
        public Metadata decoded;
        public int level;
        public MetadataTuple(Metadata parent, Metadata decoded, int level)
        {
            this.parent = parent;
            this.decoded = decoded;
            this.level = level;
        }
        
    }
    
    static final int K_MAX_SUBMETADATA_LEVEL = 1000;
    public GeometryMetadata decode(DecoderBuffer buffer)
        throws DrakoException
    {
        int numAttrMetadata = Decoding.decodeVarintU32(buffer);
        GeometryMetadata metadata = new GeometryMetadata();
        for (int i = 0; i < (0xffffffffl & numAttrMetadata); i++)
        {
            int attUniqueId = Decoding.decodeVarintU32(buffer);
            Metadata attMetadata = new Metadata();
            this.decodeMetadata(buffer, attMetadata);
            metadata.attributeMetadata.put(attUniqueId, attMetadata);
        }
        
        
        this.decodeMetadata(buffer, metadata);
        return metadata;
    }
    
    private void decodeMetadata(DecoderBuffer buffer, Metadata metadata)
        throws DrakoException
    {
        ArrayList<MetadataTuple> stack = new ArrayList<MetadataTuple>();
        stack.add(new MetadataTuple(null, metadata, 0));
        while (!stack.isEmpty())
        {
            MetadataTuple mp = stack.get(stack.size() - 1);
            stack.remove(stack.size() - 1);
            metadata = mp.decoded;
            if (mp.parent != null)
            {
                if (mp.level > K_MAX_SUBMETADATA_LEVEL)
                    throw DracoUtils.failed();
                String subMetadataName = this.decodeName(buffer);
                if (subMetadataName == null)
                    throw DracoUtils.failed();
                Metadata subMetadata = new Metadata();
                metadata = subMetadata;
                mp.parent.subMetadata.put(subMetadataName, subMetadata);
            }
            
            if (metadata == null)
                throw DracoUtils.failed();
            int numEntries = Decoding.decodeVarintU32(buffer);
            for (int i = 0; i < (0xffffffffl & numEntries); i++)
            {
                this.decodeEntry(buffer, metadata);
            }
            
            int numSubMetadata = Decoding.decodeVarintU32(buffer);
            if ((0xffffffffl & numSubMetadata) > buffer.getRemainingSize())
                throw DracoUtils.failed();
            for (int i = 0; i < (0xffffffffl & numSubMetadata); i++)
            {
                stack.add(new MetadataTuple(metadata, null, mp.parent != null ? mp.level + 1 : mp.level));
            }
            
        }
        
    }
    
    private void decodeEntry(DecoderBuffer buffer, Metadata metadata)
        throws DrakoException
    {
        String entryName = this.decodeName(buffer);
        if (entryName == null)
            throw DracoUtils.failed();
        int dataSize = Decoding.decodeVarintU32(buffer);
        if (dataSize == 0 || ((0xffffffffl & dataSize) > buffer.getRemainingSize()))
            throw DracoUtils.failed();
        byte[] entryValue = new byte[dataSize];
        if (!buffer.decode(entryValue, dataSize))
            throw DracoUtils.failed();
        metadata.entries.put(entryName, entryValue);
    }
    
    private String decodeName(DecoderBuffer buffer)
        throws DrakoException
    {
        int nameLen = Decoding.decodeVarintU32(buffer);
        byte[] bytes = new byte[nameLen];
        if (!buffer.decode(bytes, bytes.length))
            return null;
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
    }
    
    
}
