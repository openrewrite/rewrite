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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExtractVersionsAsProperties extends Recipe {

    String displayName = "Extract Maven dependency versions as properties";

    String description = "Extracts inlined dependency versions into the `<properties>` section and replaces them with `${property}` references.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            private PropertyResolver propertyResolver;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Map<String, String> existingProps = loadExistingProperties(document.getRoot());
                Map<String, String> groupSharedVersion = GroupVersionAnalyzer.analyze(document.getRoot(), existingProps);
                Map<String, String> groupExistingProperty =
                    GroupVersionAnalyzer.existingSharedProperties(document.getRoot(), groupSharedVersion, existingProps);
                propertyResolver = new PropertyResolver(groupSharedVersion, groupExistingProperty, existingProps);
                return super.visitDocument(document, ctx);
            }

            private Map<String, String> loadExistingProperties(Xml.Tag root) {
                return root.getChild("properties")
                    .map(this::collectPropertiesFrom)
                    .orElseGet(LinkedHashMap::new);
            }

            private Map<String, String> collectPropertiesFrom(Xml.Tag propsTag) {
                return propsTag.getChildren().stream()
                    .filter(child -> child.getValue().isPresent())
                    .collect(toMap(
                        Xml.Tag::getName,
                        child -> child.getValue().get(),
                        (a, b) -> a,
                        LinkedHashMap::new));
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag() || isManagedDependencyTag() || isPluginTag() || isPluginDependencyTag()) {
                    Optional<Xml.Tag> versionTag = tag.getChild("version");
                    if (versionTag.isPresent()) {
                        String version = versionTag.get().getValue().orElse(null);
                        if (version != null && !PropertyResolver.isPropertyRef(version)) {
                            String groupId = tag.getChildValue("groupId").orElse(null);
                            String artifactId = tag.getChildValue("artifactId").orElse(null);
                            if (artifactId != null) {
                                String propertyKey = propertyResolver.resolvePropertyKey(groupId, artifactId, version);
                                doAfterVisit(new AddPropertyVisitor(propertyKey, version, true));
                                doAfterVisit(new ChangeTagValueVisitor<>(versionTag.get(), "${" + propertyKey + "}"));
                            }
                        }
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

    private static Stream<Xml.Tag> allDescendants(Xml.Tag tag) {
        return Stream.concat(Stream.of(tag), tag.getChildren().stream().flatMap(ExtractVersionsAsProperties::allDescendants));
    }

    private static class PropertyResolver {
        private final Map<String, String> propertyKeyToVersion = new LinkedHashMap<>();
        private final Map<String, String> groupSharedVersion;
        private final Map<String, String> groupExistingProperty;

        PropertyResolver(Map<String, String> groupSharedVersion, Map<String, String> groupExistingProperty,
                         Map<String, String> existingProps) {
            this.groupSharedVersion = groupSharedVersion;
            this.groupExistingProperty = groupExistingProperty;
            this.propertyKeyToVersion.putAll(existingProps);
        }

        static boolean isPropertyRef(String version) {
            String trimmedVersion = version.trim();
            return trimmedVersion.startsWith("${") && trimmedVersion.endsWith("}");
        }

        static @Nullable String resolveToLiteral(String version, Map<String, String> existingProps) {
            String resolved = ResolvedPom.placeholderHelper.replacePlaceholders(version, existingProps::get);
            return ResolvedPom.placeholderHelper.hasPlaceholders(resolved) ? null : resolved;
        }

        String resolvePropertyKey(@Nullable String groupId, String artifactId, String version) {
            if (groupId != null) {
                String existingKey = groupExistingProperty.get(groupId);
                if (existingKey != null && version.equals(propertyKeyToVersion.get(existingKey))) {
                    return existingKey;
                }
            }
            String baseKey = groupId != null && groupSharedVersion.containsKey(groupId)
                ? groupId + ".version"
                : artifactId + ".version";
            String key = baseKey;
            int suffix = 1;
            while (propertyKeyToVersion.containsKey(key) && !propertyKeyToVersion.get(key).equals(version)) {
                key = baseKey + "." + suffix++;
            }
            propertyKeyToVersion.put(key, version);
            return key;
        }
    }

    private static class GroupVersionAnalyzer {
        // Returns groupId → version for groups where every dep with a resolvable version shares the same version.
        static Map<String, String> analyze(Xml.Tag root, Map<String, String> existingProps) {
            return allDescendants(root)
                .filter(tag -> "dependency".equals(tag.getName()) || "plugin".equals(tag.getName()))
                .filter(tag -> tag.getChildValue("groupId").isPresent())
                .collect(groupingBy(
                    tag -> tag.getChildValue("groupId").get(),
                    toList()))
                .entrySet().stream()
                .flatMap(groupEntry -> toSharedVersionEntry(groupEntry, existingProps))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        // For each shared-version group, the existing local property already referenced by one of its members,
        // so a newly-extracted sibling can reuse that name instead of minting a new group-standard one.
        static Map<String, String> existingSharedProperties(
            Xml.Tag root, Map<String, String> groupSharedVersion, Map<String, String> existingProps) {
            Map<String, String> result = new LinkedHashMap<>();
            allDescendants(root)
                .filter(tag -> "dependency".equals(tag.getName()) || "plugin".equals(tag.getName()))
                .forEach(tag -> {
                    String groupId = tag.getChildValue("groupId").orElse(null);
                    if (groupId == null || result.containsKey(groupId) || !groupSharedVersion.containsKey(groupId)) {
                        return;
                    }
                    String version = tag.getChild("version").flatMap(Xml.Tag::getValue).orElse(null);
                    if (version == null || !PropertyResolver.isPropertyRef(version)) {
                        return;
                    }
                    List<String> placeholders = ResolvedPom.placeholderHelper.getPlaceholders(version);
                    if (placeholders.size() == 1 && groupSharedVersion.get(groupId).equals(existingProps.get(placeholders.get(0)))) {
                        result.put(groupId, placeholders.get(0));
                    }
                });
            return result;
        }

        private static Stream<Map.Entry<String, String>> toSharedVersionEntry(
            Map.Entry<String, List<Xml.Tag>> groupEntry, Map<String, String> existingProps) {
            List<String> resolvedVersions = groupEntry.getValue().stream()
                .map(tag -> tag.getChild("version").flatMap(Xml.Tag::getValue).orElse(null))
                .filter(Objects::nonNull)
                .map(v -> PropertyResolver.resolveToLiteral(v, existingProps))
                .filter(Objects::nonNull)
                .collect(toList());
            if (resolvedVersions.size() > 1 && new HashSet<>(resolvedVersions).size() == 1) {
                return Stream.of(new AbstractMap.SimpleEntry<>(groupEntry.getKey(), resolvedVersions.get(0)));
            }
            return Stream.empty();
        }
    }
}
