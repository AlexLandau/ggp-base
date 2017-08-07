
package org.ggp.base.util.propnet.sancho;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.Role;

/**
 * @author steve
 * Collection of currently legal moves associated with a propNet.  The propNet
 * will update this collection by direct notification when legal move propositions change
 * state
 */
public class ForwardDeadReckonLegalMoveSet implements ForwardDeadReckonComponentTransitionNotifier
{
  /** List of legal move infos into which the integer of members of this
   * collection's contents are indexes
   */
  private List<ForwardDeadReckonLegalMoveInfo> masterList;
  private List<ForwardDeadReckonLegalMoveInfo> alwaysLegalMoves;
  /** Master list crystalized into an array for fast access */
  ForwardDeadReckonLegalMoveInfo[]             masterListAsArray;

  /** The set of roles whose legal moves are being tracked */
  private Role[]                               roles;
  private ForwardDeadReckonLegalMoveSetCollection[] preAllocatedCollections;

  /**
   * The linkage array contains backward and forward pointers in the high and low words for
   * each legal move.  A value other than 0xFFFF in the prev-word can be used an a O(1) test
   * for presence of that move in the set, and the list can be enumerated to iterate over
   * the set legals
   */
  final int[][]                                  linkage;
  private static final int                       LINKAGE_MASK_NEXT = 0xFFFF;
  private static final int                       LINKAGE_MASK_PREV = 0xFFFF0000;
  private static final int                       PREV_SHIFT = 16;
  /**
   * For each role, the index of the first active move
   */
  final short[]                                  firstActive;
  private final short[]                          lastImmutableActive;
  private final short[]                          lastActive;
  private final short[]                          numAlwaysActive;
  private final short[]                          numActive;

  private static final short                     INVALID_INDEX = -1;
  private static final int                       INVALID_PREV = 0xFFFF0000;
  private static final int                       SENTINEL_PREV = 0xFFFE0000;

  private final Random                         rand = new Random();

  private class ForwardDeadReckonLegalMoveSetIterator
                                                     implements
                                                     Iterator<ForwardDeadReckonLegalMoveInfo>
  {
    private final ForwardDeadReckonLegalMoveSet parent;
    int                                         index;
    private final int                           roleIndex;

    public ForwardDeadReckonLegalMoveSetIterator(ForwardDeadReckonLegalMoveSet parentSet,
                                                 int theRoleIndex)
    {
      parent = parentSet;
      roleIndex = theRoleIndex;
      reset();
    }

    void reset()
    {
      index = parent.firstActive[roleIndex];
    }

    @Override
    public boolean hasNext()
    {
      return ((index & LINKAGE_MASK_NEXT) != 0xFFFF);
    }

    @Override
    public ForwardDeadReckonLegalMoveInfo next()
    {
      ForwardDeadReckonLegalMoveInfo result = parent.masterListAsArray[index];

      index = parent.linkage[roleIndex][index] & LINKAGE_MASK_NEXT;
      return result;
    }

    @Override
    public void remove()
    {
      assert(false);
      // TODO Auto-generated method stub

    }
  }

