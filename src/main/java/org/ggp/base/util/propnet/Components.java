package org.ggp.base.util.propnet;

import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Or;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

public class Components {

    public static void addLink(Component parent, Component child) {
        parent.addOutput(child);
        child.addInput(parent);
    }

    public static void removeLink(Component parent, Component child) {
        parent.removeOutput(child);
        child.removeInput(parent);
    }

    /**
     * Returns true iff the component is an AND or OR gate with a
     * single input.
     *
     * Does not currently check if the component has 0 inputs.
     */
    public static boolean isRedundantGate(Component component) {
        if (component instanceof And || component instanceof Or) {
            return component.getInputs().size() == 1;
        }
        return false;
    }

    /**
     * Removes a redundant gate, linking its input and output(s).
     */
    public static void removeRedundantGate(Component component) {
        Preconditions.checkArgument(isRedundantGate(component));
        Component input = component.getSingleInput();
        Set<Component> outputs = component.getOutputs();

        removeLink(input, component);
        for (Component output : outputs) {
            removeLink(component, output);
            addLink(input, output);
        }
    }

    /**
     * Returns the outputs that were unlinked from the component.
     */
    public static Set<Component> removeAllOutputs(Component component) {
        Set<Component> outputs = Sets.newHashSet(component.getOutputs());
        for (Component output : outputs) {
            removeLink(component, output);
        }
        return outputs;
    }

}
