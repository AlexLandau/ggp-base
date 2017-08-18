package org.ggp.base.util.propnet.sancho;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;

/**
 * @author steve
 * Fast animator for the propNet.  This class builds a highly optimized tabular representation
 * of the propNet and animates it.  This provides a high degree of memory locality of reference
 * as well as minimal code overheads at simulation-time
 */
public class ForwardDeadReckonPropnetFastAnimator
{
//  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * @author steve
   * Opaque class that is not interpreted externally but must be retrieved and passed back
   * to the fast animator in some of its APIs.  It holds state for an instance
   */
  public class InstanceInfo
  {
    /**
     * Construct a new instance state structure
     *
     * @param numComponents Number of components comprising the fast animator
     * representation of the network
     */
    InstanceInfo(int numComponents)
    {
      state = new int[numComponents];
    }

    /**
     * Process toggling a component (output) to true
     * @param componentIdFull
     */
    void propagateComponentTrue(int componentIdFull)
    {
      int   outputIndex = ((componentIdFull & 0xFFFFFF)<<2)+1;
      int   numOutputs = componentDataTable[outputIndex];

      outputIndex += numOutputs;

      //  Test componentIdOutputUniversalLogicBits - this is the sign bit (specifically)
      //  which is faster to test with a direct sign test
      if (componentIdFull >= 0)
      {
        while(numOutputs-- > 0)
        {
          int triggerIndex = componentDataTable[outputIndex--];

          if ( triggerIndex < 0 )
          {
            legalMoveNotifier.add(triggerIndex & 0x7FFFFFFF);
          }
          else
          {
            propositionTransitionNotifier.add(triggerIndex);
          }
        }
      }
      else
      {
        assert(numOutputs>0);
        while(numOutputs-- > 0)
        {
          int outputFullId = componentDataTable[outputIndex--];

          int stateVal = ++state[(outputFullId & 0xFFFFFF)];
          if ( stateVal == 0 )
          {
            propagateComponentFalse(outputFullId);
          }
          else if ( stateVal == 0x80000000 )
          {
            propagateComponentTrue(outputFullId);
          }
        }
      }
    }

    /**
     * Process toggling a component (output) to false
     * @param componentIdFull
     */
    void propagateComponentFalse(int componentIdFull)
    {
      int   outputIndex = ((componentIdFull & 0xFFFFFF)<<2)+1;
      int   numOutputs = componentDataTable[outputIndex];

      outputIndex += numOutputs;

      //  Test componentIdOutputUniversalLogicBits - this is the sign bit (specifically)
      //  which is faster to test with a direct sign test
      if (componentIdFull >= 0)
      {
        while(numOutputs-- > 0)
        {
          int triggerIndex = componentDataTable[outputIndex--];

          if ( triggerIndex < 0 )
          {
            legalMoveNotifier.remove(triggerIndex & 0x7FFFFFFF);
          }
          else
          {
            propositionTransitionNotifier.remove(triggerIndex);
          }
        }
      }
      else
      {
        assert(numOutputs>0);
        while(numOutputs-- > 0)
        {
          int outputFullId = componentDataTable[outputIndex--];

          int stateVal = state[(outputFullId & 0xFFFFFF)]--;
          if ( stateVal == 0 )
          {
            propagateComponentTrue(outputFullId);
          }
          else if ( stateVal == 0x80000000 )
          {
            propagateComponentFalse(outputFullId);
          }
        }
      }
    }

    /**
     * Process toggling of a component state during reset processing
     * @param componentId
     * @param value
     */
    void propagateComponent(int componentId, boolean value)
    {
      if ( resetWatermark >= (componentId & 0xFFFFFF) )
      {
        int   outputIndex = ((componentId & 0xFFFFFF)<<2)+1;
        int   numOutputs = componentDataTable[outputIndex++];
        int outputIdBits;
        boolean isTrigger = (componentId >= 0);//(componentId & componentIdOutputUniversalLogicBits) == 0;

        while(numOutputs-- > 0)
        {
          int outputFullId = componentDataTable[outputIndex+numOutputs];

          if ( !isTrigger )
          {
            int outputId = (outputFullId & 0xFFFFFF);
            outputIdBits = outputFullId & componentIdTypeMask;

            switch(outputIdBits)
            {
              case componentIdPropositionBits:
                if ( value )
                {
                  state[outputId] |= componentStateCachedValMask;
                }
                else
                {
                  state[outputId] &= ~componentStateCachedValMask;
                }

                propagateComponent(outputFullId, value);
                break;
              case componentIdTransitionBits:
                assert(false);
                break;
              case componentIdUniversalLogicBits:
                int stateVal;
                if ( value )
                {
                  stateVal = ++state[outputId];
                  if ( stateVal == 0x80000000 )
                  {
                    propagateComponent( outputFullId, true);
                  }
                  else if ( stateVal == 0 )
                  {
                    propagateComponent(outputFullId, false);
                  }
                }
                else
                {
                  stateVal = --state[outputId];
                  if ( stateVal == 0x7FFFFFFF )
                  {
                    propagateComponent(outputFullId, false);
                  }
                  else if ( stateVal == 0xFFFFFFFF )
                  {
                    propagateComponent(outputFullId, true);
                  }
                }
                break;
              default:
                //  Should not happen
                throw new UnsupportedOperationException("Unexpected component type");
            }
          }
          else
          {
            int moveIndex = outputFullId;

            if ( value )
            {
              if ( moveIndex < 0 )
              {
                legalMoveNotifier.add(moveIndex & 0x7FFFFFFF);
              }
              else
              {
                propositionTransitionNotifier.add(moveIndex);
              }
            }
            else
            {
              if ( moveIndex < 0 )
              {
                legalMoveNotifier.remove(moveIndex & 0x7FFFFFFF);
              }
              else
              {
                propositionTransitionNotifier.remove(moveIndex);
              }
            }
          }
        }
      }
    }

