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

    String description = "Removes duplicated dependencies in the `<dependencies>` and `<dependencyManagement>` sections of the `pom.xml`.";

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
                Xml.Tag t = tag;
                if (isDependenciesTag()) {
                    t = removeDuplicates(t, false);
                } else if (isManagedDependenciesTag()) {
                    t = removeDuplicates(t, true);
                }
                if (t != tag) {
                    maybeUpdateModel();
                }
                return super.visitTag(t, ctx);
            }

            /**
             * Maven expects dependencies to be unique by group, artifact, type and classifier, and warns when a POM
             * declares the same one twice ({@code 'dependencies.dependency.(groupId:artifactId:type:classifier)'
             * must be unique}), but it still builds an effective model. This recipe only removes a duplicate when
             * doing so leaves that model unchanged, and the two sections resolve differently:
             * <ul>
             *     <li>in {@code <dependencies>} the last declaration is the effective one, see the
             *     {@code rootDependencies} map in {@link org.openrewrite.maven.tree.ResolvedPom}, so a differing
             *     duplicate has to take the place of the earlier declaration rather than be dropped;</li>
             *     <li>in {@code <dependencyManagement>} duplicates merge field-wise: each field comes from the
             *     first declaration that sets it, and exclusions accumulate across all declarations
             *     ({@code <optional>} excepted: Maven does not inject it from {@code <dependencyManagement>} at
             *     all, so a duplicate adding only it changes nothing either way and is conservatively kept). A later
             *     duplicate is therefore only removed when it sets no field the earlier declarations leave unset
             *     and carries no exclusion they do not already carry; otherwise several declarations make up the
             *     effective entry and all of them are left in place. A repeated BOM import is likewise only
             *     removed when it resolves to the same version, because for entries both versions manage the
             *     first import wins, pinned by {@code ResolvedPomTest#firstUniqueManagedDependencyWins}, while a
             *     different version may manage entries the first import does not.</li>
             * </ul>
             * The surviving declaration keeps the position of the first one, which is the position the resolved
             * model already gave it. Removing a duplicate therefore leaves the resolved dependencies, the
             * effective dependency management entries and their order exactly as they were.
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
             * {@link #declaredExclusions} compares by value because Maven accumulates them across duplicates
             * instead of taking them from any one declaration. Values are not compared: whether a duplicate sets
             * {@code <version>2</version>} or restates {@code <version>1</version>}, the effective entry takes
             * the version of the first declaration that sets one.
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
             * Compares the fields the effective model of a {@code <dependencies>} entry is built from, ignoring
             * formatting and comments and resolving property placeholders, so that a duplicate declared through a
             * property is recognised as the same declaration. Any difference that is not known to be irrelevant
             * counts as a difference, so that the two are only collapsed onto the earlier declaration when they
             * resolve to the same thing.
             */
            private boolean isSameDeclaration(Xml.Tag dependency, Xml.Tag other) {
                return declaredFields(dependency).equals(declaredFields(other));
            }

            private Map<String, List<String>> declaredFields(Xml.Tag dependency) {
                Map<String, List<String>> fields = new HashMap<>();
                for (Xml.Tag field : dependency.getChildren()) {
                    fields.computeIfAbsent(field.getName(), name -> new ArrayList<>()).add(fieldValue(field));
                }
                // An omitted field is generally not the same as one restating its default, because it can also be
                // supplied by `<dependencyManagement>`. `type` is defaulted anyway to keep collapsing a bare
                // declaration onto one that spells out `<type>jar</type>`, as `removeDependencyWithDefaultType`
                // expects; that stays first-declaration-wins even where the two inherit a managed `<type>`.
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
                        // Read the text directly rather than through `Xml.Tag#getValue`, which gives up as soon as a
                        // value is interrupted by a comment and would make two different values look identical
                        value.append(((Xml.CharData) child).getText().trim());
                    } else if (child instanceof Xml.Comment) {
                        // A comment is not part of the value Maven reads
                    } else if (child instanceof Xml.Tag) {
                        Xml.Tag nested = (Xml.Tag) child;
                        value.append(nested.getName()).append('=').append(fieldValue(nested)).append(';');
                        plainText = false;
                    } else {
                        // Content that cannot be compared as text falls back to its identity, so that two values are
                        // never considered equal on the strength of a part that was not actually compared
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
                DependencyKey dependencyKey;
                if (tag.getChildValue("scope").filter("import"::equalsIgnoreCase).isPresent()) {
                    dependencyKey = DependencyKey.from(tag, tag.getChild("version").map(this::fieldValue).orElse(null));
                } else {
                    ResolvedManagedDependency resolvedDependency = findManagedDependency(tag);
                    dependencyKey = resolvedDependency == null ? null : DependencyKey.from(resolvedDependency);
                }
                if (dependencyKey == null) {
                    return null;
                }
                // Additionally compare classifier and type, which are only partially compared in `findManagedDependency`
                String classifier = getResolutionResult().getPom().getValue(tag.getChildValue("classifier").orElse(null));
                String type = getResolutionResult().getPom().getValue(tag.getChildValue("type").orElse("jar"));
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
         * Only set for BOM imports, where a repeated import at a different version is not a duplicate of the
         * first import: for entries both versions manage the first import wins, but a different version may
         * manage entries the first import does not.
         */
        @Nullable
        String version;

        public static DependencyKey from(ResolvedDependency dependency, Scope scope) {
            return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getClassifier(), scope, null);
        }

        public static DependencyKey from(ResolvedManagedDependency dependency) {
            return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getClassifier(), Scope.Compile, null);
        }

        public static @Nullable DependencyKey from(Xml.Tag tag, @Nullable String version) {
            return tag.getChildValue("artifactId").map(artifactId ->
                    new DependencyKey(
                            tag.getChildValue("groupId").orElse(null),
                            artifactId,
                            tag.getChildValue("type").orElse("jar"),
                            tag.getChildValue("classifier").orElse(null),
                            tag.getChildValue("scope").map(Scope::fromName).orElse(Scope.Compile),
                            version
                    )).orElse(null);
        }
    }
}
