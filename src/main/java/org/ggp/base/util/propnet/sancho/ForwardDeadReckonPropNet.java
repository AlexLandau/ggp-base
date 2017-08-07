
package org.ggp.base.util.propnet.sancho;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.sancho.ForwardDeadReckonPropositionCrossReferenceInfo;

/**
 * @author steve
 * A PolymorphicPropnet of the ForwardDeadReckon family
 */
public class ForwardDeadReckonPropNet extends PolymorphicPropNet
{
  private ForwardDeadReckonLegalMoveSet[]         activeLegalMoves;
  private ForwardDeadReckonLegalMoveSet           alwaysTrueLegalMoves;
  private ForwardDeadReckonInternalMachineState[] activeBasePropositions;
  private ForwardDeadReckonInternalMachineState   alwaysTrueBasePropositions;
  private int                                     numInstances;

  /**
   * Fast animator instance for use with this propNet.
   */
  public ForwardDeadReckonPropnetFastAnimator     animator = null;

  /**
   * Creates a new ForwardDeadReckonPropNet from a ggp-base propNet
   * @param sourcePropnet ggp-base propNet to topologically clone
   * @param componentFactory Component factory to use
   */
  public ForwardDeadReckonPropNet(PropNet sourcePropnet,
                                  PolymorphicComponentFactory componentFactory)
  {
    super(sourcePropnet, componentFactory);

    assert(componentFactory instanceof ForwardDeadReckonComponentFactory);
  }

  /**
   * Creates a new ForwardDeadReckonPropNet from an arbitrary polymorphic propNet
   * @param sourcePropnet polymorphic propNet to topologically clone
   * @param componentFactory Component factory to use   */
  public ForwardDeadReckonPropNet(PolymorphicPropNet sourcePropnet,
                                  PolymorphicComponentFactory componentFactory)
  {
    super(sourcePropnet, componentFactory);

    assert(componentFactory instanceof ForwardDeadReckonComponentFactory);
  }

  /**
   * Creates a new ForwardDeadReckonPropNet from a specified set of components and roles
   * @param roles Set of roles to support
   * @param components Set of components that this propNet will encompass
   * @param componentFactory Component factory to use   */
public ForwardDeadReckonPropNet(Role[] roles,
                                Set<PolymorphicComponent> components,
                                PolymorphicComponentFactory componentFactory)
  {
    super(roles, components, componentFactory);

    assert(componentFactory instanceof ForwardDeadReckonComponentFactory);
  }

  @SuppressWarnings("unchecked")
  private void setUpActivePropositionSets(ForwardDeadReckonPropositionCrossReferenceInfo[] masterInfoSet,
                                          int firstBasePropIndex,
                                          ForwardDeadReckonLegalMoveInfo[]   masterMoveList)
  {
    activeLegalMoves = new ForwardDeadReckonLegalMoveSet[numInstances];
    alwaysTrueLegalMoves = new ForwardDeadReckonLegalMoveSet(getRoles());

    for (int instanceId = 0; instanceId < numInstances; instanceId++)
    {
      activeLegalMoves[instanceId] = new ForwardDeadReckonLegalMoveSet(alwaysTrueLegalMoves);
    }

    int roleIndex = 0;

    //  If we're given a pre-existing master move list then the masterIndex values must
    //  correspond - build a map to perform lookup in this case
    Map<Move, Integer>[] masterMoveIndexMap;

    if ( masterMoveList != null )
    {
      masterMoveIndexMap = new Map[getRoles().length];
      for(int i = 0; i < masterMoveIndexMap.length; i++)
      {
        masterMoveIndexMap[i] = new HashMap<>();
      }

      for(ForwardDeadReckonLegalMoveInfo moveInfo : masterMoveList)
      {
        masterMoveIndexMap[moveInfo.mRoleIndex].put(moveInfo.mMove, moveInfo.mMasterIndex);
      }
    }
    else
    {
      masterMoveIndexMap = null;
    }

    for (Role role : getRoles())
    {
      PolymorphicProposition[] legalProps = getLegalPropositions().get(role);

      for (PolymorphicProposition p : legalProps)
      {
        ForwardDeadReckonProposition pfdr = (ForwardDeadReckonProposition)p;
        ForwardDeadReckonLegalMoveInfo info = new ForwardDeadReckonLegalMoveInfo();

        info.mMove = new Move(pfdr.getName().getBody().get(1));
        info.mInputProposition = (ForwardDeadReckonProposition)getLegalInputMap().get(p);
        info.mRoleIndex = roleIndex;
        assert(masterMoveIndexMap == null || masterMoveIndexMap[info.mRoleIndex].containsKey(info.mMove));
        info.mMasterIndex = alwaysTrueLegalMoves.resolveId(info, masterMoveIndexMap == null ? -1 : masterMoveIndexMap[info.mRoleIndex].get(info.mMove));

        PolymorphicComponent propInput = p.getSingleInput();
        if (propInput instanceof PolymorphicConstant)
        {
          if (((PolymorphicConstant)propInput).getValue())
          {
            alwaysTrueLegalMoves.addAlwaysLegal(info);
          }
        }
        else
        {
          for (int instanceId = 0; instanceId < numInstances; instanceId++)
          {
            pfdr.setTransitionSet(info.mMasterIndex,
                                  instanceId,
                                  activeLegalMoves[instanceId]);
          }
        }

        //  Record the legalMoveInfo master index against the legal move prop
        ForwardDeadReckonPropositionInfo propInfo = new ForwardDeadReckonPropositionInfo();

        propInfo.sentence = p.getName();
        propInfo.index = info.mMasterIndex;

        pfdr.setInfo(propInfo);
      }

      roleIndex++;
    }

    alwaysTrueLegalMoves.crystalize();

    for (int instanceId = 0; instanceId < numInstances; instanceId++)
    {
      activeLegalMoves[instanceId].crystalize();
    }

    activeBasePropositions = new ForwardDeadReckonInternalMachineState[numInstances];
    alwaysTrueBasePropositions = new ForwardDeadReckonInternalMachineState(masterInfoSet, firstBasePropIndex);

    for (int instanceId = 0; instanceId < numInstances; instanceId++)
    {
      activeBasePropositions[instanceId] = new ForwardDeadReckonInternalMachineState(masterInfoSet, firstBasePropIndex);

      for (PolymorphicProposition p : getBasePropositions().values())
      {
        PolymorphicComponent input = p.getSingleInput();

        if (input instanceof ForwardDeadReckonTransition)
        {
          ForwardDeadReckonTransition t = (ForwardDeadReckonTransition)input;

          t.setTransitionSet(((ForwardDeadReckonProposition)p).getInfo().index,
                             instanceId,
                             activeBasePropositions[instanceId]);
        }
        else if (instanceId == 0 && input instanceof PolymorphicConstant)
        {
          if (input.getValue())
          {
            alwaysTrueBasePropositions.add(((ForwardDeadReckonProposition)p)
                .getInfo());
          }
        }
      }
    }
  }

