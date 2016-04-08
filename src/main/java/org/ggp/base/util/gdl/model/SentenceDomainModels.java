package org.ggp.base.util.gdl.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class SentenceDomainModels {
    public static enum VarDomainOpts {
        INCLUDE_HEAD,
        BODY_ONLY
    }

    public static Map<GdlVariable, Set<GdlConstant>> getVarDomains(
            GdlRule rule,
            SentenceDomainModel domainModel,
            VarDomainOpts includeHead) {
        // For each positive definition of sentences in the rule, intersect their
        // domains everywhere the variables show up
        Multimap<GdlVariable, Set<GdlConstant>> varDomainsByVar = ArrayListMultimap.create();
        for (GdlLiteral literal : getSentences(rule, includeHead)) {
            if (literal instanceof GdlSentence) {
                GdlSentence sentence = (GdlSentence) literal;
                SentenceForm form = SimpleSentenceForm.create(sentence);
                SentenceFormDomain formWithDomain = domainModel.getDomain(form);

                List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
                for (int i = 0; i < tuple.size(); i++) {
                    GdlTerm term = tuple.get(i);
                    if (term instanceof GdlVariable) {
                        GdlVariable var = (GdlVariable) term;
                        Set<GdlConstant> domain = formWithDomain.getDomainForSlot(i);
                        varDomainsByVar.put(var, domain);
                    }
                }
            }
        }

        Map<GdlVariable, Set<GdlConstant>> varDomainByVar = combineDomains(varDomainsByVar);
        return varDomainByVar;
    }

    public static Iterable<GdlLiteral> getSentences(GdlRule rule, VarDomainOpts includeHead) {
        if (includeHead == VarDomainOpts.INCLUDE_HEAD) {
            return Iterables.concat(ImmutableList.of(rule.getHead()), rule.getBody());
        } else {
            return rule.getBody();
        }
    }

    private static Map<GdlVariable, Set<GdlConstant>> combineDomains(
            Multimap<GdlVariable, Set<GdlConstant>> varDomainsByVar) {
        return ImmutableMap.copyOf(Maps.transformValues(varDomainsByVar.asMap(),
                new Function<Collection<Set<GdlConstant>>, Set<GdlConstant>>() {
            @Override
            public Set<GdlConstant> apply(Collection<Set<GdlConstant>> input) {
                return intersectSets(input);
            }
        }));
    }

    private static <T> Set<T> intersectSets(
            Collection<Set<T>> input) {
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Can't take an intersection of no sets");
        }
        Set<T> result = null;
        for (Set<T> set : input) {
            if (result == null) {
                result = Sets.newHashSet(set);
            } else {
                result.retainAll(set);
            }
        }
        assert result != null;
        return result;
    }

    public static Set<Set<GdlConstant>> getSubdomains(SentenceDomainModel model) {
        // Splits the constants used in tuples in the game's sentences into groups, based on where
        // they could show up with one another.
        Set<Set<GdlConstant>> subdomains = Sets.newHashSet();
        for (SentenceForm form : model.getSentenceForms()) {
            SentenceFormDomain domain = model.getDomain(form);
            for (int i = 0; i < form.getTupleSize(); i++) {
                subdomains.add(Sets.newHashSet(domain.getDomainForSlot(i)));
            }
        }

        return mergeOverlappingSubdomains(subdomains);
    }

    private static Set<Set<GdlConstant>> mergeOverlappingSubdomains(
            Set<Set<GdlConstant>> subdomains) {
        Set<Set<GdlConstant>> mergedSubdomains = Sets.newHashSet();
        while (!subdomains.isEmpty()) {
            Iterator<Set<GdlConstant>> domainItr = subdomains.iterator();
            Set<GdlConstant> curDomain = Sets.newHashSet(domainItr.next());
            domainItr.remove();
            boolean somethingChanged = true;
            while (somethingChanged) {
                somethingChanged = false;

                //Merge in any remaining domains that overlap with the current domain
                domainItr = subdomains.iterator();
                while (domainItr.hasNext()) {
                    Set<GdlConstant> domainToCheck = domainItr.next();
                    if (overlap(domainToCheck, curDomain)) {
                        curDomain.addAll(domainToCheck);
                        domainItr.remove();
                        somethingChanged = true;
                    }
                }
            }
            mergedSubdomains.add(curDomain);
        }
        return mergedSubdomains;
    }

    private static <T> boolean overlap(Set<T> set1,
            Set<T> set2) {
        if (set1.size() < set2.size()) {
            return !Sets.intersection(set1, set2).isEmpty();
        } else {
            return !Sets.intersection(set2, set1).isEmpty();
        }
    }
}
