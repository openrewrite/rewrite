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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeToModelVersion410 extends Recipe {

    private static final XPathMatcher MODEL_VERSION_MATCHER = new XPathMatcher("/project/modelVersion");

    @Option(displayName = "New model version",
            description = "The model version to upgrade to. Defaults to `4.1.0`.",
            example = "4.1.0",
            required = false)
    @Nullable
    String newModelVersion;

    @Override
    public String getDisplayName() {
        return "Upgrade to Maven model version 4.1.0";
    }

    @Override
    public String getDescription() {
        return "Upgrades Maven POMs from model version 4.0.0 to 4.1.0, enabling new Maven 4 features " +
                "like `<subprojects>`, `bom` packaging, and automatic version inference. " +
                "This recipe updates the `<modelVersion>` element and namespace URLs from 4.0.0 to 4.1.0.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String targetVersion = newModelVersion != null ? newModelVersion : "4.1.0";
        String targetNamespace = "http://maven.apache.org/POM/" + targetVersion;
        String targetSchemaLocation = "http://maven.apache.org/xsd/maven-" + targetVersion + ".xsd";
        XPathMatcher projectMatcher = new XPathMatcher("/project");

        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                // Update modelVersion from 4.0.0 to target version
                if (MODEL_VERSION_MATCHER.matches(getCursor())) {
                    if (t.getValue().isPresent() && "4.0.0".equals(t.getValue().get())) {
                        t = t.withValue(targetVersion);
                    }
                }

                // Update namespace URLs on project element
                if (projectMatcher.matches(getCursor())) {
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), attr -> {
                        // Update xmlns namespace from 4.0.0 to target version
                        if (attr.getKeyAsString().equals("xmlns") &&
                            attr.getValueAsString().equals("http://maven.apache.org/POM/4.0.0")) {
                            return attr.withValue(
                                new Xml.Attribute.Value(
                                    attr.getValue().getId(),
                                    attr.getValue().getPrefix(),
                                    attr.getValue().getMarkers(),
                                    attr.getValue().getQuote(),
                                    targetNamespace
                                )
                            );
                        }

                        // Update xsi:schemaLocation to replace 4.0.0 URLs with target version
                        if (attr.getKeyAsString().equals("xsi:schemaLocation")) {
                            String schemaLocation = attr.getValueAsString();
                            // Replace both namespace and XSD URLs
                            String updatedSchemaLocation = schemaLocation
                                .replaceAll(Pattern.quote("http://maven.apache.org/POM/4.0.0"), targetNamespace)
                                .replaceAll(Pattern.quote("http://maven.apache.org/xsd/maven-4.0.0.xsd"), targetSchemaLocation);

                            if (!updatedSchemaLocation.equals(schemaLocation)) {
                                return attr.withValue(
                                    new Xml.Attribute.Value(
                                        attr.getValue().getId(),
                                        attr.getValue().getPrefix(),
                                        attr.getValue().getMarkers(),
                                        attr.getValue().getQuote(),
                                        updatedSchemaLocation
                                    )
                                );
                            }
                        }

                        return attr;
                    }));
                }

                return t;
            }
        };
    }
}
