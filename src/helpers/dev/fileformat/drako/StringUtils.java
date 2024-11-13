package dev.fileformat.drako;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by lexchou on 6/16/2017.
 */
@Internal
class StringUtils {
    public static final int SPLIT_REMOVE_EMPTY_ENTRIES = 1;

    public static String trimEnd(String s) {
        if(s.length() == 0 || s.charAt(s.length() - 1) != ' ')
            return s;


        for(int p = s.length() - 1; p > 0; p--)
        {
            char ch = s.charAt(p);
            if(ch != ' ')
                return s.substring(0, p + 1);
        }
        return "";
    }
    public static String trimStart(String s) {
        if(s.length() == 0 || s.charAt(0) != ' ')
            return s;
        for(int p = 0; p < s.length(); p++)
        {
            char ch = s.charAt(p);
            if(ch != ' ')
                return s.substring(p);
        }
        return "";
    }

    public static String[] split(String str, char[] separators) {
        return split(str, separators, 0);
    }
    public static String[] split(String str, char[] separators, int flags) {
        List<String> arr = new ArrayList<>();
        int foundPosition;
        int startIndex = 0;
        boolean removeEmpty = (flags & SPLIT_REMOVE_EMPTY_ENTRIES) != 0;
        while ((foundPosition = indexOfAny(str, startIndex, separators)) > -1) {
            if(startIndex < foundPosition || !removeEmpty)
                arr.add(str.substring(startIndex, foundPosition));
            startIndex = foundPosition + 1;
        }
        if(startIndex < str.length() || !removeEmpty)
            arr.add(str.substring(startIndex));
        return arr.toArray(new String[arr.size()]);
    }

    public static String format(String str, Object... args) {
        StringFormatter fmt = new StringFormatter(str);
        return fmt.format(args);
    }

    /**
     * Split string by delimiter
     * @param strToSplit
     * @param delimiter
     * @return
     */
    public static String[] split(String strToSplit, char delimiter) {
        List<String> arr = new ArrayList<>();
        int foundPosition;
        int startIndex = 0;
        while ((foundPosition = strToSplit.indexOf(delimiter, startIndex)) > -1) {
            arr.add(strToSplit.substring(startIndex, foundPosition));
            startIndex = foundPosition + 1;
        }
        arr.add(strToSplit.substring(startIndex));
        return arr.toArray(new String[arr.size()]);
    }

    public static int indexOfAny(String str, char... chars) {
        return indexOfAny(str, 0, chars);
    }
    public static int indexOfAny(String str, int start, char... chars) {
        int len = str.length();
        for(int i = start; i < len; i++)
        {
            char ch = str.charAt(i);
            for(int j = 0; j < chars.length; j++)
            {
                if(ch == chars[j])
                    return i;
            }
        }
        return -1;
    }

    public static boolean isNullOrWhiteSpace(String str) {
        if(str == null)
            return true;
        for(int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if(!Character.isWhitespace(ch))
                return false;
        }
        return true;
    }

    static String concat(String a, String b, String c) {
        StringBuilder sb = new StringBuilder(a.length() + b.length() + c.length());
        return sb.append(a).append(b).append(c).toString();
    }
}
