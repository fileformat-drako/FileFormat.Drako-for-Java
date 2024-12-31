package dev.fileformat.drako;
import java.util.Stack;
class PredictionDegreeTraverser extends TraverserBase<CornerTable>
{    
    // For efficiency reasons, the priority traversal is implemented using buckets
    // where each buckets represent a stack of available corners for a given
    // priority. Corners with the highest priority are always processed first.
    // 
    static final int K_MAX_PRIORITY = 3;
    private Stack<Integer>[] traversal_stacks_;
    // Keep the track of the best available priority to improve the performance
    // of PopNextCornerToTraverse() method.
    // 
    int best_priority_;
    // Prediction degree available for each vertex.
    // 
    private int[] prediction_degree_;
    CornerTable cornerTable;
    private MeshAttributeIndicesEncodingObserver<CornerTable> observer;
    public PredictionDegreeTraverser(MeshAttributeIndicesEncodingObserver<CornerTable> observer)
    {
        this.$initFields$();
        this.cornerTable = observer.getCornerTable();
        this.observer = observer;
        this.init(cornerTable, observer);
        for (int i = 0; i < K_MAX_PRIORITY; i++)
        {
            traversal_stacks_[i] = new Stack<Integer>();
        }
        
    }
    
    // Called before any traversing starts.
    // 
    @Override
    public void onTraversalStart()
    {
        this.prediction_degree_ = new int[cornerTable.getNumVertices()];
    }
    
    // Called when all the traversing is done.
    // 
    @Override
    public void onTraversalEnd()
    {
    }
    
    @Override
    public void traverseFromCorner(int corner_id)
    {
        if (prediction_degree_.length == 0)
            return;
        
        // Traversal starts from the |corner_id|. It's going to follow either the
        // right or the left neighboring faces to |corner_id| based on their
        // prediction degree.
        traversal_stacks_[0].push(corner_id);
        this.best_priority_ = 0;
        int next_vert = cornerTable.vertex(cornerTable.next(corner_id));
        int prev_vert = cornerTable.vertex(cornerTable.previous(corner_id));
        if (!this.isVertexVisited(next_vert))
        {
            this.markVertexVisited(next_vert);
            observer.onNewVertexVisited(next_vert, cornerTable.next(corner_id));
        }
        
        
        if (!this.isVertexVisited(prev_vert))
        {
            this.markVertexVisited(prev_vert);
            observer.onNewVertexVisited(prev_vert, cornerTable.previous(corner_id));
        }
        
        int tip_vertex = cornerTable.vertex(corner_id);
        if (!this.isVertexVisited(tip_vertex))
        {
            this.markVertexVisited(tip_vertex);
            observer.onNewVertexVisited(tip_vertex, corner_id);
        }
        
        
        // Start the actual traversal.
        while ((corner_id = this.popNextCornerToTraverse()) != CornerTable.K_INVALID_CORNER_INDEX)
        {
            int face_id = corner_id / 3;
            // Make sure the face hasn't been visited yet.
            if (this.isFaceVisited(face_id))
                continue;
            
            while (true)
            {
                face_id = corner_id / 3;
                this.markFaceVisited(face_id);
                observer.onNewFaceVisited(face_id);
                int vert_id = cornerTable.vertex(corner_id);
                if (!this.isVertexVisited(vert_id))
                {
                    this.markVertexVisited(vert_id);
                    observer.onNewVertexVisited(vert_id, corner_id);
                }
                
                int right_corner_id = cornerTable.getRightCorner(corner_id);
                int left_corner_id = cornerTable.getLeftCorner(corner_id);
                int right_face_id = right_corner_id == CornerTable.K_INVALID_CORNER_INDEX ? CornerTable.K_INVALID_FACE_INDEX : right_corner_id / 3;
                int left_face_id = left_corner_id == CornerTable.K_INVALID_CORNER_INDEX ? CornerTable.K_INVALID_FACE_INDEX : left_corner_id / 3;
                boolean is_right_face_visited = this.isFaceVisited(right_face_id);
                boolean is_left_face_visited = this.isFaceVisited(left_face_id);
                
                if (!is_left_face_visited)
                {
                    int priority = this.computePriority(left_corner_id);
                    if (is_right_face_visited && (priority <= best_priority_))
                    {
                        // Right face has been already visited and the priority is equal or
                        // better than the best priority. We are sure that the left face
                        // would be traversed next so there is no need to put it onto the
                        // stack.
                        corner_id = left_corner_id;
                        continue;
                    }
                    else
                    {
                        this.addCornerToTraversalStack(left_corner_id, priority);
                    }
                    
                }
                
                
                if (!is_right_face_visited)
                {
                    int priority = this.computePriority(right_corner_id);
                    if (priority <= best_priority_)
                    {
                        // We are sure that the right face would be traversed next so there
                        // is no need to put it onto the stack.
                        corner_id = right_corner_id;
                        continue;
                    }
                    else
                    {
                        this.addCornerToTraversalStack(right_corner_id, priority);
                    }
                    
                }
                
                
                // Couldn't proceed directly to the next corner
                break;
            }
            
        }
        
    }
    
    // Retrieves the next available corner (edge) to traverse. Edges are processed
    // based on their priorities.
    // Returns kInvalidCornerIndex when there is no edge available.
    // 
    private int popNextCornerToTraverse()
    {
        for (int i = best_priority_; i < K_MAX_PRIORITY; ++i)
        {
            if (!traversal_stacks_[i].isEmpty())
            {
                int ret = traversal_stacks_[i].peek();
                traversal_stacks_[i].pop();
                this.best_priority_ = i;
                return ret;
            }
            
        }
        
        
        return CornerTable.K_INVALID_CORNER_INDEX;
    }
    
    private void addCornerToTraversalStack(int ci, int priority)
    {
        traversal_stacks_[priority].push(ci);
        // Make sure that the best available priority is up to date.
        if (priority < best_priority_)
        {
            this.best_priority_ = priority;
        }
        
    }
    
    // Returns the priority of traversing edge leading to |corner_id|.
    // 
    private int computePriority(int corner_id)
    {
        int v_tip = cornerTable.vertex(corner_id);
        int priority = 0;
        if (!this.isVertexVisited(v_tip))
        {
            int degree = ++prediction_degree_[v_tip];
            // Priority 1 when prediction degree > 1, otherwise 2.
            priority = degree > 1 ? 1 : 2;
        }
        
        
        // Clamp the priority to the maximum number of buckets.
        if (priority >= K_MAX_PRIORITY)
        {
            priority = K_MAX_PRIORITY - 1;
        }
        
        return priority;
    }
    
    private void $initFields$()
    {
        try
        {
            traversal_stacks_ = new Stack[K_MAX_PRIORITY];
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
