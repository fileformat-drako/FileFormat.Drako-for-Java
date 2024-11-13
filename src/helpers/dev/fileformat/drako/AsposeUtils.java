package dev.fileformat.drako;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by lexchou on 2/27/2017.
 */
@Internal
class AsposeUtils {

    public static <T> T defaultValue(Class<?> type) {
        if(!type.isPrimitive())
            return (T) null;
        if(type == byte.class)
            return (T)Byte.valueOf((byte)0);
        if(type == short.class)
            return (T)Short.valueOf((short)0);
        if(type == int.class)
            return (T)Integer.valueOf(0);
        if(type == long.class)
            return (T)Long.valueOf(0);
        if(type == boolean.class)
            return (T)Boolean.valueOf(false);
        if(type == char.class)
            return (T)Character.valueOf('\0');
        if(type == float.class)
            return (T)Float.valueOf(0);
        if(type == double.class)
            return (T)Double.valueOf(0);
        throw new IllegalStateException(String.format("Unsupported java primitive type %s", type.toString()));
    }

    public static boolean isEOF(BufferedReader r) {
        throw new RuntimeException();
    }

    /**
     * Some classes will be replaced by an interface, and their construction will be replaced by this method.
     * @param type The interface type to be created
     * @param args The arguments for constructing the object
     * @param <T> The interface type
     * @return The instance of the object.
     */
    public static <T> T create(Class<?> type, Object... args) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Get file name's extension
     * @param fileName
     * @return
     */
    public static String getFileExtension(String fileName)
    {
        if(fileName == null)
            return null;
        for(int i = fileName.length() - 1; i >= 0; i--)
        {
            char ch = fileName.charAt(i);
            if(ch == '/' || ch == '\\')
                return "";
            if(ch == '.')
                return fileName.substring(i);
        }
        return "";
    }

    /**
     * Get file name
     * @return
     */
    public static String getFileName(String fileName) {
        if(fileName == null)
            return null;
        for(int i = 0; i < fileName.length(); i++)
        {
            char ch = fileName.charAt(i);
            if(ch == '/' || ch == '\\')
                return fileName.substring(i + 1);
        }
        return fileName;
    }
    public static boolean isPathRooted(String path) {
        if(path == null)
            throw new IllegalArgumentException("path cannot be null");
        if(path.length() == 0)
            return false;
        //POSIX style root path
        if(isPathSeparator(path.charAt(0)))
            return true;
        //Windows style
        if(path.length() >= 3 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':' && isPathSeparator(path.charAt(2)))
            return true;
        return false;
    }
    public static String combineFileName(String parent, String child) {
        if(parent == null || child == null)
            throw new IllegalArgumentException("argument cannot be null");
        if(isPathRooted(child))
            return child;
        StringBuilder sb = new StringBuilder(parent.length() + child.length() + 1);
        if(parent.length() > 0)
        {
            int end = parent.length();
            if(isPathSeparator(parent.charAt(parent.length() - 1)))
                end--;
            sb.append(parent, 0, end);
        }
        if(sb.length() > 0)
            sb.append('/');
        if(child.length() > 0)
        {
            int start = 0;
            if(isPathSeparator(child.charAt(0)))
                start++;
            sb.append(child, start, child.length());

        }
        return sb.toString();
    }
    private static boolean isPathSeparator(char ch)
    {
        return ch == '/' || ch == '\\';
    }
    public static String getDirectoryName(String fileName) {
        if(fileName == null)
            return null;

        for(int i = fileName.length() - 1; i >= 0; i--)
        {
            char ch = fileName.charAt(i);
            if(ch == '/' || ch == '\\') {
                return fileName.substring(0, i);
            }
        }
        return "";
    }
    public static String getFileNameWithoutExtension(String fileName) {
        if(fileName == null)
            return null;
        for(int i = fileName.length() - 1; i >= 0; i--)
        {
            char ch = fileName.charAt(i);
            if(ch == '/' || ch == '\\') {
                int p = fileName.lastIndexOf('.');
                if(p == -1 || p < i)
                    return fileName.substring(i + 1);
                else
                    return fileName.substring(i + 1, p);
            }
        }
        int p = fileName.lastIndexOf('.');
        if(p == -1)
            return fileName;
        return fileName.substring(0, p);
    }
    /**
     * Cast object to java.lang.Number
     * @param obj
     * @return
     */
    public static Number asNumber(Object obj) {
        if(obj instanceof Number)
            return (Number)obj;
        if(obj instanceof String)
            return new StringNumber((String)obj);
        return null;
    }

    /**
     * Cast object to java.lang.Number
     * @param obj
     * @return
     */
    public static boolean asBoolean(Object obj) {
        if(obj instanceof Boolean) {
            return ((Boolean)obj).booleanValue();
        }
        if(obj instanceof Number) {
            return ((Number)obj).intValue() != 0;
        }
        if(obj instanceof String) {
            String str = (String)obj;
            return str.equals("true") || str.equals("yes") || str.equals("on");
        }
        return false;
    }
    /**
     * Returns true if the argument can be converted to Number by #link asNumber
     * @param obj
     * @return
     */
    public static boolean castableToNumber(Object obj) {
        if(obj instanceof Number)
            return true;
        if(obj instanceof String)
            return true;
        return false;
    }

