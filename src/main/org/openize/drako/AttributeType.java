package org.openize.drako;
/**
 *  Supported attribute types.
 *
 */
public final class AttributeType
{    
    public static final int INVALID = -1;
    /**
     *  Named attributes start here. The difference between named and generic
     *  attributes is that for named attributes we know their purpose and we
     *  can apply some special methods when dealing with them (e.g. during
     *  encoding).
     *
     */
    public static final int POSITION = 0;
    public static final int NORMAL = 1;
    public static final int COLOR = 2;
    public static final int TEX_COORD = 3;
    /**
     *  A special id used to mark attributes that are not assigned to any known
     *  predefined use case. Such attributes are often used for a shader specific
     *  data.
     *
     */
    public static final int GENERIC = 4;
    /**
     *  Total number of different attribute types.
     *  Always keep behind all named attributes.
     *
     */
    public static final int NAMED_ATTRIBUTES_COUNT = 5;
    
    
}
