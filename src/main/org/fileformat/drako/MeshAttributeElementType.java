package org.fileformat.drako;
/**
 *  List of different variants of mesh attributes.
 *
 */
public final class MeshAttributeElementType
{    
    /**
     *  All corners attached to a vertex share the same attribute value. A typical
     *  example are the vertex positions and often vertex colors.
     *
     */
    public static final int VERTEX = 0;
    /**
     *  The most general attribute where every corner of the mesh can have a
     *  different attribute value. Often used for texture coordinates or normals.
     *
     */
    public static final int CORNER = 1;
    /**
     *  All corners of a single face share the same value.
     *
     */
    public static final int FACE = 2;
    
    
}
