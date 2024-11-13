package dev.fileformat.drako;


import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

/**
 * Created by lexchou on 4/17/2017.
 * Little endian binary reader
 */
@Internal
class BinaryReader implements Closeable, DataInput {
    private Stream stream;
    private byte[] buf = new byte[8];
    private Charset charset;

    private ByteBuffer byteBuffer;
    private CharBuffer charBuffer;
    private CharsetDecoder decoder;
    private int maxBytesPerChar;

    public BinaryReader(Stream stream)
    {
        this(stream, StandardCharsets.UTF_8);
    }
    public BinaryReader(Stream stream, Charset charset)
    {
        this.stream = stream;
        this.charset = charset;

    }

    public Stream getBaseStream() {
        return stream;
    }
    @Override
    public void readFully(byte[] b) throws IOException {
        stream.read(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        int offset = off;
        int rest = len;
        while(rest > 0) {
            int bytesRead = read(b, offset, len);
            if(bytesRead == 0 && rest > 0)
                throw new IOException("Insufficient data to read.");
            rest -= bytesRead;
            offset += bytesRead;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        stream.seek(n, Stream.SEEK_CURRENT);
        return 0;
    }

    private void fillBuffer(int bytes) throws IOException
    {
        readFully(buf, 0, bytes);
    }

    @Override
    public boolean readBoolean() throws IOException {
        fillBuffer(1);
        return buf[0] != 0;
    }

    @Override
    public byte readByte() throws IOException {
        fillBuffer(1);
        return buf[0];
    }

    public byte[] readBytes(int size) throws IOException {
        byte[] ret = new byte[size];
        stream.read(ret);
        return ret;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        fillBuffer(1);
        return buf[0] & 0xff;
    }

    @Override
    public short readShort() throws IOException {
        fillBuffer(2);
        return (short)((buf[1] << 8) | (buf[0] & 0xff));
    }

    @Override
    public int readUnsignedShort() throws IOException {
        fillBuffer(2);
        return (((buf[1] & 0xff) << 8) | (buf[0] & 0xff));
    }


    private void initDecoder() {
        if(decoder != null)
            return;
        decoder = charset.newDecoder();
        maxBytesPerChar = (int) charset.newEncoder().maxBytesPerChar();
        byteBuffer = ByteBuffer.allocate(maxBytesPerChar);
        charBuffer = CharBuffer.allocate(10);
    }
    @Override
    public char readChar() throws IOException {
        initDecoder();
        IOUtils.clear(byteBuffer);
        IOUtils.position(byteBuffer, 0);
        IOUtils.clear(charBuffer);
        byte[] tmp = byteBuffer.array();
        for(int i = 0; i < maxBytesPerChar; i++) {
            tmp[i] = readByte();
            IOUtils.limit(byteBuffer, i + 1);
            decoder.decode(byteBuffer, charBuffer, false);
            if(charBuffer.position() == 1) {
                IOUtils.flip(charBuffer);
                return charBuffer.get();
            }
        }
        return (char)-1;
    }

    @Override
    public int readInt() throws IOException {
        fillBuffer(4);
        return (((buf[3] & 0xff) << 24) |
                ((buf[2] & 0xff) << 16) |
                ((buf[1] & 0xff) << 8) |
                (buf[0] & 0xff));
    }

    @Override
    public long readLong() throws IOException {
        fillBuffer(8);
        return (((long)(buf[7] & 0xff) << 56) |
            ((long)(buf[6] & 0xff) << 48) |
            ((long)(buf[5] & 0xff) << 40) |
            ((long)(buf[4] & 0xff) << 32) |
            ((long)(buf[3] & 0xff) << 24) |
            ((long)(buf[2] & 0xff) << 16) |
            ((long)(buf[1] & 0xff) <<  8) |
            ((long)(buf[0] & 0xff)));
    }

    @Override
    public float readFloat() throws IOException {
        int n = readInt();
        return Float.intBitsToFloat(n);
    }

    @Override
    public double readDouble() throws IOException {
        long n = readLong();
        return Double.longBitsToDouble(n);
    }

    @Override
    public String readLine() throws IOException {
        throw new RuntimeException("Not implemented");
    }
    private int readLEB128() throws IOException {
        int result = 0;
        int shift = 0;
        while(true) {
            byte b = readByte();
            result |= (b & 0x7f) << shift;
            shift += 7;
            if((b & 0x80) == 0) {
                break;
            }
        }
        return result;
    }
    @Override
    public String readUTF() throws IOException {
        int len = readLEB128();
        char[] ret = new char[len];
        for(int i = 0; i < len; i++) {
            ret[i] = readChar();
        }
        return new String(ret);
    }

    @Override
    public void close() throws IOException {

    }
}
