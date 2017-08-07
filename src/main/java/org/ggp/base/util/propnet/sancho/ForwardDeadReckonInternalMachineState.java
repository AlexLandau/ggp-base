
package org.ggp.base.util.propnet.sancho;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.lucene.util.OpenBitSet;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.sancho.ForwardDeadReckonPropositionCrossReferenceInfo;

/**
 * Internal representation of a machine state, intended for efficient runtime usage
 */
public class ForwardDeadReckonInternalMachineState implements ForwardDeadReckonComponentTransitionNotifier
{
  /**
   * An iterator over the propositions that are set in a machine state.
   *
   * This iterator must be used by a single thread at a time and cannot be used in nested form.
   */
  public static class InternalMachineStateIterator implements Iterator<ForwardDeadReckonPropositionInfo>
  {
    private ForwardDeadReckonInternalMachineState mState;
    private int                                   mIndex;
    private int                                   mFirstIndex = -1;
    private int                                   mLastIndex = -1;

    /**
     * Reset the iterator for the specified state.
     *
     * @param xiState - the state.
     */
    public void reset(ForwardDeadReckonInternalMachineState xiState)
    {
      mState = xiState;
      mFirstIndex = xiState.contents.nextSetBit(xiState.firstBasePropIndex);
      mIndex = mFirstIndex;
      mLastIndex = -1;
    }

    /**
     * Reset for re-enumeration with the same state
     */
    public void reset()
    {
      mIndex = mFirstIndex;
    }

    @Override
    public boolean hasNext()
    {
      return (mIndex != -1);
    }

    @Override
    public ForwardDeadReckonPropositionCrossReferenceInfo next()
    {
      int index = mIndex;
      ForwardDeadReckonPropositionCrossReferenceInfo result = mState.infoSet[mIndex];

      if ( index == mLastIndex )
      {
        mIndex = -1;
      }
      else
      {
        mIndex = mState.contents.nextSetBit(mIndex + 1);
        if ( mIndex == -1 )
        {
          mLastIndex = index;
        }
      }
      return result;
    }

    @Override
    public void remove()
    {
      assert(false) : "InternalMachineStateIterator doesn't support remove()";
    }
  }

  /**
   * Master list of propositions which may be included or not in the state.
   */
  final ForwardDeadReckonPropositionCrossReferenceInfo[] infoSet;
  /**
   * Index of first base prop - preceding elements are pseudo-elements representing goals
   * and terminal
   */
  public final int                                       firstBasePropIndex;

  // Optional heuristic data associated with the state.
//  private HashMap<Heuristic, Object>               heuristicData = null;

  /**
   * BitSet of which propositions are true in the state
   */
  public final OpenBitSet                                 contents;

  //  We cache the hash code to speed up equals, invalidating the cache on mutation operations
  private boolean                                  hashCached = false;
  private int                                      cachedHashCode;

  /**
   * Whether the state is one handled by the X-split of the state machine (else the O split)
   */
  public boolean                                   isXState = false;

  /**
   * Construct a new empty state for the given set of possible base propositions
   * @param masterInfoSet list of the possible base propositions that may occur
   * @param xiFirstBasePropIndex index of first base prop (previous are pseudo-triggers for goals/terminal)
   */
  public ForwardDeadReckonInternalMachineState(ForwardDeadReckonPropositionCrossReferenceInfo[] masterInfoSet, int xiFirstBasePropIndex)
  {
    assert(xiFirstBasePropIndex > 1);
    infoSet = masterInfoSet;
    firstBasePropIndex = xiFirstBasePropIndex;
    contents = new OpenBitSet(infoSet.length);
  }

  /**
   * Clone an existing state.
   *
   * Note - this does NOT preserve any attached heuristic info. !! ARR Doesn't seem to be true
   *
   * @param copyFrom - the state to copy.
   */
  public ForwardDeadReckonInternalMachineState(ForwardDeadReckonInternalMachineState copyFrom)
  {
    this(copyFrom.infoSet, copyFrom.firstBasePropIndex);
    copy(copyFrom);

    if ( copyFrom.hashCached )
    {
      cachedHashCode = copyFrom.cachedHashCode;
      hashCached = true;
    }
  }

