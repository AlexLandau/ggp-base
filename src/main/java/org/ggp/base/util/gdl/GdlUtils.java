package org.ggp.base.util.gdl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class GdlUtils {
    private GdlUtils() {
    }

    //TODO (AL): Check if we can switch over to just having this return a set.
    public static List<GdlVariable> getVariables(Gdl gdl) {
        final List<GdlVariable> variablesList = new ArrayList<GdlVariable>();
        final Set<GdlVariable> variables = new HashSet<GdlVariable>();
        GdlVisitors.visitAll(gdl, new GdlVisitor() {
            @Override
            public void visitVariable(GdlVariable variable) {
                if (!variables.contains(variable)) {
                    variablesList.add(variable);
                    variables.add(variable);
                }
            }
        });
        return variablesList;
    }

    public static Set<GdlVariable> getVariablesSet(Gdl gdl) {
        final Set<GdlVariable> variables = new HashSet<GdlVariable>();
        GdlVisitors.visitAll(gdl, new GdlVisitor() {
            @Override
            public void visitVariable(GdlVariable variable) {
                variables.add(variable);
            }
        });
        return variables;
    }

    public static Set<GdlVariable> getVariables(Collection<? extends Gdl> gdls) {
        final Set<GdlVariable> variables = Sets.newHashSet();
        GdlVisitors.visitAll(gdls, new GdlVisitor() {
            @Override
            public void visitVariable(GdlVariable variable) {
                variables.add(variable);
            }
        });
        return variables;
    }

    public static List<String> getVariableNames(Gdl gdl) {
        List<GdlVariable> variables = getVariables(gdl);
        List<String> variableNames = new ArrayList<String>();
        for (GdlVariable variable : variables) {
            variableNames.add(variable.getName());
        }
        return variableNames;
    }

    public static List<GdlSentence> getSentencesInRuleBody(GdlRule rule) {
        List<GdlSentence> result = new ArrayList<GdlSentence>();
        for(GdlLiteral literal : rule.getBody()) {
            addSentencesInLiteral(literal, result);
        }
        return result;
    }

    private static void addSentencesInLiteral(GdlLiteral literal,
            Collection<GdlSentence> sentences) {
        if(literal instanceof GdlSentence) {
            sentences.add((GdlSentence) literal);
        } else if(literal instanceof GdlNot) {
            GdlNot not = (GdlNot) literal;
            addSentencesInLiteral(not.getBody(), sentences);
        } else if(literal instanceof GdlOr) {
            GdlOr or = (GdlOr) literal;
            for(int i = 0; i < or.arity(); i++)
                addSentencesInLiteral(or.get(i), sentences);
        } else if(!(literal instanceof GdlDistinct)) {
            throw new RuntimeException("Unexpected GdlLiteral type encountered: " + literal.getClass().getSimpleName());
        }
    }

    //TODO: Write version to find Nth constant/variable in sentence, to avoid unnecessary
    //memory allocations?
    public static List<GdlTerm> getTupleFromSentence(
            GdlSentence sentence) {
        if(sentence instanceof GdlProposition)
            return Collections.emptyList();

        //A simple crawl through the sentence.
        List<GdlTerm> tuple = new ArrayList<GdlTerm>();
        try {
            addBodyToTuple(sentence.getBody(), tuple);
        } catch(RuntimeException e) {
            throw new RuntimeException(e.getMessage() + "\nSentence was " + sentence);
        }
        return tuple;
    }
    private static void addBodyToTuple(List<GdlTerm> body, List<GdlTerm> tuple) {
        for(GdlTerm term : body) {
            if(term instanceof GdlConstant) {
                tuple.add(term);
            } else if(term instanceof GdlVariable) {
                tuple.add(term);
            } else if(term instanceof GdlFunction){
                GdlFunction function = (GdlFunction) term;
                addBodyToTuple(function.getBody(), tuple);
            } else {
                throw new RuntimeException("Unforeseen Gdl tupe in SentenceModel.addBodyToTuple()");
            }
        }
    }

    public static List<GdlConstant> getTupleFromGroundSentence(
            GdlSentence sentence) {
        if(sentence instanceof GdlProposition)
            return Collections.emptyList();

        //A simple crawl through the sentence.
        List<GdlConstant> tuple = new ArrayList<GdlConstant>();
        try {
            addBodyToGroundTuple(sentence.getBody(), tuple);
        } catch(RuntimeException e) {
            throw new RuntimeException(e.getMessage() + "\nSentence was " + sentence);
        }
        return tuple;
    }
    private static void addBodyToGroundTuple(List<GdlTerm> body, List<GdlConstant> tuple) {
        for(GdlTerm term : body) {
            if(term instanceof GdlConstant) {
                tuple.add((GdlConstant) term);
            } else if(term instanceof GdlVariable) {
                throw new RuntimeException("Asking for a ground tuple of a non-ground sentence");
            } else if(term instanceof GdlFunction){
                GdlFunction function = (GdlFunction) term;
                addBodyToGroundTuple(function.getBody(), tuple);
            } else {
                throw new RuntimeException("Unforeseen Gdl tupe in SentenceModel.addBodyToTuple()");
            }
        }
    }

    /**
     * Returns null if no assignment is possible.
     *
     * <p>Occasionally useful factoid: If this returns a non-null value, then if the sentence
     * represented by right is satisfiable (when variables are completely unbound) then the
     * sentence on the left is satisfiable (when variables are completely unbound).
     */
    public static @Nullable Map<GdlVariable, GdlTerm> getAssignmentMakingLeftIntoRight(
            GdlSentence left, GdlSentence right) {
        Map<GdlVariable, GdlTerm> assignment = Maps.newHashMap();
        if(!left.getName().equals(right.getName()))
            return null;
        if(left.arity() != right.arity())
            return null;
        if(left.arity() == 0)
            return Collections.emptyMap();
        if(!fillAssignmentBody(assignment, left.getBody(), right.getBody()))
            return null;
        return assignment;
    }

    public static @Nullable Map<GdlVariable, GdlConstant> getAssignmentMakingLeftIntoGroundRight(
            GdlSentence left, GdlSentence right) {
        Preconditions.checkArgument(right.isGround());
        Map<GdlVariable, GdlTerm> assignment = getAssignmentMakingLeftIntoRight(left, right);
        return Maps.transformValues(assignment, term -> (GdlConstant) term);
    }

    private static boolean fillAssignmentBody(
            Map<GdlVariable, GdlTerm> assignment, List<GdlTerm> leftBody,
            List<GdlTerm> rightBody) {
        if (leftBody.size() != rightBody.size()) {
            return false;
        }
        for (int i = 0; i < leftBody.size(); i++) {
            GdlTerm leftTerm = leftBody.get(i);
            GdlTerm rightTerm = rightBody.get(i);
            if (leftTerm instanceof GdlConstant) {
                if (!leftTerm.equals(rightTerm)) {
                    return false;
                }
            } else if (leftTerm instanceof GdlVariable) {
                if (assignment.containsKey(leftTerm)) {
                    if (!assignment.get(leftTerm).equals(rightTerm)) {
                        return false;
                    }
                } else {
                    assignment.put((GdlVariable) leftTerm, rightTerm);
                }
            } else if(leftTerm instanceof GdlFunction) {
                if (!(rightTerm instanceof GdlFunction)) {
                    return false;
                }
                GdlFunction leftFunction = (GdlFunction) leftTerm;
                GdlFunction rightFunction = (GdlFunction) rightTerm;
                if (!leftFunction.getName().equals(rightFunction.getName())) {
                    return false;
                }
                if (leftFunction.arity() != rightFunction.arity()) {
                    return false;
                }
                if (!fillAssignmentBody(assignment, leftFunction.getBody(), rightFunction.getBody())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean containsTerm(GdlSentence sentence, GdlTerm term) {
        if(sentence instanceof GdlProposition)
            return false;
        return containsTerm(sentence.getBody(), term);
    }

    private static boolean containsTerm(List<GdlTerm> body, GdlTerm term) {
        for(GdlTerm curTerm : body) {
            if(curTerm.equals(term))
                return true;
            if(curTerm instanceof GdlFunction) {
                if(containsTerm(((GdlFunction) curTerm).getBody(), term))
                    return true;
            }
        }
        return false;
    }

    public static GdlSentence toSentence(String string) {
        try {
            return (GdlSentence) GdlFactory.create(string);
        } catch (GdlFormatException | SymbolFormatException | ClassCastException e) {
            throw new RuntimeException("String was not a properly-formatted GDL sentence: " + string, e);
        }
    }

    public static Set<Integer> getVarIndices(GdlSentence sentence) {
        List<GdlTerm> tuple = getTupleFromSentence(sentence);
        return getVarIndices(tuple);
    }

    private static Set<Integer> getVarIndices(List<GdlTerm> tuple) {
        Set<Integer> indices = Sets.newHashSet();
        for (int i = 0; i < tuple.size(); i++) {
            if (tuple.get(i) instanceof GdlVariable) {
                indices.add(i);
            }
        }
        return indices;
    }

    public static List<GdlSentence> getSentences(List<Gdl> oldRules) {
        return oldRules.stream()
                .filter(gdl -> gdl instanceof GdlSentence)
                .map(gdl -> (GdlSentence) gdl)
                .collect(Collectors.toList());
    }

    public static Comparator<Gdl> compareAlphabetically() {
        return Comparator.comparing(Gdl::toString);
    }

    public static List<GdlConstant> getRoles(List<Gdl> description) {
        ImmutableList.Builder<GdlConstant> roles = ImmutableList.builder();
        for (Gdl gdl : description) {
            if (gdl instanceof GdlRelation) {
                GdlRelation relation = (GdlRelation) gdl;
                if (relation.getName() == GdlPool.ROLE) {
                    roles.add((GdlConstant) relation.get(0));
                }
            }
        }
        return roles.build();
    }
}
