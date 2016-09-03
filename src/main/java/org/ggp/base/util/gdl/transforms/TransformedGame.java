package org.ggp.base.util.gdl.transforms;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class TransformedGame {
    private final ImmutableSet<Transform> transformsApplied;
    private final ImmutableList<Gdl> originalRules;
    private final ImmutableList<Gdl> transformedRules;

    private TransformedGame(ImmutableSet<Transform> transformsApplied, ImmutableList<Gdl> originalRules,
            ImmutableList<Gdl> transformedRules) {
        this.transformsApplied = transformsApplied;
        this.originalRules = originalRules;
        this.transformedRules = transformedRules;
    }

    public static TransformedGame create(Set<Transform> transformsApplied, List<Gdl> originalRules,
            List<Gdl> transformedRules) {
        return new TransformedGame(ImmutableSet.copyOf(transformsApplied),
                ImmutableList.copyOf(originalRules),
                ImmutableList.copyOf(transformedRules));
    }

    public ImmutableSet<Transform> getTransformsApplied() {
        return transformsApplied;
    }

    public ImmutableList<Gdl> getOriginalRules() {
        return originalRules;
    }

    public ImmutableList<Gdl> getTransformedRules() {
        return transformedRules;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((originalRules == null) ? 0 : originalRules.hashCode());
        result = prime * result + ((transformedRules == null) ? 0 : transformedRules.hashCode());
        result = prime * result + ((transformsApplied == null) ? 0 : transformsApplied.hashCode());
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
        TransformedGame other = (TransformedGame) obj;
        if (originalRules == null) {
            if (other.originalRules != null)
                return false;
        } else if (!originalRules.equals(other.originalRules))
            return false;
        if (transformedRules == null) {
            if (other.transformedRules != null)
                return false;
        } else if (!transformedRules.equals(other.transformedRules))
            return false;
        if (transformsApplied == null) {
            if (other.transformsApplied != null)
                return false;
        } else if (!transformsApplied.equals(other.transformsApplied))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TransformedGame [transformsApplied=" + transformsApplied + ", originalRules=" + originalRules
                + ", transformedRules=" + transformedRules + "]";
    }
}