    /**
     * Change the value of an input proposition, asserting that the new value IS a change
     * @param propId        component whose value is being changed
     * @param value         new value - this MUST be !(old value) but this is NOT checked
     */
    public void changeComponentValueTo(int propId, boolean value)
    {
      if ( value )
      {
        propagateComponentTrue(propId);
      }
      else
      {
        propagateComponentFalse(propId);
      }
    }

    /**
     * Vector of state values for each component indexed by component id
     */
    final int[]                                   state;
    /**
     * Interface to call for changes to the output state of legal move props
     */
    ForwardDeadReckonLegalMoveSet                 legalMoveNotifier;
    /**
     * Interface to call for changes to the output state of transitions
     */
    ForwardDeadReckonInternalMachineState         propositionTransitionNotifier;
    /**
     * Holds the current component id up to which an in-progress reset() has processed
     */
    int                                           resetWatermark;
  }

  private class ComponentIdComparator implements Comparator<Integer>{

    @Override
    public int compare(Integer o1, Integer o2) {
      return (o1 & 0xFFFFFF) - (o2 & 0xFFFFFF);
    }
  }
  private final ComponentIdComparator     componentIdComparator = new ComponentIdComparator();
  private final ForwardDeadReckonPropNet  propNet;
  private int                             nextComponentBaseId = 0;
  private final int                       numStatefulComponents;
  /**
   * The data for each component is stored in a contiguous set of integers in
   * the following form:
   * METADATA (component type, output inversion, input count)
   * NUM OUTPUTS
   * ids of output components (for logic) or of triggers (for legal move/transition triggers) (NUM OUTPUTS entries)
   * Array containing the ids of outputs to each component, packed consecutively.  The offset for
   * the first entry for each component is 4*its id
   */
  final int[]                             componentDataTable;

  private static final int                componentMetaDataTypeMask = 0x07000000; //  3-bits for component type
  private static final int                componentMetaDataTypeShift = 24;
  private static final int                componentMetaDataOutputInverted = 0x08000000; //  Flag to indicate output is inverted (only applies to AND/OR)
  private static final int                componentMetaDataOutputUniversalLogic = 0x10000000; //  Flag to indicate output is more logic (not trigger)
  private static final int                componentMetaDataInputCountMask = 0xFFFF; // Num inputs mask (16 bits)

  //  Component types
  private static final int                componentTypeNonTriggeringProposition = 7;
  private static final int                componentTypeProposition = 1;
  private static final int                componentTypeTransition = 0;
  private static final int                componentTypeOr = 2;
  //  Internally in the runtime representation AND/OR/NAND/NOR and NOT are all combined
  //  in a single universal logic component.  Because it doesn't exist in the same representations
  //  as the basic logic components it can use an id that overlaps with them, so we can keep types down to 8
  private static final int                componentTypeUniversalLogic = 2;
  private static final int                componentTypeAnd = 3;
  private static final int                componentTypeNot = 4;
  private static final int                componentTypeTrueConstant = 5;
  private static final int                componentTypeFalseConstant = 6;
  private static final int                componentTypePropositionBits = (componentTypeProposition << componentMetaDataTypeShift);
  private static final int                componentTypeTransitionBits = (componentTypeTransition << componentMetaDataTypeShift);
  private static final int                componentTypeOrBits = (componentTypeOr << componentMetaDataTypeShift);
  private static final int                componentTypeAndBits = (componentTypeAnd << componentMetaDataTypeShift);
  private static final int                componentTypeNotBits = (componentTypeNot << componentMetaDataTypeShift);
  private static final int                componentTypeTrueConstantBits = (componentTypeTrueConstant << componentMetaDataTypeShift);
  private static final int                componentTypeFalseConstantBits = (componentTypeFalseConstant << componentMetaDataTypeShift);
  private static final int                componentTypeNonTriggeringPropositionBits = (componentTypeNonTriggeringProposition << componentMetaDataTypeShift);

