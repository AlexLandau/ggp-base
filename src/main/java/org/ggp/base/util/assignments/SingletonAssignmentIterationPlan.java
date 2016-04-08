package org.ggp.base.util.assignments;

import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class SingletonAssignmentIterationPlan implements NewAssignmentIterationPlan {

    public static NewAssignmentIterationPlan create() {
        return new SingletonAssignmentIterationPlan();
    }

    @Override
    public NewAssignmentIterator getIterator() {
        return new SingletonAssignmentIterator();
    }

    public class SingletonAssignmentIterator implements NewAssignmentIterator {
        private boolean done = false;

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public Map<GdlVariable, GdlConstant> next() {
            Preconditions.checkState(!done);
            done = true;
            return ImmutableMap.of();
        }

        @Override
        public void skipForward(Set<GdlLiteral> unsatisfiableLiterals,
                Map<GdlVariable, GdlConstant> assignment) {
            //Do nothing
        }

    }

}
