package org.openize.drako;
/**
 *  A variable length encoding for storing all possible topology configurations
 *  during traversal of mesh's surface. The configurations are based on visited
 *  state of neighboring triangles around a currently processed face corner.
 *  Note that about half of the encountered configurations is expected to be of
 *  type TOPOLOGYC. It's guaranteed that the encoding will use at most 2 bits
 *  per triangle for meshes with no holes and up to 6 bits per triangle for
 *  general meshes. In addition, the encoding will take up to 4 bits per triangle
 *  for each non-position attribute attached to the mesh.
 * 
 *      *-------*          *-------*          *-------*
 *     / \     / \        / \     / \        / \     / \
 *    /   \   /   \      /   \   /   \      /   \   /   \
 *   /     \ /     \    /     \ /     \    /     \ /     \
 *  *-------v-------*  *-------v-------*  *-------v-------*
 *   \     /x\     /          /x\     /    \     /x\
 *    \   /   \   /          /   \   /      \   /   \
 *     \ /  C  \ /          /  L  \ /        \ /  R  \
 *      *-------*          *-------*          *-------*
 * 
 *      *       *
 *     / \     / \
 *    /   \   /   \
 *   /     \ /     \
 *  *-------v-------*          v
 *   \     /x\     /          /x\
 *    \   /   \   /          /   \
 *     \ /  S  \ /          /  E  \
 *      *-------*          *-------*
 *
 */
final class EdgeBreakerTopologyBitPattern
{    
    public static final int C = 0x0;
    public static final int S = 0x1;
    public static final int L = 0x3;
    public static final int R = 0x5;
    public static final int E = 0x7;
    // A special symbol that's not actually encoded, but it can be used to mark
    // the initial face that triggers the mesh encoding of a single connected
    // component.
    // 
    public static final int INIT_FACE = 8;
    // A special value used to indicate an invalid symbol.
    // 
    public static final int INVALID = 9;
    
    
}
