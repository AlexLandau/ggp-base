package org.ggp.base.util.gdl.grammar;

public final class GdlVariable extends GdlTerm
{
    private static final long serialVersionUID = 1L;

    private final String name;
    private final transient int id;

    GdlVariable(String name, int id)
    {
        this.name = name.intern();
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public int getId()
    {
        return id;
    }

    @Override
    public boolean isGround()
    {
        return false;
    }

    @Override
    public GdlSentence toSentence()
    {
        throw new RuntimeException("Unable to convert a GdlVariable to a GdlSentence!");
    }

    @Override
    public String toString()
    {
        return name;
    }

}
