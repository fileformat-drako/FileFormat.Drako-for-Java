package dev.fileformat.drako;
import dev.fileformat.drako.HashBuilder;
import dev.fileformat.drako.Struct;
import java.io.Serializable;
import java.util.Stack;
class DynamicIntegerPointsKdTreeEncoder
{    
    static final class EncodingStatus implements Struct<EncodingStatus>, Serializable
    {
        public EncodingStatus(int begin, int end, int last_axis_, int stack_pos_)
        {
            this.begin = begin;
            this.end = end;
            this.last_axis = last_axis_;
            this.stack_pos = stack_pos_;
            this.num_remaining_points = end - begin;
        }
        
        public int begin;
        public int end;
        public int last_axis;
        public int num_remaining_points;
        public int stack_pos;
        public EncodingStatus()
        {
        }
        
        private EncodingStatus(EncodingStatus other)
        {
            this.begin = other.begin;
            this.end = other.end;
            this.last_axis = other.last_axis;
            this.num_remaining_points = other.num_remaining_points;
            this.stack_pos = other.stack_pos;
        }
        
        @Override
        public EncodingStatus clone()
        {
            return new EncodingStatus(this);
        }
        
        @Override
        public void copyFrom(EncodingStatus src)
        {
            if (src == null)
                return;
            this.begin = src.begin;
            this.end = src.end;
            this.last_axis = src.last_axis;
            this.num_remaining_points = src.num_remaining_points;
            this.stack_pos = src.stack_pos;
        }
        
