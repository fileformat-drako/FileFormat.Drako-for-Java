package dev.fileformat.drako;
import dev.fileformat.drako.Stream;
import java.io.IOException;
/**
 *  Google Draco
 *
 */
public class Draco
{    
    /**
     *  Decode a {@link dev.fileformat.drako.DracoPointCloud} or {@link dev.fileformat.drako.DracoMesh} from bytes
     *
     * @param data Raw draco bytes.
     * @return a {@link dev.fileformat.drako.DracoPointCloud} or {@link dev.fileformat.drako.DracoMesh} instance
     */
    public static DracoPointCloud decode(byte[] data)
        throws DrakoException
    {
        if (data == null)
            throw new IllegalArgumentException("Argument data cannot be null");
        DecoderBuffer buffer = new DecoderBuffer(data);
        return DracoMesh.decode(buffer, true);
    }
    
    /**
     *  Encode the point cloud or mesh and get the encoded bytes in draco format.
     *
     * @param m The {@link dev.fileformat.drako.DracoPointCloud} or {@link dev.fileformat.drako.DracoMesh} instance
     * @return Bytes in draco format
     */
    public static byte[] encode(DracoPointCloud m)
        throws DrakoException
    {
        return Draco.encode(m, new DracoEncodeOptions());
    }
    
    /**
     *  Encode the point cloud or mesh and get the encoded bytes in draco format.
     *
     * @param m The {@link dev.fileformat.drako.DracoPointCloud} or {@link dev.fileformat.drako.DracoMesh} instance
     * @param options Encode options
     * @return Bytes in draco format
     */
    public static byte[] encode(DracoPointCloud m, DracoEncodeOptions options)
        throws DrakoException
    {
        if (m == null)
            throw new IllegalArgumentException("Argument m cannot be null");
        if (options == null)
            throw new IllegalArgumentException("Argument options cannot be null");
        EncoderBuffer buf = Draco.encodeImpl(m, options);
        if (buf.getData().length == buf.getBytes())
            return buf.getData();else
        {
            byte[] ret = new byte[buf.getBytes()];
            System.arraycopy(buf.getData(), 0, ret, 0, buf.getBytes());
            return ret;
        }
        
    }
    
    public static void encode(DracoPointCloud m, DracoEncodeOptions options, Stream stream)
        throws IOException, DrakoException
    {
        if (m == null)
            throw new IllegalArgumentException("Argument m cannot be null");
        if (options == null)
            throw new IllegalArgumentException("Argument options cannot be null");
        if (stream == null)
            throw new IllegalArgumentException("Argument stream cannot be null");
        EncoderBuffer buf = Draco.encodeImpl(m, options);
        stream.write(buf.getData(), 0, buf.getBytes());
    }
    
    static EncoderBuffer encodeImpl(DracoPointCloud m, DracoEncodeOptions options)
        throws DrakoException
    {
        EncoderBuffer ret = new EncoderBuffer();
        PointCloudEncoder encoder = Draco.createEncoder(m, options);
        //Encode header
        
        // Encode the header according to our v1 specification.
        // Five bytes for Draco format.
        ret.encode(new byte[] {(byte)'D', (byte)'R', (byte)'A', (byte)'C', (byte)'O'}, 5);
        byte majorVersion;
        byte minorVersion;
        if (m instanceof DracoMesh)
        {
            majorVersion = 2;
            minorVersion = 2;
        }
        else
        {
            //point cloud
            majorVersion = 2;
            minorVersion = 3;
        }
        
        ret.encode(majorVersion);
        ret.encode(minorVersion);
        // Type of the encoder (point cloud, mesh, ...).
        ret.encode((byte)(encoder.getGeometryType()));
        // Unique identifier for the selected encoding method (edgebreaker, etc...).
        ret.encode((byte)(encoder.getEncodingMethod()));
        // Reserved for flags.
        ret.encode((short)0);
        
        //encode body
        encoder.encode(options, ret);
        
        return ret;
    }
    
    private static PointCloudEncoder createEncoder(DracoPointCloud pc, DracoEncodeOptions options)
    {
        if (pc instanceof DracoMesh && (((DracoMesh)pc).getNumFaces() > 0))
        {
            MeshEncoder encoder;
            if (options.getCompressionLevel() == DracoCompressionLevel.NO_COMPRESSION)
            {
                encoder = new MeshSequentialEncoder();
            }
            else
            {
                encoder = new MeshEdgeBreakerEncoder();
            }
            
            encoder.setMesh((DracoMesh)pc);
            return encoder;
        }
        else
        {
            //check if kd-tree is possible
            // Speed < 10, use POINT_CLOUD_KD_TREE_ENCODING if possible.
            if (!Draco.isKdTreePossible(pc, options))
                throw new IllegalStateException("KD Tree encoder is not supported on this point cloud.");
            PointCloudKdTreeEncoder ret = new PointCloudKdTreeEncoder();
            ret.setPointCloud(pc);
            return ret;
        }
        
    }
    
    private static boolean isKdTreePossible(DracoPointCloud pc, DracoEncodeOptions options)
    {
        
        // Kd-Tree encoder can be currently used only when the following conditions
        // are satisfied for all attributes:
        //     -data type is float32 and quantization is enabled, OR
        //     -data type is uint32, uint16, uint8 or int32, int16, int8
        for (int i = 0; i < pc.getNumAttributes(); ++i)
        {
            PointAttribute att = pc.attribute(i);
            if (att.getDataType() != DataType.FLOAT32 && (att.getDataType() != DataType.UINT32) && (att.getDataType() != DataType.UINT16) && (att.getDataType() != DataType.UINT8) && (att.getDataType() != DataType.INT32) && (att.getDataType() != DataType.INT16) && (att.getDataType() != DataType.INT8))
                return false;
            if (att.getDataType() == DataType.FLOAT32 && (options.getQuantizationBits(att) <= 0))
                return false;
            // Quantization not enabled.
        }
        
        
        return true;
    }
    
    
}
