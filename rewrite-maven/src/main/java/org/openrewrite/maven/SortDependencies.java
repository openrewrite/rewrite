/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.Profile;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class SortDependencies extends Recipe {

    @Override
    public String getDisplayName() {
        return "Sort dependencies";
    }

    @Override
    public String getDescription() {
        return "Sort dependencies alphabetically by groupId then artifactId. " +
               "Test-scoped dependencies are sorted after non-test dependencies. " +
               "Imported BOMs retain their original positions. Applies to both `<dependencies>` and " +
               "`<dependencyManagement>` sections.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!"dependencies".equals(t.getName()) || t.getContent() == null) {
                    return t;
                }

                // Group comments with their following dependency tag
                List<DependencyGroup> groups = new ArrayList<>();
                List<Content> currentComments = new ArrayList<>();

                for (Content content : t.getContent()) {
                    if (content instanceof Xml.Tag) {
                        Xml.Tag dependency = (Xml.Tag) content;
                        groups.add(new DependencyGroup(dependency, currentComments, isImportedBom(dependency)));
                        currentComments = new ArrayList<>();
                    } else {
                        currentComments.add(content);
                    }
                }

                // If there are fewer than 2 dependency tags, nothing to sort
                if (groups.size() < 2) {
                    return t;
                }

                List<DependencyGroup> sortedReplacements = groups.stream()
                        .filter(group -> !group.importedBom)
                        .sorted(Comparator.<DependencyGroup, Boolean>comparing(
                            group -> "test".equals(group.tag.getChildValue("scope").orElse(null))
                        ).thenComparing(
                            group -> group.tag.getChildValue("groupId").orElse("") + ":" +
                                     group.tag.getChildValue("artifactId").orElse("")
                        ))
                        .collect(Collectors.toList());

                List<DependencyGroup> reorderedGroups = new ArrayList<>(groups);
                int replacementIndex = 0;
                for (int i = 0; i < groups.size(); i++) {
                    if (!groups.get(i).importedBom) {
                        reorderedGroups.set(i, sortedReplacements.get(replacementIndex++));
                    }
                }

                // Check if order actually changed
                boolean changed = false;
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).tag != reorderedGroups.get(i).tag) {
                        changed = true;
                        break;
                    }
                }

                if (!changed) {
                    return t;
                }

                // Rebuild content preserving original whitespace prefixes
                List<Content> newContent = new ArrayList<>();
                for (int i = 0; i < reorderedGroups.size(); i++) {
                    DependencyGroup original = groups.get(i);
                    DependencyGroup reordered = reorderedGroups.get(i);

                    // Apply the prefix from the original position to the reordered content
                    for (int j = 0; j < reordered.precedingContent.size(); j++) {
                        Content c = reordered.precedingContent.get(j);
                        if (j == 0 && !original.precedingContent.isEmpty()) {
                            c = (Content) c.withPrefix(original.precedingContent.get(0).getPrefix());
                        } else if (j == 0) {
                            c = (Content) c.withPrefix(original.tag.getPrefix());
                        }
                        newContent.add(c);
                    }

                    Xml.Tag reorderedTag = reordered.tag;
                    if (reordered.precedingContent.isEmpty()) {
                        // Apply prefix from the original group's first element
                        if (!original.precedingContent.isEmpty()) {
                            reorderedTag = reorderedTag.withPrefix(original.precedingContent.get(0).getPrefix());
                        } else {
                            reorderedTag = reorderedTag.withPrefix(original.tag.getPrefix());
                        }
                    }
                    newContent.add(reorderedTag);
                }

                // Append any trailing non-tag content
                newContent.addAll(currentComments);

                return t.withContent(newContent);
            }

            private boolean isImportedBom(Xml.Tag tag) {
                ResolvedPom pom = getResolutionResult().getPom();
                return isValueOrUnresolvedPlaceholder(tag, "type", "pom", pom) &&
                       isValueOrUnresolvedPlaceholder(tag, "scope", "import", pom);
            }

            private boolean isValueOrUnresolvedPlaceholder(Xml.Tag tag, String childName, String expected,
                                                            ResolvedPom pom) {
                return tag.getChildValue(childName)
                        .map(value -> isValueOrUnresolvedPlaceholder(value, expected, pom))
                        .orElse(false);
            }

            private boolean isValueOrUnresolvedPlaceholder(String value, String expected, ResolvedPom pom) {
                String resolvedValue = inactiveProfile()
                        .map(profile -> resolveProfileAwareValue(value, pom, profile))
                        .orElseGet(() -> requireNonNull(pom.getValue(value)));
                return isValueOrUnresolvedPlaceholder(resolvedValue, expected);
            }

            private boolean isValueOrUnresolvedPlaceholder(String value, String expected) {
                return ResolvedPom.placeholderHelper.hasPlaceholders(value) || expected.equals(value.trim());
            }

            private String resolveProfileAwareValue(String value, ResolvedPom pom,
                                                    Profile inactiveProfile) {
                return ResolvedPom.placeholderHelper.replacePlaceholders(
                        value, property -> profileOrPomValue(property, pom, inactiveProfile));
            }

            private Optional<Profile> inactiveProfile() {
                return getCursor().getPathAsStream(Xml.Tag.class::isInstance)
                        .map(Xml.Tag.class::cast)
                        .filter(tag -> "profile".equals(tag.getName()))
                        .findFirst()
                        .flatMap(profile -> profile.getChildValue("id"))
                        .flatMap(this::requestedProfile)
                        .filter(profile -> !isEffectivelyActive(profile));
            }

            private Optional<Profile> requestedProfile(String profileId) {
                return getResolutionResult().getPom().getRequested().getProfiles().stream()
                        .filter(profile -> profileId.equals(profile.getId()))
                        .findFirst();
            }

            private String profileOrPomValue(String property, ResolvedPom pom,
                                             Profile inactiveProfile) {
                String systemValue = System.getProperty(property);
                if (systemValue != null) {
                    return systemValue;
                }
                String userValue = getResolutionResult().getUserProperties().get(property);
                if (userValue != null) {
                    return userValue;
                }
                return pom.getRequested().getProfiles().stream()
                        .filter(profile -> profile.getProperties().containsKey(property))
                        .map(profile -> profileValue(profile, property, inactiveProfile))
                        .findFirst()
                        .orElseGet(() -> pomValue(property, pom));
            }

            private String profileValue(Profile profile, String property, Profile inactiveProfile) {
                return profile == inactiveProfile || remainsActive(profile) ?
                        Objects.toString(profile.getProperties().get(property), "") :
                        // Returning the placeholder itself keeps the property unresolved
                        asPlaceholder(property);
            }

            private String pomValue(String property, ResolvedPom pom) {
                if (pom.getRequested().getProperties().containsKey(property)) {
                    return Objects.toString(pom.getRequested().getProperties().get(property), "");
                }
                return requireNonNull(pom.getValue(asPlaceholder(property)));
            }

            private boolean isEffectivelyActive(Profile profile) {
                List<String> activeProfiles = getResolutionResult().getActiveProfiles();
                boolean hasActivePomProfile = getResolutionResult().getPom().getRequested().getProfiles().stream()
                        .anyMatch(candidate -> candidate.isActive(activeProfiles));
                return hasActivePomProfile ? profile.isActive(activeProfiles) :
                        profile.getActivation() != null &&
                        Boolean.TRUE.equals(profile.getActivation().getActiveByDefault());
            }

            private boolean remainsActive(Profile profile) {
                List<String> activeProfiles = getResolutionResult().getActiveProfiles();
                return activeProfiles.isEmpty() ?
                        profile.getActivation() != null && profile.getActivation().isActive() :
                        profile.isActive(activeProfiles);
            }
        };
    }

    private static String asPlaceholder(String property) {
        return "${" + property + "}";
    }

    private static class DependencyGroup {
        final Xml.Tag tag;
        final List<Content> precedingContent;
        final boolean importedBom;

        DependencyGroup(Xml.Tag tag, List<Content> precedingContent, boolean importedBom) {
            this.tag = tag;
            this.precedingContent = precedingContent;
            this.importedBom = importedBom;
        }
    }
}
