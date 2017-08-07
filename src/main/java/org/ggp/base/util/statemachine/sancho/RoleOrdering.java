package org.ggp.base.util.statemachine.sancho;

import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;

public class RoleOrdering
{
  //  Array of roles reordered so our role is first
  private final Role[] reorderedRoles;
  private final int[]  roleOrderMap;
  private final int[]  inverseRoleOrderMap;
  private final Role   mOurRole;
  private final int    mOurRawRoleIndex;

  /**
   * Construct a role ordering instance.  This is used to map
   * between the native (raw) ordering of the roles in the statemachine
   * as implied by the GDL, and the logical ordering Sancho uses, which
   * always has its own role first
   * @param underlyingStateMachine
   * @param ourRole
   */
  public RoleOrdering(StateMachine underlyingStateMachine, Role ourRole)
  {
    int numRoles = underlyingStateMachine.getRoles().size();
    reorderedRoles = new Role[numRoles];
    roleOrderMap = new int[numRoles];
    inverseRoleOrderMap = new int[numRoles];
    reorderedRoles[0] = ourRole;
    mOurRole = ourRole;

    int lOurRawRoleIndex = -1;
    int roleIndex = 1;
    int rawRoleIndex = 0;
    for (Role role : underlyingStateMachine.getRoles())
    {
      if ( ourRole == null )
      {
        ourRole = role;
      }
      if (role.equals(ourRole))
      {
        lOurRawRoleIndex = rawRoleIndex;
        roleOrderMap[0] = rawRoleIndex;
        inverseRoleOrderMap[rawRoleIndex] = 0;
      }
      else
      {
        roleOrderMap[roleIndex] = rawRoleIndex;
        inverseRoleOrderMap[rawRoleIndex] = roleIndex;
        reorderedRoles[roleIndex++] = role;
      }
      rawRoleIndex++;
    }

    assert(lOurRawRoleIndex != -1);
    mOurRawRoleIndex = lOurRawRoleIndex;
  }

  /**
   * Convert a (logical) role index to a role
   * @param roleIndex
   * @return corresponding Role
   */
  public Role roleIndexToRole(int roleIndex)
  {
    return reorderedRoles[roleIndex];
  }

  /**
   * Convert a role to a (logical) role index
   * @param xiRole
   * @return corresponding logical index
   */
  public int roleToRoleIndex(Role xiRole)
  {
    // Biggest game we play has 6 roles.  Cheaper to spin over array than do a map lookup.
    for (int lii = 0; lii < reorderedRoles.length; lii++)
    {
      if (reorderedRoles[lii].equals(xiRole))
      {
        return lii;
      }
    }

    assert(false) : "Couldn't find role: " + xiRole;
    return -1;
  }

  /**
   * Map a logical role index to the raw statemachine role index
   * @param roleIndex
   * @return corresponding (raw) index
   */
  public int roleIndexToRawRoleIndex(int roleIndex)
  {
    return roleOrderMap[roleIndex];
  }

  /**
   * Map a raw role index to a logical one
   * @param rawRoleIndex
   * @return corresponding logical index
   */
  public int rawRoleIndexToRoleIndex(int rawRoleIndex)
  {
    return inverseRoleOrderMap[rawRoleIndex];
  }

  /**
   * @return our role.
   */
  public Role getOurRole()
  {
    return mOurRole;
  }

  /**
   * @return our (raw) role index.  Note that our logical role index is always 0.
   */
  public int getOurRawRoleIndex()
  {
    return mOurRawRoleIndex;
  }
}
