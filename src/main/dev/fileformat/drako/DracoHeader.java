package dev.fileformat.drako;
class DracoHeader
{    
    public static final int METADATA_FLAG_MASK = 0x8000;
    private static final byte[] HEADER = {(byte)'D', (byte)'R', (byte)'A', (byte)'C', (byte)'O'};
    private byte major;
    private byte minor;
    public int version;
    public short flags;
    public int encoderType;
    public int method;
    public static DracoHeader parse(DecoderBuffer buffer)
    {
        byte[] header = new byte[5];
        final byte[] ref0 = new byte[1];
        final byte[] ref1 = new byte[1];
        final byte[] ref2 = new byte[1];
        final byte[] ref3 = new byte[1];
        final short[] ref4 = new short[1];
        if (!buffer.decode(header))
            return null;
        if (DracoUtils.compare(header, HEADER, HEADER.length) != 0)
            return null;
        DracoHeader ret = new DracoHeader();
        ret.encoderType = EncodedGeometryType.INVALID;
        ret.method = DracoEncodingMethod.SEQUENTIAL;
        if (!buffer.decode3(ref0))
        {
            ret.major = ref0[0];
            return null;
        }
        else
        {
            ret.major = ref0[0];
        }
        
        if (!buffer.decode3(ref1))
        {
            ret.minor = ref1[0];
            return null;
        }
        else
        {
            ret.minor = ref1[0];
        }
        
        ret.version = ret.major * 10 + (0xff & ret.minor);
        byte t;
        if (!buffer.decode3(ref2))
        {
            t = ref2[0];
            return null;
        }
        else
        {
            t = ref2[0];
        }
        
        ret.encoderType = (int)(0xff & t);
        if (!buffer.decode3(ref3))
        {
            t = ref3[0];
            return null;
        }
        else
        {
            t = ref3[0];
        }
        
        ret.method = (int)(0xff & t);
        if (!buffer.decode(ref4))
        {
            ret.flags = ref4[0];
            return null;
        }
        else
        {
            ret.flags = ref4[0];
        }
        
        return ret;
    }
    
    
}
