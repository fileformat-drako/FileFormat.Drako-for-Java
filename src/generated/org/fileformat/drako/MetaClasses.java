package org.fileformat.drako;
import com.aspose.csporter.helpers.MetaClass;
import com.aspose.csporter.helpers.Struct;
final class MetaClasses
{    
    public static final MetaClass<CornerTable.VertexEdgePair> VertexEdgePair = new MetaClass<CornerTable.VertexEdgePair>()
    {
        @Override
        public Object newArray(int size)
        {
            CornerTable.VertexEdgePair[] ret = new CornerTable.VertexEdgePair[size];
            for (int i = 0; i < size; i++)
            {
                ret[i] = new CornerTable.VertexEdgePair();
            }
            
            return ret;
        }
        @Override
        public CornerTable.VertexEdgePair[] unshadow(Object[] array)
        {
            CornerTable.VertexEdgePair[] ret = new CornerTable.VertexEdgePair[array.length];
            for (int i = 0; i < array.length; ++i)
            {
                ret[i] = Struct.byVal((CornerTable.VertexEdgePair)(array[i]));
            }
            
            return ret;
        }
        @Override
        public Class<CornerTable.VertexEdgePair> classOf()
        {
            return CornerTable.VertexEdgePair.class;
        }
        @Override
        public CornerTable.VertexEdgePair newInstance()
        {
            return new CornerTable.VertexEdgePair();
        }
    };
    public static final MetaClass<RAnsDecoder.ransSym> ransSym = new MetaClass<RAnsDecoder.ransSym>()
    {
        @Override
        public Object newArray(int size)
        {
            RAnsDecoder.ransSym[] ret = new RAnsDecoder.ransSym[size];
            for (int i = 0; i < size; i++)
            {
                ret[i] = new RAnsDecoder.ransSym();
            }
            
            return ret;
        }
        @Override
        public RAnsDecoder.ransSym[] unshadow(Object[] array)
        {
            RAnsDecoder.ransSym[] ret = new RAnsDecoder.ransSym[array.length];
            for (int i = 0; i < array.length; ++i)
            {
                ret[i] = Struct.byVal((RAnsDecoder.ransSym)(array[i]));
            }
            
            return ret;
        }
        @Override
        public Class<RAnsDecoder.ransSym> classOf()
        {
            return RAnsDecoder.ransSym.class;
        }
        @Override
        public RAnsDecoder.ransSym newInstance()
        {
            return new RAnsDecoder.ransSym();
        }
    };
    public static final MetaClass<RAnsBitCodec.RansSym> RansSym = new MetaClass<RAnsBitCodec.RansSym>()
    {
        @Override
        public Object newArray(int size)
        {
            RAnsBitCodec.RansSym[] ret = new RAnsBitCodec.RansSym[size];
            for (int i = 0; i < size; i++)
            {
                ret[i] = new RAnsBitCodec.RansSym();
            }
            
            return ret;
        }
        @Override
        public RAnsBitCodec.RansSym[] unshadow(Object[] array)
        {
            RAnsBitCodec.RansSym[] ret = new RAnsBitCodec.RansSym[array.length];
            for (int i = 0; i < array.length; ++i)
            {
                ret[i] = Struct.byVal((RAnsBitCodec.RansSym)(array[i]));
            }
            
            return ret;
        }
        @Override
        public Class<RAnsBitCodec.RansSym> classOf()
        {
            return RAnsBitCodec.RansSym.class;
        }
        @Override
        public RAnsBitCodec.RansSym newInstance()
        {
            return new RAnsBitCodec.RansSym();
        }
    };
}
