package org.fileformat.drako;
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
    public boolean generateSequence(int[][] outPointIds)
    {
        this.outPointIds.clear();
        boolean ret = this.generateSequenceInternal();
        outPointIds[0] = this.outPointIds.toArray();
        return ret;
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
    public boolean updatePointToAttributeIndexMapping(PointAttribute attr)
    {
        return DracoUtils.failed();
    }
    
    /**
     *  Method that needs to be implemented by the derived classes. The
     *  implementation is responsible for filling |outPointIds| with the valid
     *  sequence of point ids.
     *
     */
    protected abstract boolean generateSequenceInternal();
    
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
