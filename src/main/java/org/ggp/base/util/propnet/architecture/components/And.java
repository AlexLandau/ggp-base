package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The And class is designed to represent logical AND gates.
 */
@SuppressWarnings("serial")
public final class And extends Component
{
    /**
     * Returns true if and only if every input to the and is true.
     *
     * @see org.ggp.base.util.propnet.architecture.Component#getValue()
     */
    @Override
    public boolean getValue()
    {
        for ( Component component : getInputs() )
        {
            if ( !component.getValue() )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @see org.ggp.base.util.propnet.architecture.Component#toString()
     */
    @Override
    public String toString()
    {
        return toDot("invhouse", "grey", "AND");
    }

    @Override
    public String getShortName() {
        StringBuilder sb = new StringBuilder();
        sb.append("and(");
        for (Component component : getInputs()) {
            sb.append(component.getShortName()).append(", ");
        }
        if (getInputs().size() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
        return sb.toString();
    }

}
