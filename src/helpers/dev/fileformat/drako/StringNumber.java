package dev.fileformat.drako;


/**
 * Wrap string as Number
 * Created by lexchou on 6/14/2017.
 */
@Internal
class StringNumber extends Number{
    private String str;

    public StringNumber(String str)
    {
        this.str = str;
    }

    @Override
    public int intValue() {
        return Integer.parseInt(str);
    }

    @Override
    public long longValue() {
        return Long.parseLong(str);
    }

    @Override
    public float floatValue() {
        return Float.parseFloat(str);
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(str);
    }


    @Override
    public String toString() {
        return str;
    }
}