  /**
   * Getter
   * @return BitSet of active base propositions
   */
  public OpenBitSet getContents()
  {
    return contents;
  }

  /**
   * Resolve an index in the bitset to its corresponding base prop
   * @param index
   * @return corresponding base prop
   */
  public ForwardDeadReckonPropositionInfo resolveIndex(int index)
  {
    return infoSet[index];
  }

  /**
   * Add a proposition to the set of those present in the state
   * @param info
   */
  public void add(ForwardDeadReckonPropositionInfo info)
  {
    contents.fastSet(info.index);

    hashCached = false;
  }

  @Override
  public void add(int index)
  {
    assert(index < infoSet.length);
    contents.fastSet(index);

    //hashCached = false;
  }

  /**
   * Test whether a specified proposition is present in the state
   * @param info Meta info for the proposition to test
   * @return whether it is present
   */
  public boolean contains(ForwardDeadReckonPropositionInfo info)
  {
    return contents.fastGet(info.index);
  }

  /**
   * @return the number of propositions set in the state
   */
  public long size()
  {
    return contents.cardinality();
  }

  /**
   * XOR this state with a specified other state (leaving the state difference)
   * @param other state to XOR into this one
   */
  public void xor(ForwardDeadReckonInternalMachineState other)
  {
    contents.xor(other.contents);

    hashCached = false;
  }

  /**
   * Invert this state (propositions present cease to be so, those not become so)
   */
  public void invert()
  {
    contents.flip(0, infoSet.length);

    hashCached = false;
  }

  /**
    * Merge this state with a specified other state (leaving the union)
    * @param other state to merge into this one
    */
  public void merge(ForwardDeadReckonInternalMachineState other)
  {
    contents.or(other.contents);

    hashCached = false;
  }

  /**
   * Take the intersection of this state with a specified other state (leaving that intersection)
   * @param other state to AND into this one
   */
  public void intersect(ForwardDeadReckonInternalMachineState other)
  {
    contents.and(other.contents);

    hashCached = false;
  }

  /**
   * Test whether another state has any overlap (in set propositions) with this one
   * @param other Other state to check for intersection with
   * @return whether any intersection exists
   */
  public boolean intersects(ForwardDeadReckonInternalMachineState other)
  {
    return contents.intersects(other.contents);
  }

  /**
   * Determine the size of intersection (num common propositions) with a specified other state.
   *
   * @param other - Other state to check the intersection size with.
   *
   * @return Number of common propositions
   */
  public int intersectionSize(ForwardDeadReckonInternalMachineState other)
  {
    return (int)OpenBitSet.intersectionCount(contents, other.contents);
  }

  /**
   * Copy another state into this one.
   *
   * @param other State to copy
   */
  public void copy(ForwardDeadReckonInternalMachineState other)
  {
    assert(this != other);
    assert(other.contents.getNumWords() == contents.getNumWords());

    int firstIndex = (firstBasePropIndex >> 6);
    int modulo = (firstBasePropIndex & 0x3F);
    long h;
    long[] bits = contents.getBits();
    long[] otherBits = other.contents.getBits();
    // Start with a zero hash and use a mix that results in zero if the input is zero.
    // This effectively truncates trailing zeros without an explicit check.
    if ( modulo != 0 )
    {
      h = ((long)1 << modulo)-1;
      h = ~h;
      bits[firstIndex] = (h & otherBits[firstIndex]);

      firstIndex++;
    }
    System.arraycopy(otherBits, firstIndex, bits, firstIndex, contents.getNumWords()-firstIndex);


    isXState = other.isXState;

//    if ( other.heuristicData != null )
//    {
//      if (heuristicData == null)
//      {
//        heuristicData = new HashMap<>();
//      }
//      else
//      {
//        heuristicData.clear();
//      }
//      heuristicData.putAll(other.heuristicData);
//    }

    hashCached = false;
    cachedHashCode = other.cachedHashCode;
  }

  /**
   * Retrieve a (crude) measure of the distance between two states in state space
   * @param other State to compare with
   * @return Distance in the range [0,1]
   */
  public double distance(ForwardDeadReckonInternalMachineState other)
  {
    long diff = OpenBitSet.xorCount(contents, other.contents);
    long jointSize = OpenBitSet.unionCount(contents, other.contents);
    return (double)diff / jointSize;
  }

