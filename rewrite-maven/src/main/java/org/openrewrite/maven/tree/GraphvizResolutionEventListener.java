/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.tree;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Link.to;

@RequiredArgsConstructor
public class GraphvizResolutionEventListener implements ResolutionEventListener {
    private final Map<GroupArtifactVersion, MutableNode> pomToNode = new HashMap<>();
    private final Map<GroupArtifactVersion, MutableNode> dmToNode = new HashMap<>();
    private final MutableGraph g = mutGraph("resolution").setDirected(true);

    private final Scope scope;

    private int parentLinks = 0;
    private int dependencyLinks = 0;
    private int managedDependencies = 0;

    @Override
    public void parent(Pom parent, Pom containing) {
        Link link = to(gavNode(parent.getGav()))
                .with(Style.DASHED);
        if(parentLinks++ == 0) {
            link = link.with(Label.of("parent"));
        }

        gavNode(containing.getGav()).addLink(link);
    }

    @Override
    public void dependency(ResolvedDependency resolvedDependency, ResolvedPom containing) {
        Link link = to(gavNode(resolvedDependency.getGav()));
        if(dependencyLinks++ == 0) {
            link = link.with(Label.of("dependency"));
        }

        gavNode(containing.getGav()).addLink(link);
    }

    @Override
    public void dependencyManagement(DependencyManagementDependency dependencyManagement, Pom containing) {
        GroupArtifactVersion gav = new GroupArtifactVersion(dependencyManagement.getGroupId(), dependencyManagement.getArtifactId(), dependencyManagement.getVersion());
        Link link = to(dmNode(gav)).with(Color.RED);
        if(managedDependencies++ == 0) {
            link = link.with(Label.of("dependencyManagement"));
        }

        gavNode(containing.getGav()).addLink(link);
    }

    public Graphviz graphviz() {
        return Graphviz.fromGraph(g);
    }

    private MutableNode gavNode(ResolvedGroupArtifactVersion gav) {
        return gavNode(new GroupArtifactVersion(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()));
    }

    private MutableNode gavNode(GroupArtifactVersion gav) {
        return pomToNode.computeIfAbsent(gav, ignored -> {
            MutableNode node = mutNode(gav.toString())
                    .add(pomToNode.isEmpty() ? Style.BOLD : Style.SOLID);
            g.add(node);
            return node;
        });
    }

    private MutableNode dmNode(GroupArtifactVersion gav) {
        return pomToNode.computeIfAbsent(gav, ignored -> {
            MutableNode node = mutNode(gav.toString()).add(Color.RED);
            g.add(node);
            return node;
        });
    }
}