  private class ForwardDeadReckonLegalMoveSetCollection
                                                       implements
                                                       Collection<ForwardDeadReckonLegalMoveInfo>
  {
    private ForwardDeadReckonLegalMoveSet parent;
    int                                   roleIndex;
    /**
     * For performance reasons (minimize GC churn) we return a single pre-allocated
     * instance of an iterator on all calls, resetting it's state on each request.
     * Obviously this means that this collection may only be used by ONE thread
     * concurrently, and iterations of it must never be nested.  This is true for all
     * current and expected uses, but it is not policable, so caveat emptor!
     */
    private final ForwardDeadReckonLegalMoveSetIterator preAllocatedIterator;

    public ForwardDeadReckonLegalMoveSetCollection(ForwardDeadReckonLegalMoveSet parentSet,
                                                   int theRoleIndex)
    {
      parent = parentSet;
      roleIndex = theRoleIndex;

      preAllocatedIterator = new ForwardDeadReckonLegalMoveSetIterator(parentSet, theRoleIndex);
    }

    @Override
    public Iterator<ForwardDeadReckonLegalMoveInfo> iterator()
    {
      preAllocatedIterator.reset();
      return preAllocatedIterator;
    }

    @Override
    public boolean add(ForwardDeadReckonLegalMoveInfo xiE)
    {
      // Not supported - this is a read-only collection
      return false;
    }

    @Override
    public boolean addAll(Collection<? extends ForwardDeadReckonLegalMoveInfo> xiC)
    {
      // Not supported - this is a read-only collection
      return false;
    }

    @Override
    public void clear()
    {
      // Not supported - this is a read-only collection
    }

    @Override
    public boolean contains(Object xiO)
    {
      ForwardDeadReckonLegalMoveInfo move = (ForwardDeadReckonLegalMoveInfo)xiO;
      //return (move != null && parent.contents.fastGet(move.masterIndex));
      return (move != null && roleIndex == move.mRoleIndex && parent.isLegalMove(move.mRoleIndex, move.mMasterIndex));
    }

    @Override
    public boolean containsAll(Collection<?> xiC)
    {
      for(Object o : xiC)
      {
        if ( !contains(o))
        {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean isEmpty()
    {
      return parent.getNumChoices(roleIndex) == 0;
    }

    @Override
    public boolean remove(Object xiO)
    {
      // Not supported - this is a read-only collection
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> xiC)
    {
      // Not supported - this is a read-only collection
      return false;
    }

    @Override
    public boolean retainAll(Collection<?> xiC)
    {
      // Not supported - this is a read-only collection
      return false;
    }

    @Override
    public int size()
    {
      return parent.getNumChoices(roleIndex);
    }

    @Override
    public Object[] toArray()
    {
      Object[] result = new Object[size()];
      int index = 0;

      for(Object o : this)
      {
        result[index++] = o;
      }
      return result;
    }

    @Override
    public <T> T[] toArray(T[] xiA)
    {
      // TODO Auto-generated method stub
      return null;
    }

  }

  /**
   * Construct a legal move set with the same master associations as an
   * existing one (which is effectively used as a template)
   * @param master existing move set to use as a template
   */
  public ForwardDeadReckonLegalMoveSet(ForwardDeadReckonLegalMoveSet master)
  {
    masterList = master.masterList;
    alwaysLegalMoves = master.alwaysLegalMoves;
    masterListAsArray = master.masterListAsArray;
    roles = master.roles;
    preAllocatedCollections = new ForwardDeadReckonLegalMoveSetCollection[roles.length];
    numActive = new short[roles.length];
    numAlwaysActive = new short[roles.length];
    linkage = new int[roles.length][];
    firstActive = new short[roles.length];
    lastImmutableActive = new short[roles.length];
    lastActive = new short[roles.length];

    for (int i = 0; i < roles.length; i++)
    {
      preAllocatedCollections[i] = new ForwardDeadReckonLegalMoveSetCollection(this, i);
      if ( masterListAsArray != null )
      {
        linkage[i] = new int[masterListAsArray.length];

        for(int j = 0; j < masterListAsArray.length; j++)
        {
          linkage[i][j] = INVALID_PREV;
        }
      }
      if ( master.lastImmutableActive[i] >= 0 )
      {
        firstActive[i] = master.firstActive[i];
        lastActive[i] = master.lastImmutableActive[i];

        for(int index = master.firstActive[i]; index >= 0; index = master.linkage[i][index] & LINKAGE_MASK_NEXT)
        {
          linkage[i][index] = master.linkage[i][index];
          if ( index == lastImmutableActive[i] )
          {
            break;
          }
        }
      }
      else
      {
        firstActive[i] = INVALID_INDEX;
        lastActive[i] = INVALID_INDEX;
      }
      lastImmutableActive[i] = master.lastImmutableActive[i];
      numActive[i] = master.numAlwaysActive[i];
      numAlwaysActive[i] = master.numAlwaysActive[i];
    }

    assert(masterListAsArray == null || (master.valid() && valid()));
  }

  /**
   * Construct a new legal move set for a specified set of roles
   * @param theRoles
   */
  public ForwardDeadReckonLegalMoveSet(Role[] theRoles)
  {
    masterList = new ArrayList<>();
    alwaysLegalMoves = new ArrayList<>();
    masterListAsArray = null;
    roles = new Role[theRoles.length];
    preAllocatedCollections = new ForwardDeadReckonLegalMoveSetCollection[roles.length];
    numActive = new short[roles.length];
    numAlwaysActive = new short[roles.length];
    linkage = new int[roles.length][];
    firstActive = new short[roles.length];
    lastImmutableActive = new short[roles.length];
    lastActive = new short[roles.length];

    int i = 0;
    for (Role role : theRoles)
    {
      preAllocatedCollections[i] = new ForwardDeadReckonLegalMoveSetCollection(this, i);
      firstActive[i] = -1;
      lastActive[i] = -1;
      lastImmutableActive[i] = -1;
      this.roles[i++] = role;
    }
  }

  /**
   * Crystalize the legal move set to an optimal runtime form.  After
   * this has been called no further changes may be made to the master list
   */
  public void crystalize()
  {
    masterListAsArray = new ForwardDeadReckonLegalMoveInfo[masterList.size()];
    masterList.toArray(masterListAsArray);
    masterList = null;

    for (int i = 0; i < roles.length; i++)
    {
      linkage[i] = new int[masterListAsArray.length];

      for(int j = 0; j < masterListAsArray.length; j++)
      {
        linkage[i][j] = INVALID_PREV;
      }

      for(ForwardDeadReckonLegalMoveInfo info : alwaysLegalMoves)
      {
        if ( info.mRoleIndex == i )
        {
          add(info.mMasterIndex);
        }
      }

      lastImmutableActive[i] = lastActive[i];
      numAlwaysActive[i] = numActive[i];
    }

    alwaysLegalMoves = null;

    assert(valid());
  }

  /**
   * Retrieve the master list of all legal move infos
   * @return the full list of legal move infos
   */
  public ForwardDeadReckonLegalMoveInfo[] getMasterList()
  {
    return masterListAsArray;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    boolean firstRole = true;

    sb.append("[ ");
    for (int roleIndex = 0; roleIndex < roles.length; roleIndex++)
    {
      boolean first = true;
      int moveIndex = 1;

      sb.append("( ");

      for(int index = firstActive[roleIndex]; (index & LINKAGE_MASK_NEXT) != 0xFFFF; index = linkage[roleIndex][index] & LINKAGE_MASK_NEXT)
      {
        ForwardDeadReckonLegalMoveInfo info = masterListAsArray[index];

        assert(info.mRoleIndex == roleIndex);
        if (!first)
        {
          sb.append(", ");
        }
        sb.append(moveIndex++ + ": " + masterListAsArray[index].mMove);
        first = false;
      }

      sb.append(" )");
      if (!firstRole)
      {
        sb.append("; ");
      }
      firstRole = false;
    }
    sb.append(" ]");

    return sb.toString();
  }

  /**
   * Empty the collection
   */
  public void clear()
  {
    assert(valid());

    for(int i = 0; i < roles.length; i++)
    {
      if ( lastActive[i] >= 0 && lastActive[i] != lastImmutableActive[i] )
      {
        int index = (lastImmutableActive[i] >= 0 ? (linkage[i][lastImmutableActive[i]] & LINKAGE_MASK_NEXT) : firstActive[i]);
        do
        {
          int nextIndex = linkage[i][index] & LINKAGE_MASK_NEXT;
          linkage[i][index] = INVALID_PREV;
          index = nextIndex;
        } while((index & LINKAGE_MASK_NEXT) != 0xFFFF);
      }

      lastActive[i] = lastImmutableActive[i];
      numActive[i] = numAlwaysActive[i];

      if ( lastActive[i] == INVALID_INDEX )
      {
        firstActive[i] = INVALID_INDEX;
      }
    }

    assert(valid());
  }

  /**
   * Copy from another legal move set
   * @param source to copy from
   */
  public void copy(ForwardDeadReckonLegalMoveSet source)
  {
    clear();

    for(int i = 0; i < roles.length; i++)
    {
      assert(numAlwaysActive[i] == source.numAlwaysActive[i]);
      assert(lastImmutableActive[i] == source.lastImmutableActive[i]);

      numActive[i] = source.numActive[i];
      firstActive[i] = source.firstActive[i];
      lastActive[i] = source.lastActive[i];

      for(int index = source.firstActive[i]; (index & LINKAGE_MASK_NEXT) != 0xFFFF; index = source.linkage[i][index] & LINKAGE_MASK_NEXT)
      {
        linkage[i][index] = source.linkage[i][index];
      }
    }

    assert(valid());
  }

  /**
   * Add a new legal move info to the master list, resolving its id
   * as we do so (i.e. - giving it a concrete index in the master list
   * which will not subsequently change)
   * @param info Legal move info to add
   * @param masterIndex asserted index we want this info to be placed at, or -1 for next available
   * @return the index of the added move's info in the master list
   */
  public int resolveId(ForwardDeadReckonLegalMoveInfo info, int masterIndex)
  {
    if ( masterIndex == -1 )
    {
      masterList.add(info);

      masterIndex = masterList.size() - 1;
    }
    else
    {
      if ( masterList.size() == Short.MAX_VALUE )
      {
        throw new RuntimeException("Move count exceeds supported limit of " + Short.MAX_VALUE);
      }
      while(masterIndex >= masterList.size())
      {
        masterList.add(null);
      }
      masterList.set(masterIndex,  info);
    }

    return masterIndex;
  }

  /**
   * Add a specified always legal move to the collection.  This move must be one
   * that is already known in the master list (i.e. - resolveId() must have
   * been called with the same parameter at some time prior to this call)
   * @param info Legal move to add
   */
  public void addAlwaysLegal(ForwardDeadReckonLegalMoveInfo info)
  {
    assert(info.mMasterIndex != -1);

    alwaysLegalMoves.add(info);
  }

  /**
   * Add a specified legal move.  The collection must already have been crystalized
   * @param info
   */
  public void add(ForwardDeadReckonLegalMoveInfo info)
  {
    assert(info.mMasterIndex != -1);

    assert(valid());

    int roleIndex = info.mRoleIndex;
    short index = (short)info.mMasterIndex;
    int[] linkageForRole = linkage[roleIndex];

    if ( linkageForRole[index] == INVALID_PREV )
    {
      short lastActiveIndex = lastActive[roleIndex];

      if ( lastActiveIndex >= 0 )
      {
        linkageForRole[index] = (lastActiveIndex << PREV_SHIFT) | 0xFFFF;
        linkageForRole[lastActiveIndex] = (linkageForRole[lastActiveIndex] & LINKAGE_MASK_PREV) | index;
      }
      else
      {
        //  Set a negative prev distinguished from INVALID_INDEX so we can use
        //  a non-INVALID_INDEX value for prev as a O(1) contains check
        linkageForRole[index] = SENTINEL_PREV | 0xFFFF;
      }

      if ( firstActive[roleIndex] < 0 )
      {
        firstActive[roleIndex] = index;
      }
      lastActive[roleIndex] = index;

      numActive[roleIndex]++;
    }

    assert(valid());
 }

  private boolean valid()
  {
    //  Commented out sections are for a really full check, butn it makes it super-expensive
    //  so not included in regular debug build assertion checking
    //BitSet mirror = new BitSet();
    for(int i = 0; i < roles.length; i++)
    {
      //mirror.clear();
      int count = 0;

      for(int index = firstActive[i]; (index & LINKAGE_MASK_NEXT) != 0xFFFF; index = linkage[i][index] & LINKAGE_MASK_NEXT)
      {
        assert((linkage[i][index] & LINKAGE_MASK_PREV) != INVALID_PREV);
        assert(index != (linkage[i][index] & LINKAGE_MASK_NEXT));
        count++;
        //mirror.set(index);
      }

      assert(count == numActive[i]);
//      for(int j = 0; j < masterListAsArray.length; j++)
//      {
//        if ( !mirror.get(j))
//        {
//          assert(linkage[i][j] == INVALID_PREV);
//        }
//      }
    }

    return true;
  }


  @Override
  public void add(int index)
  {
    ForwardDeadReckonLegalMoveInfo info = masterListAsArray[index];

    add(info);
  }

  /**
   * Remove a specified legal move to the collection.  This move should be one
   * that is already known in the master list (i.e. - resolveId() must have
   * been called with the same parameter at some time prior to this call)
   * @param info Legal move to remove
   */
  public void remove(ForwardDeadReckonLegalMoveInfo info)
  {
    assert(info.mMasterIndex != -1);

    remove(info.mRoleIndex, info.mMasterIndex);
  }

  private void remove(int roleIndex, int index)
  {
    assert(valid());

    int[] linkageForRole = linkage[roleIndex];
    int prevIndex = linkageForRole[index] >> PREV_SHIFT;
    int nextIndex = linkageForRole[index] & LINKAGE_MASK_NEXT;

    assert(prevIndex != 0xFFFF);

    if ( prevIndex != (SENTINEL_PREV >> PREV_SHIFT) )
    {
      linkageForRole[prevIndex] = (linkageForRole[prevIndex] & LINKAGE_MASK_PREV) | nextIndex;
    }
    else
    {
      firstActive[roleIndex] = (short)nextIndex;
    }

    if ( nextIndex != 0xFFFF )
    {
      linkageForRole[nextIndex] = (linkageForRole[nextIndex] & LINKAGE_MASK_NEXT) | (prevIndex << PREV_SHIFT);
    }
    else
    {
      lastActive[roleIndex] = (short)prevIndex;
    }

    linkageForRole[index] = INVALID_PREV;
    numActive[roleIndex]--;

    assert(valid());
  }

  @Override
  public void remove(int index)
  {
    ForwardDeadReckonLegalMoveInfo info = masterListAsArray[index];

    remove(info);
  }

  /**
   * Merge with another legal move set collection - result is the union
   * @param other set to merge into this set
   */
  public void merge(ForwardDeadReckonLegalMoveSet other)
  {
    assert(valid());
    assert(other.valid());

    for(int i = 0; i < roles.length; i++)
    {
      assert(numAlwaysActive[i] == other.numAlwaysActive[i]);
      assert(lastImmutableActive[i] == other.lastImmutableActive[i]);

      for(int index = other.firstActive[i]; (index & LINKAGE_MASK_NEXT) != 0xFFFF; index = other.linkage[i][index] & LINKAGE_MASK_NEXT)
      {
        add(index);
      }
    }
    assert(valid());
  }

  /**
   * Intersect with another legal move set collection - result is the intersection
   * @param other set to intersect into this set
   */
  public void intersect(ForwardDeadReckonLegalMoveSet other)
  {
    assert(valid());
    assert(other.valid());

    for(int i = 0; i < roles.length; i++)
    {
      assert(numAlwaysActive[i] == other.numAlwaysActive[i]);
      assert(lastImmutableActive[i] == other.lastImmutableActive[i]);

      int nextIndex;

      for(int index = firstActive[i]; (index & LINKAGE_MASK_NEXT) != 0xFFFF; index = nextIndex)
      {
        nextIndex = linkage[i][index] & LINKAGE_MASK_NEXT;

        if ( !other.isLegalMove(i, index) )
        {
          remove(i, index);
        }
      }
    }
    assert(valid());
  }

  /**
   * retrieve the set of legal moves for a specified role
   * @param roleIndex role for which we want the legal moves
   * @return collection of legal move infos
   */
  public Collection<ForwardDeadReckonLegalMoveInfo> getContents(int roleIndex)
  {
    return preAllocatedCollections[roleIndex];
  }

  /**
   * retrieve the set of legal moves for a specified role
   * @param role role for which we want the legal moves
   * @return collection of legal move infos
   */
  public Collection<ForwardDeadReckonLegalMoveInfo> getContents(Role role)
  {
    for (int i = 0; i < roles.length; i++)
    {
      if (roles[i].equals(role))
      {
        return preAllocatedCollections[i];
      }
    }

    return null;
  }

  /**
   * Determine how many legal moves there are for a specified role
   * @param role
   * @return number of legals
   */
  public int getNumChoices(Role role)
  {
    for (int i = 0; i < roles.length; i++)
    {
      if (roles[i].equals(role))
      {
        return getNumChoices(i);
      }
    }

    return 0;
  }

  /**
   * Determine how many legal moves there are for a specified role
   * @param roleIndex
   * @return number of legals
   */
  public int getNumChoices(int roleIndex)
  {

    return numActive[roleIndex];
  }

  /**
   * Determine if a specified move is legal for a specified role
   * @param role - role to check for
   * @param move - move being checked
   * @return whether the move is legal
   */
  public boolean isLegalMove(Role role, ForwardDeadReckonLegalMoveInfo move)
  {
    for (int i = 0; i < roles.length; i++)
    {
      if (roles[i].equals(role))
      {
        return isLegalMove(i, move);
      }
    }

    return false;
  }

  /**
   * Determine if a specified move is legal for a specified role
   * @param roleIndex - index of role to check for
   * @param move - move being checked
   * @return whether the move is legal
   */
  public boolean isLegalMove(int roleIndex, ForwardDeadReckonLegalMoveInfo move)
  {
    return isLegalMove(roleIndex, move.mMasterIndex);
  }

  /**
   * Determine if a specified move is legal for a specified role
   * @param roleIndex
   * @param index
   * @return
   */
  boolean isLegalMove(int roleIndex, int index)
  {
    return (linkage[roleIndex].length > index && linkage[roleIndex][index] != INVALID_PREV);
  }

  /**
   * Generate a random move for a given role from the current legal set
   * @param roleIndex
   * @return move chosen
   */
  public ForwardDeadReckonLegalMoveInfo getRandomMove(int roleIndex)
  {
    assert(numActive[roleIndex] > 0);
    int count = rand.nextInt(numActive[roleIndex]);
    int index = firstActive[roleIndex];

    while(count-- > 0)
    {
      index = linkage[roleIndex][index] & LINKAGE_MASK_NEXT;
    }

    return masterListAsArray[index];
  }
}
