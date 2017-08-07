
package org.ggp.base.util.propnet.sancho;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.sancho.OptimizingPolymorphicPropNetFactory;
import org.ggp.base.util.statemachine.Role;


/**
 * The PolymorphicPropNet class is an instantiation vehicle for propnets using
 * polymorphic components. It constructs itself from a provided input propnet
 * (either another polymorphic one, or a basic one of class PropNet) by copying
 * the topology onto a component set created by the provided component factory,
 * preserving ordering of inputs and outputs subject to the components
 * concerned guaranteeing a meaningful enumeration order of those collections
 * and add adding to the end (if they care about order)
 */

public class PolymorphicPropNet
{
  private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

  /** References to every component in the PropNet. */
  private final Set<PolymorphicComponent>                           components;

  /** References to every Proposition in the PropNet. */
  private final Set<PolymorphicProposition>                         propositions;

  /** References to every BaseProposition in the PropNet, indexed by name. */
  private final Map<GdlSentence, PolymorphicProposition>            basePropositions;
  private PolymorphicProposition[]                                  basePropositionsArray;

  /** References to every InputProposition in the PropNet, indexed by name. */
  private final Map<GdlSentence, PolymorphicProposition>            inputPropositions;

  /** References to every LegalProposition in the PropNet, indexed by role. */
  private final Map<Role, Set<PolymorphicProposition>>              legalPropositionsMutable;
  private Map<Role, PolymorphicProposition[]>                       legalPropositions;

  /** References to every GoalProposition in the PropNet, indexed by role. */
  private Map<Role, Set<PolymorphicProposition>>                    goalPropositionsMutable;
  private Map<Role, PolymorphicProposition[]>                       goalPropositions;

  /** A reference to the single, unique, InitProposition. */
  private PolymorphicProposition                                    initProposition;

  /** A reference to the single, unique, TerminalProposition. */
  private final PolymorphicProposition                              terminalProposition;

  /** A helper mapping between input/legal propositions. */
  private final Map<PolymorphicProposition, PolymorphicProposition> legalInputMap;

  /** A helper list of all of the roles. */
  private final Role[]                                              roles;

  private PolymorphicComponentFactory                               componentFactory;

  /**
   * Creates a new PropNet from a list of Components, along with indices over
   * those components.
   * @param theRoles
   *          Roles of the game this propnet implements a state machine for
   * @param theComponents
   *          A list of Components.
   * @param theComponentFactory
   *          Factory suitable for producing new components in this propNet
   */
  public PolymorphicPropNet(Role[] theRoles,
                            Set<PolymorphicComponent> theComponents,
                            PolymorphicComponentFactory theComponentFactory)
  {
    componentFactory = theComponentFactory;
    roles = theRoles;
    components = theComponents;
    propositions = recordPropositions();
    basePropositions = recordBasePropositions();
    inputPropositions = recordInputPropositions();
    legalPropositions = null;
    legalPropositionsMutable = recordLegalPropositions();
    goalPropositions = null;
    goalPropositionsMutable = recordGoalPropositions();
    initProposition = recordInitProposition();
    terminalProposition = recordTerminalProposition();
    legalInputMap = makeLegalInputMap();
  }

  private Map<PolymorphicProposition, PolymorphicProposition> makeLegalInputMap()
  {
    Map<PolymorphicProposition, PolymorphicProposition> result = new HashMap<>();
    // Create a mapping from Body->Input.
    Map<List<GdlTerm>, PolymorphicProposition> inputPropsByBody = new HashMap<>();
    for (PolymorphicProposition inputProp : inputPropositions.values())
    {
      List<GdlTerm> inputPropBody = (inputProp.getName()).getBody();
      inputPropsByBody.put(inputPropBody, inputProp);
    }
    // Use that mapping to map Input->Legal and Legal->Input
    // based on having the same Body proposition.
    for (Set<PolymorphicProposition> legalProps : legalPropositionsMutable
        .values())
    {
      for (PolymorphicProposition legalProp : legalProps)
      {
        List<GdlTerm> legalPropBody = (legalProp.getName()).getBody();
        if (inputPropsByBody.containsKey(legalPropBody))
        {
          PolymorphicProposition inputProp = inputPropsByBody
              .get(legalPropBody);
          result.put(inputProp, legalProp);
          result.put(legalProp, inputProp);
        }
      }
    }
    return result;
  }

