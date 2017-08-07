package org.ggp.base.util.statemachine.sancho;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveInfo;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveSet;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonProposition;
import org.ggp.base.util.propnet.sancho.PolymorphicAnd;
import org.ggp.base.util.propnet.sancho.PolymorphicComponent;
import org.ggp.base.util.propnet.sancho.PolymorphicNot;
import org.ggp.base.util.propnet.sancho.PolymorphicOr;
import org.ggp.base.util.propnet.sancho.PolymorphicProposition;
import org.ggp.base.util.propnet.sancho.PolymorphicTransition;

/**
 * @author steve
 *  Analyse the state machine to determine if it is suitable for partitioned choice searching
 */
public class PartitionedChoiceAnalyser
{
//  private static final Logger LOGGER = LogManager.getLogger();

  private final ForwardDeadReckonPropnetStateMachine stateMachine;

  /**
   * Construct the analyser
   * @param xiMachine - state machine to analyze
   */
  public PartitionedChoiceAnalyser(ForwardDeadReckonPropnetStateMachine xiMachine)
  {
    stateMachine = xiMachine;
  }

  /**
   * Analyse the specified state machine to see if it is suitable for partitioned choice search,
   * and if it is generate the filter for that search partitioning
   *
   * @return suitable filter if one is found else null
   */
  public StateMachineFilter generatePartitionedChoiceFilter()
  {
    //  Currently only supported on puzzles
    if ( stateMachine.getRoles().size() > 1 )
    {
      return null;
    }

    Set<PolymorphicProposition> requiredPositiveBaseProps = new HashSet<>();
    Set<PolymorphicProposition> requiredNegativeBaseProps = new HashSet<>();
    if ( determineRequiredBasePropStatesForWin(requiredPositiveBaseProps, requiredNegativeBaseProps) )
    {
      if ( !requiredPositiveBaseProps.isEmpty())
      {
//        LOGGER.debug("Found required positive base props for win:");
//        for(PolymorphicProposition p : requiredPositiveBaseProps)
//        {
//          LOGGER.debug("\t" + p);
//        }
      }
      if ( !requiredNegativeBaseProps.isEmpty())
      {
//        LOGGER.debug("Found required negative base props for win");
//        for(PolymorphicProposition p : requiredNegativeBaseProps)
//        {
//          LOGGER.debug("\t" + p);
//        }
      }

      //  Are all the required base props latches in their required values?
      for(PolymorphicProposition p : requiredPositiveBaseProps)
      {
        if ( !stateMachine.isPositivelyLatchedBaseProp(p))
        {
          return null;
        }
      }
      for(PolymorphicProposition p : requiredNegativeBaseProps)
      {
        if ( !stateMachine.isNegativelyLatchedBaseProp(p))
        {
          return null;
        }
      }

//      LOGGER.debug("Required base props are all latches");

      //  Do the input props that directly modify each base prop latch
      //  form a partitioning of the set of all input props?
      Map<PolymorphicProposition, Set<PolymorphicProposition> > requiredBasesToInputsMap = new HashMap<>();

      for(PolymorphicProposition inputProp : stateMachine.getFullPropNet().getInputPropositions().values())
      {
        //  Currently we don't reliably trim always-illegal inputs, so until we do ignore them here
        if ( !stateMachine.getFullPropNet().getLegalInputMap().containsKey(inputProp))
        {
          continue;
        }

        Set<PolymorphicProposition> dependentBaseProps = new HashSet<>();

        recursiveFindDependentBaseProps(inputProp, dependentBaseProps);

        //  Check this dependent set has no overlap with other dependent sets
        boolean foundSet = false;

        for(PolymorphicProposition baseProp : dependentBaseProps)
        {
          if ( requiredNegativeBaseProps.contains(baseProp) || requiredPositiveBaseProps.contains(baseProp))
          {
            if ( foundSet )
            {
              //  This input impacts more than on of the required base props and
              //  so we do not have an equivalence relation
              return null;
            }

            foundSet = true;

            Set<PolymorphicProposition> inputSet = requiredBasesToInputsMap.get(baseProp);
            if ( inputSet == null )
            {
              inputSet = new HashSet<>();
              requiredBasesToInputsMap.put(baseProp, inputSet);
            }

            inputSet.add(inputProp);
          }
        }

        if ( !foundSet )
        {
          //  Input that is not in any partition - doesn't match the analysis pattern we are looking for
          return null;
        }
      }

//      LOGGER.debug("Associated input props form an equivalence relation");

      //  Form a mapping of input props to move infos
      Map<PolymorphicProposition, ForwardDeadReckonLegalMoveInfo> inputToLegalInfoMap = new HashMap<>();
      for(ForwardDeadReckonLegalMoveInfo moveInfo : stateMachine.getFullPropNet().getMasterMoveList())
      {
        if ( moveInfo.mInputProposition != null )
        {
          inputToLegalInfoMap.put(moveInfo.mInputProposition, moveInfo);
        }
      }

      //  Conditions necessary for a partitioned choice filetr are met - crate it
      PartitionedChoiceStateMachineFilter filter = new PartitionedChoiceStateMachineFilter(stateMachine);

//      LOGGER.info("Using choice partition filter for search");

      //  Form the sets of legals for each required positive base prop and add them as partitions to the filter
      for(PolymorphicProposition p : requiredPositiveBaseProps)
      {
        ForwardDeadReckonLegalMoveSet partitionMoves = new ForwardDeadReckonLegalMoveSet(stateMachine.getFullPropNet().getActiveLegalProps(0));

        for(PolymorphicProposition inputProp : requiredBasesToInputsMap.get(p))
        {
          ForwardDeadReckonLegalMoveInfo moveInfo = inputToLegalInfoMap.get(inputProp);
          assert(moveInfo != null);

          partitionMoves.add(moveInfo);
        }
        filter.addPartition(partitionMoves, null, ((ForwardDeadReckonProposition)p).getInfo());
      }

      //  Form the sets of legals for each required negative base prop and add them as partitions to the filter
      for(PolymorphicProposition p : requiredNegativeBaseProps)
      {
        ForwardDeadReckonLegalMoveSet partitionMoves = new ForwardDeadReckonLegalMoveSet(stateMachine.getFullPropNet().getActiveLegalProps(0));

        for(PolymorphicProposition inputProp : requiredBasesToInputsMap.get(p))
        {
          ForwardDeadReckonLegalMoveInfo moveInfo = inputToLegalInfoMap.get(inputProp);
          assert(moveInfo != null);

          partitionMoves.add(moveInfo);
        }
        filter.addPartition(partitionMoves, ((ForwardDeadReckonProposition)p).getInfo(), null);
      }

      return filter;
    }

    return null;
  }

