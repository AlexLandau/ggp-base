package org.ggp.base.util.statemachine.exceptions;

import org.ggp.base.util.ruleengine.RuleEngineState;
import org.ggp.base.util.statemachine.Role;

@SuppressWarnings("serial")
public final class GoalDefinitionException extends Exception
{

    private final Role role;
    private final RuleEngineState<?, ?> state;

    public GoalDefinitionException(RuleEngineState<?, ?> state, Role role, Throwable cause)
    {
        super(cause);
        this.state = state;
        this.role = role;
    }

    public GoalDefinitionException(RuleEngineState<?, ?> state, Role role)
    {
        this.state = state;
        this.role = role;
    }

    public GoalDefinitionException(RuleEngineState<?, ?> state) {
        this.state = state;
        this.role = null;
    }

    public Role getRole()
    {
        return role;
    }

    public RuleEngineState<?, ?> getState()
    {
        return state;
    }

    @Override
    public String getMessage() {
        return toString();
    }

    @Override
    public String toString()
    {
        //TODO: Translate state?
        return "Goal is poorly defined for " + role + " in " + state;
    }

}