  /**
   * Builds an index over the BasePropositions in the PropNet. This is done by
   * going over every single-input proposition in the network, and seeing
   * whether or not its input is a transition, which would mean that by
   * definition the proposition is a base proposition.
   *
   * @return An index over the BasePropositions in the PropNet.
   */
  private Map<GdlSentence, PolymorphicProposition> recordBasePropositions()
  {
    Map<GdlSentence, PolymorphicProposition> result = new HashMap<>();
    for (PolymorphicProposition proposition : propositions)
    {
      // Skip all propositions without exactly one input.
      if (proposition.getInputs().size() != 1)
        continue;

      PolymorphicComponent component = proposition.getSingleInput();
      if (component instanceof PolymorphicTransition)
      {
        result.put(proposition.getName(), proposition);
      }
    }

    return result;
  }

  /**
   * Builds an index over the GoalPropositions in the PropNet. This is done by
   * going over every function proposition in the network where the name of the
   * function is "goal", and extracting the name of the role associated with
   * that goal proposition, and then using those role names as keys that map to
   * the goal propositions in the index.
   *
   * @return An index over the GoalPropositions in the PropNet.
   */
  private Map<Role, Set<PolymorphicProposition>> recordGoalPropositions()
  {
    goalPropositionsMutable = new HashMap<>();
    for (PolymorphicProposition proposition : propositions)
    {
      // Skip all propositions that aren't GdlRelations.
      if (!(proposition.getName() instanceof GdlRelation))
        continue;

      GdlRelation relation = (GdlRelation)proposition.getName();
      if (!relation.getName().getValue().equals("goal"))
        continue;

      Role theRole = new Role((GdlConstant)relation.get(0));
      if (!goalPropositionsMutable.containsKey(theRole))
      {
        goalPropositionsMutable.put(theRole,
                                    new HashSet<PolymorphicProposition>());
      }
      goalPropositionsMutable.get(theRole).add(proposition);
    }

    return goalPropositionsMutable;
  }

  /**
   * @return a reference to the single, unique, InitProposition.
   */
  private PolymorphicProposition recordInitProposition()
  {
    for (PolymorphicProposition proposition : propositions)
    {
      // Skip all propositions that aren't GdlPropositions.
      if (!(proposition.getName() instanceof GdlProposition))
        continue;

      GdlConstant constant = ((GdlProposition)proposition.getName()).getName();
      if (constant.getValue().toUpperCase().equals("INIT"))
      {
        return proposition;
      }
    }

    return null;
  }

  /**
   * @return an index over the InputPropositions in the PropNet.
   */
  private Map<GdlSentence, PolymorphicProposition> recordInputPropositions()
  {
    Map<GdlSentence, PolymorphicProposition> result = new HashMap<>();
    for (PolymorphicProposition proposition : propositions)
    {
      // Skip all propositions that aren't GdlFunctions.
      if (!(proposition.getName() instanceof GdlRelation))
        continue;

      GdlRelation relation = (GdlRelation)proposition.getName();
      if (relation.getName().getValue().equals("does"))
      {
        result.put(proposition.getName(), proposition);
      }
    }

    return result;
  }

