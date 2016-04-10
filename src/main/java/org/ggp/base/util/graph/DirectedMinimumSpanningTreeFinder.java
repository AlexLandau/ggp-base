package org.ggp.base.util.graph;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import org.ggp.base.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class DirectedMinimumSpanningTreeFinder {

    /*
     * How do we want to specify the graph?
     *
     * Probably a Table for the edge weights?
     *
     * Or do we want our own object types? Maybe just internally?
     *
     * The tricky parts will be graph manipulation and inventing new objects
     * to use as nodes...
     *
     * We can have our own "Node" objects and a map from input objects to
     * nodes (?)
     *
     * This also means a Node could reference ____?
     *
     * Okay... main graph problem solver is going to need the set of nodes
     * as an input. That means we can get away with leaving in references to
     * outdated nodes (from cycles) in the Table or whatever.
     *
     * Return value is going to be something like the set of edges. Edges need
     * to at minimum know their associated weights, and should probably be able
     * to indicate provenance from earlier cycles.
     */
    /**
     * Input format: Map from pair of (parent, child) to minimum weight for that edge
     *
     * @return Map from children to parents
     */
    public static <T> Map<T, T> findDmst(Set<T> allValues, T root, Map<Pair<T, T>, Double> weights) {
        //		System.out.println("allValues: " + allValues);
        //		System.out.println("root: " + root);
        //		System.out.println("weights: " + weights);
        BiMap<T, Node> nodesMap = createNodes(allValues);
        Set<Node> allNodes = Sets.newHashSet(nodesMap.inverse().keySet());
        Node rootNode = nodesMap.get(root);
        Set<Edge> edges = toInitialEdges(weights, nodesMap);
        SetMultimap<Node, Edge> edgesByChildren = HashMultimap.create();
        SetMultimap<Node, Edge> edgesByParents = HashMultimap.create();
        addToMaps(edges, edgesByChildren, edgesByParents);

        Map<Node, Edge> edgesToUse = findDmst(allNodes, rootNode, edgesByChildren, edgesByParents);
        //		System.out.println("Edges to use: " + edgesToUse);

        Map<T, T> result = Maps.newHashMap();
        for (Edge edge : edgesToUse.values()) {
            T childValue = nodesMap.inverse().get(edge.getChild());
            T parentValue = nodesMap.inverse().get(edge.getParent());
            //			System.out.println("Including edge from " + parentValue + " to " + childValue);
            T previousValue = result.put(childValue, parentValue);
            Preconditions.checkState(previousValue == null);
        }
        //TODO: Check that there's no cycle
        return result;
    }

    private static <T> BiMap<T, Node> createNodes(Set<T> allValues) {
        BiMap<T, Node> nodesMap = HashBiMap.create();
        for (T value : allValues) {
            nodesMap.put(value, new Node(value.toString()));
        }
        return nodesMap;
    }

    private static <T> Set<Edge> toInitialEdges(Map<Pair<T, T>, Double> weights, BiMap<T, Node> nodesMap) {
        Set<Edge> edges = Sets.newHashSet();

        for (Entry<Pair<T, T>, Double> entry : weights.entrySet()) {
            T parent = entry.getKey().left;
            T child = entry.getKey().right;
            double weight = entry.getValue();
            Node parentNode = nodesMap.get(parent);
            Node childNode = nodesMap.get(child);
            edges.add(new Edge(parentNode, childNode, weight, null));
        }
        return edges;
    }

    //Write the recursive function first, then ___, I guess
    //TODO: Pass in Edge objects
    //Maybe as target of edgeWeights table?
    //TODO: Verify which direction the "edgeWeights" (should rename) table should go
    //We currently go child -> parent -> edge in the table...
    //The parent part MIGHT not be necessary? (We might be able to just use a map)
    //TODO: Consider replacing table with two maps
    //NOTE: Map is from nodes to the incoming edge to that node, which should always be unique
    private static Map<Node, Edge> findDmst(Set<Node> nodes, Node root,
            SetMultimap<Node, Edge> edgesByChildren, SetMultimap<Node, Edge> edgesByParents) {
        Map<Node, Edge> markedEdges = getInitialEdgeSet(nodes, root, edgesByChildren);

        @Nullable Map<Node, Edge> edgesInCycle = findCycle(markedEdges);
        //Base case: No cycle exists
        if (edgesInCycle == null) {
            return markedEdges;
        }
        Preconditions.checkState(!edgesInCycle.containsKey(root));
        //		System.out.println("Edges in cycle: " + edgesInCycle);

        //Replace the nodes in the cycle with a new node
        nodes.removeAll(edgesInCycle.keySet());
        Node cycleNode = new Node("cycle node");
        nodes.add(cycleNode);
        //Replace the edges related to the cycle with edges related to the cycle node
        //TODO: Replace/add edges
        Set<Edge> newEdges = createCycleNodeEdges(edgesInCycle, cycleNode, edgesByChildren, edgesByParents);
        //		edges.addAll(newEdges); //?
        addToMaps(newEdges, edgesByChildren, edgesByParents);
        Map<Node, Edge> recursiveMarkedEdges = findDmst(nodes, root, edgesByChildren, edgesByParents);
        //		System.out.println("Edges in cycle: " + edgesInCycle);
        //		System.out.println("Immediate recursiveMarkedEdges: " + recursiveMarkedEdges);
        //Restore the "nodes" and "edges"
        nodes.remove(cycleNode);
        nodes.addAll(edgesInCycle.keySet());
        //		edges.removeAll(newEdges); //?
        //TODO: Remove from table? (Not strictly necessary)

        //Fix up the recursive marked edges and return as our solution
        //1) Add all the edges from the cycle
        for (Node nodeInCycle : edgesInCycle.keySet()) {
            Preconditions.checkState(!recursiveMarkedEdges.containsKey(nodeInCycle));
        }
        recursiveMarkedEdges.putAll(edgesInCycle);
        //		System.out.println("recursiveMarkedEdges after adding edges in cycle: " + recursiveMarkedEdges);
        //2) Find the edge into the cycle node; replace it with its source edge.
        //Also, remove the edge in the cycle ____.
        @Nullable Edge edgeIntoCycleNode = recursiveMarkedEdges.get(cycleNode);
        if (edgeIntoCycleNode != null) { //should always be the case with this algorithm...
            recursiveMarkedEdges.remove(edgeIntoCycleNode.getChild());
            Edge replacement = edgeIntoCycleNode.getSource();
            recursiveMarkedEdges.put(replacement.getChild(), replacement);
        }
        //		System.out.println("recursiveMarkedEdges after step 2: " + recursiveMarkedEdges);
        //3) Find the edges coming out of the cycle node; replace them with their source edges.
        for (Edge edge : Lists.newArrayList(recursiveMarkedEdges.values())) {
            if (edge.getParent() == cycleNode) {
                recursiveMarkedEdges.remove(edge.getChild());
                Edge replacement = edge.getSource();
                recursiveMarkedEdges.put(replacement.getChild(), replacement);
            }
        }

        //		System.out.println("recursiveMarkedEdges to return: " + recursiveMarkedEdges);
        return recursiveMarkedEdges;
    }

    //	private @Nullable Edge findEdgeIntoNode(Map<Node, Edge> recursiveMarkedEdges, Node node) {
    //		for (Edge edge : recursiveMarkedEdges) {
    //			if (edge.getChild() == node) {
    //				return edge;
    //			}
    //		}
    //		return null;
    //	}

    private static void addToMaps(Set<Edge> newEdges,
            SetMultimap<Node, Edge> edgesByChildren,
            SetMultimap<Node, Edge> edgesByParents) {
        for (Edge edge : newEdges) {
            edgesByChildren.put(edge.getChild(), edge);
            edgesByParents.put(edge.getParent(), edge);
        }
    }


    //	private static void addToTable(Set<Edge> newEdges,
    //			Table<Node, Node, Edge> edgeWeights) {
    //		for (Edge edge : newEdges) {
    //			edgeWeights.put(edge.getChild(), edge.getParent(), edge);
    //		}
    //	}

    //Note: All edges created by this method should have source edges
    //TODO: Just nodesInCycle for argument here?
    private static Set<Edge> createCycleNodeEdges(Map<Node, Edge> edgesInCycle,
            Node cycleNode, SetMultimap<Node, Edge> edgesByChildren, SetMultimap<Node, Edge> edgesByParents) {
        Set<Edge> newEdges = Sets.newHashSet();
        for (Node nodeInCycle : edgesInCycle.keySet()) {
            double intraCycleEdgeWeight = edgesInCycle.get(nodeInCycle).getWeight();
            //Edges that currently exist going into the cycle from non-cycle edges...
            for (Edge edgeEnteringCycle : edgesByChildren.get(nodeInCycle)) {
                Node parent = edgeEnteringCycle.getParent();
                if (!edgesInCycle.containsKey(parent)) {
                    //Create a new edge...
                    double enteringEdgeWeight = edgeEnteringCycle.getWeight();
                    double newEdgeWeight = enteringEdgeWeight - intraCycleEdgeWeight;
                    Edge newEdge = new Edge(parent, cycleNode, newEdgeWeight, edgeEnteringCycle);
                    newEdges.add(newEdge);
                }
            }
            //Edges that currently exist going out of the cycle from in-cycle edges...
            for (Edge edgeLeavingCycle : edgesByParents.get(nodeInCycle)) {
                Node child = edgeLeavingCycle.getChild();
                if (!edgesInCycle.containsKey(child)) {
                    //Create a new edge...
                    double newEdgeWeight = edgeLeavingCycle.getWeight();
                    Edge newEdge = new Edge(cycleNode, child, newEdgeWeight, edgeLeavingCycle);
                    newEdges.add(newEdge);
                }
            }
        }
        return newEdges;
    }

    private static @Nullable Map<Node, Edge> findCycle(Map<Node, Edge> markedEdges) {
        Set<Node> nodesTriedSoFar = Sets.newHashSet();

        for (Node curNode : markedEdges.keySet()) {
            if (nodesTriedSoFar.contains(curNode)) {
                continue;
            }

            //Follow the edges until we hit a cycle or a dead end
            Map<Node, Edge> edgesInCycle = Maps.newHashMap();
            Edge curEdge = markedEdges.get(curNode);
            edgesInCycle.put(curNode, curEdge);
            while (markedEdges.containsKey(curEdge.getParent())) {
                Node parent = curEdge.getParent();
                if (edgesInCycle.containsKey(parent)) {
                    return edgesInCycle;
                }
                curNode = parent;
                curEdge = markedEdges.get(parent);
                edgesInCycle.put(curNode, curEdge);
            }
            //Not a cycle
            nodesTriedSoFar.addAll(edgesInCycle.keySet());
        }
        //No cycle found
        return null;
    }

    private static Map<Node, Edge> getInitialEdgeSet(Set<Node> nodes,
            Node root, SetMultimap<Node, Edge> edgesByChildren) {
        Map<Node, Edge> results = Maps.newHashMap();
        for (Node node : nodes) {
            if (node != root) {
                Collection<Edge> incomingEdges = edgesByChildren.get(node);
                Edge chosenEdge = pickOneWithMinWeight(incomingEdges);
                results.put(node, chosenEdge);
            }
        }
        return results;
    }

    private static Edge pickOneWithMinWeight(Collection<Edge> edges) {
        Preconditions.checkArgument(!edges.isEmpty());
        return edges.stream()
                .min(Comparator.comparing(Edge::getWeight)).get();
    }

    private static class Node {
        //For debugging purposes only; do NOT use for equality/hashCode
        private final String name;

        public Node(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class Edge {
        private final Node parent;
        private final Node child;
        private final double weight;
        private final @Nullable Edge source;

        public Edge(Node parent, Node child, double weight, @Nullable Edge source) {
            this.parent = parent;
            this.child = child;
            this.weight = weight;
            this.source = source;
        }

        public Edge getSource() {
            return source;
        }

        public Node getParent() {
            return parent;
        }

        public Node getChild() {
            return child;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return "(" + parent + " to " + child + ", " + weight + ")";
        }

    }
}
