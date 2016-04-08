package org.ggp.base.util.ruleengine;

public interface RuleEngineMove {
    /**
     * Move types should implement hashCode so they can be put in sets and maps
     * and deduplicated safely.
     */
    @Override
    public int hashCode();
    /**
     * Move types should implement hashCode so they can be put in sets and maps
     * and deduplicated safely.
     */
    @Override
    public boolean equals(Object obj);
}