  /**
   * @return an index over the LegalPropositions in the PropNet.
   */
  private Map<Role, Set<PolymorphicProposition>> recordLegalPropositions()
  {
    Map<Role, Set<PolymorphicProposition>> result = new HashMap<>();
    for (PolymorphicProposition proposition : propositions)
    {
      // Skip all propositions that aren't GdlRelations.
      if (!(proposition.getName() instanceof GdlRelation))
        continue;

      GdlRelation relation = (GdlRelation)proposition.getName();
      if (relation.getName().getValue().equals("legal"))
      {
        GdlConstant name = (GdlConstant)relation.get(0);
        Role r = new Role(name);
        if (!result.containsKey(r))
        {
          result.put(r, new HashSet<PolymorphicProposition>());
        }
        result.get(r).add(proposition);
      }
    }

    return result;
  }

  /**
   * Builds an index over the Propositions in the PropNet.
   *
   * @return An index over Propositions in the PropNet.
   */
  private Set<PolymorphicProposition> recordPropositions()
  {
    Set<PolymorphicProposition> result = new HashSet<>();
    for (PolymorphicComponent component : components)
    {
      if (component instanceof PolymorphicProposition)
      {
        result.add((PolymorphicProposition)component);
      }
    }
    return result;
  }

  /**
   * Records a reference to the single, unique, TerminalProposition.
   *
   * @return A reference to the single, unqiue, TerminalProposition.
   */
  private PolymorphicProposition recordTerminalProposition()
  {
    for (PolymorphicProposition proposition : propositions)
    {
      if (proposition.getName() instanceof GdlProposition)
      {
        GdlConstant constant = ((GdlProposition)proposition.getName())
            .getName();
        if (constant.getValue().equals("terminal"))
        {
          return proposition;
        }
      }
    }

    return null;
  }

  /**
   * Clones a new PolymorphicPropnet of instantiation type defined by the
   * provided component factory from an extant ggp-base propNet.
   * @param sourcePropnet ggp-base propNet to clone
   * @param theComponentFactory Factory to use to generate new components
   */
  public PolymorphicPropNet(PropNet sourcePropnet,
                            PolymorphicComponentFactory theComponentFactory)
  {
    componentFactory = theComponentFactory;

    Map<Component, PolymorphicComponent> sourceToTargetMap = new HashMap<>();

    components = new HashSet<>();

    //	Create the components
    for (Component old : sourcePropnet.getComponents())
    {
      PolymorphicComponent newComp;

      if (old instanceof And)
      {
        newComp = theComponentFactory.createAnd(old.getInputs().size(), old
            .getOutputs().size());
      }
      else if (old instanceof Or)
      {
        newComp = theComponentFactory.createOr(old.getInputs().size(), old
            .getOutputs().size());
      }
      else if (old instanceof Not)
      {
        newComp = theComponentFactory.createNot(old.getOutputs().size());
      }
      else if (old instanceof Proposition)
      {
        newComp = theComponentFactory.createProposition(old.getOutputs().size(),
                                                     ((Proposition)old)
                                                         .getName());
      }
      else if (old instanceof Transition)
      {
        newComp = theComponentFactory.createTransition(old.getOutputs().size());
      }
      else if (old instanceof Constant)
      {
        newComp = theComponentFactory.createConstant(old.getOutputs().size(),
                                                  ((Constant)old).getValue());
      }
      else
      {
        throw new RuntimeException("Invalid propnet");
      }

      sourceToTargetMap.put(old, newComp);
      components.add(newComp);
    }

    //	Connect them up
    for (Component old : sourcePropnet.getComponents())
    {
      PolymorphicComponent newComp = sourceToTargetMap.get(old);

      for (Component oldInput : old.getInputs())
      {
        PolymorphicComponent newInput = sourceToTargetMap.get(oldInput);

        newComp.addInput(newInput);
      }

      for (Component oldOutput : old.getOutputs())
      {
        PolymorphicComponent newOutput = sourceToTargetMap.get(oldOutput);

        newComp.addOutput(newOutput);
      }
    }

    //	Construct the various maps and collections we need to supply
    propositions = new HashSet<>();
    for (Proposition oldProp : sourcePropnet.getPropositions())
    {
      PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
          .get(oldProp);

      propositions.add(newProp);
    }
    basePropositions = new HashMap<>();
    for (Entry<GdlSentence, Proposition> oldEntry : sourcePropnet
        .getBasePropositions().entrySet())
    {
      PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
          .get(oldEntry.getValue());

      basePropositions.put(oldEntry.getKey(), newProp);
    }
    inputPropositions = new HashMap<>();
    for (Entry<GdlSentence, Proposition> oldEntry : sourcePropnet
        .getInputPropositions().entrySet())
    {
      PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
          .get(oldEntry.getValue());

      inputPropositions.put(oldEntry.getKey(), newProp);
    }
    legalPropositions = null;
    legalPropositionsMutable = new HashMap<>();
    for (Entry<Role, Set<Proposition>> oldEntry : sourcePropnet
        .getLegalPropositions().entrySet())
    {
      Set<PolymorphicProposition> newProps = new HashSet<>();

      for (Proposition oldProp : oldEntry.getValue())
      {
        PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
            .get(oldProp);

        newProps.add(newProp);
      }

      legalPropositionsMutable.put(oldEntry.getKey(), newProps);
    }
    goalPropositions = null;
    goalPropositionsMutable = new HashMap<>();
    for (Entry<Role, Set<Proposition>> oldEntry : sourcePropnet
        .getGoalPropositions().entrySet())
    {
      Set<PolymorphicProposition> newProps = new HashSet<>();

      for (Proposition oldProp : oldEntry.getValue())
      {
        PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
            .get(oldProp);

        newProps.add(newProp);
      }

      goalPropositionsMutable.put(oldEntry.getKey(), newProps);
    }
    initProposition = (PolymorphicProposition)sourceToTargetMap
        .get(sourcePropnet.getInitProposition());
    terminalProposition = (PolymorphicProposition)sourceToTargetMap
        .get(sourcePropnet.getTerminalProposition());
    legalInputMap = new HashMap<>();
    for (Entry<Proposition, Proposition> oldEntry : sourcePropnet
        .getLegalInputMap().entrySet())
    {
      PolymorphicProposition newProp1 = (PolymorphicProposition)sourceToTargetMap
          .get(oldEntry.getKey());
      PolymorphicProposition newProp2 = (PolymorphicProposition)sourceToTargetMap
          .get(oldEntry.getValue());

      legalInputMap.put(newProp1, newProp2);
    }

    roles = sourcePropnet.getRoles().toArray(new Role[0]);
  }

