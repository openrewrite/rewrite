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
package org.openrewrite.maven;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * A multi-rooted tree of inter-project dependencies. Iteration over this ordering proceeds according to the
 * <a href="https://maven.apache.org/guides/mini/guide-multiple-modules.html#reactor-sorting>Reactor ordering</a>
 * of the projects.
 */
public class MavenReactorOrdering implements Iterable<MavenReactorOrdering.MavenProjectRelation> {
    @Nullable
    List<MavenProjectRelation> nodes;

    /**
     * @return In-order (reactor order) traversal of the project dependency tree(s).
     */
    @Override
    public Iterator<MavenProjectRelation> iterator() {
        Stack<MavenProjectRelation> relations = new Stack<>();
        relations.addAll(nodes);

        return new Iterator<MavenProjectRelation>() {
            @Override
            public boolean hasNext() {
                return !relations.isEmpty();
            }

            @Override
            public MavenProjectRelation next() {
                MavenProjectRelation relation = relations.pop();
                relations.addAll(relation.getDependencies());
                return relation;
            }
        };
    }

    public List<MavenProjectRelation> getNodes() {
        if (nodes == null) {
            nodes = new ArrayList<>(3);
        }
        return nodes;
    }

    public static List<SourceFile> visitPomsInReactorOrder(List<SourceFile> before, ExecutionContext ctx,
                                                           Function<MavenProjectRelation, Xml.Document> mapping) {
        boolean changed = false;

        MavenReactorOrdering reactorOrdering = MavenReactorOrdering.build(before);
        for (MavenProjectRelation relation : reactorOrdering) {
            Xml.Document mapped = mapping.apply(relation);
            if (mapped != relation.pom) {
                changed = true;
            }
            relation.pom = mapped;
        }

        if (changed) {
            return ListUtils.map(before, sourceFile -> {
                if (sourceFile instanceof Xml.Document) {
                    for (MavenProjectRelation relation : reactorOrdering) {
                        if (sourceFile.isScope(relation.pom)) {
                            return relation.pom;
                        }
                    }
                }
                return sourceFile;
            });
        }

        return before;
    }

    public static MavenReactorOrdering build(List<SourceFile> sourceFiles) {
        List<Xml.Document> mavens = new ArrayList<>();
        for (SourceFile sourceFile : sourceFiles) {
            if (sourceFile instanceof Xml.Document) {
                mavens.add((Xml.Document) sourceFile);
            }
        }

        // the value is the set of maven projects that depend on the key
        Map<Xml.Document, Set<Xml.Document>> byDependedOn = new HashMap<>();

        for (Xml.Document maven : mavens) {
            byDependedOn.computeIfAbsent(maven, m -> new HashSet<>());
            MavenResolutionResult mavenResolutionResult = maven.getMarkers().findFirst(MavenResolutionResult.class)
                    .orElseThrow(() -> new IllegalStateException("Maven AST without a MavenResolutionResult marker"));

            for (List<ResolvedDependency> dependencies : mavenResolutionResult.getDependencies().values()) {
                for (ResolvedDependency dependency : dependencies) {
                    for (Xml.Document test : mavens) {
                        MavenResolutionResult testMavenResolutionResult = MavenProjectRelation.getResolutionResult(test);
                        if (testMavenResolutionResult.getPom().getGroupId().equals(dependency.getGroupId()) &&
                                testMavenResolutionResult.getPom().getArtifactId().equals(dependency.getArtifactId())) {
                            byDependedOn.get(maven).add(test);
                        }
                    }
                }
            }
        }

        MavenReactorOrdering wholeProject = new MavenReactorOrdering();
        for (Map.Entry<Xml.Document, Set<Xml.Document>> relation : byDependedOn.entrySet()) {
            MavenProjectRelation from = wholeProject.insertProject(relation.getKey());
            for (Xml.Document dependent : relation.getValue()) {
                MavenProjectRelation to = wholeProject.insertProject(dependent);
                from.dependencies.add(to);

                // "to" is no longer a project root, since it depends on another project
                wholeProject.getNodes().remove(to);
            }
        }

        return wholeProject;
    }

    private MavenProjectRelation insertProject(Xml.Document doc) {
        MavenProjectRelation node = null;
        if (nodes != null) {
            for (MavenProjectRelation node1 : nodes) {
                node = node1.find(doc);
                if (node != null) {
                    break;
                }
            }
        }
        if (node == null) {
            node = new MavenProjectRelation(doc, new ArrayList<>(3));
            getNodes().add(node);
        }
        return node;
    }

    @Value
    public static class MavenProjectRelation {
        @NonFinal
        Xml.Document pom;

        List<MavenProjectRelation> dependencies;

        public MavenResolutionResult getResolutionResult() {
            return getResolutionResult(pom);
        }

        @Nullable
        private MavenReactorOrdering.MavenProjectRelation find(Xml.Document doc) {
            for (MavenProjectRelation dep : dependencies) {
                if (dep.getPom().isScope(doc)) {
                    return dep;
                }
                for (MavenProjectRelation dependency : dep.getDependencies()) {
                    MavenProjectRelation node = dependency.find(doc);
                    if (node != null) {
                        return node;
                    }
                }
            }

            return null;
        }

        public boolean anyProjectMatches(BiPredicate<Xml.Document, MavenResolutionResult> matcher) {
            for (MavenProjectRelation dependency : dependencies) {
                Xml.Document pom = dependency.getPom();
                if (matcher.test(pom, getResolutionResult(pom))) {
                    return true;
                }
                if (dependency.anyProjectMatches(matcher)) {
                    return true;
                }
            }
            return false;
        }

        private static MavenResolutionResult getResolutionResult(Xml.Document pom) {
            return pom.getMarkers().findFirst(MavenResolutionResult.class)
                    .orElseThrow(() -> new IllegalStateException("Maven AST without a MavenResolutionResult marker"));
        }
    }
}
