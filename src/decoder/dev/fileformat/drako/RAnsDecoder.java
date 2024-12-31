package dev.fileformat.drako;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
/**
 *  Class for performing rANS decoding using a desired number of precision bits.
 *  The number of precision bits needs to be the same as with the RAnsEncoder
 *  that was used to encode the input data.
 *
 */
class RAnsDecoder
{    
    static final class ransDecSym implements Struct<ransDecSym>, Serializable
    {
        int val;
        int prob;
        int cumProb;
        public ransDecSym()
        {
        }
        
        private ransDecSym(ransDecSym other)
        {
            this.val = other.val;
            this.prob = other.prob;
            this.cumProb = other.cumProb;
        }
        
        @Override
        public ransDecSym clone()
        {
            return new ransDecSym(this);
        }
        
        @Override
        public void copyFrom(ransDecSym src)
        {
            if (src == null)
                return;
            this.val = src.val;
            this.prob = src.prob;
            this.cumProb = src.cumProb;
        }
        
        static final long serialVersionUID = 1044714435L;
        @Override
        public int hashCode()
        {
            HashBuilder builder = new HashBuilder();
            builder.hash(this.val);
            builder.hash(this.prob);
            builder.hash(this.cumProb);
            return builder.hashCode();
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof ransDecSym))
                return false;
            ransDecSym rhs = (ransDecSym)obj;
            if (this.val != rhs.val)
                return false;
            if (this.prob != rhs.prob)
                return false;
            if (this.cumProb != rhs.cumProb)
                return false;
            return true;
        }
        
    }
    
    static final class ransSym implements Struct<ransSym>, Serializable
    {
        int prob;
        int cumProb;
        public ransSym()
        {
        }
        
        private ransSym(ransSym other)
        {
            this.prob = other.prob;
            this.cumProb = other.cumProb;
        }
        
        @Override
        public ransSym clone()
        {
            return new ransSym(this);
        }
        
        @Override
        public void copyFrom(ransSym src)
        {
            if (src == null)
                return;
            this.prob = src.prob;
            this.cumProb = src.cumProb;
        }
        
        static final long serialVersionUID = 31145064L;
        @Override
        public int hashCode()
        {
            HashBuilder builder = new HashBuilder();
            builder.hash(this.prob);
            builder.hash(this.cumProb);
            return builder.hashCode();
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof ransSym))
                return false;
            ransSym rhs = (ransSym)obj;
            if (this.prob != rhs.prob)
                return false;
            if (this.cumProb != rhs.cumProb)
                return false;
            return true;
        }
        
    }
    
    private static final int IO_BASE = 256;
    private int ransPrecisionBits;
    private int ransPrecision;
    private int lRansBase;
    private int[] lutTable;
    private ransSym[] probabilityTable;
    private BytePointer buf = new BytePointer();
    private int bufOffset;
    private int state;
    public RAnsDecoder(int ransPrecisionBits)
    {
        this.ransPrecisionBits = ransPrecisionBits;
        this.ransPrecision = 1 << ransPrecisionBits;
        this.lRansBase = ransPrecision * 4;
    }
    
    /**
     *  Initializes the decoder from the input buffer. The |offset| specifies the
     *  number of bytes encoded by the encoder. A non zero return value is an
     *  error.
     *
     */
    public int readInit(BytePointer buf, int offset)
    {
        buf = buf == null ? new BytePointer() : buf.clone();
        int x;
        if (offset < 1)
            return 1;
        this.buf.copyFrom(buf);
        x = (int)((0xff & buf.get(offset - 1)) >>> 6);
        if (x == 0)
        {
            this.bufOffset = offset - 1;
            this.state = (int)(0xff & buf.get(offset - 1) & 0x3F);
        }
        else if (x == 1)
        {
            if (offset < 2)
                return 1;
            this.bufOffset = offset - 2;
            this.state = (int)(0xffff & buf.toUInt16LE(offset - 2)) & 0x3FFF;
        }
        else if (x == 2)
        {
            if (offset < 3)
                return 1;
            this.bufOffset = offset - 3;
            this.state = buf.toUInt24LE(offset - 3) & 0x3FFFFF;
        }
        else if (x == 3)
        {
            this.bufOffset = offset - 4;
            this.state = buf.toUInt32LE(offset - 4) & 0x3FFFFFFF;
        }
        else
            return 1;
        this.state += lRansBase;
        if ((0xffffffffl & this.state) >= (lRansBase * IO_BASE))
            return 1;
        return 0;
    }
    
    public boolean readEnd()
    {
        return this.state == lRansBase;
    }
    
    public boolean readerHasError()
    {
        return (0xffffffffl & this.state) < lRansBase && (this.bufOffset == 0);
    }
    
    public int read()
    {
        int rem;
        int quo;
        ransDecSym sym = new ransDecSym();
        while ((0xffffffffl & this.state) < lRansBase && (this.bufOffset > 0))
        {
            this.state = (int)(this.state * IO_BASE + (0xff & this.buf.get(--this.bufOffset)));
        }
        
        // |ransPrecision| is a power of two compile time constant, and the below
        // division and modulo are going to be optimized by the compiler.
        quo = (int)(this.state / ransPrecision);
        rem = (int)(this.state % ransPrecision);
        this.fetchSym(sym, rem);
        this.state = (int)(quo * sym.prob + rem - sym.cumProb);
        return sym.val;
    }
    
    /**
     *  Construct a look up table with |ransPrecision| number of entries.
     *  Returns false if the table couldn't be built (because of wrong input data).
     *
     */
    public boolean buildLookupTable(int[] tokenProbs, int numSymbols)
    {
        this.lutTable = new int[ransPrecision];
        this.probabilityTable = (ransSym[])(MetaClasses.ransSym.newArray(numSymbols));
        int cumProb = 0;
        int actProb = 0;
        for (int i = 0; i < numSymbols; ++i)
        {
            ransSym sym = new ransSym();
            sym.prob = tokenProbs[i];
            sym.cumProb = cumProb;
            probabilityTable[i] = Struct.byVal(sym);
            cumProb += tokenProbs[i];
            if ((0xffffffffl & cumProb) > ransPrecision)
                return false;
            for (int j = actProb; j < (0xffffffffl & cumProb); ++j)
            {
                lutTable[j] = i;
            }
            
            actProb = cumProb;
        }
        
        if ((0xffffffffl & cumProb) != ransPrecision)
            return false;
        return true;
    }
    
    private void fetchSym(ransDecSym res, int rem)
    {
        int symbol = lutTable[rem];
        res.val = symbol;
        res.prob = probabilityTable[symbol].prob;
        res.cumProb = probabilityTable[symbol].cumProb;
    }
    
}
