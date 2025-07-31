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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build//plugins");

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    @Override
    public String getDisplayName() {
        return "Update Maven Java project properties";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "The Java version is determined by several project properties, including:\n\n" +
               " * `java.version`\n" +
               " * `jdk.version`\n" +
               " * `javaVersion`\n" +
               " * `jdkVersion`\n" +
               " * `maven.compiler.source`\n" +
               " * `maven.compiler.target`\n" +
               " * `maven.compiler.release`\n" +
               " * `release.version`\n\n" +
               "If none of these properties are in use and the maven compiler plugin is not otherwise configured, adds the `maven.compiler.release` property.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            boolean compilerPluginConfiguredExplicitly;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                // Update properties already defined in the current pom
                Xml.Document d = super.visitDocument(document, ctx);

                // Return early if the parent appears to be within the current repository, as properties defined there will be updated
                if (getResolutionResult().getParent() != null && getResolutionResult().parentPomIsProjectPom()) {
                    // Unless the plugin config in the parent defines source/target/release with a property
                    for (Plugin plugin : getResolutionResult().getParent().getPom().getPlugins()) {
                        if ("org.apache.maven.plugins".equals(plugin.getGroupId()) && "maven-compiler-plugin".equals(plugin.getArtifactId()) && plugin.getConfiguration() != null) {
                            for (String property : JAVA_VERSION_PROPERTIES) {
                                if (getResolutionResult().getPom().getRequested().getProperties().get(property) != null) {
                                    try {
                                        float parsed = Float.parseFloat(getResolutionResult().getPom().getProperties().get(property));
                                        if (parsed < version &&
                                            ((plugin.getConfiguration().get("source") != null && plugin.getConfiguration().get("source").textValue().contains(property)) ||
                                            (plugin.getConfiguration().get("target") != null && plugin.getConfiguration().get("target").textValue().contains(property)) ||
                                            (plugin.getConfiguration().get("release") != null && plugin.getConfiguration().get("release").textValue().contains(property)))) {
                                            d = (Xml.Document) new AddPropertyVisitor(property, String.valueOf(version), null)
                                                    .visitNonNull(d, ctx);
                                            maybeUpdateModel();
                                        }
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        }
                    }
                    return d;
                }

                // Otherwise override remote parent's properties locally
                Map<String, String> currentProperties = getResolutionResult().getPom().getProperties();
                boolean foundProperty = false;
                for (String property : JAVA_VERSION_PROPERTIES) {
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
                        if (compilerPluginConfig.getChildValue("source").isPresent() ||
                            compilerPluginConfig.getChildValue("target").isPresent() ||
                            compilerPluginConfig.getChildValue("release").isPresent()) {
                            compilerPluginConfiguredExplicitly = true;
                        }
                    });
                }
                return t;
            }
        };
    }
}