  /**
   * Clones a new PolymorphicPropnet of instantiation type defined by the
   * provided component factory from an extant polymorphic propNet.
   * @param sourcePropnet polymorphic propNet to clone
   * @param theComponentFactory Factory to use to generate new components
   */
  public PolymorphicPropNet(PolymorphicPropNet sourcePropnet,
                            PolymorphicComponentFactory theComponentFactory)
  {
    componentFactory = theComponentFactory;

    Map<PolymorphicComponent, PolymorphicComponent> sourceToTargetMap = new HashMap<>();
    components = new HashSet<>();

    //	Create the components
    for (PolymorphicComponent old : sourcePropnet.getComponents())
    {
      PolymorphicComponent newComp;

      if (old instanceof PolymorphicAnd)
      {
        newComp = componentFactory.createAnd(-1, -1);
      }
      else if (old instanceof PolymorphicOr)
      {
        newComp = componentFactory.createOr(-1, -1);
      }
      else if (old instanceof PolymorphicNot)
      {
        newComp = componentFactory.createNot(-1);
      }
      else if (old instanceof PolymorphicProposition)
      {
        newComp = componentFactory
            .createProposition(-1, ((PolymorphicProposition)old).getName());
      }
      else if (old instanceof PolymorphicTransition)
      {
        newComp = componentFactory.createTransition(-1);
      }
      else if (old instanceof PolymorphicConstant)
      {
        newComp = componentFactory.createConstant(-1,
                                                  ((PolymorphicConstant)old)
                                                      .getValue());
      }
      else
      {
        throw new RuntimeException("Invalid propnet");
      }

      sourceToTargetMap.put(old, newComp);
      components.add(newComp);
    }

    //	Connect them up
    for (PolymorphicComponent old : sourcePropnet.getComponents())
    {
      PolymorphicComponent newComp = sourceToTargetMap.get(old);

      for (PolymorphicComponent oldInput : old.getInputs())
      {
        PolymorphicComponent newInput = sourceToTargetMap.get(oldInput);

        newComp.addInput(newInput);
      }

      for (PolymorphicComponent oldOutput : old.getOutputs())
      {
        PolymorphicComponent newOutput = sourceToTargetMap.get(oldOutput);

        newComp.addOutput(newOutput);
      }
    }

    //	Construct the various maps and collections we need to supply
    propositions = new HashSet<>();
    for (PolymorphicProposition oldProp : sourcePropnet.getPropositions())
    {
      PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
          .get(oldProp);

      propositions.add(newProp);
    }
    basePropositions = new HashMap<>();
    for (Entry<GdlSentence, PolymorphicProposition> oldEntry : sourcePropnet
        .getBasePropositions().entrySet())
    {
      PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
          .get(oldEntry.getValue());

      basePropositions.put(oldEntry.getKey(), newProp);
    }
    inputPropositions = new HashMap<>();
    for (Entry<GdlSentence, PolymorphicProposition> oldEntry : sourcePropnet
        .getInputPropositions().entrySet())
    {
      PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
          .get(oldEntry.getValue());

      inputPropositions.put(oldEntry.getKey(), newProp);
    }
    legalPropositions = null;
    legalPropositionsMutable = new HashMap<>();
    for (Entry<Role, PolymorphicProposition[]> oldEntry : sourcePropnet
        .getLegalPropositions().entrySet())
    {
      Set<PolymorphicProposition> newProps = new HashSet<>();

      for (PolymorphicProposition oldProp : oldEntry.getValue())
      {
        PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
            .get(oldProp);

        newProps.add(newProp);
      }

      legalPropositionsMutable.put(oldEntry.getKey(), newProps);
    }
    goalPropositions = null;
    goalPropositionsMutable = new HashMap<>();
    for (Entry<Role, PolymorphicProposition[]> oldEntry : sourcePropnet
        .getGoalPropositions().entrySet())
    {
      Set<PolymorphicProposition> newProps = new HashSet<>();

      for (PolymorphicProposition oldProp : oldEntry.getValue())
      {
        PolymorphicProposition newProp = (PolymorphicProposition)sourceToTargetMap
            .get(oldProp);

        newProps.add(newProp);
      }

      goalPropositionsMutable.put(oldEntry.getKey(), newProps);
    }
    initProposition = (PolymorphicProposition)sourceToTargetMap.get(sourcePropnet.getInitProposition());
    terminalProposition = (PolymorphicProposition)sourceToTargetMap.get(sourcePropnet.getTerminalProposition());
    legalInputMap = new HashMap<>();
    for (Entry<PolymorphicProposition, PolymorphicProposition> oldEntry : sourcePropnet.getLegalInputMap().entrySet())
    {
      PolymorphicProposition newProp1 = (PolymorphicProposition)sourceToTargetMap.get(oldEntry.getKey());
      PolymorphicProposition newProp2 = (PolymorphicProposition)sourceToTargetMap.get(oldEntry.getValue());
      legalInputMap.put(newProp1, newProp2);
    }

    roles = sourcePropnet.getRoles();
  }