  //  We us the top byte of the component id to encode some commonly required
  //  features so as to avoid the need for a lookup
  private static final int                componentIdTypeMask = 0x07000000;
  private static final int                componentIdTypeShift = 24;
  private static final int                componentIdPropositionBits = (componentTypeProposition << componentIdTypeShift);
  private static final int                componentIdTransitionBits = (componentTypeTransition << componentIdTypeShift);
  private static final int                componentIdUniversalLogicBits = (componentTypeUniversalLogic << componentIdTypeShift);
  private static final int                componentIdOutputInvertedFlag = (1<<29);
  private static final int                componentIdOutSingleTrigger = (1<<30);
  private static final int                componentIdOutputUniversalLogicBits = (1 << 31);
  private static final int                componentIdDeferredAsignmentBits = (1 << 28);

  // states will be represented by per-thread int arrays, with each int packed as follows
  private static final int                componentStateCachedValMask = 1<<31; // Current state
  // Remaining bits are free for opaque use by the implementation.  For triggers (props with associated
  // legals or transitions) they are unused.  For the universal logic element it is a count which is
  // incremented on an input transitioning to TRUE, and decremented for a transition to FALSE.  These
  // increments/decrements are deliberately allowed to overflow into the sign bit, which is the state mask
  // This allows a single inc/dec to operate the components as any of OR/AND/NOR/NAND depending on how
  // reset initializes the value

  //  Reserved id used to denote a component whose id has not yet been set
  private static final int notSetComponentId = -2;
  /**
   * Reserved id used to denote a component which has been subsumed into another
   * component for fast propagation, or which has no connected output
   */
  public static final int notNeededComponentId = -1;

  private int             nextDeferredComponentId = 0;

  // Each instance (which typically maps to a thread) has its own InstanceInfo to hold the complete state
  // of that instance's propNet
  private InstanceInfo[]           instances;

  private class PassThroughComponentInfo
  {
    public PassThroughComponentInfo()
    {
      // TODO Auto-generated constructor stub
    }
    int                       addedOrId;
    Set<PolymorphicComponent> components;
  }

