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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.ManagedDependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDuplicateDependencies extends Recipe {

    private static final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");
    private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");
    private static final XPathMatcher PROFILE_DEPENDENCIES_MATCHER = new XPathMatcher("/project/profiles/profile/dependencies");
    private static final XPathMatcher PROFILE_MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/profiles/profile/dependencyManagement/dependencies");
    private static final List<String> DECLARATION_FIELDS = Arrays.asList("version", "scope", "optional", "systemPath");

    String displayName = "Remove duplicate Maven dependencies";

    String description = "Maven 3.10 and Maven 4 fail the build when a `<dependencies>` or " +
                         "`<dependencyManagement>` section, including those of a profile, declares the same " +
                         "`groupId:artifactId:type:classifier` more than once, regardless of `<scope>`. Earlier " +
                         "Maven versions only warned and used the last declaration. A project's own " +
                         "`<dependencyManagement>` was the exception when no parent or active profile managed " +
                         "dependencies too. There, duplicate BOM imports used the first one. Without any BOM " +
                         "import, child modules and projects importing it as a BOM used the first declaration, and " +
                         "the project itself took each field from the first declaration that set it. This recipe " +
                         "removes the duplicates in the position of the first, keeping the declaration earlier " +
                         "Maven versions used so that the resolved dependencies stay the same. Without any BOM " +
                         "import, it keeps the first declaration of a `pom` project and merges the declarations of " +
                         "any other project.";

    Duration estimatedEffortPerOccurrence = Duration.ofMinutes(2);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!"dependencies".equals(t.getName())) {
                    return t;
                }
                if (MANAGED_DEPENDENCIES_MATCHER.matches(getCursor())) {
                    return removeDuplicates(t, this::managedDeclarationToKeep);
                }
                if (DEPENDENCIES_MATCHER.matches(getCursor()) ||
                    PROFILE_DEPENDENCIES_MATCHER.matches(getCursor()) ||
                    PROFILE_MANAGED_DEPENDENCIES_MATCHER.matches(getCursor())) {
                    return removeDuplicates(t, () -> this::last);
                }
                return t;
            }

            private Function<List<Xml.Tag>, Xml.Tag> managedDeclarationToKeep() {
                ResolvedPom pom = getResolutionResult().getPom();
                List<ManagedDependency> own = pom.getRequested().getDependencyManagement();
                if (inheritsManagement(pom, own)) {
                    return this::last;
                }
                if (own.stream().anyMatch(ManagedDependency.Imported.class::isInstance)) {
                    return this::firstImportOrLast;
                }
                return "pom".equals(pom.getPackaging()) ? this::first : this::merge;
            }

            // Managed dependencies from a parent or active profile make Maven keep the last duplicate
            private boolean inheritsManagement(ResolvedPom pom, List<ManagedDependency> own) {
                MavenResolutionResult parent = getResolutionResult().getParent();
                return (parent != null && !parent.getPom().getDependencyManagement().isEmpty()) ||
                       pom.getDependencyManagement().stream()
                               .anyMatch(managed -> !own.contains(declaringEntry(managed)));
            }

            private ManagedDependency declaringEntry(ResolvedManagedDependency managed) {
                return managed.getRequestedBom() == null ? managed.getRequested() : managed.getRequestedBom();
            }

            // Maven imports duplicate BOM imports in order, so the first one's versions apply
            private Xml.Tag firstImportOrLast(List<Xml.Tag> duplicates) {
                return "import".equals(value(duplicates.get(0), "scope")) ? first(duplicates) : last(duplicates);
            }

            private Xml.Tag first(List<Xml.Tag> duplicates) {
                return duplicates.get(0);
            }

            private Xml.Tag last(List<Xml.Tag> duplicates) {
                return duplicates.get(duplicates.size() - 1);
            }

            // Maven takes each field from the first duplicate that sets it, and applies every duplicate's exclusions
            private Xml.Tag merge(List<Xml.Tag> duplicates) {
                Xml.Tag merged = duplicates.get(0);
                for (Xml.Tag duplicate : duplicates.subList(1, duplicates.size())) {
                    for (String field : DECLARATION_FIELDS) {
                        merged = mergeField(merged, duplicate, field);
                    }
                    merged = mergeExclusions(merged, duplicate);
                }
                return merged;
            }

            private Xml.Tag mergeField(Xml.Tag merged, Xml.Tag duplicate, String field) {
                if (merged.getChild(field).isPresent()) {
                    return merged;
                }
                return duplicate.getChild(field)
                        .map(value -> AddToTagVisitor.addToTag(merged, value, getCursor()))
                        .orElse(merged);
            }

            private Xml.Tag mergeExclusions(Xml.Tag merged, Xml.Tag duplicate) {
                return duplicate.getChild("exclusions")
                        .map(exclusions -> addExclusions(merged, exclusions))
                        .orElse(merged);
            }

            private Xml.Tag addExclusions(Xml.Tag merged, Xml.Tag exclusions) {
                return merged.getChild("exclusions")
                        .map(existing -> addEachExclusion(merged, existing, exclusions))
                        .orElseGet(() -> AddToTagVisitor.addToTag(merged, exclusions, getCursor()));
            }

            private Xml.Tag addEachExclusion(Xml.Tag merged, Xml.Tag existing, Xml.Tag exclusions) {
                Xml.Tag result = merged;
                for (Xml.Tag exclusion : exclusions.getChildren("exclusion")) {
                    result = AddToTagVisitor.addToTag(result, existing, exclusion, getCursor());
                }
                return result;
            }

            private Xml.Tag removeDuplicates(Xml.Tag dependencies,
                                             Supplier<Function<List<Xml.Tag>, Xml.Tag>> declarationToKeepSupplier) {
                Map<String, List<Xml.Tag>> declarations = new HashMap<>();
                for (Xml.Tag dependency : dependencies.getChildren("dependency")) {
                    String key = managementKey(dependency);
                    if (key != null) {
                        declarations.computeIfAbsent(key, k -> new ArrayList<>()).add(dependency);
                    }
                }
                if (declarations.values().stream().allMatch(duplicates -> duplicates.size() == 1)) {
                    return dependencies;
                }

                Function<List<Xml.Tag>, Xml.Tag> declarationToKeep = declarationToKeepSupplier.get();
                List<Content> content = new ArrayList<>();
                for (Content c : dependencies.getContent()) {
                    String key = c instanceof Xml.Tag && "dependency".equals(((Xml.Tag) c).getName()) ?
                            managementKey((Xml.Tag) c) : null;
                    if (key == null) {
                        content.add(c);
                        continue;
                    }
                    List<Xml.Tag> duplicates = declarations.remove(key);
                    if (duplicates != null) {
                        Xml.Tag first = (Xml.Tag) c;
                        Xml.Tag kept = declarationToKeep.apply(duplicates);
                        content.add(sameDeclaration(first, kept) ? first : kept.withPrefix(first.getPrefix()));
                    }
                }
                maybeUpdateModel();
                return dependencies.withContent(content);
            }

            private @Nullable String managementKey(Xml.Tag dependency) {
                String artifactId = value(dependency, "artifactId");
                if (artifactId == null) {
                    return null;
                }
                String type = value(dependency, "type");
                String classifier = dependency.getChild("classifier").isPresent() ?
                        Objects.toString(value(dependency, "classifier"), "") : null;
                return value(dependency, "groupId") + ":" + artifactId + ":" +
                       (type == null ? "jar" : type) +
                       (classifier == null ? "" : ":" + classifier);
            }

            // Raw text without comments, as Maven compares it: a property can resolve differently per profile
            private @Nullable String value(Xml.Tag dependency, String child) {
                return dependency.getChild(child)
                        .map(this::text)
                        .map(String::trim)
                        .filter(v -> !v.isEmpty())
                        .orElse(null);
            }

            private String text(Xml.Tag tag) {
                return tag.getContent() == null ? "" : tag.getContent().stream()
                        .filter(Xml.CharData.class::isInstance)
                        .map(Xml.CharData.class::cast)
                        .map(Xml.CharData::getText)
                        .collect(joining());
            }

            private boolean sameDeclaration(Xml.Tag a, Xml.Tag b) {
                for (String child : DECLARATION_FIELDS) {
                    if (!Objects.equals(value(a, child), value(b, child))) {
                        return false;
                    }
                }
                return exclusions(a).equals(exclusions(b));
            }

            private Set<String> exclusions(Xml.Tag dependency) {
                Set<String> exclusions = new HashSet<>();
                dependency.getChild("exclusions").ifPresent(e -> {
                    for (Xml.Tag exclusion : e.getChildren("exclusion")) {
                        exclusions.add(value(exclusion, "groupId") + ":" + value(exclusion, "artifactId"));
                    }
                });
                return exclusions;
            }
        };
    }
}
