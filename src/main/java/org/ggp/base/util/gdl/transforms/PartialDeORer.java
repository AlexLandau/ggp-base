package org.ggp.base.util.gdl.transforms;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.Immutables;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Unlike the normal DeORer, this only removes ORs in a very particular
 * (and somewhat problematic) case, in which positive conjuncts within
 * the disjunction introduce variables that are not in positive conjuncts
 * elsewhere in the rule body.
 *
 * This is most useful when trying to preserve the performance advantages
 * of disjunctions in a prover-type engine. As such, you probably want to
 * run some version of the DistinctAndNotMover after applying this. Otherwise,
 * you may prefer to just use the DeORer.
 *
 * @author Alex Landau
 */
public class PartialDeORer {
    public static List<Gdl> run(List<Gdl> rules) {
        List<Gdl> newRules = Lists.newArrayList();

        for (Gdl gdl : rules) {
            if (gdl instanceof GdlRule) {
                newRules.addAll(removeBadOrs((GdlRule) gdl));
            } else {
                newRules.add(gdl);
            }
        }
        return newRules;
    }

    private static List<GdlRule> removeBadOrs(GdlRule rule) {
        Set<GdlVariable> varsInPositiveConjuncts = rule.getBody().stream()
            .filter(literal -> literal instanceof GdlSentence)
            .flatMap(sentence -> GdlUtils.getVariables(sentence).stream())
            .collect(Immutables.collectSet());

        for (GdlLiteral literal : rule.getBody()) {
            if (literal instanceof GdlOr) {
                Set<GdlVariable> varsInOr = GdlUtils.getVariablesSet(literal);
                if (!varsInPositiveConjuncts.containsAll(varsInOr)) {
                    return removeBadOrs(removeBadOr(rule, (GdlOr) literal));
                }
            }
        }
        return ImmutableList.of(rule);
    }

    private static List<GdlRule> removeBadOrs(List<GdlRule> rules) {
        return rules.stream()
            .flatMap(rule -> removeBadOrs(rule).stream())
            .collect(Immutables.collectList());
    }

    private static List<GdlRule> removeBadOr(GdlRule rule, GdlOr or) {
        List<GdlRule> newRules = Lists.newArrayList();
        for (GdlLiteral disjunct : or.getDisjuncts()) {
            List<GdlLiteral> newBody = Lists.newArrayList(rule.getBody());
            newBody.remove(or);
            newBody.add(disjunct);
            newRules.add(GdlPool.getRule(rule.getHead(), newBody));
        }
        return newRules;
    }
}