  /**
   * Constructs a new fast animator for the given network.  The network must not be changed
   * after this call is made or the generated fast animator will no longer be valid
   *
   * @param thePropNet Network to construct an animator for
   */
  public ForwardDeadReckonPropnetFastAnimator(ForwardDeadReckonPropNet thePropNet)
  {

    propNet = thePropNet;

    int componentType = 0;

    //  Initialize component values to a distinguished 'not set' value
    for(PolymorphicComponent c : thePropNet.getComponents())
    {
      boolean isBaseProp = false;
      boolean isLegalProp = false;
      boolean isTerminalProp = false;
      boolean isGoalProp = false;
      boolean isInitProp = false;

      if ( c instanceof PolymorphicProposition )
      {
        GdlConstant propTypeName = ((PolymorphicProposition)c).getName().getName();
        isBaseProp = propTypeName.equals(GdlPool.TRUE);
        isLegalProp = propTypeName.equals(GdlPool.LEGAL);
        isTerminalProp = propTypeName.equals(GdlPool.TERMINAL);
        isGoalProp = propTypeName.equals(GdlPool.GOAL);
        isInitProp = propTypeName.equals(GdlPool.INIT);
      }
      if ( (c instanceof PolymorphicTransition) ||
           isLegalProp ||
           isTerminalProp ||
           isGoalProp ||
           (c.getOutputs().isEmpty() && (c.getInputs().isEmpty() ||
                                         //(isLegalProp && c.getInputs().size() == 1 && c.getSingleInput() instanceof PolymorphicConstant)) ||
                                         isBaseProp ||
                                         isInitProp)) )
      {
        ((ForwardDeadReckonComponent)c).id = notNeededComponentId;
      }
      else
      {
        ((ForwardDeadReckonComponent)c).id = notSetComponentId;
      }
    }

    //  Dummy components are (virtually) added to ensure that all outputs from any
    //  particular component are either all universal logic elements or are triggers.
    //  In either case this allows the propagation methods to assume that all outputs are
    //  of  matching type, and loop through them without type-checks.
    //  Where the original (PolymorphicComponent) network has a component with outputs of
    //  mixed type, we add a dummy 1-input OR buffer to separate the triggers from the
    //  logic
    Map<PolymorphicComponent, PassThroughComponentInfo> addedPassThroughComponents = new HashMap<>();

    //  Number components and set up metadata flags stored in their high id bits
    //  In order to make some attempt to improve CPU predictability of stride
    //  in the primary loop (which iterates over outputs of a component) we number
    //  so as to encourage generation of consecutive ids for outputs.
    //  Starting from the base propositions deal with their descendant networks
    //  first recursively, numbering components as they are encountered if not
    //  already numbered
    for(PolymorphicComponent c : propNet.getBasePropositions().values())
    {
      ForwardDeadReckonComponent fdrc = (ForwardDeadReckonComponent)c;

      if ( fdrc.id == notSetComponentId )
      {
        setComponentId(c, addedPassThroughComponents, true);
        recursivelySetComponentOutputIds(c, addedPassThroughComponents);
      }
    }

    //  Similarly the does props
    for(PolymorphicComponent c : propNet.getInputPropositions().values())
    {
      ForwardDeadReckonComponent fdrc = (ForwardDeadReckonComponent)c;

      if ( fdrc.id == notSetComponentId )
      {
        setComponentId(c, addedPassThroughComponents, true);
        recursivelySetComponentOutputIds(c, addedPassThroughComponents);
      }
    }

    //  Any remaining non-deferred components
    for(PolymorphicComponent c : thePropNet.getComponents())
    {
      ForwardDeadReckonComponent fdrc = (ForwardDeadReckonComponent)c;

      if ( fdrc.id == notSetComponentId )
      {
        setComponentId(c, addedPassThroughComponents, false);
      }
    }

    //  Remaining components (whose id allocation we have deferred) do not need to retain state in the instance
    //  state table.  These are the does props and the base props, which are all inputs to the network rather
    //  than really part of its state
    numStatefulComponents = nextComponentBaseId;
//    LOGGER.info("Animator instance state vector size: " + numStatefulComponents);

    componentDataTable = new int[(nextComponentBaseId+nextDeferredComponentId)*4];

    for(PolymorphicComponent c : thePropNet.getComponents())
    {
      ForwardDeadReckonComponent fdrc = (ForwardDeadReckonComponent)c;

      if ( fdrc.id == notNeededComponentId )
      {
        //  Redundant NOT which has been subsumed into a NAND/NOR
        continue;
      }

      int id;
      if ( (fdrc.id & componentIdDeferredAsignmentBits) != 0 )
      {
        //  Allocate now
        id = nextComponentBaseId;
      }
      else
      {
        id = fdrc.id & 0xFFFFFF;
      }
      boolean outputInverted = ((fdrc.id & componentIdOutputInvertedFlag) != 0);

      Collection<? extends PolymorphicComponent> outputs = c.getOutputs();
      Collection<? extends PolymorphicComponent> inputs = c.getInputs();
      if ( c instanceof PolymorphicProposition )
      {
        int moveIndex = ((ForwardDeadReckonProposition)c).getAssociatedTriggerIndex();
        if ( moveIndex < 0 )
        {
          ForwardDeadReckonPropositionInfo info = ((ForwardDeadReckonProposition)c).getInfo();
          if ( info != null && info.index >= thePropNet.getBasePropositionsArray().length )
          {
            componentType = componentTypeProposition;
          }
          else
          {
            componentType = componentTypeNonTriggeringProposition;
            assert(c.getInputs().isEmpty() ||
                   thePropNet.getBasePropositions().values().contains(c) ||
                   (c.getInputs().size()==1 && c.getSingleInput() instanceof PolymorphicConstant));
          }
        }
        else
        {
          componentType = componentTypeProposition;
        }
      }
      else if ( c instanceof PolymorphicTransition )
      {
        componentType = componentTypeTransition;
      }
      else if ( c instanceof PolymorphicOr )
      {
        componentType = componentTypeOr;
      }
      else if ( c instanceof PolymorphicAnd )
      {
        componentType = componentTypeAnd;
      }
      else if ( c instanceof PolymorphicNot )
      {
        componentType = componentTypeNot;
      }
      else if ( c instanceof PolymorphicConstant )
      {
        if ( c.getValue() )
        {
          componentType = componentTypeTrueConstant;
        }
        else
        {
          componentType = componentTypeFalseConstant;
        }
      }

      //  If we subsumed a NOT into a universal logic component then it's that NOT's
      //  outputs that are the outputs of the universal component
      if ( outputInverted )
      {
        outputs = c.getSingleOutput().getOutputs();
      }

      int outputTypeBits = 0;
      boolean outTypeSet = false;
      boolean outTypeClash = false;

      for(PolymorphicComponent output : outputs)
      {
        if ( !outTypeSet )
        {
          outTypeSet = true;
          if ( (output instanceof PolymorphicOr) || (output instanceof PolymorphicAnd) || (output instanceof PolymorphicNot) )
          {
            outputTypeBits = componentIdOutputUniversalLogicBits;
          }
          else if ( output instanceof PolymorphicProposition )
          {
            if ( getTriggerId(output) == -1 )
            {
              outputTypeBits = componentIdOutputUniversalLogicBits;
            }
          }
        }
        else
        {
          int thisOutputTypeBits = 0;
          if ( (output instanceof PolymorphicOr) || (output instanceof PolymorphicAnd) || (output instanceof PolymorphicNot) )
          {
            thisOutputTypeBits = componentIdOutputUniversalLogicBits;
          }
          else if ( output instanceof PolymorphicProposition )
          {
            if ( getTriggerId(output) == -1 )
            {
              thisOutputTypeBits = componentIdOutputUniversalLogicBits;
            }
          }

          if ( thisOutputTypeBits == componentIdOutputUniversalLogicBits )
          {
            if ( outputTypeBits != componentIdOutputUniversalLogicBits)
            {
              outputTypeBits = componentIdOutputUniversalLogicBits;
              outTypeClash = true;
              break;
            }
          }
          else if ( outputTypeBits != 0 )
          {
            outTypeClash = true;
            break;
          }
        }
      }

      int numOutputs = outputs.size();
      int dummyOrId = -1;

      final Integer[] sortingBuffer = new Integer[65536];

      if ( outTypeClash )
      {
        //  Insert pass-through 1-input OR
        PassThroughComponentInfo movedTriggers = addedPassThroughComponents.get(c);

        dummyOrId = movedTriggers.addedOrId;

        int dummyComponentIndex = dummyOrId*4;

        int typeInfo = (1 | componentTypeOrBits);  // 1-input OR;
        if ( movedTriggers.components.size() == 1 )
        {
          typeInfo |= componentIdOutSingleTrigger;
        }
        componentDataTable[dummyComponentIndex++] = typeInfo;

        int numOutputsIndex = dummyComponentIndex;
        componentDataTable[dummyComponentIndex++] = movedTriggers.components.size();       // Number of outputs

        for(PolymorphicComponent output : movedTriggers.components)
        {
          int triggerId = getTriggerId(output);

          assert(outputs.contains(output));

          componentDataTable[dummyComponentIndex++] = triggerId;
        }

        sortOutputs(sortingBuffer, numOutputsIndex);

        //  Original component now outputs to the remaining subset of its original
        //  outputs plus the dummy OR
        numOutputs -= (movedTriggers.components.size()-1);
      }

      if (numOutputs == 0 )
      {
        outputTypeBits |= componentIdOutputUniversalLogicBits;
      }

      int index = id*4;
      componentDataTable[index++] = (componentType << componentMetaDataTypeShift) |
                                 (outputInverted ? componentMetaDataOutputInverted : 0) |
                                 (((outputTypeBits & componentIdOutputUniversalLogicBits) != 0) ? componentMetaDataOutputUniversalLogic : 0) |
                                 inputs.size();
      int numOutputsIndex = index;
      componentDataTable[index++] = numOutputs;
      assert(numOutputs>0);

      for(PolymorphicComponent output : outputs)
      {
        int triggerId = getTriggerId(output);
        if ( !outTypeClash ||
            triggerId == -1 )
        {
          if ( triggerId != -1 )
          {
            assert((outputTypeBits & componentIdOutputUniversalLogicBits) == 0);
            componentDataTable[index++] = triggerId;
          }
          else
          {
            if ( ((ForwardDeadReckonComponent)output).id == notNeededComponentId )
            {
              componentDataTable[numOutputsIndex]--;
              numOutputs--;
              assert(componentDataTable[numOutputsIndex]>0 || c instanceof PolymorphicConstant);
              continue;
            }
            assert((outputTypeBits & componentIdOutputUniversalLogicBits) != 0);
            assert(((ForwardDeadReckonComponent)output).id != notSetComponentId);
            componentDataTable[index++] = ((ForwardDeadReckonComponent)output).id;
          }
        }
      }
      if ( dummyOrId != -1 )
      {
        //  Add the dummy OR to the original's output list
        componentDataTable[index++] = dummyOrId | (componentTypeUniversalLogic << 24);
      }

      sortOutputs(sortingBuffer, numOutputsIndex);

      if ( (fdrc.id & componentIdDeferredAsignmentBits) != 0 )
      {
        assert(c instanceof PolymorphicProposition);
        assert((fdrc.id & 0xFFFFFF) < nextDeferredComponentId);
        fdrc.id = (fdrc.id & ~0xFFFFFF) | ((id & 0xFFFFFF));

        nextComponentBaseId += (5+numOutputs)/4;
      }
    }

    assert(noConstantBasePropsPropagatable());
  }

