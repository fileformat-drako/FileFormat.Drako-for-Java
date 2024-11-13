package dev.fileformat.drako;
import java.util.Stack;
class DynamicIntegerPointsKdTreeDecoder
{    
    static class Status
    {
        public Status(int num_remaining_points_, int last_axis_, int stack_pos_)
        {
            this.num_remaining_points = num_remaining_points_;
            this.last_axis = last_axis_;
            this.stack_pos = stack_pos_;
        }
        
        public int num_remaining_points;
        public int last_axis;
        public int stack_pos;
    }
    
    int bit_length_;
    int num_points_;
    int num_decoded_points_;
    int dimension_;
    IBitDecoder numbers_decoder_;
    DirectBitDecoder remaining_bits_decoder_;
    DirectBitDecoder axis_decoder_;
    DirectBitDecoder half_decoder_;
    int[] p_;
    int[] axes_;
    int[][] base_stack_;
    int[][] levels_stack_;
    private int compression_level_t;
    public DynamicIntegerPointsKdTreeDecoder(int compression_level, int dimension)
    {
        this.compression_level_t = compression_level;
        this.dimension_ = dimension;
        
        this.p_ = new int[dimension];
        this.axes_ = new int[dimension];
        // Init the stack with the maximum depth of the tree.
        // +1 for a second leaf.
        this.base_stack_ = new int[32 * dimension + 1][];
        this.levels_stack_ = new int[32 * dimension + 1][];
        for (int i = 0; i < base_stack_.length; i++)
        {
            base_stack_[i] = new int[dimension];
            levels_stack_[i] = new int[dimension];
        }
        
        
        switch(compression_level)
        {
            case 0:
            case 1:
            {
                this.numbers_decoder_ = new DirectBitDecoder();
                break;
            }
            case 2:
            case 3:
            {
                this.numbers_decoder_ = new RAnsBitDecoder();
                break;
            }
            case 4:
            case 5:
            case 6:
            {
                this.numbers_decoder_ = new FoldedBit32Decoder();
                break;
            }
            default:
            {
                throw new IllegalStateException("Invalid compression level.");
            }
        }
        
        this.remaining_bits_decoder_ = new DirectBitDecoder();
        this.axis_decoder_ = new DirectBitDecoder();
        this.half_decoder_ = new DirectBitDecoder();
    }
    
    public boolean decodePoints(DecoderBuffer buffer, PointAttributeVectorOutputIterator oit)
    {
        final int[] ref0 = new int[1];
        final int[] ref1 = new int[1];
        buffer.decode6(ref0);
        bit_length_ = ref0[0];
        if (bit_length_ > 32)
            return false;
        buffer.decode6(ref1);
        num_points_ = ref1[0];
        if (num_points_ == 0)
            return true;
        this.num_decoded_points_ = 0;
        
        if (!numbers_decoder_.startDecoding(buffer))
            return false;
        if (!remaining_bits_decoder_.startDecoding(buffer))
            return false;
        if (!axis_decoder_.startDecoding(buffer))
            return false;
        if (!half_decoder_.startDecoding(buffer))
            return false;
        
        if (!this.decodeInternal(num_points_, oit))
            return false;
        
        numbers_decoder_.endDecoding();
        remaining_bits_decoder_.endDecoding();
        axis_decoder_.endDecoding();
        half_decoder_.endDecoding();
        
        return true;
    }
    
    int getAxis(int num_remaining_points, int[] levels, int last_axis)
    {
        boolean select_axis = compression_level_t == 6;
        if (!select_axis)
            return DracoUtils.incrementMod(last_axis, dimension_);
        int best_axis = 0;
        if (num_remaining_points < 64)
        {
            for (int axis = 1; axis < dimension_; ++axis)
            {
                if (levels[best_axis] > levels[axis])
                {
                    best_axis = axis;
                }
                
            }
            
        }
        else
        {
            best_axis = axis_decoder_.decodeLeastSignificantBits32(4);
        }
        
        
        return best_axis;
    }
    
    boolean decodeInternal(int num_points, PointAttributeVectorOutputIterator oit)
    {
        base_stack_[0] = new int[dimension_];
        levels_stack_[0] = new int[dimension_];
        Status init_status = new Status(num_points, 0, 0);
        Stack<Status> status_stack = new Stack<Status>();
        status_stack.push(init_status);
        
        // TODO(hemmer): use preallocated vector instead of stack.
        while (!status_stack.isEmpty())
        {
            Status status = status_stack.peek();
            status_stack.pop();
            int num_remaining_points = status.num_remaining_points;
            int last_axis = status.last_axis;
            int stack_pos = status.stack_pos;
            int[] old_base = base_stack_[stack_pos];
            int[] levels = levels_stack_[stack_pos];
            
            if (num_remaining_points > num_points)
                return false;
            int axis = this.getAxis(num_remaining_points, levels, last_axis);
            if (axis >= dimension_)
                return false;
            int level = levels[axis];
            
            // All axes have been fully subdivided, just output points.
            if (bit_length_ - level == 0)
            {
                for (int i = 0; i < num_remaining_points; i++)
                {
                    oit.set(old_base);
                    oit.next();
                    ++num_decoded_points_;
                }
                
                
                continue;
            }
            
            int num_remaining_bits;
            if (num_remaining_points <= 2)
            {
                // TODO(hemmer): axes_ not necessary, remove would change bitstream!
                axes_[0] = axis;
                for (int i = 1; i < dimension_; i++)
                {
                    axes_[i] = DracoUtils.incrementMod(axes_[i - 1], dimension_);
                }
                
                
                for (int i = 0; i < num_remaining_points; ++i)
                {
                    for (int j = 0; j < dimension_; j++)
                    {
                        p_[axes_[j]] = 0;
                        num_remaining_bits = bit_length_ - levels[axes_[j]];
                        if (num_remaining_bits != 0)
                        {
                            p_[axes_[j]] = remaining_bits_decoder_.decodeLeastSignificantBits32(num_remaining_bits);
                        }
                        
                        p_[axes_[j]] = old_base[axes_[j]] | p_[axes_[j]];
                    }
                    
                    
                    oit.set(p_);
                    oit.next();
                    ++num_decoded_points_;
                }
                
                
                continue;
            }
            
            
            if (num_decoded_points_ > num_points_)
                return false;
            
            num_remaining_bits = bit_length_ - level;
            int modifier = 1 << (num_remaining_bits - 1);
            System.arraycopy(old_base, 0, base_stack_[stack_pos + 1], 0, dimension_);
            // copy
            (base_stack_[stack_pos + 1])[axis] += modifier;
            // new base
            int incoming_bits = DracoUtils.mostSignificantBit(num_remaining_points);
            int number = this.decodeNumber(incoming_bits);
            int first_half = num_remaining_points / 2 - number;
            int second_half = num_remaining_points - first_half;
            
            if (first_half != second_half)
            {
                if (!half_decoder_.decodeNextBit())
                {
                    int t = first_half;
                    first_half = second_half;
                    second_half = t;
                }
                
            }
            
            
            
            (levels_stack_[stack_pos])[axis] += 1;
            System.arraycopy(levels_stack_[stack_pos], 0, levels_stack_[stack_pos + 1], 0, dimension_);
            // copy
            if (first_half != 0)
            {
                status_stack.push(new Status(first_half, axis, stack_pos));
            }
            
            if (second_half != 0)
            {
                status_stack.push(new Status(second_half, axis, stack_pos + 1));
            }
            
        }
        
        
        return true;
    }
    
    private int decodeNumber(int nbits)
    {
        return numbers_decoder_.decodeLeastSignificantBits32(nbits);
    }
    
}
