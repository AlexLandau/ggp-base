package org.ggp.base.util.assignments;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.ggp.base.util.Immutables;
import org.ggp.base.util.gdl.grammar.GdlConstant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

@NotThreadSafe
public class DependentAssignmentStrategy implements AssignmentStrategy {
    private final ImmutableList<Integer> dependentIndices;
    private final ImmutableList<Integer> definedIndices;

    //TODO: Something better and trie-based?
    private final Node contents;

    public DependentAssignmentStrategy(ImmutableList<Integer> dependentIndices,
            ImmutableList<Integer> definedIndices, Node contents) {
        this.dependentIndices = dependentIndices;
        this.definedIndices = definedIndices;
        this.contents = contents;
    }

    public static AssignmentStrategy create(List<Integer> dependentIndices,
            List<Integer> definedIndices,
            Map<List<GdlConstant>, ? extends Collection<List<GdlConstant>>> contents) {
        if (dependentIndices.isEmpty()) {
            return MultipleAssignmentStrategy.create(definedIndices,
                    contents.get(ImmutableList.of()));
        }
        Node contentsNode = toNode(contents);
        return new DependentAssignmentStrategy(
                ImmutableList.copyOf(dependentIndices),
                ImmutableList.copyOf(definedIndices),
                contentsNode);
    }

    private static Node toNode(
            Map<List<GdlConstant>, ? extends Collection<List<GdlConstant>>> contents) {
        Node rootNode = new BranchNode();
        for (Entry<List<GdlConstant>, ? extends Collection<List<GdlConstant>>> entry : contents.entrySet()) {
            List<GdlConstant> inputs = entry.getKey();
            Collection<List<GdlConstant>> partialAssignments = entry.getValue();
            Node curNode = rootNode;
            for (int i = 0; i < inputs.size(); i++) {
                GdlConstant input = inputs.get(i);
                if (i == inputs.size() - 1) {
                    curNode.createLeafChild(input, partialAssignments);
                } else {
                    curNode = curNode.getBranchChildConstructively(input);
                }
            }
        }
        return rootNode;
    }

    @Override
    public List<Integer> getDependentIndices() {
        return dependentIndices;
    }

    @Override
    public List<Integer> getDefinedIndices() {
        return definedIndices;
    }

    @Override
    public List<? extends List<GdlConstant>> getPartialAssignments(
            List<GdlConstant> inputs) {
        Node curNode = contents;
        for (GdlConstant input : inputs) {
            curNode = curNode.getChildForInput(input);
            if (curNode == null) {
                return ImmutableList.of();
            }
        }
        return curNode.getPartialAssignments();
    }

    @Override
    public int getRejectedIndex(List<GdlConstant> inputs) {
        Node curNode = contents;
        int inputIndex = 0;
        for (GdlConstant input : inputs) {
            curNode = curNode.getChildForInput(input);
            if (curNode == null) {
                return dependentIndices.get(inputIndex);
            }
            inputIndex++;
        }
        return NO_INDEX_REJECTED;
    }

    private static interface Node {
        @Nullable Node getChildForInput(GdlConstant input);
        Node getBranchChildConstructively(GdlConstant input);
        void createLeafChild(GdlConstant input, Collection<List<GdlConstant>> partialAssignments);
        List<? extends List<GdlConstant>> getPartialAssignments();
    }
    private static class BranchNode implements Node {
        private final Map<GdlConstant, Node> children = Maps.newHashMap();
        @Override
        public Node getChildForInput(GdlConstant input) {
            return children.get(input);
        }
        @Override
        public List<? extends List<GdlConstant>> getPartialAssignments() {
            throw new UnsupportedOperationException();
        }
        @Override
        public Node getBranchChildConstructively(GdlConstant input) {
            Node result = children.get(input);
            if (result != null) {
                return result;
            }
            result = new BranchNode();
            children.put(input, result);
            return result;
        }
        @Override
        public void createLeafChild(GdlConstant input,
                Collection<List<GdlConstant>> partialAssignments) {
            children.put(input, new LeafNode(partialAssignments.stream()
                    .map(ImmutableList::copyOf)
                    .collect(Immutables.collectList())));
        }
    }
    private static class LeafNode implements Node {
        private final ImmutableList<ImmutableList<GdlConstant>> partialAssignments;
        public LeafNode(
                ImmutableList<ImmutableList<GdlConstant>> partialAssignments) {
            this.partialAssignments = partialAssignments;
        }
        @Override
        public Node getChildForInput(GdlConstant input) {
            throw new UnsupportedOperationException();
        }
        @Override
        public List<? extends List<GdlConstant>> getPartialAssignments() {
            return partialAssignments;
        }
        @Override
        public Node getBranchChildConstructively(GdlConstant input) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void createLeafChild(GdlConstant input,
                Collection<List<GdlConstant>> partialAssignments) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        return "DependentAssignmentStrategy [From " + dependentIndices + " to " + definedIndices + "]";
    }
}
