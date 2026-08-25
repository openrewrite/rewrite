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
            "release.version");

    private static final List<XPathMatcher> JAVA_VERSION_XPATH_MATCHERS =
            JAVA_VERSION_PROPERTIES.stream()
                    .map(property -> "/project/properties/" + property)
                    .map(XPathMatcher::new).collect(toList());

    private static final List<String> COMPILER_LEVEL_CONFIGURATION = Arrays.asList("source", "target", "release");

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
               " * `release.version`\n\n" +
               "Properties of any other name are updated too when the `maven-compiler-plugin` `source`, `target` or `release` " +
               "configuration of this pom, or of a pom it inherits from, resolves to them.\n\n" +
               "If none of these properties are in use and the maven compiler plugin is not otherwise configured, adds the `maven.compiler.release` property.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            final Set<String> compilerLevelProperties = new LinkedHashSet<>();

            boolean compilerPluginConfiguredExplicitly;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                // Update properties already defined in the current pom
                Xml.Document d = super.visitDocument(document, ctx);

                Collection<String> javaVersionProperties = javaVersionProperties();

                // Return early if the parent is within the current repository, as properties defined there will be updated
                if (getResolutionResult().parentPomIsProjectPom()) {
                    // Unless this pom redefines a property itself, as that value shadows whatever the parent defines
                    Map<String, String> requestedProperties = getResolutionResult().getPom().getRequested().getProperties();
                    Map<String, String> resolvedProperties = getResolutionResult().getPom().getProperties();
                    for (String property : javaVersionProperties) {
                        String propertyValue = resolvedProperties.get(property);
                        if (requestedProperties.containsKey(property) && propertyValue != null) {
                            try {
                                if (Float.parseFloat(propertyValue) < version) {
                                    d = (Xml.Document) new AddPropertyVisitor(property, String.valueOf(version), null)
                                            .visitNonNull(d, ctx);
                                    maybeUpdateModel();
                                }
                            } catch (NumberFormatException ex) {
                                // either an expression or something else, don't touch
                            }
                        }
                    }
                    return d;
                }

                // Otherwise override remote parent's properties locally
                Map<String, String> currentProperties = getResolutionResult().getPom().getProperties();
                boolean foundProperty = false;
                for (String property : javaVersionProperties) {
                    String propertyValue = currentProperties.get(property);
                    if (propertyValue != null) {
                        foundProperty = true;
                        try {
                            if (Float.parseFloat(propertyValue) < version) {
                                d = (Xml.Document) new AddProperty(property, String.valueOf(version), null, false)
                                        .getVisitor()
                                        .visitNonNull(d, ctx);
                                maybeUpdateModel();
                            }
                        } catch (NumberFormatException ex) {
                            // either an expression or something else, don't touch
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
                if (isPluginTag("org.apache.maven.plugins", "maven-compiler-plugin")) {
                    t.getChild("configuration").ifPresent(compilerPluginConfig -> {
                        for (String configuration : COMPILER_LEVEL_CONFIGURATION) {
                            Optional<String> compilerLevel = compilerPluginConfig.getChildValue(configuration);
                            if (compilerLevel.isPresent()) {
                                compilerPluginConfiguredExplicitly = true;
                                addCompilerLevelProperty(compilerLevel.get(), compilerLevelProperties);
                            }
                        }
                    });
                }
                return t;
            }

            // Whatever the compiler plugin routes its level through is a Java version property, regardless of its name
            private Collection<String> javaVersionProperties() {
                Set<String> properties = new LinkedHashSet<>(JAVA_VERSION_PROPERTIES);
                properties.addAll(compilerLevelProperties);
                ResolvedPom pom = getResolutionResult().getPom();
                addCompilerLevelProperties(pom.getPlugins(), properties);
                addCompilerLevelProperties(pom.getPluginManagement(), properties);
                return properties;
            }

            private void addCompilerLevelProperties(List<Plugin> plugins, Set<String> properties) {
                for (Plugin plugin : plugins) {
                    if ("org.apache.maven.plugins".equals(plugin.getGroupId()) &&
                        "maven-compiler-plugin".equals(plugin.getArtifactId())) {
                        JsonNode configuration = plugin.getConfiguration();
                        if (configuration != null) {
                            for (String name : COMPILER_LEVEL_CONFIGURATION) {
                                addCompilerLevelProperty(configuration.path(name).asText(""), properties);
                            }
                        }
                    }
                }
            }

            private void addCompilerLevelProperty(String compilerLevel, Set<String> properties) {
                Matcher propertyReference = PROPERTY_REFERENCE.matcher(compilerLevel.trim());
                if (propertyReference.matches()) {
                    properties.add(propertyReference.group(1));
                }
            }
        };
    }
}
