package org.openize.drako;
/**
 *  A simple sequencer that generates a linear sequence [0, numPoints - 1].
 *  I.e., the order of the points is preserved for the input data.
 *
 */
class LinearSequencer extends PointsSequencer
{    
    private int numPoints;
    public LinearSequencer(int numPoints)
    {
        this.numPoints = numPoints;
    }
    
    @Override
    public boolean updatePointToAttributeIndexMapping(PointAttribute attribute)
    {
        attribute.setIdentityMapping(true);
        return true;
    }
    
    @Override
    protected boolean generateSequenceInternal()
    {
        this.outPointIds.setCapacity(numPoints);
        for (int i = 0; i < numPoints; ++i)
        {
            this.outPointIds.add(i);
        }
        
        return true;
    }
    
}
