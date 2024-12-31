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
        throws DrakoException
    {
        byte[] header = new byte[5];
        if (!buffer.decode(header))
            return null;
        if (DracoUtils.compare(header, HEADER, HEADER.length) != 0)
            return null;
        DracoHeader ret = new DracoHeader();
        ret.encoderType = EncodedGeometryType.INVALID;
        ret.method = DracoEncodingMethod.SEQUENTIAL;
        ret.major = buffer.decodeU8();
        ret.minor = buffer.decodeU8();
        ret.version = ret.major * 10 + (0xff & ret.minor);
        byte t = buffer.decodeU8();
        ret.encoderType = (int)(0xff & t);
        t = buffer.decodeU8();
        ret.method = (int)(0xff & t);
        ret.flags = buffer.decodeU16();
        return ret;
    }
    
    
}
