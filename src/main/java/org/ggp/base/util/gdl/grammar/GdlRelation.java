package org.ggp.base.util.gdl.grammar;

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class GdlRelation extends GdlSentence
{
    private static final long serialVersionUID = 1L;

    private final ImmutableList<GdlTerm> body;
    private transient Boolean ground;
    private final GdlConstant name;
    private transient volatile String cachedString;

    GdlRelation(GdlConstant name, ImmutableList<GdlTerm> body)
    {
        this.name = name;
        this.body = body;
        ground = null;
    }

    @Override
    public int arity()
    {
        return body.size();
    }

    private boolean computeGround()
    {
        for (GdlTerm term : body)
        {
            if (!term.isGround())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public GdlTerm get(int index)
    {
        return body.get(index);
    }

    @Override
    public GdlConstant getName()
    {
        return name;
    }

    @Override
    public boolean isGround()
    {
        if (ground == null)
        {
            ground = computeGround();
        }

        return ground;
    }

    @Override
    public String toString()
    {
        String theString = this.cachedString;
        if (theString == null) {
            StringBuilder sb = new StringBuilder();

            sb.append("( " + name + " ");
            for (GdlTerm term : body)
            {
                sb.append(term + " ");
            }
            sb.append(")");

            theString = sb.toString();
            this.cachedString = theString;
        }
        return theString;
    }

    @Override
    public GdlTerm toTerm()
    {
        return GdlPool.getFunction(name, body);
    }

    @Override
    public List<GdlTerm> getBody()
    {
        return body;
    }

    @Override
    public GdlSentence withName(GdlConstant newName) {
        return GdlPool.getRelation(newName, body);
    }

}
