package org.fileformat.drako;
/**
 *  Compression level for draco file
 *
 */
public enum DracoCompressionLevel
{    
    /**
     *  No compression, this will result in the minimum encoding time.
     *
     */
    NO_COMPRESSION,
    /**
     *  Encoder will perform a compression as quickly as possible.
     *
     */
    FAST,
    /**
     *  Standard mode, with good compression and speed.
     *
     */
    STANDARD,
    /**
     *  Encoder will compress the scene optimally, which may takes longer time to finish.
     *
     */
    OPTIMAL;
    
    
}
