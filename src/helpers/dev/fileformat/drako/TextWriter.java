package dev.fileformat.drako;


import java.io.Closeable;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Created by lexchou on 12/30/2016.
 */
@Internal
class TextWriter implements Closeable {
    protected String eol = "\r\n";
    protected CharsetEncoder encoder;


    protected TextWriter()
    {
        this(StandardCharsets.UTF_8);
    }
    protected TextWriter(Charset charset)
    {
        encoder = charset.newEncoder();
    }


    public void write(CharSequence value) throws IOException {
    }

    public void write(char ch)  throws IOException{
    }

    public void write(boolean bool)  throws IOException{

    }
    public void write(int i)  throws IOException{

    }
    public void write(float i)  throws IOException{

    }
    public void write(double i)  throws IOException{

    }
    public void write(long i)  throws IOException{

    }

    public void writeLine() throws IOException {
        write(eol);
    }
    public void writeLine(String value)  throws IOException{
        write(value);
        writeLine();
    }

    public void flush() throws IOException {

    }
    public void setNewLine(String newLine) {
        eol = newLine;
    }

    @Override
    public void close() throws IOException {

    }
}
