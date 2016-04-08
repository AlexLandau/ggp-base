package org.ggp.base.util.gdl.grammar;

@SuppressWarnings("serial") //abstract
public abstract class GdlLiteral extends Gdl
{

    @Override
    public abstract boolean isGround();

    @Override
    public abstract String toString();

}
