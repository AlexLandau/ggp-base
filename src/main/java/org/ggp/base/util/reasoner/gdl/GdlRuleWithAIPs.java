package org.ggp.base.util.reasoner.gdl;

import java.util.Map;

import org.ggp.base.util.assignments.LegacyAssignmentIterationPlanFactory;
import org.ggp.base.util.assignments.NewAssignmentIterationPlan;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.assignments.FunctionInfo;
import org.ggp.base.util.gdl.transforms.ConstantChecker;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class GdlRuleWithAIPs {
    private final GdlRule rule;
    private final NewAssignmentIterationPlan aipForNothing;
    private final NewAssignmentIterationPlan aipForHead;
    private final ImmutableMap<GdlLiteral, NewAssignmentIterationPlan> aipForBodyLiteral;

    public GdlRuleWithAIPs(GdlRule rule,
            NewAssignmentIterationPlan aipForNothing,
            NewAssignmentIterationPlan aipForHead,
            ImmutableMap<GdlLiteral, NewAssignmentIterationPlan> aipForBodyLiteral) {
        this.rule = rule;
        this.aipForNothing = aipForNothing;
        this.aipForHead = aipForHead;
        this.aipForBodyLiteral = aipForBodyLiteral;
    }

    public GdlRule getRule() {
        return rule;
    }

    public NewAssignmentIterationPlan getAipWithNothingSpecified() {
        return aipForNothing;
    }

    public NewAssignmentIterationPlan getAipWithHeadSpecified() {
        return aipForHead;
    }

    public NewAssignmentIterationPlan getAipWithBodyLiteralSpecified(GdlLiteral literal) {
        return aipForBodyLiteral.get(literal);
    }

    @Override
    public String toString() {
        return "GdlRuleWithAIPs [rule=" + rule + ", aipForNothing="
                + aipForNothing + ", aipForHead=" + aipForHead
                + ", aipForBodyLiteral=" + aipForBodyLiteral + "]";
    }

    public static GdlRuleWithAIPs createFrom(GdlRule rule, SentenceDomainModel domainModel,
            ConstantChecker constantChecker, Map<SentenceForm, FunctionInfo> functionInfoMap) {
        //TODO: Use separate plans for different situations
        NewAssignmentIterationPlan aip = LegacyAssignmentIterationPlanFactory.create(rule, domainModel, constantChecker, functionInfoMap);
        Map<GdlLiteral, NewAssignmentIterationPlan> aipsForBodyLiterals = Maps.newHashMap();
        for (GdlLiteral conjunct : rule.getBody()) {
            aipsForBodyLiterals.put(conjunct, aip);
        }
        return new GdlRuleWithAIPs(rule, aip, aip, ImmutableMap.copyOf(aipsForBodyLiterals));
    }
}
