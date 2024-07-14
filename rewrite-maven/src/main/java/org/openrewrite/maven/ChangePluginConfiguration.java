/*
 * Copyright 2021 the original author or authors.
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
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XsltTransformation;
import org.openrewrite.xml.XsltTransformationVisitor;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePluginConfiguration extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build/plugins");

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "org.openrewrite.maven")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "rewrite-maven-plugin")
    String artifactId;

    @Language("xml")
    @Option(displayName = "Configuration",
            description = "Plugin configuration provided as raw XML. Supplying `null` will remove any existing configuration.",
            example = "<foo>bar</foo>",
            required = false)
    @Nullable
    String configuration;

    @Nullable
    @Language("xml")
    @Option(displayName = "XSLT Configuration transformation",
            description = "The transformation to be applied on the <configuration> element.",
            example = "<xsl:stylesheet ...>...</xsl:stylesheet>",
            required = false)
    String xslt;

    @Nullable
    @Option(displayName = "XSLT Configuration transformation classpath resource",
            description = "The transformation to be applied on the <configuration> element provided as a classpath resource.",
            example = "/changePlugin.xslt",
            required = false)
    String xsltResource;

    @Override
    public String getDisplayName() {
        return "Change Maven plugin configuration";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("for `%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Apply the specified configuration to a Maven plugin. Will not add the plugin if it does not already exist in the pom.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("configuration", "Configuration set at most once", configuration,
                cfg -> Stream.of(configuration, xslt, xsltResource).filter(StringUtils::isBlank).count() >= 2));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugins = (Xml.Tag) super.visitTag(tag, ctx);
                if (PLUGINS_MATCHER.matches(getCursor())) {
                    Optional<Xml.Tag> maybePlugin = plugins.getChildren().stream()
                            .filter(plugin ->
                                    "plugin".equals(plugin.getName()) &&
                                    groupId.equals(plugin.getChildValue("groupId").orElse(null)) &&
                                    artifactId.equals(plugin.getChildValue("artifactId").orElse(null))
                            )
                            .findAny();
                    if (maybePlugin.isPresent()) {
                        Xml.Tag plugin = maybePlugin.get();
                        if (configuration == null && xslt == null && xsltResource == null) {
                            plugins = filterChildren(plugins, plugin,
                                    child -> !(child instanceof Xml.Tag && "configuration".equals(((Xml.Tag) child).getName())));
                        } else if (configuration != null) {
                            plugins = addOrUpdateChild(plugins, plugin,
                                    Xml.Tag.build("<configuration>\n" + configuration + "\n</configuration>"),
                                    getCursor().getParentOrThrow());
                        } else {
                            // Implied that xslt or xsltResource is not null
                            Optional<Xml.Tag> configurationTag = plugin.getChild("configuration");
                            if (configurationTag.isPresent()) {
                                String xsltTransformation = loadResource(xslt, xsltResource);
                                plugins = addOrUpdateChild(plugins, plugin,
                                        XsltTransformationVisitor.transformTag(configurationTag.get().printTrimmed(getCursor()), xsltTransformation),
                                        getCursor().getParentOrThrow());
                            }
                        }
                    }
                }
                return plugins;
            }
        };
    }

    private static String loadResource(@Nullable String xslt, @Nullable String xsltResource) {
        if (StringUtils.isBlank(xsltResource)) {
            return requireNonNull(xslt);
        }
        try (InputStream is = XsltTransformation.class.getResourceAsStream(xsltResource)) {
            assert is != null;
            return StringUtils.readFully(is, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
