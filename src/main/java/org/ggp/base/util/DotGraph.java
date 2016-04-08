package org.ggp.base.util;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

@Immutable
public class DotGraph {
    private final ImmutableSet<Node> nodes;
    private final ImmutableSetMultimap<Node, Node> edges;

    private DotGraph(ImmutableSet<Node> nodes,
            ImmutableSetMultimap<Node, Node> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    private static class Node {
        private final String label;

        private Node(String label) {
            this.label = label;
        }

        public static Node create(String label) {
            return new Node(label);
        }

        public String getId() {
            return "\"@" + Integer.toHexString(hashCode()) + "\"";
        }

        public String getShape() {
            return "circle";
        }

        public String getColor() {
            return "white";
        }

        public String getLabel() {
            return label;
        }
    }

    @NotThreadSafe
    public static class DotGraphBuilder<T> {
        private final Map<T, Node> nodes = Maps.newHashMap();
        private final SetMultimap<T, T> edges = HashMultimap.create();

        private DotGraphBuilder() {
        }

        public void addNode(T object, String nodeLabel) {
            Preconditions.checkArgument(!nodes.containsKey(object));
            nodes.put(object, Node.create(nodeLabel));
        }

        public void addEdges(SetMultimap<T, T> dependencyGraph) {
            for (Entry<T, T> entry : dependencyGraph.entries()) {
                addEdge(entry.getKey(), entry.getValue());
            }
        }

        public void addEdge(T source, T destination) {
            edges.put(source, destination);
        }

        public DotGraph build() {
            ImmutableSet<Node> graphNodes = ImmutableSet.copyOf(nodes.values());
            ImmutableSetMultimap.Builder<Node, Node> graphEdges = ImmutableSetMultimap.builder();
            for (Entry<T, T> entry : edges.entries()) {
                graphEdges.put(nodes.get(entry.getKey()), nodes.get(entry.getValue()));
            }
            return new DotGraph(graphNodes,
                    graphEdges.build());
        }

    }

    public static <T> DotGraphBuilder<T> builder() {
        return new DotGraphBuilder<T>();
    }

    public String print() {
        //TODO: Implement
        StringBuilder sb = new StringBuilder();
        sb.append("digraph customGraph\n")
        .append("{\n");
        for (Node node : nodes) {
            sb.append("\t")
            .append(node.getId())
            .append("[shape=").append(node.getShape())
            .append(", style=filled, fillcolor=").append(node.getColor())
            .append(", label=\"").append(node.getLabel()).append("\"];\n");
        }
        for (Entry<Node, Node> edge : edges.entries()) {
            sb.append("\t")
            .append(edge.getKey().getId())
            .append("->")
            .append(edge.getValue().getId())
            .append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