    public static byte[] toArray(InputStream input) throws IOException {
        if(input == null)
            throw new IllegalArgumentException("Invalid input stream");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copyTo(input, out);
        return out.toByteArray();
    }
    public static int copyTo(Stream input, Stream output) throws IOException {
        if(input == null)
            throw new IllegalArgumentException("Invalid input stream");
        if(output == null)
            throw new IllegalArgumentException("Invalid output stream");
        byte[] tmp = new byte[10240];
        int ret = 0;
        int bytesRead;
        while ((bytesRead = input.read(tmp)) > 0) {
            ret += bytesRead;
            output.write(tmp, 0, bytesRead);
        }
        return ret;
    }
    public static int copyTo(InputStream input, OutputStream output) throws IOException {
        if(input == null)
            throw new IllegalArgumentException("Invalid input stream");
        if(output == null)
            throw new IllegalArgumentException("Invalid output stream");
        byte[] tmp = new byte[10240];
        int ret = 0;
        int bytesRead;
        while ((bytesRead = input.read(tmp)) > 0) {
            ret += bytesRead;
            output.write(tmp, 0, bytesRead);
        }
        return ret;
    }

    public static <E> void copyTo(Collection<E> src, int srcOffset, E[] dst, int offset, int size)
    {
        if(src instanceof List)
        {
            List<E> list = (List<E>)src;
            for(int i = 0; i < size; i++)
            {
                dst[offset + i] = list.get(i + srcOffset);
            }
        }
        else
        {
            int p = offset;
            int rest = size;
            for(E e : src)
            {
                if(srcOffset > 0)
                {
                    srcOffset--;
                    continue;//skip unused elements
                }
                if(rest-- <= 0)
                    break;
                dst[p++] = e;
            }
        }
    }
    public static <E> void copyTo(Collection<E> src, E[] dst, int offset, int size)
    {
        copyTo(src, 0, dst, offset, size);
    }
    public static <E> void copyTo(Collection<E> src, E[] dst, int offset)
    {
        copyTo(src, dst, offset, src.size());
    }

    private static TimeZone UTC_Timezone = TimeZone.getTimeZone("UTC");
    public static Calendar toUTC(Calendar cal)
    {
        Calendar ret = Calendar.getInstance(UTC_Timezone);
        ret.setTimeInMillis(cal.getTimeInMillis());
        return ret;
    }


    public static boolean tryGetValue(Map<Integer, Integer> map, int key, @Out int[] ret)
    {
        Integer v =  map.get(key);
        if(v != null)
            ret[0] = v.intValue();
        return v != null;
    }
    public static <K, V>boolean tryGetValue(Map<K, V> map, K key, @Out V[] ret)
    {
        ret[0] = map.get(key);
        return ret[0] != null;
    }
    public static void safeClose(AutoCloseable closeable)
    {
        try {
            closeable.close();
        } catch(Exception e) {

        }
    }

    /***
     * Check if two objects are equals
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(Object a, Object b) {
        if(a == b)
            return true;
        if(a == null || b == null)
            return false;
        return a.equals(b);

    }

    /**
     * Check if the contents of two StringBuilder instances are equal
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(StringBuilder a, StringBuilder b) {
        if(a == b)
            return true;
        if(a.length() != b.length())
            return false;
        int len = a.length();
        for(int i = 0; i < len; i++) {
            if(a.charAt(i) != b.charAt(i))
                return false;
        }
        return true;
    }
    /**
     * Equivalent to Files.readString, replacement when CsPorter can't read the module files
     * @param path
     * @return
     */
    public static String readString(Path path) throws IOException {
        if(path == null || !Files.exists(path))
            throw new IllegalArgumentException("Invalid path");
        byte[] bytes = Files.readAllBytes(path);
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();

    }

    /**
     * Equivalent to System.Text.Encoding.GetChars
     * @param charset
     * @param bytes
     * @param offset
     * @param numBytes
     * @param result
     * @param resultOffset
     * @return the number of characters decoded from bytes
     */
    public static int charsFromBytes(Charset charset, byte[] bytes, int offset, int numBytes, char[] result, int resultOffset) {
        ByteBuffer input = ByteBuffer.wrap(bytes, offset, numBytes);
        CharBuffer chars = charset.decode(input);

        int numChars = Math.min(chars.limit(), result.length - resultOffset);
        chars.get(result, 0, numChars);
        return numChars;
    }

    /**
     * Parse command line into array
     * @param commandLine
     * @return
     */
    public static List<String> parseCommandLineArguments(String... commandLine) {
        List<String> ret = new ArrayList<String>();
        for(int i = 0; i < commandLine.length; i++) {
            parseCommandLineArgumentsImpl(ret, commandLine[i]);
        }
        return ret;
    }

    private static void parseCommandLineArgumentsImpl(List<String> ret, String commandLine) {
        for(int i = 0; i < commandLine.length(); i++) {
            char ch = commandLine.charAt(i);
            if(ch == '\'' || ch == '\"') {
                char quote = ch;
                i++;
                int start = i;
                for(;i < commandLine.length(); i++) {
                    ch = commandLine.charAt(i);
                    if(ch == quote)
                        break;
                }
                String segment = commandLine.substring(start, i);
                ret.add(segment);
            }
            else if(Character.isWhitespace(ch)) {
                continue;
            } else {
                //scan to end or white space
                int start = i;
                for(; i < commandLine.length(); i++) {
                    ch = commandLine.charAt(i);
                    if(Character.isWhitespace(ch))
                        break;
                }
                String segment = commandLine.substring(start, i);
                ret.add(segment);
            }
        }
    }
}
