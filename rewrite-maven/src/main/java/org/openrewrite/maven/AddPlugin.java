/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.PathUtils;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddPlugin extends Recipe {

    private static final XPathMatcher BUILD_MATCHER = new XPathMatcher("/project/build");

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "org.openrewrite.maven")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "rewrite-maven-plugin")
    String artifactId;

    @Option(displayName = "Version",
            description = "A fixed version of the plugin to add.",
            example = "1.0.0",
            required = false)
    @Nullable
    String version;

    @Language("xml")
    @Option(displayName = "Configuration",
            description = "Optional plugin configuration provided as raw XML",
            example = "<configuration><foo>foo</foo></configuration>",
            required = false)
    @Nullable
    String configuration;

    @Option(displayName = "Dependencies",
            description = "Optional plugin dependencies provided as raw XML.",
            example = "<dependencies><dependency><groupId>com.yourorg</groupId><artifactId>core-lib</artifactId><version>1.0.0</version></dependency></dependencies>",
            required = false)
    @Nullable
    String dependencies;

    @Option(displayName = "Executions",
            description = "Optional executions provided as raw XML.",
            example = "<executions><execution><phase>generate-sources</phase><goals><goal>add-source</goal></goals></execution></executions>",
            required = false)
    @Nullable
    String executions;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                    "Multiple patterns may be specified, separated by a semicolon `;`. " +
                    "If multiple patterns are supplied any of the patterns matching will be interpreted as a match. " +
                    "When not set, all source files are searched. ",
            required = false,
            example = "**/*-parent/grpc-*/pom.xml")
    @Nullable
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Add Maven plugin";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Add the specified Maven plugin to the pom.xml.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddPluginVisitor();
    }

    private class AddPluginVisitor extends MavenIsoVisitor<ExecutionContext> {

        @Override
        public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
            if (filePattern != null) {
                return PathUtils.matchesGlob(sourceFile.getSourcePath(), filePattern) && super.isAcceptable(sourceFile, ctx);
            }
            return super.isAcceptable(sourceFile, ctx);
        }

        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
            Xml.Tag root = document.getRoot();
            if (!root.getChild("build").isPresent()) {
                document = (Xml.Document) new AddToTagVisitor<>(root, Xml.Tag.build("<build/>"))
                        .visitNonNull(document, ctx, getCursor().getParentOrThrow());
            }
            return super.visitDocument(document, ctx);
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);

            if (BUILD_MATCHER.matches(getCursor())) {
                Optional<Xml.Tag> maybePlugins = t.getChild("plugins");
                Xml.Tag plugins;
                if(maybePlugins.isPresent()) {
                    plugins = maybePlugins.get();
                } else {
                    t = (Xml.Tag) new AddToTagVisitor<>(t, Xml.Tag.build("<plugins/>")).visitNonNull(t, ctx, getCursor().getParentOrThrow());
                    //noinspection OptionalGetWithoutIsPresent
                    plugins = t.getChild("plugins").get();
                }

                Optional<Xml.Tag> maybePlugin = plugins.getChildren().stream()
                        .filter(plugin ->
                                "plugin".equals(plugin.getName()) &&
                                        groupId.equals(plugin.getChildValue("groupId").orElse(null)) &&
                                        artifactId.equals(plugin.getChildValue("artifactId").orElse(null))
                        )
                        .findAny();

                if (maybePlugin.isPresent()) {
                    Xml.Tag plugin = maybePlugin.get();
                    if (version != null && !version.equals(plugin.getChildValue("version").orElse(null))) {
                        if (plugin.getChild("version").isPresent()) {
                            t = (Xml.Tag) new ChangeTagValueVisitor<>(plugin.getChild("version").get(), version).visitNonNull(t, ctx, getCursor().getParentOrThrow());
                        }
                    }
                } else {
                    Xml.Tag pluginTag = Xml.Tag.build("<plugin>\n" +
                            "<groupId>" + groupId + "</groupId>\n" +
                            "<artifactId>" + artifactId + "</artifactId>\n" +
                            (version != null ? "<version>" + version + "</version>\n" : "") +
                            (executions != null ? executions.trim() + "\n" : "") +
                            (configuration != null ? configuration.trim() + "\n" : "") +
                            (dependencies != null ? dependencies.trim() + "\n" : "") +
                            "</plugin>");
                    t = (Xml.Tag) new AddToTagVisitor<>(plugins, pluginTag).visitNonNull(t, ctx, getCursor().getParentOrThrow());
                }
            }

            return t;
        }
    }
}
