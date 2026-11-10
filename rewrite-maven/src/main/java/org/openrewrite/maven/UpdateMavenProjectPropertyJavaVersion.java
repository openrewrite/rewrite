/*
 * Copyright 2025 the original author or authors.
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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateMavenProjectPropertyJavaVersion extends Recipe {

    private static final List<String> JAVA_VERSION_PROPERTIES = Arrays.asList(
            "java.version",
            "jdk.version",
            "javaVersion",
            "jdkVersion",
            "maven.compiler.source",
            "maven.compiler.target",
            "maven.compiler.release",
            "maven.compiler.testSource",
            "maven.compiler.testTarget",
            "maven.compiler.testRelease",
            "release.version");

    private static final List<XPathMatcher> JAVA_VERSION_XPATH_MATCHERS =
            JAVA_VERSION_PROPERTIES.stream()
                    .map(property -> "/project/properties/" + property)
                    .map(XPathMatcher::new).collect(toList());

    private static final List<String> COMPILER_LEVEL_CONFIGURATION = Arrays.asList("source", "target", "release",
            "testSource", "testTarget", "testRelease");

    private static final Pattern PROPERTY_REFERENCE = Pattern.compile("\\$\\{([^${}]+)}");

    private static final XPathMatcher COMPILER_PLUGIN_CONFIGURATION_MATCHER =
            new XPathMatcher("//plugin[artifactId='maven-compiler-plugin']/configuration/*");

    private static final XPathMatcher COMPILER_PLUGIN_EXECUTION_CONFIGURATION_MATCHER =
            new XPathMatcher("//plugin[artifactId='maven-compiler-plugin']/executions/execution/configuration/*");

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    String displayName = "Update Maven Java project properties";

    String description = "The Java version is determined by several project properties, including:\n\n" +
               " * `java.version`\n" +
               " * `jdk.version`\n" +
               " * `javaVersion`\n" +
               " * `jdkVersion`\n" +
               " * `maven.compiler.source`\n" +
               " * `maven.compiler.target`\n" +
               " * `maven.compiler.release`\n" +
               " * `maven.compiler.testSource`\n" +
               " * `maven.compiler.testTarget`\n" +
               " * `maven.compiler.testRelease`\n" +
               " * `release.version`\n\n" +
               "Properties of any other name are updated too when the `maven-compiler-plugin` `source`, `target`, `release`, " +
               "`testSource`, `testTarget` or `testRelease` configuration of this pom, or of a pom it inherits from, resolves to them. " +
               "A property whose value is only a reference to another property, such as " +
               "`<maven.compiler.release>${custom.release}</maven.compiler.release>`, has the referenced property updated instead, " +
               "unless the pom uses that one for something else as well.\n\n" +
               "If none of these properties are in use and the maven compiler plugin is not otherwise configured, adds the `maven.compiler.release` property.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            // Properties the compiler plugin configuration in this pom's XML routes its levels through
            final Set<String> localCompilerLevelProperties = new LinkedHashSet<>();

            // Referenced property -> properties of this pom whose whole value is `${referenced}`
            final Map<String, Set<String>> referencingProperties = new HashMap<>();

            // Properties this pom references in any other way, such as `eclipse-temurin:${name}-jre` or `<version>${name}</version>`
            final Set<String> otherwiseReferencedProperties = new HashSet<>();

            boolean compilerPluginConfiguredExplicitly;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                // Update properties already defined in the current pom
                Xml.Document d = super.visitDocument(document, ctx);

                ResolvedPom pom = getResolutionResult().getPom();
                Map<String, String> resolvedProperties = pom.getProperties();
                Set<String> compilerLevels = new LinkedHashSet<>(localCompilerLevelProperties);
                addCompilerLevelProperties(pom.getPlugins(), compilerLevels);
                addCompilerLevelProperties(pom.getPluginManagement(), compilerLevels);
                // Properties set to the Java version themselves, instead of the property they reference
                Set<String> pinnedProperties = new HashSet<>();
                Set<String> javaVersionProperties = javaVersionProperties(compilerLevels, resolvedProperties, pinnedProperties);

                // Return early if the parent is within the current repository, as properties defined there will be updated
                if (getResolutionResult().parentPomIsProjectPom()) {
                    // Unless this pom redefines a property itself, as that value shadows whatever the parent defines
                    Map<String, String> requestedProperties = pom.getRequested().getProperties();
                    for (String property : javaVersionProperties) {
                        if (!requestedProperties.containsKey(property)) {
                            continue;
                        }
                        if (referencesPropertyLeftToParent(property, requestedProperties, resolvedProperties, javaVersionProperties, compilerLevels)) {
                            pinnedProperties.add(property);
                        }
                        if (isBelowTargetVersion(comparedValue(property, resolvedProperties, pinnedProperties))) {
                            d = (Xml.Document) new AddPropertyVisitor(property, String.valueOf(version), null)
                                    .visitNonNull(d, ctx);
                            maybeUpdateModel();
                        }
                    }
                    return d;
                }

                // Otherwise override remote parent's properties locally
                boolean foundProperty = false;
                for (String property : javaVersionProperties) {
                    if (resolvedProperties.get(property) != null) {
                        foundProperty = true;
                        if (isBelowTargetVersion(comparedValue(property, resolvedProperties, pinnedProperties))) {
                            d = (Xml.Document) new AddProperty(property, String.valueOf(version), null, false)
                                    .getVisitor()
                                    .visitNonNull(d, ctx);
                            maybeUpdateModel();
                        }
                    }
                }

                // When none of the relevant properties are explicitly configured Maven defaults to Java 8
                // The release option was added in 9
                // If no properties have yet been updated then set release explicitly
                if (!foundProperty && version >= 9 && !compilerPluginConfiguredExplicitly) {
                    d = (Xml.Document) new AddProperty("maven.compiler.release", String.valueOf(version), null, false)
                            .getVisitor()
                            .visitNonNull(d, ctx);
                    maybeUpdateModel();
                }

                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isPluginTag(Plugin.PLUGIN_DEFAULT_GROUPID, "maven-compiler-plugin")) {
                    t.getChild("configuration").ifPresent(configuration -> collectCompilerLevels(configuration, true));
                    t.getChild("executions").ifPresent(executions -> {
                        for (Xml.Tag execution : executions.getChildren("execution")) {
                            execution.getChild("configuration").ifPresent(configuration -> collectCompilerLevels(configuration, false));
                        }
                    });
                }
                Optional<String> value = t.getValue();
                if (value.isPresent()) {
                    String referenced = referencedProperty(value.get());
                    if (referenced != null && (isPropertyTag() || isProfilePropertyTag())) {
                        referencingProperties.computeIfAbsent(referenced, k -> new HashSet<>()).add(t.getName());
                    } else if (referenced == null || !COMPILER_LEVEL_CONFIGURATION.contains(t.getName()) ||
                               !COMPILER_PLUGIN_CONFIGURATION_MATCHER.matches(getCursor()) &&
                               !COMPILER_PLUGIN_EXECUTION_CONFIGURATION_MATCHER.matches(getCursor())) {
                        // Anything but a level of the compiler plugin, or of one of its executions, is another use
                        otherwiseReferencedProperties.addAll(ResolvedPom.placeholderHelper.getPlaceholders(value.get()));
                    }
                }
                return t;
            }

            private void collectCompilerLevels(Xml.Tag configuration, boolean ofPlugin) {
                for (String level : COMPILER_LEVEL_CONFIGURATION) {
                    configuration.getChildValue(level).ifPresent(compilerLevel -> {
                        // The test levels default to the main ones, so only those configure the level of the compiler plugin
                        compilerPluginConfiguredExplicitly |= ofPlugin && !level.startsWith("test");
                        addCompilerLevelProperty(compilerLevel, localCompilerLevelProperties);
                    });
                }
            }

            // Whatever the compiler plugin routes its levels through is a Java version property, regardless of its name
            private Set<String> javaVersionProperties(Set<String> compilerLevels, Map<String, String> pomProperties,
                                                      Set<String> pinnedProperties) {
                Set<String> properties = new LinkedHashSet<>(JAVA_VERSION_PROPERTIES);
                properties.addAll(compilerLevels);
                Set<String> followedFrom = new LinkedHashSet<>(properties);
                if (!compilerLevels.contains("release.version")) {
                    // Unless the compiler uses it, `release.version` may be the version of a release rather than of Java
                    followedFrom.remove("release.version");
                }

                // A property that only references another one, such as `maven.compiler.release` set to
                // `${custom.release}` by a parent, takes its value from that one, possibly over several levels.
                // Raising a property this pom uses for something else as well would change that too,
                // so whatever references it is set to the Java version instead
                Set<String> usedOtherwise = new HashSet<>();
                while (true) {
                    pinnedProperties.clear();
                    Set<String> followed = followReferences(properties, followedFrom, pomProperties, usedOtherwise, pinnedProperties);
                    boolean foundUsedOtherwise = false;
                    for (String property : followed) {
                        if (!properties.contains(property) && isReferencedOtherwise(property, followed)) {
                            foundUsedOtherwise |= usedOtherwise.add(property);
                        }
                    }
                    if (!foundUsedOtherwise) {
                        return followed;
                    }
                }
            }

            private Set<String> followReferences(Set<String> properties, Set<String> followedFrom, Map<String, String> pomProperties,
                                                 Set<String> usedOtherwise, Set<String> pinnedProperties) {
                Set<String> followed = new LinkedHashSet<>(properties);
                Deque<String> toFollow = new ArrayDeque<>(followedFrom);
                while (!toFollow.isEmpty()) {
                    String referencing = toFollow.poll();
                    String referenced = referencedProperty(pomProperties.get(referencing));
                    if (referenced == null) {
                        continue;
                    }
                    if (usedOtherwise.contains(referenced)) {
                        pinnedProperties.add(referencing);
                    } else if (followed.add(referenced)) {
                        toFollow.add(referenced);
                    }
                }
                return followed;
            }

            private boolean isReferencedOtherwise(String property, Set<String> javaVersionProperties) {
                if (otherwiseReferencedProperties.contains(property)) {
                    return true;
                }
                for (String referencing : referencingProperties.getOrDefault(property, emptySet())) {
                    if (!javaVersionProperties.contains(referencing)) {
                        return true;
                    }
                }
                return false;
            }

            // A property of this pom that only references a property of a parent in the project, which that parent does not
            // update, would keep the old version, so it gets the Java version itself
            private boolean referencesPropertyLeftToParent(String property, Map<String, String> requestedProperties,
                                                           Map<String, String> resolvedProperties, Set<String> javaVersionProperties,
                                                           Set<String> compilerLevels) {
                String referenced = referencedProperty(requestedProperties.get(property));
                if (referenced == null || requestedProperties.containsKey(referenced) || !resolvedProperties.containsKey(referenced) ||
                    JAVA_VERSION_PROPERTIES.contains(referenced) || compilerLevels.contains(referenced)) {
                    return false;
                }
                String reference = "${" + referenced + "}";
                boolean updatedByParent = false;
                for (Map.Entry<String, String> inherited : resolvedProperties.entrySet()) {
                    String value = inherited.getValue();
                    if (requestedProperties.containsKey(inherited.getKey()) || value == null || !value.contains(reference)) {
                        continue;
                    }
                    if (!javaVersionProperties.contains(inherited.getKey()) || !referenced.equals(referencedProperty(value))) {
                        // The parent uses it for something else as well, so it leaves it as is
                        return true;
                    }
                    updatedByParent = true;
                }
                return !updatedByParent;
            }

            // A property is compared by the value it resolves to, unless it only references a property that is updated instead
            private @Nullable String comparedValue(String property, Map<String, String> resolvedProperties, Set<String> pinnedProperties) {
                String value = resolvedProperties.get(property);
                if (value == null || (referencedProperty(value) != null && !pinnedProperties.contains(property))) {
                    return null;
                }
                return ResolvedPom.placeholderHelper.replacePlaceholders(value, resolvedProperties::get);
            }

            private boolean isBelowTargetVersion(@Nullable String value) {
                if (value == null) {
                    return false;
                }
                try {
                    return Float.parseFloat(value) < version;
                } catch (NumberFormatException ex) {
                    // either an expression or something else, don't touch
                    return false;
                }
            }

            private void addCompilerLevelProperties(List<Plugin> plugins, Set<String> properties) {
                for (Plugin plugin : plugins) {
                    if (Plugin.PLUGIN_DEFAULT_GROUPID.equals(plugin.getGroupId()) &&
                        "maven-compiler-plugin".equals(plugin.getArtifactId())) {
                        addCompilerLevelProperties(plugin.getConfiguration(), properties);
                        for (Plugin.Execution execution : plugin.getExecutions()) {
                            addCompilerLevelProperties(execution.getConfiguration(), properties);
                        }
                    }
                }
            }

            private void addCompilerLevelProperties(@Nullable JsonNode configuration, Set<String> properties) {
                if (configuration != null) {
                    for (String level : COMPILER_LEVEL_CONFIGURATION) {
                        addCompilerLevelProperty(configuration.path(level).asText(""), properties);
                    }
                }
            }

            private void addCompilerLevelProperty(String compilerLevel, Set<String> properties) {
                String referenced = referencedProperty(compilerLevel);
                if (referenced != null) {
                    properties.add(referenced);
                }
            }

            // The name of the property a value consists of, if the value is nothing but a reference to it
            private @Nullable String referencedProperty(@Nullable String value) {
                if (value == null) {
                    return null;
                }
                Matcher propertyReference = PROPERTY_REFERENCE.matcher(value.trim());
                return propertyReference.matches() ? propertyReference.group(1) : null;
            }
        };
    }
}
