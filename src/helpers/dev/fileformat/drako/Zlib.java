package dev.fileformat.drako;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * Created by lexchou on 6/22/2017.
 */
@Internal
final class Zlib {


    /**
     * Compress data using zlib format
     * @param data
     * @return
     */
    public static byte[] compress(byte[] data) {
        return compress(data, 0, data.length);
    }
    /**
     * Compress data using zlib format
     * @param data
     * @return
     */
    public static byte[] compress(byte[] data, int start, int length) {
        return deflate(data, start, length, false);
    }

        /**
         * Decompress zlib data
         * @param data
         * @return
         */
    public static byte[] decompress(byte[] data, int off, int len) {
        return inflate(data, off, len, false);
    }
    public static byte[] decompress(byte[] data) {
        return decompress(data, 0, data.length);
    }

    public static void decompressToStream(byte[] data, int off, int len, Stream output, byte[] buffer) {
        InflaterOutputStream inflater = null;
        try {
            OutputStream  outputStream = output.getOutputStream();
            inflater = new InflaterOutputStream(outputStream, new Inflater(false));
            inflater.write(data, off, len);
            inflater.close();
        } catch (IOException e) {
            AsposeUtils.safeClose(inflater);
        }
    }
    /**
     * Decompress data compressed using deflate algorithm
     * @param data
     * @return
     */
    public static byte[] inflate(byte[] data) {
        return inflate(data, 0, data.length, true);
    }
    private static byte[] inflate(byte[] data, int off, int len, boolean nowrap) {
        InflaterOutputStream out = null;
        try {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            out = new InflaterOutputStream(tmp, new Inflater(nowrap));
            out.write(data, off, len);
            out.close();
            return tmp.toByteArray();
        } catch (IOException e) {
            AsposeUtils.safeClose(out);
            return null;
        }
    }
    /**
     * Compress data using deflate algorithm
     * @param data
     * @return
     */
    public static byte[] deflate(byte[] data) {
        return deflate(data, 0, data.length, true);
    }

    private static byte[] deflate(byte[] data, int start, int length, boolean nowrap) {
        DeflaterOutputStream out = null;
        try {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            out = new DeflaterOutputStream(tmp, new Deflater(Deflater.DEFAULT_COMPRESSION, nowrap));
            out.write(data, start, length);
            out.close();
            return tmp.toByteArray();
        } catch (IOException e) {
            AsposeUtils.safeClose(out);
            return null;
        }

    }
}
