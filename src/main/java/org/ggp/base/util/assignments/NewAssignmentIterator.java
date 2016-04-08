package org.ggp.base.util.assignments;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlVariable;

public interface NewAssignmentIterator extends Iterator<Map<GdlVariable, GdlConstant>> {

    void skipForward(Set<GdlLiteral> unsatisfiableLiterals,
            Map<GdlVariable, GdlConstant> assignment);

}
