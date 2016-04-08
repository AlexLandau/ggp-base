package org.ggp.base.util.gdl.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

/**
 * A {@link SentenceFormDomain} implementation that stores every possible
 * version of the sentence.
 */
public class FullSentenceFormDomain implements SentenceFormDomain {
    private final SentenceForm form;
    private final ImmutableList<GdlSentence> domain;
    private final ImmutableList<ImmutableSet<GdlConstant>> domainsForSlots;

    public FullSentenceFormDomain(
            SentenceForm form,
            ImmutableList<GdlSentence> domain,
            ImmutableList<ImmutableSet<GdlConstant>> domainsForSlots) {
        this.form = form;
        this.domain = domain;
        this.domainsForSlots = domainsForSlots;
    }

    public static FullSentenceFormDomain create(SentenceForm form, Collection<GdlSentence> domain) {
        List<ImmutableSet.Builder<GdlConstant>> domainsForSlotsBuilder = Lists.newArrayList();
        for (int i = 0; i < form.getTupleSize(); i++) {
            domainsForSlotsBuilder.add(ImmutableSet.<GdlConstant>builder());
        }
        for (GdlSentence sentence : domain) {
            assert form.matches(sentence);
            List<GdlConstant> tuple = GdlUtils.getTupleFromGroundSentence(sentence);
            if (tuple.size() != form.getTupleSize()) {
                throw new IllegalArgumentException();
            }
            for (int i = 0; i < tuple.size(); i++) {
                GdlConstant constant = tuple.get(i);
                domainsForSlotsBuilder.get(i).add(constant);
            }
        }
        ImmutableList.Builder<ImmutableSet<GdlConstant>> domainsForSlots = ImmutableList.builder();
        for (ImmutableSet.Builder<GdlConstant> builder : domainsForSlotsBuilder) {
            domainsForSlots.add(builder.build());
        }
        return new FullSentenceFormDomain(form, ImmutableList.copyOf(domain),
                domainsForSlots.build());
    }

    @Override
    public Iterator<GdlSentence> iterator() {
        return domain.iterator();
    }

    @Override
    public SentenceForm getForm() {
        return form;
    }

    @Override
    public Set<GdlConstant> getDomainForSlot(int slotIndex) {
        Preconditions.checkElementIndex(slotIndex, form.getTupleSize());
        return domainsForSlots.get(slotIndex);
    }

    private static final Function<Collection<GdlConstant>, Set<GdlConstant>> CAST_FUNCTION =
            new Function<Collection<GdlConstant>, Set<GdlConstant>>() {
        @Override
        public Set<GdlConstant> apply(Collection<GdlConstant> input) {
            return (Set<GdlConstant>) input;
        }
    };

    //TODO: Memoize?
    @Override
    public Map<GdlConstant, Set<GdlConstant>> getDomainsForSlotGivenValuesOfOtherSlot(
            int slotOfInterest, int inputSlot) {
        SetMultimap<GdlConstant, GdlConstant> result = HashMultimap.create();
        for (GdlSentence sentence : domain) {
            List<GdlConstant> tuple = GdlUtils.getTupleFromGroundSentence(sentence);
            GdlConstant inputValue = tuple.get(inputSlot);
            GdlConstant targetValue = tuple.get(slotOfInterest);
            result.put(inputValue, targetValue);
        }
        return Maps.transformValues(result.asMap(), CAST_FUNCTION);
    }

    @Override
    public int getDomainSize() {
        return domain.size();
    }
}
