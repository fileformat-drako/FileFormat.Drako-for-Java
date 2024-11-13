package dev.fileformat.drako;


import java.io.*;

/**
 * Created by lexchou on 6/13/2017.
 */
public class MemoryStream extends Stream implements AutoCloseable {

    private static final int INITIAL_CAPACITY = 4 * 1024;
    protected byte[] data;
    protected int cursor;
    protected int size;

    public MemoryStream()
    {
        this.data = new byte[INITIAL_CAPACITY];
    }
    public MemoryStream(int capacity)
    {
        this.data = new byte[capacity];
    }
    public MemoryStream(byte[] data)
    {
        this.data = data;
        size = data.length;
    }
    private final void setCursor(int c) {
        cursor = c;
    }
    public int getSize() {
        return size;
    }
    public byte[] getBuffer() {
        return data;
    }
    private void ensureCapacity(int expected)
    {
        if(data != null && data.length >= expected)
            return;
        int newCap = data == null ? 0 : data.length;
        if(newCap == 0)
            newCap = INITIAL_CAPACITY;
        while(newCap < expected)
            newCap *= 2;
        setCapacity(newCap);
    }

    /**
     * Sets the capacity to specified value
     * @param cap new capacity of the memory stream.
     */
    public void setCapacity(int cap) {
        if(cap < 0)
            throw new IllegalArgumentException();
        byte[] newData = new byte[cap];
        if(data != null)
            System.arraycopy(data, 0, newData, 0, Math.min(cap, data.length));
        data = newData;
        if(size > cap)
            size = cap;
    }

    public long getLength() {
        return size;
    }


    public void setLength(long len) {
        ensureCapacity((int)len);
        size = (int)len;
        if(cursor > size)
            setCursor(size);
    }
    @Override
    public void flush() throws IOException {

    }
    @Override
    public int readByte() throws IOException {
        if(cursor >= data.length)
            return -1;
        validateCursor();
        int n = cursor;
        setCursor(cursor + 1);
        return data[n];
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    @Override
    public int read(byte[] buf, int start, int len) throws IOException {
        validateCursor();
        int l = Math.max(0, Math.min(Math.min(size - start, len), Math.min(size - cursor, len)));
        if(l == 0)
            return 0;
        System.arraycopy(data, cursor, buf, start, l);
        setCursor(cursor + l);
        return l;
    }

    @Override
    public void write(byte[] buf) throws IOException {
        validateCursor();
        write(buf, 0, buf.length);
    }

    @Override
    public void write(byte[] buf, int start, int len) throws IOException {
        validateCursor();
        ensureCapacity(cursor + len);
        System.arraycopy(buf, start, data, cursor, len);
        setCursor(cursor + len);
        if(cursor > size)
            size = cursor;
    }

    @Override
    public void writeByte(int b) throws IOException {
        validateCursor();
        ensureCapacity(cursor + 1);
        data[cursor] = (byte)b;
        setCursor(cursor + 1);
        size++;
    }

    @Override
    public long seek(long offset, int seek) throws IOException {
        switch(seek)
        {
            case SEEK_SET:
                setCursor((int)offset);
                break;
            case SEEK_CURRENT:
                setCursor(cursor + (int)offset);
                break;
            case SEEK_END:
                setCursor(size + (int)offset);
                break;
        }
        return cursor;
    }
    private void validateCursor() {
        if(cursor < 0 || cursor > size)
            throw new IllegalStateException("Invalid cursor");
    }

    public byte[] toArray() {
        byte[] ret = new byte[size];
        if(size == 0)
            return ret;
        System.arraycopy(data, 0, ret, 0, size);
        return ret;
    }

    @Override
    public void close() throws IOException {

    }
    public void copyTo(Stream stream) throws IOException{
        int rest = size - cursor;
        if(rest > 0) {
            stream.write(data, cursor, rest);
        }
    }
    public void copyTo(OutputStream stream) throws IOException{
        int rest = size - cursor;
        if(rest > 0) {
            stream.write(data, cursor, rest);
        }
    }

}
