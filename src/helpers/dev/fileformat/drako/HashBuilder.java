package dev.fileformat.drako;


/**
 * Created by lexchou on 12/1/2017.
 * Utility to build hash code in method chaining
 */
@Internal
final class HashBuilder {
    private int hashCode;

    private HashBuilder impl(int n) {
        hashCode = hashCode * 31 + n;
        return this;
    }

    public HashBuilder hash(Object obj) {
        return impl(obj == null ? 0 : obj.hashCode());
    }
    public HashBuilder hash(boolean obj) {
        return impl(obj ? 1 : 0);
    }
    public HashBuilder hash(byte obj) {
        return impl(obj);
    }
    public HashBuilder hash(short obj) {
        return impl(obj);
    }
    public HashBuilder hash(int obj) {
        return impl(obj);
    }
    public HashBuilder hash(long n) {
        impl((int)n);
        return impl((int)(n >>> 32));
    }

    public HashBuilder hash(float n) {
        return impl(Float.floatToRawIntBits(n));
    }

    public HashBuilder hash(double v) {
        long n  = Double.doubleToRawLongBits(v);
        return hash(n);
    }
    @Override
    public int hashCode() {
        return hashCode;
    }
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof HashBuilder))
            return false;
        return hashCode == ((HashBuilder)obj).hashCode;
    }
}
