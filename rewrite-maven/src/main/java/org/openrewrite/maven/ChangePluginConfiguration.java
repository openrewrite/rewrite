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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePluginConfiguration extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build/plugins");

    @Option(displayName = "Group",
            description = "The first part of the coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION' of the plugin to modify.",
            example = "org.openrewrite.maven")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION' of the plugin to modify.",
            example = "rewrite-maven-plugin")
    String artifactId;

    @Language("xml")
    @Option(displayName = "Configuration",
            description = "Plugin configuration provided as raw XML overriding any existing configuration. " +
                          "Configuration inside `<executions>` blocks will not be altered. " +
                          "Supplying `null` will remove any existing configuration. " +
                          "To include a literal `${...}` property reference in the configuration " +
                          "(e.g. a Maven property like `${java.version}`), escape it as `\\${...}` " +
                          "in your recipe YAML to prevent it from being resolved as a recipe placeholder.",
            example = "<foo>bar</foo>",
            required = false)
    @Nullable
    String configuration;

    String displayName = "Change Maven plugin configuration";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("for `%s:%s`", groupId, artifactId);
    }

    String description = "Apply the specified configuration to a Maven plugin. Will not add the plugin if it does not already exist in the pom.";

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
                        if (configuration == null) {
                            plugins = filterChildren(plugins, plugin,
                                    child -> !(child instanceof Xml.Tag && "configuration".equals(((Xml.Tag) child).getName())));
                        } else  {
                            plugins = addOrUpdateChild(plugins, plugin,
                                    Xml.Tag.build("<configuration>\n" + configuration + "\n</configuration>"),
                                    getCursor().getParentOrThrow());
                        }
                    }
                }
                return plugins;
            }
        };
    }
}
