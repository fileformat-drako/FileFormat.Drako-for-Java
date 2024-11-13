package dev.fileformat.drako;



import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

/**
 * Created by lexchou on 11/10/2017.
 */
@Internal
class StreamReader extends Reader implements Closeable {
    private CharsetDecoder decoder;
    private Stream stream;
    private boolean eos;
    private int totalRead;
    private int charsRead;
    private ByteBuffer buffer;
    private boolean skipBOM = true;
    private Charset charset;
    private CharBuffer charBuffer = CharBuffer.allocate(512);

    public StreamReader(Stream stream) throws IOException {
        this(stream, StandardCharsets.UTF_8, 512);
    }
    public StreamReader(Stream stream, Charset charset, int buffer) throws IOException {
        this.stream = stream;
        this.charset = charset;
        this.buffer = ByteBuffer.allocate(buffer);
        IOUtils.flip(charBuffer);
    }
    private CharsetDecoder getDecoder() {
        if(decoder == null) {
            Charset cs = charset == null ? Charset.defaultCharset() : charset;
            decoder = cs.newDecoder().onMalformedInput(CodingErrorAction.IGNORE);
        }
        return decoder;
    }
    private boolean fill() throws IOException {
        if(charBuffer.hasRemaining())
            return true;//no need to read
        if(eos) {
            return false;
        }
        CoderResult cr = fillImpl();
        if(cr == null)
            return false;
        if(!charBuffer.hasRemaining() && cr.isMalformed() && !eos) {
            //failed to decode, read more bytes and try again
            if(debug)
                System.out.printf("Failed to decode buffer, cr = %s;%s\n", cr.toString(), this.toString());
        }
        return charBuffer.hasRemaining();
    }
    private int fillCount = 0;
    private CoderResult fillImpl() throws IOException {
        byte[] arr = buffer.array();

        if(!buffer.hasRemaining()) {
            IOUtils.clear(buffer);
        }
        int size = buffer.remaining();
        int pos = buffer.position();
        int bytesRead = stream.read(arr, pos, size);
        if(debug)
            System.out.printf("%d = read(arr, %d, %d)\n", bytesRead, pos, size);
        eos = bytesRead <= 0 || bytesRead < size;
        if(bytesRead == -1)
            return null;
        int skip = 0;
        if(totalRead == 0 && skipBOM) {
            int bom = Charsets.parseByteOrderMark(arr, buffer.position(), bytesRead);
            skip = Charsets.getBOMLength(bom);
            if(bom != Charsets.BOM_UNSUPPORTED)
                charset = Charsets.charsetFromBOM(bom);
        }
        IOUtils.position(buffer, skip);
        IOUtils.limit(buffer, bytesRead + pos);
        if(debug)
            System.out.printf("buf = %d,%d\n", buffer.position(), buffer.limit());
        totalRead += bytesRead;
        return decode();
    }
    private CoderResult decode() {
        IOUtils.limit(charBuffer, charBuffer.capacity());
        IOUtils.position(charBuffer, 0);
        CharsetDecoder decoder = getDecoder();
        CoderResult cr = decoder.decode(buffer, charBuffer, eos);
        if(eos)
            decoder.flush(charBuffer);
        buffer.compact();
        IOUtils.flip(charBuffer);
        return cr;
    }

    public boolean isEndOfStream() {
        if(charBuffer.hasRemaining())
            return false;//has buffered content
        return eos;
    }
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("StreamReader[");
        ret.append(String.format("cbuf= %d", charBuffer.remaining()));
        ret.append(String.format("buf=%d;", buffer.remaining()));
        ret.append(String.format("eof=%d;", eos));
        ret.append(String.format("pos=%d/%d;", totalRead, charsRead));
        ret.append(charBuffer);
        ret.append("]");
        return ret.toString();
    }
    public String readLine()  throws IOException {
        if(isEndOfStream())
            return null;
        StringBuilder sb = new StringBuilder();
        char[] ch = charBuffer.array();
        boolean lineFound = false;
        while(!lineFound) {
            if(!fill())
                break;
            int start = charBuffer.position();
            int eol = charBuffer.limit();
            int len = eol - start;
            int newPos = eol;
            for(int p = start; p < eol; p++) {
                if (ch[p] == '\n') {
                    len = p - start;
                    newPos = p + 1;
                    lineFound = true;
                    break;
                }
            }
            sb.append(ch, start, len);
            charsRead += len;
            IOUtils.position(charBuffer, newPos);
        }
        fill();
        if(sb.length() > 0 && sb.charAt(sb.length() - 1) == '\r')
            sb.setLength(sb.length() - 1);
        return sb.toString();
    }
    public String readToEnd() throws IOException {
        if(isEndOfStream())
            return null;
        StringBuilder sb = new StringBuilder();
        char[] ch = charBuffer.array();
        while(fill()) {
            int start = charBuffer.position();
            int eol = charBuffer.limit();
            int len = eol - start;
            sb.append(ch, start, len);
            charsRead += len;
            IOUtils.position(charBuffer, eol);
        }
        return sb.toString();
    }
    public int read() throws IOException {
        if(!fill()) {
            return -1;
        }
        char ret = charBuffer.get();
        if(debug) {
            System.out.print(ret);
            System.out.printf("    cbuf=[%d,%d], buf=[%d,%d]\n", charBuffer.position(), charBuffer.limit(), buffer.position(), buffer.limit());
        }
        charsRead++;
        return ret;
    }

    private boolean debug = false;
    @Override
    public boolean equals(Object rhs) {
        if("debug:on".equals(rhs))
            debug = true;
        else if("debug:off".equals(rhs))
            debug = false;
        else
            return super.equals(rhs);
        return false;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if(len == 0)
            return 0;
        int end = off + len;
        int ret = 0;
        for(int i = off; i < end; i++) {
            int ch = read();
            if (ch == -1)
                break;
            cbuf[i] = (char) ch;
            ret++;
        }
        if(ret == 0)
            return -1;
        return ret;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
