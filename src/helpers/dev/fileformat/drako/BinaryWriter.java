package dev.fileformat.drako;


import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Created by lexchou on 12/30/2016.
 * Little endian binary writer
 */
@Internal
class BinaryWriter implements Closeable, DataOutput {
    private Stream out;
    private Charset charset;
    private byte[] writeBuffer = new byte[8];

    private CharsetEncoder encoder;
    private CharBuffer charBuffer = CharBuffer.allocate(1);
    private ByteBuffer wrappedBuffer = ByteBuffer.wrap(writeBuffer);


    public BinaryWriter(Stream stream)
    {
        this(stream, StandardCharsets.UTF_8);
    }
    public BinaryWriter(Stream stream, Charset charset) {
        this.out = stream;
        this.charset = charset;
        encoder = charset.newEncoder();
    }
    public Stream getBaseStream() {
        return out;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public void write(int n) throws IOException{
        out.writeByte(n);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        write(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        writeBuffer[0] = (byte)(v >>> 0);
        writeBuffer[1] = (byte)(v >>> 8);
        out.write(writeBuffer, 0, 2);


    }

    @Override
    public void writeChar(int v) throws IOException {
        //writeBuffer[0] = (byte)(v >>> 0);
        //writeBuffer[1] = (byte)(v >>> 8);
        clear(charBuffer);
        //charBuffer.array()[0] = (char)v;
        charBuffer.put((char)v);
        flip(charBuffer);
        clear(wrappedBuffer);
        encoder.encode(charBuffer, wrappedBuffer, false);
        flip(wrappedBuffer);
        writeBytes(wrappedBuffer);
    }
    private void write(ByteBuffer buf) throws IOException {
        int offset = buf.arrayOffset() + buf.position();
        int len = buf.limit();
        byte[] arr = buf.array();
        out.write(arr, offset, len);
    }

    @Override
    public void writeInt(int v) throws IOException {
        BitUtils.toBytes(writeBuffer, 0, v);
        out.write(writeBuffer, 0, 4);
        /*
        out.write((v >>>  0) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
        */
    }

    @Override
    public void writeLong(long v) throws IOException {
        /*
        writeBuffer[7] = (byte)(v >>> 56);
        writeBuffer[6] = (byte)(v >>> 48);
        writeBuffer[5] = (byte)(v >>> 40);
        writeBuffer[4] = (byte)(v >>> 32);
        writeBuffer[3] = (byte)(v >>> 24);
        writeBuffer[2] = (byte)(v >>> 16);
        writeBuffer[1] = (byte)(v >>>  8);
        writeBuffer[0] = (byte)(v >>>  0);
        */
        BitUtils.toBytes(writeBuffer, 0, v);
        out.write(writeBuffer, 0, 8);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            out.writeByte((byte)s.charAt(i));
        }
    }
    public void writeBytes(ByteBuffer s) throws IOException {
        int len = s.limit();
        int offset = s.arrayOffset();
        byte[] array = s.array();
        out.write(array, offset, len);
    }

    private void writeLEB128(int value) throws IOException {
        if(value == 0)
        {
            writeByte(0);
            return;
        }
        int pos = 0;
        byte[] buf = writeBuffer;
        while (value != 0) {
            buf[pos++] = (byte)(value & 0x7F | 0x80);
            value >>= 7;
        }
        buf[pos-1] &= 0x7F;
        write(buf, 0, pos);
    }

    @Override
    public void writeChars(String s) throws IOException {
        ByteBuffer buf = encoder.encode(CharBuffer.wrap(s));
        writeLEB128(s.length());
        writeBytes(buf);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        writeChars(s);
    }

    public void flush() throws IOException{
        out.flush();
    }
    private static void clear(Buffer buffer)
    {
        buffer.clear();
    }
    private static void flip(Buffer buffer)
    {
        buffer.flip();
    }
}
