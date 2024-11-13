package dev.fileformat.drako;
import java.util.Arrays;
/**
 *  Decoder for traversal encoded with the
 *  MeshEdgeBreakerTraversalPredictiveEncoder. The decoder maintains valences
 *  of the decoded portion of the traversed mesh and it uses them to predict
 *  symbols that are about to be decoded.
 *
 */
class MeshEdgeBreakerTraversalPredictiveDecoder extends MeshEdgeBreakerTraversalDecoder
{    
    private CornerTable corner_table_;
    private int num_vertices_;
    private int[] vertex_valences_;
    private RAnsBitDecoder prediction_decoder_;
    private int last_symbol_;
    private int predicted_symbol_;
    @Override
    public void init(IMeshEdgeBreakerDecoderImpl decoder)
    {
        super.init(decoder);
        this.corner_table_ = decoder.getCornerTable();
    }
    
    @Override
    public void setNumEncodedVertices(int num_vertices)
    {
        this.num_vertices_ = num_vertices;
    }
    
    @Override
    public boolean start(DecoderBuffer[] out_buffer)
    {
        final int[] ref0 = new int[1];
        if (!super.start(out_buffer))
            return DracoUtils.failed();
        int num_split_symbols;
        if (!out_buffer[0].decode6(ref0))
        {
            num_split_symbols = ref0[0];
            return DracoUtils.failed();
        }
        else
        {
            num_split_symbols = ref0[0];
        }
        
        // Add one vertex for each split symbol.
        num_vertices_ += num_split_symbols;
        // Set the valences of all initial vertices to 0.
        vertex_valences_ = vertex_valences_ == null ? new int[num_vertices_] : Arrays.copyOf(vertex_valences_, num_vertices_);
        if (!prediction_decoder_.startDecoding(out_buffer[0]))
            return DracoUtils.failed();
        return true;
    }
    
    @Override
    public int decodeSymbol()
    {
        // First check if we have a predicted symbol.
        if (predicted_symbol_ != EdgeBreakerTopologyBitPattern.INVALID)
        {
            // Double check that the predicted symbol was predicted correctly.
            if (prediction_decoder_.decodeNextBit())
            {
                this.last_symbol_ = predicted_symbol_;
                return predicted_symbol_;
            }
            
        }
        
        // We don't have a predicted symbol or the symbol was mis-predicted.
        // Decode it directly.
        this.last_symbol_ = super.decodeSymbol();
        return last_symbol_;
    }
    
    @Override
    public void newActiveCornerReached(int corner)
    {
        int next = corner_table_.next(corner);
        int prev = corner_table_.previous(corner);
        // Update valences.
        switch(last_symbol_)
        {
            case EdgeBreakerTopologyBitPattern.C:
            case EdgeBreakerTopologyBitPattern.S:
            {
                vertex_valences_[corner_table_.vertex(next)] += 1;
                vertex_valences_[corner_table_.vertex(prev)] += 1;
                break;
            }
            case EdgeBreakerTopologyBitPattern.R:
            {
                vertex_valences_[corner_table_.vertex(corner)] += 1;
                vertex_valences_[corner_table_.vertex(next)] += 1;
                vertex_valences_[corner_table_.vertex(prev)] += 2;
                break;
            }
            case EdgeBreakerTopologyBitPattern.L:
            {
                vertex_valences_[corner_table_.vertex(corner)] += 1;
                vertex_valences_[corner_table_.vertex(next)] += 2;
                vertex_valences_[corner_table_.vertex(prev)] += 1;
                break;
            }
            case EdgeBreakerTopologyBitPattern.E:
            {
                vertex_valences_[corner_table_.vertex(corner)] += 2;
                vertex_valences_[corner_table_.vertex(next)] += 2;
                vertex_valences_[corner_table_.vertex(prev)] += 2;
                break;
            }
            default:
            {
                break;
            }
        }
        
        // Compute the new predicted symbol.
        if (last_symbol_ == EdgeBreakerTopologyBitPattern.C || (last_symbol_ == EdgeBreakerTopologyBitPattern.R))
        {
            int pivot = corner_table_.vertex(corner_table_.next(corner));
            if (vertex_valences_[pivot] < 6)
            {
                this.predicted_symbol_ = EdgeBreakerTopologyBitPattern.R;
            }
            else
            {
                this.predicted_symbol_ = EdgeBreakerTopologyBitPattern.C;
            }
            
        }
        else
        {
            this.predicted_symbol_ = EdgeBreakerTopologyBitPattern.INVALID;
        }
        
    }
    
    @Override
    public void mergeVertices(int dest, int source)
    {
        // Update valences on the merged vertices.
        vertex_valences_[dest] += vertex_valences_[source];
    }
    
    public MeshEdgeBreakerTraversalPredictiveDecoder()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            prediction_decoder_ = new RAnsBitDecoder();
            last_symbol_ = EdgeBreakerTopologyBitPattern.INVALID;
            predicted_symbol_ = EdgeBreakerTopologyBitPattern.INVALID;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