  //  Sort outputs of a component into component index order to improve cache
  //  access patterns when propagating
  private void sortOutputs(Integer[] sortingBuffer, int numOutputsIndex)
  {
    if ( componentDataTable[numOutputsIndex] > 1 )
    {
      for(int i = 0; i < componentDataTable[numOutputsIndex]; i++)
      {
        sortingBuffer[i] = componentDataTable[numOutputsIndex+i+1];
      }
      Arrays.sort(sortingBuffer,0,componentDataTable[numOutputsIndex], componentIdComparator);
      for(int i = 0; i < componentDataTable[numOutputsIndex]; i++)
      {
        componentDataTable[numOutputsIndex+i+1] = sortingBuffer[i];
      }
    }
  }

  //  Purely debugging routine to assert some conditions that the factory optimizer should have
  //  eliminated
  private boolean noConstantBasePropsPropagatable()
  {
    for(PolymorphicProposition c : propNet.getBasePropositionsArray())
    {
      //  Base props whose state can never change should have their ids set to unused so
      //  that external changes to them are not attempted.  We have to do this here (after
      //  processing the component) because animator resets still need to propagate their
      //  (fixed) value through the network, so they must have been allocated slots in the
      //  connectivity tables
      if ( c.getInputs().size() == 1 && c.getSingleInput() instanceof PolymorphicConstant )
      {
        if (((ForwardDeadReckonProposition)c).id != notNeededComponentId)
        {
          return false;
        }
      }
    }

    return true;
  }