  private static final long[] nullPage = new long[256];
  static
  {
    for(int i = 0; i < nullPage.length; i++)
    {
      nullPage[i] = 0;
    }
  }

  /**
   * Clear all propositions leaving an empty state
   */
  public void clear()
  {
    int index = 0;
    long[] Bits = contents.getBits();
    int wordCount = (infoSet.length+63)>>6;

    while(index < wordCount)
    {
      if ( index + nullPage.length > wordCount )
      {
        System.arraycopy(nullPage, 0, Bits, index, wordCount - index);
      }
      else
      {
        System.arraycopy(nullPage, 0, Bits, index, nullPage.length);
      }
      index += nullPage.length;
    }

    isXState = false;

    hashCached = false;
  }

  /**
   * Remove a specified proposition from the state
   * @param info meta info of the proposition to remove
   */
  public void remove(ForwardDeadReckonPropositionInfo info)
  {
    contents.fastClear(info.index);

    hashCached = false;
  }

  @Override
  public void remove(int index)
  {
    contents.fastClear(index);
  }

  /**
   * Convert to a ggp-base MachineState
   * @return MachineState instance corresponding to this state
   */
  public MachineState getMachineState()
  {
    MachineState result = new MachineState(new HashSet<GdlSentence>());

    for (int i = contents.nextSetBit(0); i >= 0; i = contents.nextSetBit(i + 1))
    {
      result.getContents().add(infoSet[i].sentence);
    }

    return result;
  }

//  /**
//   * Store data associated with a heuristic for this machine state.
//   *
//   * @param xiHeuristic - the heuristic.
//   * @param xiData      - the data to store.
//   */
//  public void putHeuristicData(Heuristic xiHeuristic, Object xiData)
//  {
//    if ( heuristicData == null )
//    {
//      heuristicData = new HashMap<>();
//    }
//    heuristicData.put(xiHeuristic, xiData);
//  }

//  /**
//   * Retrieve data for a heuristic, previously stored with {@link #putHeuristicData()}.
//   *
//   * @param xiHeuristic - the heuristic.
//   * @return the previously stored data.
//   */
//  public Object getHeuristicData(Heuristic xiHeuristic)
//  {
//    return (heuristicData == null ? null : heuristicData.get(xiHeuristic));
//  }

  /* Utility methods */
  @Override
  public int hashCode()
  {
    if ( !hashCached )
    {
      cachedHashCode = contents.hashCode();
      hashCached = true;
    }

    return cachedHashCode;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }

    if (o instanceof ForwardDeadReckonInternalMachineState && hashCode() == o.hashCode() )
    {
      return contents.equals(((ForwardDeadReckonInternalMachineState)o).contents);
    }

    return false;
  }

  /**
   * Explicitly mark as dirty (used during state propagation so as to avoid the
   * overhead of having to set the dirty flag on each instance of a trigger)
   */
  public void markDirty()
  {
    hashCached = false;
  }

  /**
   * Known-type version of equals (slightly higher performance due to
   * lack of need to cast from unknown type)
   * @param other
   * @return true if equal
   */
  public boolean equals(ForwardDeadReckonInternalMachineState other)
  {
    if (this == other)
    {
      return true;
    }

    if (other !=null && hashCode() == other.hashCode())
    {
      return contents.equals(other.contents);//basePropsEquals(other);
    }

    return false;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    boolean first = true;

    sb.append("( ");
    for (int i = contents.nextSetBit(0); i >= 0; i = contents.nextSetBit(i + 1))
    {
      if (!first)
      {
        sb.append(", ");
      }
      sb.append(infoSet[i].sentence);
      first = false;
    }
    sb.append(" )");

    return sb.toString();
  }

  /**
   * Determine whether a specified other state is a subset of this state
   * @param other State to test
   * @return whether it is a subset
   */
  public boolean contains(ForwardDeadReckonInternalMachineState other)
  {
    return (OpenBitSet.intersectionCount(contents, other.contents) == other.contents.cardinality());
  }
}