  /**
   * Get the list of roles involved in the game for which this propnet implements the statemachine.
   *
   * @return list of roles, in the order that they appear in the GDL.
   */
  public Role[] getRoles()
  {
    return roles;
  }

  /**
   * Get a map of the correspondence of input propositions to legal
   * propositions for the same move.  Both directions of the mapping
   * (which is always (1:1)) are present in the returned map
   * @return the legal &lt;-&gt; input map
   */
  public Map<PolymorphicProposition, PolymorphicProposition> getLegalInputMap()
  {
    return legalInputMap;
  }

  /**
   * Getter method.
   *
   * @return References to every BaseProposition in the PropNet, indexed by
   *         name.
   */
  public Map<GdlSentence, PolymorphicProposition> getBasePropositions()
  {
    return basePropositions;
  }

  /**
   * Getter method.
   *
   * @return References to every BaseProposition in the PropNet as an array
   */
  public PolymorphicProposition[] getBasePropositionsArray()
  {
    synchronized (this)
    {
      if (basePropositionsArray == null)
      {
        basePropositionsArray = new PolymorphicProposition[basePropositions.size()];
        int index = 0;
        for (PolymorphicProposition p : basePropositions.values())
        {
          basePropositionsArray[index++] = p;
        }
      }
      return basePropositionsArray;
    }
  }