        static final long serialVersionUID = -1528942074L;
        @Override
        public int hashCode()
        {
            HashBuilder builder = new HashBuilder();
            builder.hash(this.begin);
            builder.hash(this.end);
            builder.hash(this.last_axis);
            builder.hash(this.num_remaining_points);
            builder.hash(this.stack_pos);
            return builder.hashCode();
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof EncodingStatus))
                return false;
            EncodingStatus rhs = (EncodingStatus)obj;
            if (this.begin != rhs.begin)
                return false;
            if (this.end != rhs.end)
                return false;
            if (this.last_axis != rhs.last_axis)
                return false;
            if (this.num_remaining_points != rhs.num_remaining_points)
                return false;
            if (this.stack_pos != rhs.stack_pos)
                return false;
            return true;
        }
        
    }
    
    private int compression_level_t;
    int bit_length_;
    int num_points_;
    int dimension_;
    IBitEncoder numbers_encoder_;
    private IBitEncoder remaining_bits_encoder_;
    private IBitEncoder axis_encoder_;
    private IBitEncoder half_encoder_;
    int[] deviations_;
    int[] num_remaining_bits_;
    int[] axes_;
    int[][] base_stack_;
    int[][] levels_stack_;
    public DynamicIntegerPointsKdTreeEncoder(int compression_level, int dimension)
    {
        this.$initFields$();
        this.compression_level_t = compression_level;
        this.dimension_ = dimension;
        this.deviations_ = new int[dimension];
        this.num_remaining_bits_ = new int[dimension];
        this.axes_ = new int[dimension];
        this.base_stack_ = new int[32 * dimension + 1][];
        for (int i = 0; i < base_stack_.length; i++)
        {
            base_stack_[i] = new int[dimension];
        }
        
        this.levels_stack_ = new int[32 * dimension + 1][];
        for (int i = 0; i < levels_stack_.length; i++)
        {
            levels_stack_[i] = new int[dimension];
        }
        
        
        this.numbers_encoder_ = this.createNumbersEncoder();
    }
    
    private IBitEncoder createNumbersEncoder()
    {
        switch(compression_level_t)
        {
            case 0:
            case 1:
                return new DirectBitEncoder();
            case 2:
            case 3:
                return new RAnsBitEncoder();
            case 4:
            case 5:
            case 6:
                return new FoldedBit32Encoder();
            default:
            {
                throw new IllegalStateException("Invalid compression level");
            }
        }
        
    }
    
    public void encodePoints(int[][] array, int bit_length, EncoderBuffer buffer)
    {
        this.bit_length_ = bit_length;
        this.num_points_ = array.length;
        
        buffer.encode(bit_length_);
        buffer.encode2(num_points_);
        if (num_points_ == 0)
            return;
        
        numbers_encoder_.startEncoding();
        remaining_bits_encoder_.startEncoding();
        axis_encoder_.startEncoding();
        half_encoder_.startEncoding();
        
        this.encodeInternal(array);
        
        numbers_encoder_.endEncoding(buffer);
        remaining_bits_encoder_.endEncoding(buffer);
        axis_encoder_.endEncoding(buffer);
        half_encoder_.endEncoding(buffer);
    }
    
    public void encodeNumber(int nbits, int value)
    {
        numbers_encoder_.encodeLeastSignificantBits32(nbits, value);
    }
    
    public int getAndEncodeAxis(int[][] array, int begin, int end, int[] old_base, int[] levels, int last_axis)
    {
        boolean select_axis = compression_level_t == 6;
        if (!select_axis)
            return DracoUtils.incrementMod(last_axis, dimension_);
        int best_axis = 0;
        if (array.length < 64)
        {
            for (int axis = 1; axis < dimension_; ++axis)
            {
                if ((0xffffffffl & levels[best_axis]) > (0xffffffffl & levels[axis]))
                {
                    best_axis = axis;
                }
                
            }
            
        }
        else
        {
            int size = array.length;
            for (int i = 0; i < dimension_; i++)
            {
                deviations_[i] = 0;
                num_remaining_bits_[i] = bit_length_ - levels[i];
                if ((0xffffffffl & num_remaining_bits_[i]) > 0)
                {
                    int split = old_base[i] + (1 << (int)(num_remaining_bits_[i] - 1));
                    int deviation = 0;
                    for (int it = 0; it < (0xffffffffl & size); it++)
                    {
                        //deviation += (array[it][i] < split) ? 1U : 0U;
                        if ((array[it])[i] < split)
                        {
                            deviation++;
                        }
                        
                    }
                    
                    
                    deviations_[i] = Math.max(size - deviation, deviation);
                }
                
            }
            
            int max_value = 0;
            best_axis = 0;
            for (int i = 0; i < dimension_; i++)
            {
                // If axis can be subdivided.
                if ((0xffffffffl & num_remaining_bits_[i]) != 0)
                {
                    // Check if this is the better axis.
                    if ((0xffffffffl & max_value) < (0xffffffffl & deviations_[i]))
                    {
                        max_value = deviations_[i];
                        best_axis = i;
                    }
                    
                }
                
            }
            
            
            axis_encoder_.encodeLeastSignificantBits32(4, best_axis);
        }
        
        return best_axis;
    }
    
    void encodeInternal(int[][] array)
    {
        
        base_stack_[0] = new int[dimension_];
        levels_stack_[0] = new int[dimension_];
        EncodingStatus init_status = new EncodingStatus(0, array.length, 0, 0);
        Stack<EncodingStatus> status_stack = new Stack<EncodingStatus>();
        status_stack.push(Struct.byVal(init_status));
        
        // TODO(hemmer): use preallocated vector instead of stack.
        while (!status_stack.isEmpty())
        {
            EncodingStatus status = status_stack.peek();
            status_stack.pop();
            int begin = status.begin;
            int end = status.end;
            int last_axis = status.last_axis;
            int stack_pos = status.stack_pos;
            int[] old_base = base_stack_[stack_pos];
            int[] levels = levels_stack_[stack_pos];
            int axis = this.getAndEncodeAxis(array, begin, end, old_base, levels, last_axis);
            int level = levels[axis];
            int num_remaining_points = end - begin;
            
            // If this happens all axis are subdivided to the end.
            if (bit_length_ - level == 0)
                continue;
            
            // Fast encoding of remaining bits if number of points is 1 or 2.
            // Doing this also for 2 gives a slight additional speed up.
            if (num_remaining_points <= 2)
            {
                // TODO(hemmer): axes_ not necessary, remove would change bitstream!
                axes_[0] = axis;
                for (int i = 1; (0xffffffffl & i) < dimension_; i++)
                {
                    axes_[i] = DracoUtils.incrementMod(axes_[i - 1], dimension_);
                }
                
                
                for (int i = 0; (0xffffffffl & i) < num_remaining_points; ++i)
                {
                    int[] p = array[begin + i];
                    for (int j = 0; (0xffffffffl & j) < dimension_; j++)
                    {
                        int num_remaining_bits = bit_length_ - levels[axes_[j]];
                        if (num_remaining_bits != 0)
                        {
                            remaining_bits_encoder_.encodeLeastSignificantBits32(num_remaining_bits, p[axes_[j]]);
                        }
                        
                    }
                    
                }
                
                
                continue;
            }
            
            int modifier = 1 << (int)(bit_length_ - level - 1);
            System.arraycopy(old_base, 0, base_stack_[stack_pos + 1], 0, old_base.length);
            // copy
            (base_stack_[stack_pos + 1])[axis] += modifier;
            int[] new_base = base_stack_[stack_pos + 1];
            int split = this.partition(array, begin, end, axis, new_base[axis]);
            int required_bits = DracoUtils.mostSignificantBit(num_remaining_points);
            int first_half = split - begin;
            int second_half = end - split;
            boolean left = first_half < second_half;
            
            if (first_half != second_half)
            {
                half_encoder_.encodeBit(left);
            }
            
            
            if (left)
            {
                this.encodeNumber(required_bits, (int)(num_remaining_points / 2 - first_half));
            }
            else
            {
                this.encodeNumber(required_bits, (int)(num_remaining_points / 2 - second_half));
            }
            
            
            (levels_stack_[stack_pos])[axis] += 1;
            System.arraycopy(levels_stack_[stack_pos], 0, levels_stack_[stack_pos + 1], 0, dimension_);
            // copy
            if (split != begin)
            {
                status_stack.push(new EncodingStatus(begin, split, axis, stack_pos));
            }
            
            if (split != end)
            {
                status_stack.push(new EncodingStatus(split, end, axis, stack_pos + 1));
            }
            
        }
        
    }
    
    private int partition(int[][] points, int first, int last, int axis, int v)
    {
        
        for (; ; )
        {
            for (; ; )
            {
                if (first == last)
                    return first;
                if (!((points[first])[axis] < (0xffffffffl & v)))
                    break;
                ++first;
            }
            
            
            do
            {
                --last;
                if (first == last)
                    return first;
            } while (!((points[last])[axis] < (0xffffffffl & v)));
            
            int[] t = points[first];
            points[first] = points[last];
            points[last] = t;
            ++first;
        }
        
    }
    
    private void $initFields$()
    {
        try
        {
            remaining_bits_encoder_ = new DirectBitEncoder();
            axis_encoder_ = new DirectBitEncoder();
            half_encoder_ = new DirectBitEncoder();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
