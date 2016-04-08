package org.ggp.base.util.gdl.grammar;

public final class GdlConstant extends GdlTerm
{
    private static final long serialVersionUID = 1L;

    private final String value;
    private final transient int id;

    GdlConstant(String value, int id)
    {
        this.value = value.intern();
        this.id = id;
    }

    public String getValue()
    {
        return value;
    }

    public int getId()
    {
        return id;
    }

    @Override
    public boolean isGround()
    {
        return true;
    }

    @Override
    public GdlSentence toSentence()
    {
        return GdlPool.getProposition(this);
    }

    @Override
    public String toString()
    {
        return value;
    }

}