  private int getTriggerId(PolymorphicComponent c)
  {
    if ( c instanceof PolymorphicProposition )
    {
      if ( ((ForwardDeadReckonProposition)c).getName().getName().equals(GdlPool.LEGAL))
      {
        int moveIndex = ((ForwardDeadReckonProposition)c).getAssociatedTriggerIndex();
        if( moveIndex >= 0 )
        {
          return moveIndex | 0x80000000;
        }
      }
      else
      {
        ForwardDeadReckonPropositionInfo info = ((ForwardDeadReckonProposition)c).getInfo();
        if ( info != null && info.index < propNet.getActiveBaseProps(0).infoSet.length - propNet.getBasePropositionsArray().length )
        {
          assert(info.index < propNet.getActiveBaseProps(0).contents.capacity());
          return info.index;
        }
      }
    }
    else if ( c instanceof PolymorphicTransition )
    {
      int result = ((ForwardDeadReckonTransition)c).getAssociatedPropositionIndex();

      assert(result >= propNet.getActiveBaseProps(0).infoSet.length - propNet.getBasePropositionsArray().length);
      return result;
    }

    return -1;
  }

  /**
   * Set the components ids of the sub-network rooted in a specified component, stopping at
   * transitions, in such a way as to increase the probability of consecutive numbering of outputs of each
   * component (which helps cache locality and stride predictability)
   * @param c root component to start from (must already have had its id set)
   * @param addedPassThroughComponents set of any pseudo-components added during numbering
    */
  private void recursivelySetComponentOutputIds(PolymorphicComponent c, Map<PolymorphicComponent, PassThroughComponentInfo> addedPassThroughComponents)
  {
    assert(((ForwardDeadReckonComponent)c).id != notSetComponentId);

    Set<PolymorphicComponent> processedOutputs = new HashSet<>();

    for(PolymorphicComponent output : c.getOutputs())
    {
      ForwardDeadReckonComponent fdrc = (ForwardDeadReckonComponent)output;
      if ( fdrc.id == notSetComponentId )
      {
        setComponentId(output, addedPassThroughComponents, false);
        processedOutputs.add(output);
      }
    }

    for(PolymorphicComponent output : processedOutputs)
    {
      if ( !(output instanceof PolymorphicTransition))
      {
        recursivelySetComponentOutputIds(output, addedPassThroughComponents);
      }
    }
  }

