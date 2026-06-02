/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.PathUtils;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddPluginVisitor extends MavenIsoVisitor<ExecutionContext> {
    private static final XPathMatcher BUILD_MATCHER = new XPathMatcher("/project/build");
    private static final XPathMatcher MANAGEMENT_MATCHER = new XPathMatcher("/project/build/pluginManagement");

    boolean asManagedPlugin;

    String groupId;

    String artifactId;

    @Nullable
    String version;

    @Language("xml")
    @Nullable
    String configuration;

    @Language("xml")
    @Nullable
    String dependencies;

    @Language("xml")
    @Nullable
    String executions;

    @Nullable
    String filePattern;

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        if (filePattern != null) {
            return PathUtils.matchesGlob(sourceFile.getSourcePath(), filePattern) && super.isAcceptable(sourceFile, ctx);
        }

        MavenResolutionResult mrr = sourceFile.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
        if (mrr == null || mrr.parentPomIsProjectPom()) {
            return false;
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
        Xml.Tag build = document.getRoot().getChild("build").get(); // we know its there we add it one line above!
        if (asManagedPlugin && !build.getChild("pluginManagement").isPresent()) {
            document = (Xml.Document) new AddToTagVisitor<>(build, Xml.Tag.build("<pluginManagement/>"))
                    .visitNonNull(document, ctx, getCursor().getParentOrThrow());
        }

        return super.visitDocument(document, ctx);
    }

    @Override
    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
        Xml.Tag t = super.visitTag(tag, ctx);

        if ( (!asManagedPlugin && BUILD_MATCHER.matches(getCursor())) ||
                (asManagedPlugin && MANAGEMENT_MATCHER.matches(getCursor())) ) {
            Optional<Xml.Tag> maybePlugins = t.getChild("plugins");
            Xml.Tag plugins;
            if (maybePlugins.isPresent()) {
                plugins = maybePlugins.get();
            } else {
                t = (Xml.Tag) new AddToTagVisitor<>(t, Xml.Tag.build("<plugins/>")).visitNonNull(t, ctx, getCursor().getParentOrThrow());
                //noinspection OptionalGetWithoutIsPresent
                plugins = t.getChild("plugins").get();
            }

            Optional<Xml.Tag> maybePlugin = plugins.getChildren().stream()
                    .filter(plugin ->
                            "plugin".equals(plugin.getName()) &&
                                    groupId.equals(plugin.getChildValue("groupId").orElse("org.apache.maven.plugins")) &&
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
                Xml.Tag pluginTag = Xml.Tag.build(
                        "<plugin>\n" +
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
