package org.ggp.base.util.gdl.transforms;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * As a GDL transformer, this class takes in a GDL description of a game,
 * transforms it in some way, and outputs a new GDL descriptions of a game
 * which is functionally equivalent to the original game.
 *
 * The AimaProver does not correctly apply "distinct" literals in rules if
 * they have not yet been bound. (See test_distinct_beginning_rule.kif for
 * an example where this comes up.) The same is true for "not" literals.
 * This transformation moves "distinct" and "not" literals later in the
 * rule, so they always appear after sentence literals have defined those
 * variables.
 *
 * This variant does not remove all disjunctions in the process, unlike the
 * original implementation. Instead, it only removes disjunctions if they
 * introduce new variables.
 *
 * This should be applied to the input to the ProverStateMachine until this
 * bug is fixed some other way.
 */
public class DistinctAndNotMover2 {
    public static List<Gdl> run(List<Gdl> oldRules) {
        //This should guarantee that a disjunction doesn't introduce any variables that
        //aren't already introduced elsewhere in the rule.
        oldRules = PartialDeORer.run(oldRules);

        List<Gdl> newRules = Lists.newArrayListWithCapacity(oldRules.size());
        for (Gdl gdl : oldRules) {
            if (gdl instanceof GdlRule) {
                GdlRule rule = (GdlRule) gdl;
                newRules.add(reorderRule(rule));
            } else {
                newRules.add(gdl);
            }
        }
        return newRules;
    }

    private static GdlRule reorderRule(GdlRule oldRule) {
        List<GdlLiteral> newBody = Lists.newArrayList(oldRule.getBody());
        rearrangeDistinctsAndNots(newBody);
        return GdlPool.getRule(oldRule.getHead(), newBody);
    }

    private static void rearrangeDistinctsAndNots(List<GdlLiteral> ruleBody) {
        Integer oldIndex = findDistinctNotOrDisjunctionToMoveIndex(ruleBody);
        while (oldIndex != null) {
            GdlLiteral literalToMove = ruleBody.get(oldIndex);
            ruleBody.remove((int) oldIndex);
            reinsertLiteralInRightPlace(ruleBody, literalToMove);

            oldIndex = findDistinctNotOrDisjunctionToMoveIndex(ruleBody);
        }
    }

    //Returns null if no distincts have to be moved.
    private static Integer findDistinctNotOrDisjunctionToMoveIndex(List<GdlLiteral> ruleBody) {
        Set<GdlVariable> setVars = Sets.newHashSet();
        for (int i = 0; i < ruleBody.size(); i++) {
            GdlLiteral literal = ruleBody.get(i);
            if (literal instanceof GdlSentence) {
                setVars.addAll(GdlUtils.getVariables(literal));
            } else if (literal instanceof GdlDistinct
                    || literal instanceof GdlNot
                    || literal instanceof GdlOr) {
                if (!allVarsInLiteralAlreadySet(literal, setVars)) {
                    return i;
                }
            }
        }
        return null;
    }

    private static void reinsertLiteralInRightPlace(List<GdlLiteral> ruleBody,
            GdlLiteral literalToReinsert) {
        Set<GdlVariable> setVars = Sets.newHashSet();
        for (int i = 0; i < ruleBody.size(); i++) {
            GdlLiteral literal = ruleBody.get(i);
            if (literal instanceof GdlSentence) {
                setVars.addAll(GdlUtils.getVariables(literal));

                if (allVarsInLiteralAlreadySet(literalToReinsert, setVars)) {
                    ruleBody.add(i + 1, literalToReinsert);
                    return;
                }
            }
        }
    }

    private static boolean allVarsInLiteralAlreadySet(GdlLiteral literal,
            Set<GdlVariable> setVars) {
        List<GdlVariable> varsInLiteral = GdlUtils.getVariables(literal);
        for (GdlVariable varInLiteral : varsInLiteral) {
            if (!setVars.contains(varInLiteral)) {
                return false;
            }
        }
        return true;
    }
}