  /**
   * Set the component id of a specified component
   * @param c  component to set the id of
   * @param addedPassThroughComponents set of any pseudo-components added during numbering
   */
  private void setComponentId(PolymorphicComponent c, Map<PolymorphicComponent, PassThroughComponentInfo> addedPassThroughComponents, boolean deferFinalIdChoice)
  {
    ForwardDeadReckonComponent fdrc = (ForwardDeadReckonComponent)c;
    Collection<? extends PolymorphicComponent> outputs = fdrc.getOutputs();
    boolean hasTrigger = false;
    boolean outputsInverted = false;
    int componentType = 0;

    if ( c instanceof PolymorphicProposition )
    {
      hasTrigger = (getTriggerId(c) != -1);

      if ( hasTrigger )
      {
        componentType = componentTypeProposition;
      }
      else
      {
        //  Remaining props should be either does props or base props, both of which
        //  are not reachable via propnet propagation and act only as inputs, or init
        //  props that (currently - we should trim there really) just have constant inputs
        assert(c.getInputs().isEmpty() || (c.getInputs().size() == 1 && c.getSingleInput() instanceof PolymorphicConstant) || propNet.getBasePropositions().values().contains(c));

        componentType = componentTypeUniversalLogic;
      }
    }
    else if ( c instanceof PolymorphicTransition )
    {
      componentType = componentTypeTransition;
      hasTrigger = (((ForwardDeadReckonTransition)c).getAssociatedPropositionIndex() != -1);
      //  Transitions should always have triggers (else what are they doing?)
      assert(hasTrigger);
    }
    else if ( (c instanceof PolymorphicOr) || (c instanceof PolymorphicAnd) )
    {
      componentType = componentTypeUniversalLogic;

      //  If it has a single output to a NOT roll the NOT into this component and a NAND/NOR
      if ( outputs.size() == 1 )
      {
        PolymorphicComponent output = c.getSingleOutput();

        if ( output instanceof PolymorphicNot )
        {
          outputs = output.getOutputs();
          outputsInverted = true;
        }
      }
    }
    else if ( c instanceof PolymorphicNot )
    {
      //  Check this isn't a single output of an AND/OR in which case the AND/OR will be processed as
      //  a NAND/NOR and this component need not be processed
      PolymorphicComponent input = c.getSingleInput();
      if ( ((input instanceof PolymorphicOr) || (input instanceof PolymorphicAnd)) &&
           input.getOutputs().size() == 1 )
      {
        fdrc.id = notNeededComponentId;
        return;
      }

      componentType = componentTypeUniversalLogic;
    }
    else if ( c instanceof PolymorphicConstant )
    {
      if ( c.getValue() )
      {
        componentType = componentTypeTrueConstant;
      }
      else
      {
        componentType = componentTypeFalseConstant;
      }
    }

    int outputTypeBits = 0;
    boolean outTypeSet = false;
    boolean outTypeClash = false;

    //  Check that all outputs are of the same type (either represented in our fast propagator
    //  tables as universal logic elements or as triggers, but not both)
    for(PolymorphicComponent output : outputs)
    {
      if ( !outTypeSet )
      {
        outTypeSet = true;
        if ( (output instanceof PolymorphicOr) || (output instanceof PolymorphicAnd) || (output instanceof PolymorphicNot))
        {
          outputTypeBits = componentIdOutputUniversalLogicBits;
        }
        else if ( output instanceof PolymorphicProposition )
        {
          if ( getTriggerId(output) == -1 )
          {
            outputTypeBits = componentIdOutputUniversalLogicBits;
          }
        }
      }
      else
      {
        int thisOutputBits = 0;

        if ( (output instanceof PolymorphicOr) || (output instanceof PolymorphicAnd) || (output instanceof PolymorphicNot))
        {
          thisOutputBits = componentIdOutputUniversalLogicBits;
        }
        else if ( output instanceof PolymorphicProposition )
        {
          if ( getTriggerId(output) == -1 )
          {
            thisOutputBits = componentIdOutputUniversalLogicBits;
          }
        }
        if ( thisOutputBits == componentIdOutputUniversalLogicBits )
        {
          if ( outputTypeBits != componentIdOutputUniversalLogicBits)
          {
            outputTypeBits = 0;
            outTypeClash = true;
            break;
          }
        }
        else if ( outputTypeBits != 0 )
        {
          outputTypeBits = 0;
          outTypeClash = true;
          break;
        }
      }
    }

    int numOutputs = outputs.size();

    if (numOutputs == 0 )
    {
      outputTypeBits |= componentIdOutputUniversalLogicBits;
    }

    if ( outTypeClash )
    {
      PassThroughComponentInfo passThroughInfo = new PassThroughComponentInfo();

      //  Insert a dummy pass-through (1-input OR) component to restore
      //  strict layering
      Set<PolymorphicComponent> movedTriggers = new HashSet<>();

      for(PolymorphicComponent output : outputs)
      {
        if ( getTriggerId(output) != -1 )
        {
          movedTriggers.add(output);
        }
      }

      passThroughInfo.addedOrId = nextComponentBaseId;
      passThroughInfo.components = movedTriggers;

      numOutputs -= (movedTriggers.size()-1);

      outputTypeBits = componentIdOutputUniversalLogicBits;

      nextComponentBaseId += ((movedTriggers.size()+5)/4);

      addedPassThroughComponents.put(c, passThroughInfo);
    }
    else if ( outputTypeBits == 0 && numOutputs == 1 )
    {
      outputTypeBits = componentIdOutSingleTrigger;
    }

    if ( deferFinalIdChoice )
    {
      assert(c instanceof PolymorphicProposition);
      fdrc.id = (nextDeferredComponentId) |
                (componentType << 24) |
                 outputTypeBits |
                (outputsInverted ? componentIdOutputInvertedFlag : 0) |
                componentIdDeferredAsignmentBits;

      nextDeferredComponentId += ((numOutputs+5)/4);
    }
    else
    {
      fdrc.id = (nextComponentBaseId) |
                (componentType << 24) |
                 outputTypeBits |
                (outputsInverted ? componentIdOutputInvertedFlag : 0);

      nextComponentBaseId += ((numOutputs+5)/4);
    }

    assert(nextComponentBaseId < 0x01000000);
  }

