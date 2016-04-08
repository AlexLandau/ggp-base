package org.ggp.base.util.prover.query;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.IntStream;

import javax.annotation.concurrent.Immutable;

import org.ggp.base.util.Immutables;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.model.SentenceForm;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

@Immutable
public class QueryType {
    private final SentenceForm form;
    private final ImmutableSortedSet<Integer> definedSlots;
    /**
     * Each variable that is constrained to be the same as a variable at an
     * earlier index has an entry with its own index as the map entry key and the
     * earliest index with the same value as the map entry value.
     */
    private final ImmutableMap<Integer, Integer> varEqualities;

    private QueryType(SentenceForm form, ImmutableSortedSet<Integer> definedSlots,
            ImmutableMap<Integer, Integer> varEqualities) {
        validateVarEqualities(varEqualities, definedSlots, form.getTupleSize());
        this.form = form;
        this.definedSlots = definedSlots;
        this.varEqualities = varEqualities;
    }

    private static void validateVarEqualities(
            ImmutableMap<Integer, Integer> varEqualities, ImmutableSortedSet<Integer> definedSlots, int tupleSize) {
        for (Entry<Integer, Integer> entry : varEqualities.entrySet()) {
            Preconditions.checkElementIndex(entry.getKey(), tupleSize);
            Preconditions.checkElementIndex(entry.getValue(), tupleSize);
            //Defined slots are not "variables" in the relevant sense
            Preconditions.checkArgument(!definedSlots.contains(entry.getKey()));
            Preconditions.checkArgument(!definedSlots.contains(entry.getValue()));
            //Later index references earlier index
            Preconditions.checkArgument(entry.getKey() > entry.getValue());
            //Value should be the earliest such index, i.e. no chaining
            //(this prevents multiple representations of the same query)
            Preconditions.checkArgument(!varEqualities.containsKey(entry.getValue()));
        }
    }

    public static QueryType create(SentenceForm form, Set<Integer> definedSlots,
            Map<Integer, Integer> varEqualities) {
        return new QueryType(form, ImmutableSortedSet.copyOf(definedSlots),
                ImmutableMap.copyOf(varEqualities));
    }

    public static QueryType createFullyDefined(SentenceForm form) {
        return create(form,
                IntStream.range(0, form.getTupleSize())
                .boxed()
                .collect(Immutables.collectSet()),
                ImmutableMap.of());
    }

    public SentenceForm getForm() {
        return form;
    }

    public SortedSet<Integer> getDefinedSlots() {
        return definedSlots;
    }

    public ImmutableMap<Integer, Integer> getVarEqualities() {
        return varEqualities;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((definedSlots == null) ? 0 : definedSlots.hashCode());
        result = prime * result + ((form == null) ? 0 : form.hashCode());
        result = prime * result
                + ((varEqualities == null) ? 0 : varEqualities.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QueryType other = (QueryType) obj;
        if (definedSlots == null) {
            if (other.definedSlots != null)
                return false;
        } else if (!definedSlots.equals(other.definedSlots))
            return false;
        if (form == null) {
            if (other.form != null)
                return false;
        } else if (!form.equals(other.form))
            return false;
        if (varEqualities == null) {
            if (other.varEqualities != null)
                return false;
        } else if (!varEqualities.equals(other.varEqualities))
            return false;
        return true;
    }

    @Override
    public String toString() {
        //TODO: Memoize?
        List<GdlTerm> tuple = Lists.newArrayList();
        for (int i = 0; i < form.getTupleSize(); i++) {
            if (definedSlots.contains(i)) {
                tuple.add(GdlPool.getConstant("c" + i));
            } else if (varEqualities.containsKey(i)) {
                int earlierIndex = varEqualities.get(i);
                tuple.add(GdlPool.getVariable("?v" + earlierIndex));
            } else {
                tuple.add(GdlPool.getVariable("?v" + i));
            }
        }
        return form.getSentenceFromTuple(tuple).toString();
    }

    public boolean isFullyDefined() {
        return definedSlots.size() == form.getTupleSize();
    }

    /**
     * Returns the arity of the query, which equals the number of
     * non-defined slots it has.
     */
    public int getArity() {
        return form.getTupleSize() - definedSlots.size();
    }

}
