package dev.fileformat.drako;


import java.io.IOException;
import java.io.OutputStream;

/**
 * Base class of struct array
 */
public abstract class Structs {
    private static final byte[] Empty = new byte[0];
    private byte[] data;
    private int length;
    private int structSize;

    protected Structs(int length, int structSize)
    {
        this.length = length;
        this.structSize = structSize;
        if(length == 0)
            this.data = Empty;
        else
            this.data = new byte[length * structSize];
    }


    /**
     * Length of this array
     * @return number of elements in this array
     */
    public final int length() {
        return length;
    }

    /**
     * Size in bytes of single struct
     * @return the size of the struct
     */
    public final int structSize() {
        return structSize;
    }

    /**
     * Gets the byte offset of the specified struct identified by index
     * @param  index the index of the array
     * @return byte offset to the index-th struct.
     */
    protected final int structOffset(int index) {
        return index * structSize;
    }

    @Override
    public String toString() {
        return String.format("%d structs", length);
    }

    /**
     * Write the content to specified stream
     * @param stream  Which stream to write the struct to
     * @throws IOException when failed to write to stream.
     */
    public void writeTo(Stream stream) throws IOException {
        if(length > 0)
            stream.write(data);

    }
    public void writeTo(OutputStream stream) throws IOException {
        if(length > 0)
            stream.write(data);
    }

}
