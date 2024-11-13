package dev.fileformat.drako;


import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by lexchou on 12/7/2017.
 */
@Internal
class Charsets {
    public static final int BOM_UTF_8 = 0x13;
    public static final int BOM_UTF_16BE = 0x22;
    public static final int BOM_UTF_16LE = 0x32;
    public static final int BOM_UTF_32BE = 0x44;
    public static final int BOM_UTF_32LE = 0x54;
    public static final int BOM_UNSUPPORTED = 0;

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
    public static final Charset UTF_16LE = Charset.forName("UTF-16LE");
    public static final Charset ASCII = Charset.forName("US-ASCII");

    /**
     * Parse the byte-order-mark
     * @param arr
     * @param start
     * @param len
     * @return
     */
    public static int parseByteOrderMark(byte[] arr, int start, int len) {
        int size = Math.min(len, arr.length - start);
        if(size >= 4) {
            //check UTF-32 BOM
            long a = 0xff & arr[start + 0];
            long b = 0xff & arr[start + 1];
            long c = 0xff & arr[start + 2];
            long d = 0xff & arr[start + 3];
            long bom = (a << 24) | (b << 16) | (c << 8) | d;
            if(bom == 0xFEFF)
                return BOM_UTF_32BE;
            if(bom == 0xFFFE0000)
                return BOM_UTF_32LE;
        }
        if(size >= 3) {
            //check UTF-8 BOM
            int a = 0xff & arr[start + 0];
            int b = 0xff & arr[start + 1];
            int c = 0xff & arr[start + 2];
            int bom = (a << 16) | (b << 8) | c;
            if(bom == 0xEFBBBF)
                return BOM_UTF_8;
        }
        if(size >= 2) {
            //check UTF-16 BOM
            int a = 0xff & arr[start + 0];
            int b = 0xff & arr[start + 1];
            int bom = (a << 8) | b;
            if(bom == 0xFEFF)
                return BOM_UTF_16BE;
            if(bom == 0xFFFE)
                return BOM_UTF_16LE;
        }
        return BOM_UNSUPPORTED;
    }

    /**
     * Gets the number of bytes used by the byte-order-mark
     * @param bom
     * @return
     */
    public static int getBOMLength(int bom) {
        return bom & 0xf;
    }

    /**
     * Return the charset that identified by the byte-order-mark
     * @param bom
     * @return
     */
    public static Charset charsetFromBOM(int bom) {
        switch(bom) {
            case BOM_UTF_8:
                return UTF_8;
            case BOM_UTF_16BE:
                return UTF_16BE;
            case BOM_UTF_16LE:
                return UTF_16LE;
            default:
                return ASCII;
        }

    }

    /**
     * Encode the string into bytes and return the number of bytes encoded
     * @param charset
     * @param str
     * @param charIdx
     * @param charCount
     * @param buffer
     * @param bufferIdx
     * @return
     */
    public static int getBytes(Charset charset, String str, int charIdx, int charCount, byte[] buffer, int bufferIdx) {
        CharBuffer cb = CharBuffer.wrap(str, charIdx, charCount + charIdx);
        return getBytesImpl(charset, cb, buffer, bufferIdx);

    }

    /**
     * Encode the string into bytes and return the number of bytes encoded
     * @param charset
     * @param str
     * @param charIdx
     * @param charCount
     * @param buffer
     * @param bufferIdx
     * @return
     */
    public static int getBytes(Charset charset, char[] str, int charIdx, int charCount, byte[] buffer, int bufferIdx) {
        CharBuffer cb = CharBuffer.wrap(str, charIdx, charCount + charIdx);
        return getBytesImpl(charset, cb, buffer, bufferIdx);
    }

    /**
     * Encode the string into bytes and return the number of bytes encoded
     * @param charset
     * @param chars
     * @param buffer
     * @param bufferIdx
     * @return
     */
    private static int getBytesImpl(Charset charset, CharBuffer chars, byte[] buffer, int bufferIdx) {
        ByteBuffer bytes = charset.encode(chars);
        int size = Math.min(bytes.limit(), buffer.length - bufferIdx);
        if(size > 0) {
            bytes.get(buffer, bufferIdx, size);
        }
        return size;
    }
}
