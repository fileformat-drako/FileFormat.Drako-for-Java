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
    {
        int numAttrMetadata = 0;
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        if (!Decoding.decodeVarint(ref0, buffer))
        {
            numAttrMetadata = ref0[0];
            return null;
        }
        else
        {
            numAttrMetadata = ref0[0];
        }
        
        GeometryMetadata metadata = new GeometryMetadata();
        for (int i = 0; i < (0xffffffffl & numAttrMetadata); i++)
        {
            int attUniqueId;
            if (!Decoding.decodeVarint(ref1, buffer))
            {
                attUniqueId = ref1[0];
                return null;
            }
            else
            {
                attUniqueId = ref1[0];
            }
            
            Metadata attMetadata = new Metadata();
            if (!this.decodeMetadata(buffer, attMetadata))
                return null;
            metadata.attributeMetadata.put(attUniqueId, attMetadata);
        }
        
        
        this.decodeMetadata(buffer, metadata);
        return metadata;
    }
    
    private boolean decodeMetadata(DecoderBuffer buffer, Metadata metadata)
    {
        ArrayList<MetadataTuple> stack = new ArrayList<MetadataTuple>();
        final int[] ref2 = new int[1];
        final int[] ref3 = new int[1];
        stack.add(new MetadataTuple(null, metadata, 0));
        while (!stack.isEmpty())
        {
            MetadataTuple mp = stack.get(stack.size() - 1);
            stack.remove(stack.size() - 1);
            metadata = mp.decoded;
            if (mp.parent != null)
            {
                if (mp.level > K_MAX_SUBMETADATA_LEVEL)
                    return false;
                String subMetadataName = this.decodeName(buffer);
                if (subMetadataName == null)
                    return false;
                Metadata subMetadata = new Metadata();
                metadata = subMetadata;
                mp.parent.subMetadata.put(subMetadataName, subMetadata);
            }
            
            if (metadata == null)
                return false;
            int numEntries = 0;
            if (!Decoding.decodeVarint(ref2, buffer))
            {
                numEntries = ref2[0];
                return false;
            }
            else
            {
                numEntries = ref2[0];
            }
            
            for (int i = 0; i < (0xffffffffl & numEntries); i++)
            {
                if (!this.decodeEntry(buffer, metadata))
                    return false;
            }
            
            int numSubMetadata = 0;
            if (!Decoding.decodeVarint(ref3, buffer))
            {
                numSubMetadata = ref3[0];
                return false;
            }
            else
            {
                numSubMetadata = ref3[0];
            }
            
            if ((0xffffffffl & numSubMetadata) > buffer.getRemainingSize())
                return false;
            for (int i = 0; i < (0xffffffffl & numSubMetadata); i++)
            {
                stack.add(new MetadataTuple(metadata, null, mp.parent != null ? mp.level + 1 : mp.level));
            }
            
        }
        
        
        return true;
    }
    
    private boolean decodeEntry(DecoderBuffer buffer, Metadata metadata)
    {
        String entryName = this.decodeName(buffer);
        final int[] ref4 = new int[1];
        if (entryName == null)
            return false;
        int dataSize = 0;
        if (!Decoding.decodeVarint(ref4, buffer))
        {
            dataSize = ref4[0];
            return false;
        }
        else
        {
            dataSize = ref4[0];
        }
        
        if (dataSize == 0 || ((0xffffffffl & dataSize) > buffer.getRemainingSize()))
            return false;
        byte[] entryValue = new byte[dataSize];
        if (!buffer.decode(entryValue, dataSize))
            return false;
        metadata.entries.put(entryName, entryValue);
        return true;
    }
    
    private String decodeName(DecoderBuffer buffer)
    {
        int nameLen = 0;
        final int[] ref5 = new int[1];
        if (!Decoding.decodeVarint(ref5, buffer))
        {
            nameLen = ref5[0];
            return null;
        }
        else
        {
            nameLen = ref5[0];
        }
        
        byte[] bytes = new byte[nameLen];
        if (!buffer.decode(bytes, bytes.length))
            return null;
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
    }
    
    
}
