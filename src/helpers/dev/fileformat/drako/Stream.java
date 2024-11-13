package dev.fileformat.drako;


import java.io.*;
import java.nio.ByteBuffer;

/**
 * Created by lexchou on 12/14/2016.
 */
public abstract class Stream implements Closeable {
    public static final int SEEK_SET = 0;
    public static final int SEEK_CURRENT = 1;
    public static final int SEEK_END = 2;



    protected Stream()
    {

    }

    public int readByte() throws IOException
    {
        throw new UnsupportedOperationException();
    }
    public int read(byte[] buf) throws IOException
    {
        return read(buf, 0, buf.length);
    }

    @Internal
    public int read(ByteSpan bytes) throws IOException
    {
        return read(bytes.array, bytes.offset, bytes.length);
    }

    public int read(byte[] buf, int start, int len) throws IOException
    {
        for(int i = start; i < start + len; i++)
        {
            buf[i] = (byte)readByte();
        }
        return len;
    }

    @Internal
    public void write(ByteSpan bytes) throws IOException
    {
        write(bytes.array, bytes.offset, bytes.length);
    }

    public void write(byte[] buf) throws IOException
    {
        write(buf, 0, buf.length);

    }
    public void write(byte[] buf, int start, int len) throws IOException
    {
        for(int i =  start;i < start + len; i++)
        {
            writeByte(buf[i]);
        }
    }

    public void writeByte(int b) throws IOException
    {
        throw new UnsupportedOperationException();

    }
    public long seek(long offset, int seek) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    public void copyTo(Stream output) throws IOException {
        byte[] buffer = new byte[40960];
        while(true) {
            int n = read(buffer);
            if(n <= 0)
                break;
            output.write(buffer, 0, n);
        }
    }


    public void close() throws IOException{}
    public void flush() throws IOException{}

    public long getLength() throws IOException {
        throw new UnsupportedOperationException();
    }
    public void setLength(long len) throws IOException {
        throw new UnsupportedOperationException();
    }

    public InputStream getInputStream() { return new InputStreamAdapter();}
    public OutputStream getOutputStream() { return new OutputStreamAdapter();}

    class InputStreamAdapter extends InputStream
    {
        @Override
        public int read() throws IOException {
            return Stream.this.readByte();
        }
        @Override
        public int read(byte b[], int off, int len) throws IOException {
            if(len == 0)
                return 0;
            int ret = Stream.this.read(b, off, len);
            if(ret <= 0)
                return -1;
            return ret;
        }
        @Override
        public long skip(long n) throws IOException {
            Stream.this.seek(n, SEEK_CURRENT);
            return n;
        }

        @Override
        public int available() throws IOException {
            long length = Stream.this.getLength();
            long pos = Stream.this.seek(0, SEEK_CURRENT);
            return (int)(length - pos);
        }
    }
    class OutputStreamAdapter extends OutputStream
    {
        @Override
        public void write(int val) throws IOException {
            Stream.this.writeByte(val);
        }
        @Override
        public void write(byte b[], int off, int len) throws IOException {
            Stream.this.write(b, off, len);
        }
        @Override
        public void flush() throws IOException {
            Stream.this.flush();
        }
    }

    static class MemoryStreamAdapter extends MemoryStream
    {
        private OutputStream stream;
        public MemoryStreamAdapter(OutputStream stream)
        {
            this.stream = stream;
        }
        @Override
        public void close() throws IOException
        {
            try(OutputStream s = stream) {
                if (data != null)
                    s.write(data, 0, cursor);
            }
        }
    }

    /**
     * Wrap an OutputStream as Stream, the stream must be closed to flush data to output stream.
     * @param stream output stream to wrap
     * @return wrapped Stream instance
     */
    public static Stream wrap(OutputStream stream)
    {
        return new MemoryStreamAdapter(stream);
    }
    public static Stream wrap(InputStream stream) throws IOException
    {
        ByteArrayOutputStream mem = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        while(true)
        {
            int len = stream.read(buffer);
            if(len <= 0)
                break;
            mem.write(buffer, 0, len);
        }
        return new MemoryStream(mem.toByteArray());
    }

}
