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
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Link.to;
import static java.util.Collections.emptySet;

@RequiredArgsConstructor
public class GraphvizResolutionEventListener implements ResolutionEventListener {
    public static final String GRAPH_NAME = "resolution";

    private final Map<GroupArtifactVersion, MutableNode> pomToNode = new HashMap<>();
    private final Map<GroupArtifactVersion, MutableNode> dmToNode = new HashMap<>();
    private final Map<GroupArtifactVersion, Set<GroupArtifactVersion>> alreadySeenDm = new HashMap<>();
    private final MutableGraph g = mutGraph(GRAPH_NAME).setDirected(true);

    private final Scope scope;

    private int parentLinks = 0;
    private int dependencyLinks = 0;
    private int managedDependencies = 0;

    @Override
    public void parent(Pom parent, Pom containing) {
        Link link = to(gavNode(parent.getGav()).add(Style.FILLED, Color.rgb("deefee"))).with(Style.DASHED);
        if (parentLinks++ == 0) {
            link = link.with(Label.of("parent"));
        }
        gavNode(containing.getGav()).addLink(link);
    }

    @Override
    public void dependency(Scope scope, ResolvedDependency resolvedDependency, ResolvedPom containing) {
        if (scope != this.scope) {
            return;
        }

        Link link = to(gavNode(resolvedDependency.getGav()).add(Style.FILLED, Color.rgb("e6eaff")));
        if (dependencyLinks++ == 0) {
            link = link.with(Label.of("dependency"));
        }
        gavNode(containing.getGav()).addLink(link);
    }

    @Override
    public void dependencyManagement(DependencyManagementDependency dependencyManagement, Pom containing) {
        GroupArtifactVersion gav = new GroupArtifactVersion(dependencyManagement.getGroupId(), dependencyManagement.getArtifactId(), dependencyManagement.getVersion());

        if (alreadySeenDm.getOrDefault(simplifyGav(containing.getGav()), emptySet()).contains(gav)) {
            return;
        }
        alreadySeenDm.computeIfAbsent(simplifyGav(containing.getGav()), g -> new HashSet<>()).add(gav);

        Link link = to(dmNode(gav).add(Style.FILLED, Color.rgb("d6d6de")));
        if (managedDependencies++ == 0) {
            link = link.with(Label.of("dependencyManagement"));
        }
        gavNode(containing.getGav()).addLink(link);
    }

    @Override
    public void downloadError(GroupArtifactVersion gav, Pom containing) {
        Link link = to(gavNode(gav).add(Style.FILLED, Color.rgb("ff1947")))
                .with(Label.of("error"));
        gavNode(containing.getGav())
                .addLink(link);
    }

    @Override
    public void bomImport(ResolvedGroupArtifactVersion gav, Pom containing) {
        Link link = to(gavNode(gav).add(Style.FILLED, Color.rgb("e6eaff")));
        if (dependencyLinks++ == 0) {
            link = link.with(Label.of("dependency"));
        }
        gavNode(containing.getGav()).addLink(link);
    }

    public Graphviz graphviz() {
        return Graphviz.fromGraph(g);
    }

    private GroupArtifactVersion simplifyGav(ResolvedGroupArtifactVersion gav) {
        return new GroupArtifactVersion(gav.getGroupId(), gav.getArtifactId(),
                gav.getDatedSnapshotVersion() == null ? gav.getVersion() : gav.getDatedSnapshotVersion());
    }

    private MutableNode gavNode(ResolvedGroupArtifactVersion gav) {
        return gavNode(simplifyGav(gav));
    }

    private MutableNode gavNode(GroupArtifactVersion gav) {
        return pomToNode.computeIfAbsent(gav, ignored -> {
            MutableNode node = mutNode(Label.lines(gav.getGroupId(), gav.getArtifactId(), gav.getVersion())).add(Shape.RECTANGLE);
            String url = gavUrl(gav);
            if (url != null) {
                node = node.add("URL", url);
            }
            g.add(node);
            return node;
        });
    }

    private MutableNode dmNode(GroupArtifactVersion gav) {
        return pomToNode.computeIfAbsent(gav, ignored -> {
            MutableNode node = mutNode(Label.lines(gav.getGroupId(), gav.getArtifactId(), gav.getVersion())).add(Shape.RECTANGLE);
            String url = gavUrl(gav);
            if (url != null) {
                node = node.add("URL", url);
            }
            g.add(node);
            return node;
        });
    }

    @Nullable
    private String gavUrl(GroupArtifactVersion gav) {
        if (gav.getGroupId() == null || gav.getArtifactId() == null || gav.getVersion() == null) {
            return null;
        }
        try {
            return "https://repo1.maven.org/maven2/" +
                    Arrays.stream(gav.getGroupId().split("\\."))
                            .map(g -> {
                                try {
                                    return URLEncoder.encode(g, StandardCharsets.UTF_8.name());
                                } catch (UnsupportedEncodingException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.joining("/")) + '/' +
                    URLEncoder.encode(gav.getArtifactId(), StandardCharsets.UTF_8.name()) + '/' +
                    URLEncoder.encode(gav.getVersion(), StandardCharsets.UTF_8.name()) + '/' +
                    URLEncoder.encode(gav.getArtifactId() + '-' + gav.getVersion() + ".pom", StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
