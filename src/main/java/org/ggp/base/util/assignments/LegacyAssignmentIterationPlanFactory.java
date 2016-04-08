package org.ggp.base.util.assignments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.model.SentenceDomainModels;
import org.ggp.base.util.gdl.model.SentenceDomainModels.VarDomainOpts;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SimpleSentenceForm;
import org.ggp.base.util.gdl.model.assignments.FunctionInfo;
import org.ggp.base.util.gdl.model.assignments.IterationOrderCandidate;
import org.ggp.base.util.gdl.transforms.ConstantChecker;

import com.google.common.collect.Maps;

/**
 * This is probably the best iteration plan factory at the moment; a lot of
 * work went into it the first time around. This is "rewritten" for clarity
 * and to work with the new assignment iteration classes and MPNF.
 *
 * Adapted from code in the old AssignmentsImpl, mostly.
 */
public class LegacyAssignmentIterationPlanFactory {
    public static final boolean ANALYTIC_FUNCTION_ORDERING = false;
    //TODO: Test above with true vs. false

    public static NewAssignmentIterationPlan create(GdlRule rule, SentenceDomainModel domainModel,
            ConstantChecker constantChecker,
            Map<SentenceForm, FunctionInfo> functionInfoMap) {

        //		System.out.println("rule: " + rule);
        Map<GdlVariable, Set<GdlConstant>> varDomains =
                SentenceDomainModels.getVarDomains(rule, domainModel, VarDomainOpts.INCLUDE_HEAD);
        //		System.out.println("varDomains: " + varDomains);
        //TODO: This should probably just check the sizes in the ConstantChecker?
        //Or perhaps accept some smarter notion of ___...
        //TODO: Pass this in
        Map<SentenceForm, Integer> completedSentenceFormSizes =
                Maps.newHashMap();
        for (SentenceForm constantForm : constantChecker.getConstantSentenceForms()) {
            completedSentenceFormSizes.put(constantForm, constantChecker.getTrueSentences(constantForm).size());
        }
        IterationOrderCandidate bestIterationOrderCandidate = getBestIterationOrderCandidate(rule,
                varDomains,
                functionInfoMap,
                completedSentenceFormSizes,
                ANALYTIC_FUNCTION_ORDERING);

        return toPlan(bestIterationOrderCandidate, varDomains, constantChecker);
    }

    private static NewAssignmentIterationPlan toPlan(
            IterationOrderCandidate bestOrder,
            Map<GdlVariable, Set<GdlConstant>> varDomains, ConstantChecker constantChecker) {
        //		List<GdlVariable> assignmentOrder = bestOrder.getVariableOrdering();
        //		List<AssignmentStrategy> strategies = Lists.newArrayList();
        //
        //		bestOrder.
        //
        //		return ComplexAssignmentIterationPlan.create(assignmentOrder, strategies);
        return bestOrder.toAssignmentIterationPlan(varDomains, constantChecker);
    }

