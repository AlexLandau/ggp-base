package org.ggp.base.util.gdl.transforms;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

//TODO: Maybe put this inside TransformedGame?
//TODO: Put this everywhere we currently need to use transforms.
//TODO: Move transforms that currently live in the Alloy repo into here.
public enum Transform {
    CLEAN(GdlCleaner::run),
    REMOVE_ORS(DeORer::run),
    REPLACE_FUNCTION_VALUED_VARIABLES(VariableConstrainer::replaceFunctionValuedVariables, CLEAN, REMOVE_ORS),
    ISOLATE_CONDENSATIONS(CondensationIsolator::run, REPLACE_FUNCTION_VALUED_VARIABLES),
    MOVE_DISTINCTS_AND_NOTS(DistinctAndNotMover::run, REMOVE_ORS),
    /**
     * Rewrites the game's rules so that each rule has at most two literals in its body.
     */
    DUALIZE_CONJUNCTS(ConjunctDualizer::apply, CLEAN, REMOVE_ORS, MOVE_DISTINCTS_AND_NOTS),
    /**
     * Turns unchanging base propositions into non-base propositions.
     *
     * TODO: Think of a more descriptive name.
     */
    RELATIONIZER(Relationizer::run, REPLACE_FUNCTION_VALUED_VARIABLES),
    ;
    private final Transformer transformer;
    private final ImmutableSet<Transform> prerequisites;

    // This allows InterruptedException to be thrown, unlike
    /*package-private*/ static interface Transformer {
        List<Gdl> transform(List<Gdl> rules) throws InterruptedException;
    }

    private Transform(Transformer transformer, Transform... prerequisites) {
        this.transformer = transformer;
        this.prerequisites = ImmutableSet.copyOf(prerequisites);
    }

    public Set<Transform> getPrerequisiteTransforms() {
        return prerequisites;
    }

    public TransformedGame applyTo(List<Gdl> rules) throws InterruptedException {
        ImmutableSet<Transform> alreadyApplied = ImmutableSet.of();
        ImmutableList<Gdl> originalRules = ImmutableList.copyOf(rules);
        return applyTo(originalRules, rules, alreadyApplied);
    }

    private TransformedGame applyTo(List<Gdl> originalRules, List<Gdl> rules, ImmutableSet<Transform> alreadyApplied)
            throws InterruptedException {
        List<Transform> toApply = getTransformsToApplyFirst(alreadyApplied);
        for (Transform prerequisite : toApply) {
            rules = prerequisite.getTransformer().transform(rules);
        }
        rules = transformer.transform(rules);
        Set<Transform> transformsUsed = Sets.newHashSet(toApply);
        transformsUsed.add(this);
        return TransformedGame.create(transformsUsed, originalRules, rules);
    }

    private List<Transform> getTransformsToApplyFirst(Set<Transform> alreadyApplied) {
        // Making this a set makes this easier for various reasons, but we also want the
        // property that the order we put things into the set is preserved.
        LinkedHashSet<Transform> ordering = Sets.newLinkedHashSet();
        for (Transform prereq : prerequisites) {
            if (!alreadyApplied.contains(prereq) && !ordering.contains(prereq)) {
                ordering.addAll(prereq.getTransformsToApplyFirst(Sets.union(alreadyApplied, ordering)));
                ordering.add(prereq);
            }
        }
        return Lists.newArrayList(ordering);
    }

    public TransformedGame applyTo(TransformedGame game) throws InterruptedException {
        return applyTo(game.getOriginalRules(), game.getTransformedRules(), game.getTransformsApplied());
    }

    /*package-private*/ Transformer getTransformer() {
        return transformer;
    }
}