  /**
   * Getter method.
   *
   * @return References to every Component in the PropNet.
   */
  public Set<PolymorphicComponent> getComponents()
  {
    return components;
  }

  /**
   * Getter method.
   *
   * @return References to every GoalProposition in the PropNet, indexed by
   *         player name.
   */
  public Map<Role, PolymorphicProposition[]> getGoalPropositions()
  {
    if (goalPropositions == null)
    {
      goalPropositions = new HashMap<>();
      for (Role role : goalPropositionsMutable.keySet())
      {
        PolymorphicProposition[] goalsForRole = new PolymorphicProposition[goalPropositionsMutable.get(role).size()];
        int index = 0;
        for (PolymorphicProposition p : goalPropositionsMutable.get(role))
        {
          goalsForRole[index++] = p;
        }

        goalPropositions.put(role, goalsForRole);
      }
    }
    return goalPropositions;
  }

  /**
   * @return the INIT proposition for the propNet, or null if it has been optimised away.
   */
  public PolymorphicProposition getInitProposition()
  {
    return initProposition;
  }

  /**
   * Remove init propositions from the network
   */
  public void RemoveInits()
  {
    OptimizingPolymorphicPropNetFactory.removeInitPropositions(this);

    initProposition = null;
  }

  /**
   * Remove goal propositions from the network
   * Note that this will not remove goal props that are required
   * for the calculation of non-goal outputs
   */
  public void RemoveGoals()
  {
    OptimizingPolymorphicPropNetFactory.removeGoalPropositions(this);
  }

  /**
   * Remove things we do not need to support goal determination
   */
  public void RemoveAllButGoals()
  {
    RemoveInits();
    OptimizingPolymorphicPropNetFactory.removeAllButGoalPropositions(this);
  }

  /**
   * Remove things we do not need to support terminality determination
   */
  public void RemoveAllButTerminal()
  {
    RemoveInits();
    OptimizingPolymorphicPropNetFactory.removeAllButTerminalProposition(this);
  }

  /**
   * Getter method.
   *
   * @return References to every InputProposition in the PropNet, indexed by
   *         name.
   */
  public Map<GdlSentence, PolymorphicProposition> getInputPropositions()
  {
    return inputPropositions;
  }

  /**
   * Getter method.
   *
   * @return References to every LegalProposition in the PropNet, indexed by
   *         player name.
   */
  public Map<Role, PolymorphicProposition[]> getLegalPropositions()
  {
    if (legalPropositions == null)
    {
      legalPropositions = new HashMap<>();
      for (Role role : legalPropositionsMutable.keySet())
      {
        PolymorphicProposition[] legalsForRole = new PolymorphicProposition[legalPropositionsMutable
            .get(role).size()];
        int index = 0;
        for (PolymorphicProposition p : legalPropositionsMutable.get(role))
        {
          legalsForRole[index++] = p;
        }

        legalPropositions.put(role, legalsForRole);
      }

    }
    return legalPropositions;
  }

  /**
   * Getter method.
   *
   * @return References to every Proposition in the PropNet.
   */
  public Set<PolymorphicProposition> getPropositions()
  {
    return propositions;
  }

