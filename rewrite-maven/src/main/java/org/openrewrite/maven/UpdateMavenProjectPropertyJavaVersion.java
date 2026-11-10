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
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
               "so that anything else using it stays in sync.\n\n" +
               "If none of these properties are in use and the maven compiler plugin is not otherwise configured, adds the `maven.compiler.release` property.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);

                ResolvedPom pom = getResolutionResult().getPom();
                Map<String, String> resolvedProperties = pom.getProperties();
                List<JsonNode> compilerConfigurations = compilerPluginConfigurations(pom);
                Set<String> compilerLevels = compilerLevelProperties(compilerConfigurations);
                Set<String> javaVersionProperties = javaVersionProperties(compilerLevels, resolvedProperties);

                // Return early if the parent is within the current repository, as properties defined there will be updated
                MavenResolutionResult parent = getResolutionResult().getParent();
                if (parent != null && getResolutionResult().parentPomIsProjectPom()) {
                    // Unless this pom redefines a property itself, as that value shadows whatever the parent defines
                    Map<String, String> requestedProperties = pom.getRequested().getProperties();
                    for (String property : javaVersionProperties) {
                        if (!requestedProperties.containsKey(property)) {
                            continue;
                        }
                        boolean pinned = referencesPropertyLeftToParent(property, requestedProperties, parent.getPom().getProperties(),
                                javaVersionProperties, compilerLevels);
                        if (isBelowTargetVersion(comparedValue(property, pom, pinned))) {
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
                        if (isBelowTargetVersion(comparedValue(property, pom, false))) {
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
                if (!foundProperty && version >= 9 && !configuresCompilerLevel(compilerConfigurations)) {
                    d = (Xml.Document) new AddProperty("maven.compiler.release", String.valueOf(version), null, false)
                            .getVisitor()
                            .visitNonNull(d, ctx);
                    maybeUpdateModel();
                }

                return d;
            }

            // Whatever the compiler plugin routes its levels through is a Java version property, regardless of its name
            private Set<String> javaVersionProperties(Set<String> compilerLevels, Map<String, String> pomProperties) {
                Set<String> properties = new LinkedHashSet<>(JAVA_VERSION_PROPERTIES);
                properties.addAll(compilerLevels);
                Deque<String> toFollow = new ArrayDeque<>(properties);
                if (!compilerLevels.contains("release.version")) {
                    // Unless the compiler uses it, `release.version` may be the version of a release rather than of Java
                    toFollow.remove("release.version");
                }

                // A property that only references another one, such as `maven.compiler.release` set to
                // `${custom.release}` by a parent, takes its value from that one, possibly over several levels
                while (!toFollow.isEmpty()) {
                    String referenced = referencedProperty(pomProperties.get(toFollow.poll()));
                    if (referenced != null && properties.add(referenced)) {
                        toFollow.add(referenced);
                    }
                }
                return properties;
            }

            // A property of this pom that only references a property of a parent in the project, which that parent does not
            // update, would keep the old version, so it gets the Java version itself
            private boolean referencesPropertyLeftToParent(String property, Map<String, String> requestedProperties,
                                                           Map<String, String> parentProperties, Set<String> javaVersionProperties,
                                                           Set<String> compilerLevels) {
                String referenced = referencedProperty(requestedProperties.get(property));
                if (referenced == null || requestedProperties.containsKey(referenced) || !parentProperties.containsKey(referenced) ||
                    JAVA_VERSION_PROPERTIES.contains(referenced) || compilerLevels.contains(referenced)) {
                    return false;
                }
                for (Map.Entry<String, String> parentProperty : parentProperties.entrySet()) {
                    if (javaVersionProperties.contains(parentProperty.getKey()) &&
                        referenced.equals(referencedProperty(parentProperty.getValue()))) {
                        return false;
                    }
                }
                return true;
            }

            // A property is compared by the value it resolves to, unless it only references a property that is updated instead
            private @Nullable String comparedValue(String property, ResolvedPom pom, boolean pinned) {
                String value = pom.getProperties().get(property);
                if (value == null || (referencedProperty(value) != null && !pinned)) {
                    return null;
                }
                return pom.getValue(value);
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

            // The configuration of the compiler plugin and of each of its executions, whether declared here or inherited
            private List<JsonNode> compilerPluginConfigurations(ResolvedPom pom) {
                List<JsonNode> configurations = new ArrayList<>();
                for (List<Plugin> plugins : Arrays.asList(pom.getPlugins(), pom.getPluginManagement())) {
                    for (Plugin plugin : plugins) {
                        if (Plugin.PLUGIN_DEFAULT_GROUPID.equals(plugin.getGroupId()) &&
                            "maven-compiler-plugin".equals(plugin.getArtifactId())) {
                            if (plugin.getConfiguration() != null) {
                                configurations.add(plugin.getConfiguration());
                            }
                            for (Plugin.Execution execution : plugin.getExecutions()) {
                                if (execution.getConfiguration() != null) {
                                    configurations.add(execution.getConfiguration());
                                }
                            }
                        }
                    }
                }
                return configurations;
            }

            private Set<String> compilerLevelProperties(List<JsonNode> compilerConfigurations) {
                Set<String> properties = new LinkedHashSet<>();
                for (JsonNode configuration : compilerConfigurations) {
                    for (String level : COMPILER_LEVEL_CONFIGURATION) {
                        String referenced = referencedProperty(configuration.path(level).asText(""));
                        if (referenced != null) {
                            properties.add(referenced);
                        }
                    }
                }
                return properties;
            }

            // The test levels default to the main ones, so only those configure the level of the compiler plugin
            private boolean configuresCompilerLevel(List<JsonNode> compilerConfigurations) {
                for (JsonNode configuration : compilerConfigurations) {
                    for (String level : COMPILER_LEVEL_CONFIGURATION) {
                        if (!level.startsWith("test") && configuration.hasNonNull(level)) {
                            return true;
                        }
                    }
                }
                return false;
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
