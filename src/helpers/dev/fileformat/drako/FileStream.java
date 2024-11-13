package dev.fileformat.drako;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by lexchou on 6/16/2017.
 */
public class FileStream extends Stream{
    public static final int CREATE = 0;
    public static final int OPEN = 1;
    public static final int APPEND = 2;

    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = READ | WRITE;
    private static final int WRITE_BUF_SIZE = 2048;
    private static final int READ_BUF_SIZE = 2048;

    RandomAccessFile ra;
    private long cursor;
    private long length;
    private byte[] writeBuf;
    private int writeBufSize = 0;
    private byte[] readBuf;
    private int readBufSize = 0;
    private int readBufStart = 0;
    private byte[] tmp = new byte[1];


    public FileStream(String fileName, int fileMode, int fileAccess) throws IOException {
        init(fileName, fileMode, fileAccess);
    }
    public FileStream(String fileName, int fileMode)
            throws IOException
    {
        init(fileName, fileMode, fileMode == APPEND ? WRITE : READ_WRITE);
    }

    public FileStream(File file, int fileMode, int fileAccess)
            throws IOException
    {
        init(file, fileMode, fileAccess);
    }
    private void init(String fileName, int fileMode, int fileAccess)
            throws IOException
    {
        File f = new File(fileName);
        if(!f.exists() && fileMode != CREATE)
            throw new FileNotFoundException();
        init(f, fileMode, fileAccess);
    }
    private void init(File file, int fileMode, int fileAccess)
            throws IOException
    {
        if(fileMode == CREATE && file.exists())
            file.delete();
        String mode = null;
        if(fileAccess == READ)
            mode = "r";
        else if(fileAccess == WRITE)
            mode = "rw";
        else if(fileAccess == READ_WRITE)
            mode = "rw";
        else
            throw new IOException("Invalid file access.");

        this.ra = new RandomAccessFile(file, mode);
        length = ra.length();
        if((fileAccess & WRITE) != 0)
            writeBuf = new byte[WRITE_BUF_SIZE];
        if((fileAccess & READ) != 0)
            readBuf = new byte[READ_BUF_SIZE];
    }
    public long getLength() throws IOException {
        return ra.length();
    }
    public void setLength(long len) throws IOException {
        ra.setLength(len);
    }

    @Override
    public int readByte() throws IOException {
        int bytesRead = read(tmp, 0, 1);
        if(bytesRead == 0)
            return -1;
        return tmp[0] & 0xff;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    @Override
    public int read(byte[] buf, int start, int len) throws IOException {
        int p = start;
        int end = start + len;
        boolean eof = false;
        long available = length - cursor;
        while(p < end && available > 0) {
            int expectToRead = (int)Math.min(available, Math.min(end - p, readBufSize));
            if(expectToRead == 0) {
                int t = ra.read(readBuf, 0, readBuf.length);
                if(t == -1) {
                    eof = true;
                    //really no data to read
                    break;
                }
                readBufStart = 0;
                readBufSize = t;
                expectToRead = Math.min(end - p, readBufSize);
            }
            if(expectToRead == -1)//really no data to read
                break;
            System.arraycopy(readBuf, readBufStart, buf, p, expectToRead);
            p += expectToRead;
            readBufStart += expectToRead;
            readBufSize -= expectToRead;
            available -= expectToRead;
        }
        int bytesToRead = p -start;
        cursor += bytesToRead;
        return bytesToRead;
    }

    @Override
    public void write(byte[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(byte[] buf, int start, int len) throws IOException {
        int p = start;
        int end = start + len;
        int bytesToWrite = 0;
        while(p < end) {
            int rest = end - p;
            int cap = WRITE_BUF_SIZE - writeBufSize;
            int write = Math.min(cap, rest);
            if(write == 0) {
                flush();
                write = Math.min(cap, rest);
            }
            System.arraycopy(buf, p, writeBuf, writeBufSize, write);
            p += write;
            writeBufSize += write;
            bytesToWrite += write;
        }
        cursor += bytesToWrite;
        if(cursor > length)
            length = cursor;
        //ra.write(buf, start, len);
    }

    @Override
    public void writeByte(int b) throws IOException {
        tmp[0] = (byte)b;
        write(tmp, 0, 1);
    }

    @Override
    public long seek(long offset, int seek) throws IOException {
        if(offset == 0 && seek == SEEK_CURRENT)
            return cursor;
        long newCursor = 0L;
        flush();
        switch (seek)
        {
            case Stream.SEEK_SET:
                newCursor = offset;
                break;
            case Stream.SEEK_CURRENT:
                newCursor = cursor + offset;
                break;
            case Stream.SEEK_END:
                newCursor = length + offset;
                break;
            default:
                throw new IllegalStateException("Unknown SeekOrigin type.");
        }
        if(newCursor != cursor) {
            cursor = newCursor;
            ra.seek(newCursor);
            readBufSize = 0;
            readBufStart = 0;
        }
        return cursor;
    }

    @Override
    public void close() throws IOException {
        flush();
        ra.close();
    }

    @Override
    public void flush() throws IOException {
        if(writeBufSize > 0) {
            ra.write(writeBuf, 0, writeBufSize);
            writeBufSize = 0;
        }
    }
}
