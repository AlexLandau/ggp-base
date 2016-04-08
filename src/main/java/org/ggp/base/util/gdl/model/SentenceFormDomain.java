package org.ggp.base.util.gdl.model;

import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;

/**
 * A SentenceFormDomain contains information about the possible
 * sentences of a particular sentence form within a game. In other
 * words, it captures information about which constants can be
 * in which positions in the SentenceForm.
 */
public interface SentenceFormDomain extends Iterable<GdlSentence> {
    /**
     * Returns the SentenceForm associated with this domain.
     */
    SentenceForm getForm();

    /**
     * Returns a set containing every constant that can appear in
     * the given slot index in the sentence form.
     */
    Set<GdlConstant> getDomainForSlot(int slotIndex);

    /**
     * Returns a map from possible values of slot inputSlot to the associated possible values of
     * slotOfInterest.
     *
     * Note: For efficiency's sake, this is not a SetMultimap; see the implementation
     * in {@link CartesianSentenceFormDomain}.
     */
    Map<GdlConstant, Set<GdlConstant>> getDomainsForSlotGivenValuesOfOtherSlot(int slotOfInterest, int inputSlot);

    /**
     * Returns the number of sentences that will be returned by this domain's iterator.
     */
    int getDomainSize();
}