  /**
   * Crystalize the number of instances that will be required at simulation-time
   * This must be called before any simulation is attempted and may only be called once
   *
   * @param numInstances Number of separate thread-safe instances to support
   */
  public void crystalize(int numInstances)
  {
    instances = new InstanceInfo[numInstances];

    for(int i = 0; i < numInstances; i++)
    {
      instances[i] = new InstanceInfo(numStatefulComponents);
      instances[i].legalMoveNotifier = propNet.getActiveLegalProps(i);
      instances[i].propositionTransitionNotifier = propNet.getActiveBaseProps(i);
    }
  }

  /**
   * Retrieve the InstanceInfo for a given instance id
   * @param instanceId id to retrieve the state structure for
   * @return state structure for the specified instance
   */
  public InstanceInfo getInstanceInfo(int instanceId)
  {
    return instances[instanceId];
  }

  /**
   * Reset the network to its default state (all inputs to all
   * components assumed FALSE).  If a full reset is performed
   * then any components whose outputs are TRUE will be propagated
   * to arrive at a globally consistent state
   * @param instanceId id of the instance to reset
   * @param fullPropagate whether to propagate to full global consistency
   */
  public void reset(int instanceId, boolean fullPropagate)
  {
    InstanceInfo instanceInfo = instances[instanceId];
    int[] state = instanceInfo.state;
    int i = 0;

    while(i < numStatefulComponents)
    {
      int componentIndex = i*4;
      int stateEntryIndex = i;
      //  Retrieve the actual component type - note that we MUST retrieve this from
      //  the component info NOT its id, since the id representation uses the same
      //  code for AND and OR and encodes NAND and NOR as well
      int compMetaData = componentDataTable[componentIndex];
      int numOutputs = componentDataTable[componentIndex+1];
      int componentTypeBits = (compMetaData & componentMetaDataTypeMask);
      boolean outputInverted = ((compMetaData & componentMetaDataOutputInverted) != 0);

      i += ((numOutputs + 5)/4);

      switch(componentTypeBits)
      {
        case componentTypePropositionBits:
        case componentTypeTransitionBits:
        case componentTypeFalseConstantBits:
          state[stateEntryIndex] = 0;
          break;
        case componentTypeNonTriggeringPropositionBits:
        case componentTypeOrBits:
          if ( outputInverted )
          {
            state[stateEntryIndex] = 0xFFFFFFFF;
          }
          else
          {
            state[stateEntryIndex] = 0x7FFFFFFF;
          }
          break;
        case componentTypeAndBits:
          int   numInputs = (compMetaData & componentMetaDataInputCountMask);
          if ( outputInverted )
          {
            state[stateEntryIndex] = -numInputs;
          }
          else
          {
            state[stateEntryIndex] = 0x80000000 - numInputs;
          }
          break;
        case componentTypeNotBits:
          state[stateEntryIndex] = 0xFFFFFFFF;
          break;
        case componentTypeTrueConstantBits:
          state[stateEntryIndex] = componentStateCachedValMask;
          break;
        default:
          break;
      }
    }

    if ( fullPropagate )
    {
      i = 0;

      while(i < numStatefulComponents)
      {
        instanceInfo.resetWatermark = i;

        int componentIndex = i*4;
        int stateEntryIndex = i;
        //  Retrieve the actual component type - note that we MUST retrieve this from
        //  the component info NOT its id, since the id representation uses the same
        //  code for AND and OR and encodes NAND and NOR as well
        int compMetaData = componentDataTable[componentIndex];
        int numOutputs = componentDataTable[componentIndex+1];

        if ( numOutputs > 0 )
        {
          boolean current = ((state[stateEntryIndex] & componentStateCachedValMask) != 0);
          int type = ((compMetaData & componentMetaDataTypeMask) >> componentMetaDataTypeShift);

          if ( current && type != componentTypeTransition )
          {
            int fullId = instanceInfo.resetWatermark;
            if ( (compMetaData & componentMetaDataOutputUniversalLogic) != 0 )
            {
              fullId |= componentIdOutputUniversalLogicBits;
            }

            instanceInfo.propagateComponent(fullId, current);
          }
        }

        i += ((numOutputs + 5)/4);
      }
    }
    else
    {
      instanceInfo.resetWatermark = nextComponentBaseId;
    }
  }

}
