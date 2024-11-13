package dev.fileformat.drako;


import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Created by lexchou on 4/17/2017.
 */
public class Version implements Cloneable, Serializable {
    private int major;
    private int minor;
    private int build;

    public Version(int major, int minor, int build)
    {
        this.major = major;
        this.minor = minor;
        this.build = build;
    }
    public Version(int major, int minor)
    {
        this.major = major;
        this.minor = minor;
    }
    public Version(String ver)
    {
        if(ver == null || ver.length() == 0)
            throw new IllegalArgumentException("Invalid version");
        StringTokenizer tok = new StringTokenizer(ver, ".", false);
        if(tok.hasMoreTokens())
            major = Convert.tryParseInt(tok.nextToken(), 0);
        if(tok.hasMoreTokens())
            minor = Convert.tryParseInt(tok.nextToken(), 0);
        if(tok.hasMoreTokens())
            build = Convert.tryParseInt(tok.nextToken(), 0);
    }

    public int getMajor()
    {
        return major;
    }
    public int getBuild() {
        return build;
    }
    public int getMinor()
    {
        return minor;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof Version))
            return false;
        Version rhs = (Version)obj;
        return major == rhs.major && minor == rhs.minor && build == rhs.build;
    }
    @Override
    public Version clone() {
        return new Version(major, minor, build);
    }

    @Override
    public int hashCode() {
        return (major *100 + minor) * 100000 + build;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, build);
    }
}