  /**
   * Getter method.
   *
   * @return A reference to the single, unique, TerminalProposition.
   */
  public PolymorphicProposition getTerminalProposition()
  {
    return terminalProposition;
  }

  /**
   * Outputs the propnet in .dot format to a particular file. This can be
   * viewed with tools like Graphviz and ZGRViewer.
   *
   * @param filename
   *          the name of the file to output to
   */
  public void renderToFile(String filename)
  {
    try
    {
      File f = new File(TEMP_DIR, filename);
      try(FileOutputStream fos = new FileOutputStream(f))
      {
        try(BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8")))
        {
          renderAsDot(fout);
        }
      }
    }
    catch (Exception e)
    {
      GamerLogger.logStackTrace("StateMachine", e);
    }
  }

  private void renderAsDot(Writer xiOutput) throws IOException
  {
    xiOutput.write("digraph propNet\n{\n");
    for (PolymorphicComponent component : components)
    {
      xiOutput.write('\t');
      component.renderAsDot(xiOutput);
      xiOutput.write('\n');
    }
    xiOutput.write('}');
  }

  /**
   * Removes a component from the propnet. Be very careful when using this
   * method, as it is not thread-safe. It is highly recommended that this
   * method only be used in an optimization period between the propnet's
   * creation and its initial use, during which it should only be accessed by a
   * single thread. The INIT and terminal components cannot be removed.
   * @param c component to remove
   */
  public void removeComponent(PolymorphicComponent c)
  {
    //Go through all the collections it could appear in
    if (c instanceof PolymorphicProposition)
    {
      PolymorphicProposition p = (PolymorphicProposition)c;
      GdlSentence name = p.getName();
      if (basePropositions.containsKey(name))
      {
        basePropositionsArray = null;
        basePropositions.remove(name);
      }
      else if (inputPropositions.containsKey(name))
      {
        inputPropositions.remove(name);
        //The map goes both ways...
        PolymorphicProposition partner = legalInputMap.get(p);
        if (partner != null)
        {
          legalInputMap.remove(partner);
          legalInputMap.remove(p);
        }
      }
      else if (name == GdlPool.getProposition(GdlPool.TERMINAL))
      {
        throw new RuntimeException("The terminal component cannot be removed.");
      }
      else
      {
        for (Set<PolymorphicProposition> legalProps : legalPropositionsMutable
            .values())
        {
          if (legalProps.contains(p))
          {
            legalPropositions = null;
            legalProps.remove(p);
            PolymorphicProposition partner = legalInputMap.get(p);
            if (partner != null)
            {
              legalInputMap.remove(partner);
              legalInputMap.remove(p);
            }
          }
        }
        for (Set<PolymorphicProposition> goalProps : goalPropositionsMutable
            .values())
        {
          goalPropositions = null;
          goalProps.remove(p);
        }
      }
      propositions.remove(p);
    }
    components.remove(c);

    //Remove all the local links to the component
    for (PolymorphicComponent parent : c.getInputs())
      parent.removeOutput(c);
    for (PolymorphicComponent child : c.getOutputs())
      child.removeInput(c);
    //These are actually unnecessary...
    //c.removeAllInputs();
    //c.removeAllOutputs();
  }

  /**
   * Adds a component to the propnet
   * @param c component to add
   */
  public void addComponent(PolymorphicComponent c)
  {
    components.add(c);
    if (c instanceof PolymorphicProposition)
      propositions.add((PolymorphicProposition)c);
  }

  /**
   * Getter method
   * @return a factory for components of the type comprising this propNet
   */
  public PolymorphicComponentFactory getComponentFactory()
  {
    return componentFactory;
  }

  /**
   * Crystalize the propNet into an optimally runtime efficient
   * form.  Once this is done no further changes may be made to the
   * propNet's topology (changing connections or adding/removing
   * components)
   */
  public void crystalize()
  {
    for (PolymorphicComponent c : components)
    {
      c.crystalize();
    }
  }
}