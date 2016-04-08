package org.ggp.base.util.propnet.architecture.components;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Or class is designed to represent logical OR gates.
 */
@SuppressWarnings("serial")
public final class Or extends Component
{
    /**
     * Returns true if and only if at least one of the inputs to the or is true.
     *
     * @see org.ggp.base.util.propnet.architecture.Component#getValue()
     */
    @Override
    public boolean getValue()
    {
        for ( Component component : getInputs() )
        {
            if ( component.getValue() )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.ggp.base.util.propnet.architecture.Component#toString()
     */
    @Override
    public String toString()
    {
        return toDot("ellipse", "grey", "OR");
    }

    @Override
    public String getShortName() {
        StringBuilder sb = new StringBuilder();
        sb.append("or(");
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