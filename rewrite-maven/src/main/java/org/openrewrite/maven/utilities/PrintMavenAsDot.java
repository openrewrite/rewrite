/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven.utilities;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.stream.Collectors;

public class PrintMavenAsDot extends Recipe {
    @Override
    public String getDisplayName() {
        return "Print Maven dependency hierarchy in DOT format";
    }

    @Override
    public String getDescription() {
        return "The DOT language format is specified [here](https://graphviz.org/doc/info/lang.html).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                if (!document.getMarkers().findFirst(SearchResult.class).isPresent()) {
                    return document.withMarkers(document.getMarkers().add(new SearchResult(Tree.randomId(), dot(mrr))));
                }
                return super.visitDocument(document, ctx);
            }

            private String dot(MavenResolutionResult mrr) {
                StringBuilder dot = new StringBuilder("digraph main {\n");

                Map<ResolvedGroupArtifactVersion, Integer> index = new HashMap<>();

                // for convenience, we construct a ResolvedDependency out of the POM's GAV.
                ResolvedGroupArtifactVersion root =
                        new ResolvedGroupArtifactVersion(
                                null,
                                mrr.getPom().getGroupId(),
                                mrr.getPom().getArtifactId(),
                                mrr.getPom().getVersion(),
                                null);
                dotLabel(dot, 0, root);
                index.put(root, 0);

                for (List<ResolvedDependency> deps : mrr.getDependencies().values()) {
                    for (ResolvedDependency dep : deps) {
                        if (!index.containsKey(dep.getGav())) {
                            dotLabel(dot, index.size(), dep.getGav());
                            index.put(dep.getGav(), index.size());
                        }
                    }
                }

                Set<ResolvedGroupArtifactVersion> seen = Collections.newSetFromMap(new IdentityHashMap<>());
                for (Scope scope : Scope.values()) {
                    if (scope.ordinal() >= Scope.Compile.ordinal() && scope.ordinal() <= Scope.Test.ordinal()) {
                        dotEdges(
                                dot, root, scope,
                                mrr.getDependencies().get(scope).stream()
                                        .filter(dep -> dep.getDepth() == 0 && seen.add(dep.getGav()))
                                        .collect(Collectors.toList()),
                                index
                        );
                    }
                }

                dot.append("}");
                return dot.toString();
            }

            private void dotLabel(StringBuilder dot, int index, ResolvedGroupArtifactVersion gav) {
                dot.append(index)
                        .append(" [label=\"")
                        .append(gav.getGroupId())
                        .append(":")
                        .append(gav.getArtifactId())
                        .append(":")
                        .append(gav.getVersion())
                        .append("\"];\n");
            }

            private void dotEdges(StringBuilder dot,
                                  ResolvedGroupArtifactVersion head,
                                  Scope scope,
                                  List<ResolvedDependency> resolvedDependencies,
                                  Map<ResolvedGroupArtifactVersion, Integer> index) {
                int headIndex = index.get(head);
                for (ResolvedDependency dep : resolvedDependencies) {
                    dot.append(headIndex)
                            .append(" -> ")
                            .append(index.get(dep.getGav()))
                            .append(" [taillabel=\"")
                            .append(scope)
                            .append("\"];\n");
                    dotEdges(dot, dep.getGav(), scope, dep.getDependencies(), index);
                }
            }
        };
    }
}
