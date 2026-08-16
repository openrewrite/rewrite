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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDuplicateDependencies extends Recipe {

    String displayName = "Remove duplicate Maven dependencies";

    String description = "Removes duplicated dependencies in the `<dependencies>` and `<dependencyManagement>` sections of the `pom.xml`. " +
                         "The declaration Maven resolves to is the one kept, at the position of the first of the duplicates, so the effective dependency model is unchanged.";

    Duration estimatedEffortPerOccurrence = Duration.ofMinutes(2);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag root = document.getRoot();
                if (root.getChild("dependencies").isPresent() || root.getChild("dependencyManagement").isPresent()) {
                    return SearchResult.found(document);
                }
                return document;
            }
        }, new MavenIsoVisitor<ExecutionContext>() {
            private final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");
            private final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag visitedTag = tag;
                if (isDependenciesTag()) {
                    visitedTag = removeDuplicates(visitedTag, false);
                } else if (isManagedDependenciesTag()) {
                    visitedTag = removeDuplicates(visitedTag, true);
                }
                if (visitedTag != tag) {
                    maybeUpdateModel();
                }
                return super.visitTag(visitedTag, ctx);
            }

            /**
             * Maven warns when a POM declares the same dependency twice
             * ({@code 'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique}) but still
             * builds an effective model, and the two sections resolve differently. In {@code <dependencies>} the
             * last declaration wins, see {@code rootDependencies} in {@link org.openrewrite.maven.tree.ResolvedPom},
             * so a differing duplicate takes the place of the earlier one rather than being dropped. In
             * {@code <dependencyManagement>} entries merge field-wise from the first declaration that sets each
             * field, with exclusions accumulating, so a later duplicate only goes when it adds neither; a repeated
             * BOM import is a duplicate only at the same version, since the first import wins for the entries both
             * manage. Either way the survivor keeps the first declaration's position, which is where the resolved
             * model already put it.
             */
            private Xml.Tag removeDuplicates(Xml.Tag dependencies, boolean managed) {
                List<? extends Content> content = dependencies.getContent();
                if (content == null) {
                    return dependencies;
                }

                List<Content> deduplicated = new ArrayList<>(content.size());
                Map<DependencyKey, Integer> firstDeclarations = new HashMap<>();
                Map<DependencyKey, Set<String>> managedFields = new HashMap<>();
                Map<DependencyKey, Set<String>> managedExclusions = new HashMap<>();
                boolean removed = false;
                for (Content child : content) {
                    if (child instanceof Xml.Tag && "dependency".equals(((Xml.Tag) child).getName())) {
                        Xml.Tag dependency = (Xml.Tag) child;
                        DependencyKey dependencyKey = managed ? getManagedDependencyKey(dependency) : getDependencyKey(dependency);
                        if (dependencyKey != null) {
                            if (managed) {
                                Set<String> fields = declaredManagedFields(dependency);
                                Set<String> exclusions = declaredExclusions(dependency);
                                Set<String> earlierFields = managedFields.get(dependencyKey);
                                if (earlierFields == null) {
                                    managedFields.put(dependencyKey, fields);
                                    managedExclusions.put(dependencyKey, exclusions);
                                } else {
                                    Set<String> earlierExclusions = managedExclusions.get(dependencyKey);
                                    if (earlierFields.containsAll(fields) && earlierExclusions.containsAll(exclusions)) {
                                        removed = true;
                                        continue;
                                    }
                                    // The duplicate contributes to the effective entry, so it has to stay
                                    earlierFields.addAll(fields);
                                    earlierExclusions.addAll(exclusions);
                                }
                            } else {
                                Integer firstDeclaration = firstDeclarations.putIfAbsent(dependencyKey, deduplicated.size());
                                if (firstDeclaration != null) {
                                    Xml.Tag effective = (Xml.Tag) deduplicated.get(firstDeclaration);
                                    if (!isSameDeclaration(effective, dependency)) {
                                        deduplicated.set(firstDeclaration, dependency.withPrefix(effective.getPrefix()));
                                    }
                                    removed = true;
                                    continue;
                                }
                            }
                        }
                    }
                    deduplicated.add(child);
                }
                return removed ? dependencies.withContent(deduplicated) : dependencies;
            }

            /**
             * The names of the fields this declaration sets, {@code exclusions} excepted, which
             * {@link #declaredExclusions} compares by value because Maven accumulates them rather than taking
             * them from one declaration. Values are not compared, as the effective entry takes each field from
             * the first declaration that sets it whatever a later one says.
             */
            private Set<String> declaredManagedFields(Xml.Tag dependency) {
                Set<String> fields = new HashSet<>();
                for (Xml.Tag field : dependency.getChildren()) {
                    if (!"exclusions".equals(field.getName()) && !fieldValue(field).isEmpty()) {
                        fields.add(field.getName());
                    }
                }
                return fields;
            }

            private Set<String> declaredExclusions(Xml.Tag dependency) {
                Set<String> exclusions = new HashSet<>();
                for (Xml.Tag field : dependency.getChildren()) {
                    if ("exclusions".equals(field.getName())) {
                        for (Xml.Tag exclusion : field.getChildren()) {
                            exclusions.add(fieldValue(exclusion));
                        }
                    }
                }
                return exclusions;
            }

            /**
             * Whether both entries resolve to the same declaration, ignoring formatting and comments and
             * resolving property placeholders. Any difference not known to be irrelevant counts as one.
             */
            private boolean isSameDeclaration(Xml.Tag dependency, Xml.Tag other) {
                return declaredFields(dependency).equals(declaredFields(other));
            }

            private Map<String, List<String>> declaredFields(Xml.Tag dependency) {
                Map<String, List<String>> fields = new HashMap<>();
                for (Xml.Tag field : dependency.getChildren()) {
                    fields.computeIfAbsent(field.getName(), name -> new ArrayList<>()).add(fieldValue(field));
                }
                // An omitted field is not generally the same as one restating its default, as it can also come from
                // `<dependencyManagement>`; `type` is defaulted anyway to keep collapsing a bare declaration onto
                // one spelling out `<type>jar</type>`, as `removeDependencyWithDefaultType` expects.
                fields.putIfAbsent("type", singletonList("jar"));
                return fields;
            }

            private String fieldValue(Xml.Tag field) {
                List<? extends Content> content = field.getContent();
                if (content == null) {
                    return "";
                }
                StringBuilder value = new StringBuilder();
                boolean plainText = true;
                for (Content child : content) {
                    if (child instanceof Xml.CharData) {
                        // Not `Xml.Tag#getValue`, which gives up on a value interrupted by a comment and would
                        // then make two different values look identical
                        value.append(((Xml.CharData) child).getText().trim());
                    } else if (child instanceof Xml.Comment) {
                        // Not part of the value Maven reads
                    } else if (child instanceof Xml.Tag) {
                        Xml.Tag nested = (Xml.Tag) child;
                        value.append(nested.getName()).append('=').append(fieldValue(nested)).append(';');
                        plainText = false;
                    } else {
                        // Content that cannot be compared as text falls back to its identity, so two values are
                        // never equal on the strength of a part that was not actually compared
                        value.append(child.getId());
                        plainText = false;
                    }
                }
                if (!plainText) {
                    return value.toString();
                }
                String resolved = getResolutionResult().getPom().getValue(value.toString());
                return resolved != null ? resolved : value.toString();
            }

            private boolean isDependenciesTag() {
                return DEPENDENCIES_MATCHER.matches(getCursor());
            }

            private boolean isManagedDependenciesTag() {
                return MANAGED_DEPENDENCIES_MATCHER.matches(getCursor());
            }

            private @Nullable DependencyKey getDependencyKey(Xml.Tag tag) {
                Map<Scope, List<ResolvedDependency>> dependencies = getResolutionResult().getDependencies();
                Scope scope = tag.getChildValue("scope").map(Scope::fromName).orElse(Scope.Compile);
                if (dependencies.containsKey(scope)) {
                    for (ResolvedDependency resolvedDependency : dependencies.get(scope)) {
                        Dependency req = resolvedDependency.getRequested();
                        String reqGroup = req.getGroupId();
                        if ((reqGroup == null || reqGroup.equals(tag.getChildValue("groupId").orElse(null))) &&
                                Objects.equals(req.getArtifactId(), tag.getChildValue("artifactId").orElse(null)) &&
                                Objects.equals(Optional.ofNullable(req.getType()).orElse("jar"), tag.getChildValue("type").orElse("jar")) &&
                                Objects.equals(req.getClassifier(), tag.getChildValue("classifier").orElse(null))) {
                            return DependencyKey.from(resolvedDependency, scope);
                        }
                    }
                }
                return null;
            }

            private @Nullable DependencyKey getManagedDependencyKey(Xml.Tag tag) {
                String classifier = getResolutionResult().getPom().getValue(tag.getChildValue("classifier").orElse(null));
                String type = getResolutionResult().getPom().getValue(tag.getChildValue("type").orElse("jar"));
                if (tag.getChildValue("scope").filter("import"::equalsIgnoreCase).isPresent()) {
                    String artifactId = getResolutionResult().getPom().getValue(tag.getChildValue("artifactId").orElse(null));
                    if (artifactId == null) {
                        return null;
                    }
                    return new DependencyKey(
                            getResolutionResult().getPom().getValue(tag.getChildValue("groupId").orElse(null)),
                            artifactId,
                            type,
                            classifier,
                            Scope.Import,
                            tag.getChild("version").map(this::fieldValue).orElse(null));
                }
                ResolvedManagedDependency resolvedDependency = findManagedDependency(tag);
                if (resolvedDependency == null) {
                    return null;
                }
                DependencyKey dependencyKey = DependencyKey.from(resolvedDependency);
                // Additionally compare classifier and type, which are only partially compared in `findManagedDependency`
                return Objects.equals(classifier, dependencyKey.getClassifier()) &&
                        Objects.equals(type, dependencyKey.getType()) ? dependencyKey : null;
            }
        });
    }

    @Value
    private static class DependencyKey {
        @Nullable
        String groupId;

        String artifactId;
        String type;

        @Nullable
        String classifier;

        Scope scope;

        /**
         * Only set for BOM imports: the first import wins for the entries both versions manage, but a
         * different version may manage entries the first one does not.
         */
        @Nullable
        String version;

        public static DependencyKey from(ResolvedDependency dependency, Scope scope) {
            return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getClassifier(), scope, null);
        }

        public static DependencyKey from(ResolvedManagedDependency dependency) {
            return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getClassifier(), Scope.Compile, null);
        }
    }
}