  /**
   * Determine if there is a common set of base proposition values required for all
   * winning states (winning defined as scoring the maximum achievable goal)
   * @param positives set to fill in with base props that must be TRUE
   * @param negatives set to fill in with base props that must be FALSE
   * @return true if such a set was identified
   */
  private boolean determineRequiredBasePropStatesForWin(Set<PolymorphicProposition> positives, Set<PolymorphicProposition> negatives)
  {
    int maxScore = Integer.MIN_VALUE;
    PolymorphicProposition winningGoalProp = null;

    for(PolymorphicProposition goalProp : stateMachine.getFullPropNet().getGoalPropositions().get(stateMachine.getRoles().get(0)))
    {
      int goalValue = Integer.parseInt(goalProp.getName().getBody().get(1).toString());
      if ( goalValue > maxScore )
      {
        maxScore = goalValue;
        winningGoalProp = goalProp;
      }
    }

    assert(winningGoalProp!=null);
    return determineRequiredBasePropStatesForProp(winningGoalProp.getSingleInput(), positives, negatives);
  }

  /**
   * Determine if there is a common set of base proposition values required for
   * a specified proposition to be true
   * @param p - proposition that we are seeking support for
   * @param positives - set to fill in with base props that must be TRUE
   * @param negatives - set to fill in with base props that must be FALSE
   * @return true if such a set was identified
   */
  private boolean determineRequiredBasePropStatesForProp(PolymorphicComponent c, Set<PolymorphicProposition> positives, Set<PolymorphicProposition> negatives)
  {
    return recursiveDetermineRequiredBasePropStatesForProp(c, true, positives, negatives);
  }

  /**
   * Determine if there is a common set of base proposition values required for
   * a specified proposition to have a specified value
   * @param p - proposition that we are seeking support for
   * @param requiredValue - value of p that must be supported
   * @param positives set to fill in with base props that must be TRUE
   * @param negatives set to fill in with base props that must be FALSE
   * @return true if such a set was identified
   */
  private boolean recursiveDetermineRequiredBasePropStatesForProp(PolymorphicComponent c, boolean requiredValue, Set<PolymorphicProposition> positives, Set<PolymorphicProposition> negatives)
  {
    if ( c instanceof PolymorphicProposition )
    {
      if ( stateMachine.getFullPropNet().getBasePropositions().values().contains(c))
      {
        if ( requiredValue && !negatives.contains(c) )
        {
          positives.add((PolymorphicProposition)c);
          return true;
        }
        else if ( !requiredValue && !positives.contains(c) )
        {
          negatives.add((PolymorphicProposition)c);
          return true;
        }
      }
      //  Arriving at a dependency on a proposition other than a base prop is
      //  unexpected and we will treat it as unsuccessful supporting set identification
    }
    else if ( c instanceof PolymorphicNot )
    {
      return recursiveDetermineRequiredBasePropStatesForProp( c.getSingleInput(), !requiredValue, positives, negatives );
    }
    else if ( c instanceof PolymorphicOr )
    {
      //  We don't cope with cases that are not prescriptive, so alternatives do not
      //  identify a supporting set
      if ( requiredValue )
      {
        return false;
      }
      for(PolymorphicComponent input : c.getInputs())
      {
        if ( !recursiveDetermineRequiredBasePropStatesForProp( input, false, positives, negatives) )
        {
          return false;
        }
      }

      return true;
    }
    else if ( c instanceof PolymorphicAnd )
    {
      //  We don't cope with cases that are not prescriptive, so alternatives do not
      //  identify a supporting set
      if ( !requiredValue )
      {
        return false;
      }
      for(PolymorphicComponent input : c.getInputs())
      {
        if ( !recursiveDetermineRequiredBasePropStatesForProp( input, true, positives, negatives) )
        {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  /**
   * Find base props that are directly dependent on a specified component (i.e. - not via other base props)
   * @param c - source component to find dependents of
   * @param dependentBaseProps - set of dependents to add to
   */
  private void recursiveFindDependentBaseProps(PolymorphicComponent c, Set<PolymorphicProposition> dependentBaseProps)
  {
    if ( c instanceof PolymorphicTransition )
    {
      dependentBaseProps.add((PolymorphicProposition)c.getSingleOutput());
    }
    else
    {
      for(PolymorphicComponent output : c.getOutputs())
      {
        recursiveFindDependentBaseProps(output, dependentBaseProps);
      }
    }
  }
}
