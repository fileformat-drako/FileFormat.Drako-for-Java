package dev.fileformat.drako;
/**
 *  Class for generating a sequence of point ids that can be used to encode
 *  or decode attribute values in a specific order.
 *  See sequentialAttributeEncoders/decodersController.h for more details.
 *
 */
abstract class PointsSequencer
{    
    protected IntList outPointIds;
    /**
     *  Fills the |outPointIds| with the generated sequence of point ids.
     *
     */
    public int[] generateSequence()
        throws DrakoException
    {
        this.outPointIds.clear();
        this.generateSequenceInternal();
        return this.outPointIds.toArray();
    }
    
    /**
     *  Appends a point to the sequence.
     *
     */
    public void addPointId(int pointId)
    {
        outPointIds.add(pointId);
    }
    
    /**
     *  Sets the correct mapping between point ids and value ids. I.e., the inverse
     *  of the |outPointIds|. In general, |outPointIds| does not contain
     *  sufficient information to compute the inverse map, because not all point
     *  ids are necessarily contained within the map.
     *  Must be implemented for sequencers that are used by attribute decoders.
     *
     */
    public void updatePointToAttributeIndexMapping(PointAttribute attr)
        throws DrakoException
    {
        throw DracoUtils.failed();
    }
    
    /**
     *  Method that needs to be implemented by the derived classes. The
     *  implementation is responsible for filling |outPointIds| with the valid
     *  sequence of point ids.
     *
     */
    protected abstract void generateSequenceInternal()
        throws DrakoException;
    
    public PointsSequencer()
    {
        this.$initFields$();
    }
    
    private void $initFields$()
    {
        try
        {
            outPointIds = new IntList();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
    }
    
}
