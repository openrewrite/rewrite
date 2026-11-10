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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateMavenProjectPropertyJavaVersion extends ScanningRecipe<Map<Path, Set<String>>> {

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

    // The test levels default to the main ones, so only those configure the level of the compiler plugin
    private static final List<String> MAIN_COMPILER_LEVEL_CONFIGURATION = Arrays.asList("source", "target", "release");

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
               "so that anything else using it stays in sync. " +
               "Each property is updated in the pom of the project that defines it, " +
               "or overridden in the topmost pom of the project when it is inherited from outside of it.\n\n" +
               "If none of these properties are in use and the maven compiler plugin is not otherwise configured, adds the `maven.compiler.release` property.";

    @Override
    public Map<Path, Set<String>> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<Path, Set<String>> propertiesToUpdateByPom) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult module = getResolutionResult();
                for (String property : javaVersionProperties(module.getPom())) {
                    String value = module.getPom().getProperties().get(property);
                    // A property that only references another one keeps doing so, as the referenced one is updated
                    if (value != null && referencedProperty(value) == null) {
                        Path definingPom = definingPom(module, property);
                        if (definingPom != null) {
                            propertiesToUpdateByPom.computeIfAbsent(definingPom, k -> new LinkedHashSet<>())
                                    .add(property);
                        }
                    }
                }
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<Path, Set<String>> propertiesToUpdateByPom) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = document;
                // Recipes that ran before this one in the same cycle may have changed the values the scan saw
                ResolvedPom pom = getResolutionResult().getPom();
                Set<String> properties = new LinkedHashSet<>(
                        propertiesToUpdateByPom.getOrDefault(document.getSourcePath(), emptySet()));
                // or added properties the scan could not see yet, such as an earlier migration to a lower version
                for (String property : javaVersionProperties(pom)) {
                    if (pom.getRequested().getProperties().containsKey(property)) {
                        properties.add(property);
                    }
                }
                for (String property : properties) {
                    if (isBelowTargetVersion(resolvedValue(pom, property))) {
                        d = (Xml.Document) new AddPropertyVisitor(property, String.valueOf(version), null)
                                .visitNonNull(d, ctx);
                    }
                }

                // When none of the relevant properties are explicitly configured Maven defaults to Java 8
                // The release option was added in 9
                if (getResolutionResult().getParent() == null && version >= 9 &&
                    !propertiesToUpdateByPom.containsKey(document.getSourcePath()) && !usesJavaVersionProperty(pom) &&
                    !configuresCompilerLevel(compilerPluginConfigurations(pom))) {
                    d = (Xml.Document) new AddPropertyVisitor("maven.compiler.release", String.valueOf(version), null)
                            .visitNonNull(d, ctx);
                }
                return d;
            }
        };
    }

    // The topmost pom of the project when the property is inherited from outside of it
    private static @Nullable Path definingPom(MavenResolutionResult module, String property) {
        MavenResolutionResult pom = module;
        MavenResolutionResult parent;
        while (!pom.getPom().getRequested().getProperties().containsKey(property) &&
               (parent = pom.getParent()) != null) {
            pom = parent;
        }
        return pom.getPom().getRequested().getSourcePath();
    }

    private static @Nullable String resolvedValue(ResolvedPom pom, String property) {
        String value = pom.getProperties().get(property);
        if (value == null || referencedProperty(value) != null) {
            return null;
        }
        return pom.getValue(value);
    }

    private static boolean usesJavaVersionProperty(ResolvedPom pom) {
        for (String property : javaVersionProperties(pom)) {
            if (pom.getProperties().get(property) != null) {
                return true;
            }
        }
        return false;
    }

    // Whatever the compiler plugin routes its levels through is a Java version property, regardless of its name
    private static Set<String> javaVersionProperties(ResolvedPom pom) {
        Set<String> compilerLevels = compilerLevelProperties(compilerPluginConfigurations(pom));
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
            String referenced = referencedProperty(pom.getProperties().get(toFollow.poll()));
            if (referenced != null && properties.add(referenced)) {
                toFollow.add(referenced);
            }
        }
        return properties;
    }

    private boolean isBelowTargetVersion(@Nullable String value) {
        if (value == null) {
            return false;
        }
        try {
            return Float.parseFloat(value) < version;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    // Includes configuration inherited from parents
    private static List<JsonNode> compilerPluginConfigurations(ResolvedPom pom) {
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

    private static Set<String> compilerLevelProperties(List<JsonNode> compilerConfigurations) {
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

    private static boolean configuresCompilerLevel(List<JsonNode> compilerConfigurations) {
        for (JsonNode configuration : compilerConfigurations) {
            for (String level : MAIN_COMPILER_LEVEL_CONFIGURATION) {
                if (configuration.hasNonNull(level)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @Nullable String referencedProperty(@Nullable String value) {
        if (value == null) {
            return null;
        }
        Matcher propertyReference = PROPERTY_REFERENCE.matcher(value.trim());
        return propertyReference.matches() ? propertyReference.group(1) : null;
    }
}
