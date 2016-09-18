package org.ggp.base.util.propnet.factory;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.model.DependencyGraphs;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Transition;

import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class PropNetOrderingBuilder {
    private final ImmutableSet<Component> allComponents;
    private final ImmutableSetMultimap<Component, Component> dependencies;
    private final List<Layer> layers = Lists.newArrayList();

    private static interface Layer {
        List<Component> orderComponents(Set<Component> addedSoFar) throws InterruptedException;
    }

    private PropNetOrderingBuilder(ImmutableSet<Component> allComponents,
            ImmutableSetMultimap<Component, Component> dependencies) {
        this.allComponents = allComponents;
        this.dependencies = dependencies;
    }

    public static PropNetOrderingBuilder builder(PropNet pn) {
        return new PropNetOrderingBuilder(ImmutableSet.copyOf(pn.getComponents()), computeDependencies(pn.getComponents()));
    }

    private static ImmutableSetMultimap<Component, Component> computeDependencies(Set<Component> components) {
        SetMultimap<Component, Component> dependencies = HashMultimap.create();
        for (Component component : components) {
            if (!(component instanceof Transition)) {
                //Add its outputs
                for (Component output : component.getOutputs()) {
                    dependencies.put(output, component);
                }
            }
        }
        return ImmutableSetMultimap.copyOf(dependencies);
    }

    public PropNetOrderingBuilder addComponents(Set<? extends Component> components) {
        layers.add(new Layer() {
            @Override
            public List<Component> orderComponents(Set<Component> addedSoFar) throws InterruptedException {
                List<Component> ordering = Lists.newArrayList();
                List<Set<Component>> strata = DependencyGraphs.toposortSafe(components, dependencies);
                for (Set<Component> stratum : strata) {
                    for (Component component : stratum) {
                        if (!addedSoFar.contains(component)) {
                            //TODO: Verify that all dependencies have already been added
                            ordering.add(component);
                        }
                    }
                }
                return ordering;
            }
        });
        return this;
    }

    public PropNetOrderingBuilder addComponentsAndAncestors(Set<? extends Component> components) {
        addAncestorsOf(components);
        addComponents(components);
        return this;
    }

    public PropNetOrderingBuilder addComponentsAndAncestors(Component component, Component... moreComponents) {
        return addComponentsAndAncestors(Sets.newHashSet(Lists.asList(component, moreComponents)));
    }

    public PropNetOrderingBuilder addComponentsInOrder(List<? extends Component> components) {
        layers.add(new Layer() {
            @Override
            public List<Component> orderComponents(Set<Component> addedSoFar) throws InterruptedException {
                List<Component> ordering = Lists.newArrayList();
                for (Component component : components) {
                    if (!addedSoFar.contains(component)) {
                        //TODO: Verify that all dependencies have already been added
                        ordering.add(component);
                    }
                }
                return ordering;
            }
        });
        return this;
    }

    public PropNetOrderingBuilder addAncestorsOf(Set<? extends Component> components) {
        ImmutableSet<Component> matchingAndUpstream = DependencyGraphs.getMatchingAndUpstream(allComponents, dependencies, Predicates.in(components));
        Set<Component> upstreamOnly = ImmutableSet.copyOf(Sets.difference(matchingAndUpstream, components));
        return addComponents(upstreamOnly);
    }

    public PropNetOrderingBuilder addAncestorsOf(Component component, Component... moreComponents) {
        return addAncestorsOf(Sets.newHashSet(Lists.asList(component, moreComponents)));
    }

    public List<Component> build() throws InterruptedException {
        List<Component> ordering = Lists.newArrayList();
        Set<Component> addedSoFar = Sets.newHashSet();
        for (Layer layer : layers) {
            List<Component> order = layer.orderComponents(addedSoFar);
            ordering.addAll(order);
            addedSoFar.addAll(order);
        }
        return ordering;
    }
}
