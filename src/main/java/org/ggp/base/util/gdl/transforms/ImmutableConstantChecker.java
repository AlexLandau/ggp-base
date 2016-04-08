package org.ggp.base.util.gdl.transforms;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.model.ImmutableSentenceFormModel;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormModel;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

public class ImmutableConstantChecker implements ConstantChecker {
    private final ImmutableSentenceFormModel sentenceModel;
    private final ImmutableSetMultimap<SentenceForm, GdlSentence> sentencesByForm;
    private final ImmutableSet<GdlSentence> allSentences;

    private ImmutableConstantChecker(ImmutableSentenceFormModel sentenceModel,
            ImmutableSetMultimap<SentenceForm, GdlSentence> sentencesByForm) {
        Preconditions.checkArgument(sentenceModel.getConstantSentenceForms().containsAll(sentencesByForm.keySet()));
        this.sentenceModel = sentenceModel;
        this.sentencesByForm = sentencesByForm;
        ImmutableSet.Builder<GdlSentence> allSentencesBuilder = ImmutableSet.builder();
        for (GdlSentence sentence : sentencesByForm.values()) {
            allSentencesBuilder.add(sentence);
        }
        this.allSentences = allSentencesBuilder.build();
    }

    /**
     * Returns an ImmutableConstantChecker with contents identical to the
     * given ConstantChecker.
     *
     * May not actually make a copy if the input is immutable.
     */
    public static ImmutableConstantChecker copyOf(ConstantChecker other) {
        if (other instanceof ImmutableConstantChecker) {
            return (ImmutableConstantChecker) other;
        }

        SentenceFormModel model = other.getSentenceFormModel();
        SetMultimap<SentenceForm, GdlSentence> sentencesByForm = HashMultimap.create();
        for (SentenceForm form : other.getConstantSentenceForms()) {
            sentencesByForm.putAll(form, other.getTrueSentences(form));
        }
        return new ImmutableConstantChecker(ImmutableSentenceFormModel.copyOf(model),
                ImmutableSetMultimap.copyOf(sentencesByForm));
    }

    public static ImmutableConstantChecker create(SentenceFormModel sentenceModel,
            Multimap<SentenceForm, GdlSentence> sentencesByForm) {
        return new ImmutableConstantChecker(ImmutableSentenceFormModel.copyOf(sentenceModel),
                ImmutableSetMultimap.copyOf(sentencesByForm));
    }

    @Override
    public boolean hasConstantForm(GdlSentence sentence) {
        for (SentenceForm form : getConstantSentenceForms()) {
            if (form.matches(sentence)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isConstantForm(SentenceForm form) {
        return sentenceModel.getConstantSentenceForms().contains(form);
    }

    @Override
    public ImmutableSet<GdlSentence> getTrueSentences(SentenceForm form) {
        return sentencesByForm.get(form);
    }

    @Override
    public ImmutableSet<SentenceForm> getConstantSentenceForms() {
        return sentenceModel.getConstantSentenceForms();
    }

    @Override
    public boolean isTrueConstant(GdlSentence sentence) {
        return allSentences.contains(sentence);
    }

    @Override
    public SentenceFormModel getSentenceFormModel() {
        return sentenceModel;
    }
}
