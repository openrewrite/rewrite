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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePluginExecutions extends Recipe {
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
    @Option(displayName = "Executions",
            description = "Plugin goal executions provided as raw XML. Supplying `null` will remove any existing executions.",
            example = "<execution><phase>validate</phase><goals><goal>dryRun</goal></goals></execution>",
            required = false)
    @Nullable
    String executions;

    @Override
    public String getDisplayName() {
        return "Change Maven plugin executions";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("for `%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Apply the specified executions to a Maven plugin. Will not add the plugin if it does not already exist in the pom.";
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
                        if (executions == null) {
                            plugins = filterChildren(plugins, plugin, child -> !(child instanceof Xml.Tag && "executions".equals(((Xml.Tag) child).getName())));
                        } else {
                            plugins = addOrUpdateChild(plugins, plugin,
                                    Xml.Tag.build("<executions>\n" + executions + "\n</executions>"),
                                    getCursor().getParentOrThrow());
                        }
                    }
                }
                return plugins;
            }
        };
    }
}