    /**
     * Finds the iteration order (including variables, functions, and
     * source conjuncts) that is expected to result in the fastest iteration.
     *
     * The value that is compared for each ordering is the product of:
     * - For each source conjunct, the number of tuples offered by the conjunct;
     * - For each variable not defined by a function, the size of its domain.
     *
     * @param functionInfoMap
     * @param completedSentenceFormSizes For each sentence form, this may optionally
     * contain the number of possible sentences of this form. This is useful if the
     * number of sentences is much lower than the product of its variables' domain
     * sizes; however, if this contains sentence forms where the set of sentences
     * is unknown, then it may return an ordering that is unusable.
     */
    protected static IterationOrderCandidate getBestIterationOrderCandidate(GdlRule rule,
            /*SentenceModel model,*/
            Map<GdlVariable, Set<GdlConstant>> varDomains,
            Map<SentenceForm, ? extends FunctionInfo> functionInfoMap,
            Map<SentenceForm, Integer> completedSentenceFormSizes,
            //			Map<GdlVariable, GdlConstant> preassignment,
            boolean analyticFunctionOrdering) {
        //Here are the things we need to pass into the first IOC constructor
        List<GdlSentence> sourceConjunctCandidates = new ArrayList<GdlSentence>();
        //What is a source conjunct candidate?
        //- It is a positive conjunct in the rule (i.e. a GdlSentence in the body).
        //- It has already been fully defined; i.e. it is not recursively defined in terms of the current form.
        //Furthermore, we know the number of potentially true tuples in it.
        List<GdlVariable> varsToAssign = GdlUtils.getVariables(rule);
        List<GdlVariable> newVarsToAssign = new ArrayList<GdlVariable>();
        for(GdlVariable var : varsToAssign)
            if(!newVarsToAssign.contains(var))
                newVarsToAssign.add(var);
        varsToAssign = newVarsToAssign;
        //		if(preassignment != null)
        //			varsToAssign.removeAll(preassignment.keySet());

        //Calculate var domain sizes
        Map<GdlVariable, Integer> varDomainSizes = getVarDomainSizes(varDomains/*rule, model*/);

        List<Integer> sourceConjunctSizes = new ArrayList<Integer>();
        for(GdlLiteral conjunct : rule.getBody()) {
            if(conjunct instanceof GdlRelation) {
                SentenceForm form = SimpleSentenceForm.create((GdlRelation)conjunct);
                if(completedSentenceFormSizes != null
                        && completedSentenceFormSizes.containsKey(form)) {
                    int size = completedSentenceFormSizes.get(form);
                    //New: Don't add if it will be useless as a source
                    //For now, we take a strict definition of that
                    //Compare its size with the product of the domains
                    //of the variables it defines
                    //In the future, we could require a certain ratio
                    //to decide that this is worthwhile
                    GdlRelation relation = (GdlRelation) conjunct;
                    int maxSize = 1;
                    Set<GdlVariable> vars = new HashSet<GdlVariable>(GdlUtils.getVariables(relation));
                    for(GdlVariable var : vars) {
                        int domainSize = varDomainSizes.get(var);
                        maxSize *= domainSize;
                    }
                    if(size >= maxSize)
                        continue;
                    sourceConjunctCandidates.add(relation);
                    sourceConjunctSizes.add(size);
                }
            }
        }

        List<GdlSentence> functionalSentences = new ArrayList<GdlSentence>();
        List<FunctionInfo> functionalSentencesInfo = new ArrayList<FunctionInfo>();
        for(GdlLiteral conjunct : rule.getBody()) {
            if(conjunct instanceof GdlSentence) {
                SentenceForm form = SimpleSentenceForm.create((GdlSentence) conjunct);
                if(functionInfoMap != null && functionInfoMap.containsKey(form)) {
                    functionalSentences.add((GdlSentence) conjunct);
                    functionalSentencesInfo.add(functionInfoMap.get(form));
                }
            }
        }

        //TODO: If we have a head assignment, treat everything as already replaced
        //Maybe just translate the rule? Or should we keep the pool clean?

        IterationOrderCandidate emptyCandidate = new IterationOrderCandidate(varsToAssign, sourceConjunctCandidates,
                sourceConjunctSizes, functionalSentences, functionalSentencesInfo, varDomainSizes);
        PriorityQueue<IterationOrderCandidate> searchQueue = new PriorityQueue<IterationOrderCandidate>();
        searchQueue.add(emptyCandidate);

        while(!searchQueue.isEmpty()) {
            IterationOrderCandidate curNode = searchQueue.remove();
            //			System.out.println("Node being checked out: " + curNode);
            if(curNode.isComplete()) {
                //This is the complete ordering with the lowest heuristic value
                return curNode;
            }
            searchQueue.addAll(curNode.getChildren(analyticFunctionOrdering));
        }
        throw new RuntimeException("Found no complete iteration orderings");
    }

    private static Map<GdlVariable, Integer> getVarDomainSizes(/*GdlRule rule,
			SentenceModel model*/Map<GdlVariable, Set<GdlConstant>> varDomains) {
        Map<GdlVariable, Integer> varDomainSizes = new HashMap<GdlVariable, Integer>();
        //Map<GdlVariable, Set<GdlConstant>> varDomains = model.getVarDomains(rule);
        for(GdlVariable var : varDomains.keySet()) {
            varDomainSizes.put(var, varDomains.get(var).size());
        }
        return varDomainSizes;
    }

}