  /**
   * Crystalize this propnet into a maximally runtime efficient form
   * Once this is done no further changes may be made to the components or their
   * connections
   * @param masterInfoSet set of base propositions in some defined order
   * @param masterMoveList master list of moves - may be null to generate
   */
  private void crystalize(ForwardDeadReckonPropositionCrossReferenceInfo[] masterInfoSet,
                          int firstBasePropIndex,
                          ForwardDeadReckonLegalMoveInfo[]   masterMoveList)
  {
    for (PolymorphicComponent c : getComponents())
    {
      ForwardDeadReckonComponent fdrc = (ForwardDeadReckonComponent)c;

      fdrc.crystalize(numInstances);
      fdrc.setPropnet(this);
    }

    setUpActivePropositionSets(masterInfoSet, firstBasePropIndex, masterMoveList);

    // Calculate useful goal information.
    for (Role lRole : getRoles())
    {
      PolymorphicProposition[] lGoalProps  = getGoalPropositions().get(lRole);
      for (PolymorphicProposition lGoalProp : lGoalProps)
      {
        ForwardDeadReckonProposition lFDRGoalProp = (ForwardDeadReckonProposition)lGoalProp;
        lFDRGoalProp.setGoalInfo(lRole);
      }
    }

    animator = new ForwardDeadReckonPropnetFastAnimator(this);
    animator.crystalize(numInstances);
  }

  /**
   * Crystalize this propnet into a maximally runtime efficient form
   * Once this is done no further changes may be made to the components or their
   * connections.  A specified number of mutually thread-safe concurrently
   * usable instances is specified
   * @param masterInfoSet set of base propositions in some defined order
   * @param masterMoveList master list of moves - may be null to generate
   * @param theNumInstances Number of independent instances to support
   */
  public void crystalize(ForwardDeadReckonPropositionCrossReferenceInfo[] masterInfoSet,
                         int firstBasePropIndex,
                         ForwardDeadReckonLegalMoveInfo[]   masterMoveList,
                         int theNumInstances)
  {
    numInstances = theNumInstances;

    crystalize(masterInfoSet, firstBasePropIndex, masterMoveList);
  }

  /**
   * @return the collection of legal moves for a specified instance.
   *
   * @param instanceId - Instance to retrieve for.
   */
  public ForwardDeadReckonLegalMoveSet getActiveLegalProps(int instanceId)
  {
    return activeLegalMoves[instanceId];
  }

  /**
   * Retrieve the collection of active base propositions for a specified instance
   * @param instanceId Instance to retrieve for
   * @return the set of currently active base props
   */
  public ForwardDeadReckonInternalMachineState getActiveBaseProps(int instanceId)
  {
    return activeBasePropositions[instanceId];
  }

  /**
   * Retrieve the master list of all possible moves
   * @return the master move list
   */
  public ForwardDeadReckonLegalMoveInfo[] getMasterMoveList()
  {
    return alwaysTrueLegalMoves.getMasterList();
  }

  /**
   * Reset the state of the propNet (to the all inputs of all components
   * assumed to be FALSE state).  Optionally then propagate any components with TRUE
   * outputs to achieve a globally consistent network state
   * @param fullEquilibrium whether to propagate a fully consistent network state
   */
  public void reset(boolean fullEquilibrium)
  {
    for (int instanceId = 0; instanceId < numInstances; instanceId++)
    {
      activeBasePropositions[instanceId].clear();
      activeBasePropositions[instanceId].merge(alwaysTrueBasePropositions);

      if (activeLegalMoves != null)
      {
        activeLegalMoves[instanceId].clear();
        activeLegalMoves[instanceId].merge(alwaysTrueLegalMoves);
      }

      animator.reset(instanceId, fullEquilibrium);
    }
  }

  @SuppressWarnings("unused")
  private void validate()
  {
    for (PolymorphicComponent c : getComponents())
    {
      ((ForwardDeadReckonComponent)c).validate();
    }
  }

  /**
   * Find the proposition with the specified name
   * @param queryProposition
   * @return matching proposition (if any)
   */
  public PolymorphicProposition findProposition(GdlSentence queryProposition)
  {
    for (PolymorphicProposition p : getPropositions())
    {
      if (p.getName() == queryProposition)
      {
        return p;
      }
    }

    return null;
  }
}
