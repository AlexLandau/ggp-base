package org.ggp.base.util.gdl.grammar;

import org.ggp.base.util.ruleengine.RuleEngineMove;

@SuppressWarnings("serial") //abstract
public abstract class GdlTerm extends Gdl implements RuleEngineMove
{

    @Override
    public abstract boolean isGround();

    public abstract GdlSentence toSentence();

    @Override
    public abstract String toString();

}
