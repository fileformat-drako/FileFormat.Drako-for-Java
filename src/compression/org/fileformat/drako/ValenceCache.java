package org.fileformat.drako;
// ValenceCache provides support for the caching of valences off of some kind of
// CornerTable 'type' of class.
// No valences should be queried before Caching is
// performed and values should be removed/recached when changes to the
// underlying mesh are taking place.
// 
class ValenceCache
{    
    private CornerTable table_;
    private byte[] vertex_valence_cache_8_bit_;
    private int[] vertex_valence_cache_32_bit_;
    public ValenceCache(CornerTable table)
    {
        this.table_ = table;
    }
    
    // Do not call before CacheValences() / CacheValencesInaccurate().
    // 
    public byte valenceFromCacheInaccurateC(int c)
    {
        if (c == -1)
            return -1;
        return this.valenceFromCacheInaccurateV(table_.vertex(c));
    }
    
    public int valenceFromCacheC(int c)
    {
        if (c == -1)
            return -1;
        return this.valenceFromCacheV(table_.vertex(c));
    }
    
    public int confidentValenceFromCacheV(int v)
    {
        //DRACO_DCHECK_LT(v, table_.NumVertices);
        //DRACO_DCHECK_EQ(vertex_valence_cache_32_bit_.Length, table_.NumVertices);
        return vertex_valence_cache_32_bit_[v];
    }
    
    // Collect the valence for all vertices so they can be reused later.  The
    // 'inaccurate' versions of this family of functions clips the true valence
    // of the vertices to 8 signed bits as a space optimization.  This clipping
    // will lead to occasionally wrong results.  If accurate results are required
    // under all circumstances, do not use the 'inaccurate' version or else
    // use it and fetch the correct result in the event the value appears clipped.
    // The topology of the mesh should be a constant when Valence Cache functions
    // are being used.  Modification of the mesh while cache(s) are filled will
    // not guarantee proper results on subsequent calls unless they are rebuilt.
    // 
    public void cacheValencesInaccurate()
    {
        if (vertex_valence_cache_8_bit_ == null)
        {
            int vertex_count = table_.getNumVertices();
            this.vertex_valence_cache_8_bit_ = new byte[vertex_count];
            for (int v = 0; v < vertex_count; v += 1)
            {
                vertex_valence_cache_8_bit_[v] = (byte)((byte)Math.min(Byte.MAX_VALUE, table_.valence(v)));
            }
            
        }
        
    }
    
    public void cacheValences()
    {
        if (vertex_valence_cache_32_bit_ == null)
        {
            int vertex_count = table_.getNumVertices();
            this.vertex_valence_cache_32_bit_ = new int[vertex_count];
            for (int v = 0; v < vertex_count; v += 1)
            {
                vertex_valence_cache_32_bit_[v] = table_.valence(v);
            }
            
        }
        
    }
    
    public byte confidentValenceFromCacheInaccurateC(int c)
    {
        //DRACO_DCHECK_GE(c, 0);
        return this.confidentValenceFromCacheInaccurateV(table_.confidentVertex(c));
    }
    
    public int confidentValenceFromCacheC(int c)
    {
        //DRACO_DCHECK_GE(c, 0);
        return this.confidentValenceFromCacheV(table_.confidentVertex(c));
    }
    
    public byte valenceFromCacheInaccurateV(int v)
    {
        //DRACO_DCHECK_EQ(vertex_valence_cache_8_bit_.Length, table_.NumVertices);
        if (v == -1 || (v >= table_.getNumVertices()))
            return -1;
        return this.confidentValenceFromCacheInaccurateV(v);
    }
    
    public byte confidentValenceFromCacheInaccurateV(int v)
    {
        return vertex_valence_cache_8_bit_[v];
    }
    
    // TODO(draco-eng) Add unit tests for ValenceCache functions.
    // 
    public int valenceFromCacheV(int v)
    {
        //DRACO_DCHECK_EQ(vertex_valence_cache_32_bit_.Length, table_.NumVertices);
        if (v == -1 || (v >= table_.getNumVertices()))
            return -1;
        return this.confidentValenceFromCacheC(v);
    }
    
    // Clear the cache of valences and deallocate the memory.
    // 
    public void clearValenceCacheInaccurate()
    {
        this.vertex_valence_cache_8_bit_ = null;
    }
    
    public void clearValenceCache()
    {
        this.vertex_valence_cache_32_bit_ = null;
    }
    
    public boolean isCacheEmpty()
    {
        return vertex_valence_cache_8_bit_ == null && (vertex_valence_cache_32_bit_ == null);
    }
    
}
