package dev.fileformat.drako;


import java.util.IllegalFormatException;

/**
 * Created by lexchou on 10/10/2017.
 */
class StringFormatter {
    private static final char[] UHEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] LHEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private char[] format;
    public StringFormatter(String format)
    {
        this.format = format.toCharArray();
    }

    public String format(Object... args)
    {
        StringBuilder result = new StringBuilder(format.length * 2);
        for(int i = 0; i < format.length; i++)
        {
            char ch = format[i];
            if(ch != '{')
            {
                result.append(ch);
                continue;
            }
            i++;//skip {
            if(format[i] == '{')
            {
                result.append('{');
                continue;
            }
            int idx = 0;
            for(; Character.isDigit(format[i]); i++)
            {
                idx = idx * 10 + (format[i] - '0');
            }
            switch(format[i])
            {
                case '}':
                    result.append(args[idx]);
                    break;
                case ':':
                    i++;
                    appendFormat(result, args[idx], i);
                    while(format[i] != '}')
                        i++;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format string");
            }
        }
        return result.toString();
    }
    private void appendFormat(StringBuilder ret, Object obj, int i)
    {
        char fmt = format[i++];
        switch(fmt)
        {
            case 'x':
            case 'X':
            {
                int pad = 0;
                if(format[i] != '}')
                {
                    pad = format[i] - '0';
                }
                appendHex(ret, ((Number)obj).intValue(), pad, fmt == 'X');
                break;
            }
            case '#':
            case '0':
            {
                int fracts = 0;
                while(format[i] != '}' && format[i] != '.')
                    i++;
                if(format[i] == '.') {
                    do{
                        i++;
                        char ch = format[i];
                        if(ch == '0' || ch == '#')
                            fracts++;
                    }while(format[i] != '}');
                }
                if(fracts == 0)
                    ret.append(Long.toString(((Number)obj).longValue()));
                else
                {
                    String s = Double.toString(((Number)obj).doubleValue());
                    ret.append(s);
                    int actualFracts = 0;
                    int p = s.indexOf('.');
                    if(p == -1)
                    {
                        ret.append('.');
                    }
                    else
                    {
                        actualFracts = s.length() - p - 1;
                    }
                    while(actualFracts < fracts)
                    {
                        ret.append('0');
                        actualFracts++;
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported format string");
        }
    }
    private void appendHex(StringBuilder ret, int val, int pad, boolean upperCase)
    {
        String hex = Integer.toHexString(val);
        if(upperCase)
            hex = hex.toUpperCase();
        int zeros = pad - hex.length();
        while(zeros-- > 0)
            ret.append('0');
        ret.append(hex);
    }

}
