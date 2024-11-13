package dev.fileformat.drako;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by lexchou on 6/22/2017.
 */
@Internal
class StreamWriter extends TextWriter {
    private boolean autoFlush;
    private Stream stream;
    private CharBuffer chars = CharBuffer.allocate(20);
    private ByteBuffer bytes = ByteBuffer.allocate(50);

    public StreamWriter(Stream stream) {
        this(stream, StandardCharsets.UTF_8);
    }
    public StreamWriter(Stream stream, Charset charset) {
        super(charset);
        this.stream = stream;
    }

    public boolean isAutoFlush() {
        return autoFlush;
    }
    public void setAutoFlush(boolean val) {
        autoFlush = val;
    }

    public void write(CharSequence value) throws IOException {
        if(value == null || value.length() == 0)
            return;
        ByteBuffer bytes = encoder.encode(CharBuffer.wrap(value));
        encoder.reset();
        byte[] arr = bytes.array();
        int len = bytes.limit();
        stream.write(arr, 0, len);
    }

    private void resetBuffers() {
        IOUtils.clear(bytes);
        IOUtils.clear(chars);
    }
    private void encodeBuffer() throws IOException {
        IOUtils.flip(chars);
        encoder.encode(chars, bytes, false);
        encoder.reset();

        byte[] arr = bytes.array();
        int len = bytes.position();
        stream.write(arr, 0, len);
    }

    public void write(char ch)  throws IOException{
        resetBuffers();
        chars.put(ch);
        encodeBuffer();
    }
    public void write(float i)  throws IOException{
        resetBuffers();
        chars.put(Float.toString(i));
        encodeBuffer();
    }
    public void write(double i)  throws IOException{
        resetBuffers();
        chars.put(Double.toString(i));
        byte[] bytes = null;
        encodeBuffer();
    }

    public void write(boolean bool) throws IOException {
        resetBuffers();
        chars.put(bool ? "true" : "false");
        encodeBuffer();
    }
    public void write(int i) throws IOException {
        resetBuffers();
        chars.put(Integer.toString(i));
        encodeBuffer();
    }
    public void write(long i) throws IOException {
        resetBuffers();
        chars.put(Long.toString(i));
        encodeBuffer();
    }

    public void flush() throws IOException {
        stream.flush();
    }
    public void setNewLine(String newLine) {
        eol = newLine;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
