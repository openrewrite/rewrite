/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.Parser;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.search.FindPlugins;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.openrewrite.gradle.plugins.GradlePluginUtils.availablePluginVersions;

@Incubating(since = "7.33.0")
@Value
@EqualsAndHashCode(callSuper = false)
public class AddPluginVisitor extends GroovyIsoVisitor<ExecutionContext> {
    String pluginIdPattern;
    String newVersion;

    @Nullable
    String versionPattern;

    @Override
    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;

        if (FindPlugins.find(cu, pluginIdPattern).isEmpty()) {
            Optional<String> version;
            if (versionComparator instanceof ExactVersion) {
                version = versionComparator.upgrade("0", singletonList(newVersion));
            } else {
                version = versionComparator.upgrade("0", availablePluginVersions(pluginIdPattern, ctx));
            }

            if (version.isPresent()) {
                List<Statement> statements = GradleParser.builder().build()
                        .parseInputs(
                                singletonList(
                                        Parser.Input.fromString(Paths.get("settings.gradle"),
                                                "plugins {\n" +
                                                "    id '" + pluginIdPattern + "' version '" + version.get() + "'\n" +
                                                "}")),
                                null,
                                ctx
                        )
                        .get(0)
                        .getStatements();

                if (FindMethods.find(cu, "RewriteSettings plugins(..)").isEmpty()) {
                    Space leadingSpace = Space.firstPrefix(cu.getStatements());
                    return cu.withStatements(ListUtils.concatAll(statements,
                            Space.formatFirstPrefix(cu.getStatements(), leadingSpace.withWhitespace("\n\n" + leadingSpace.getWhitespace()))));
                } else {
                    MethodMatcher settingsPluginsMatcher = new MethodMatcher("RewriteSettings plugins(groovy.lang.Closure)");
                    MethodMatcher buildPluginsMatcher = new MethodMatcher("RewriteGradleProject plugins(groovy.lang.Closure)");
                    return cu.withStatements(ListUtils.map(cu.getStatements(), stat -> {
                        JavaTemplate addPlugin = JavaTemplate.builder(this::getCursor, "#{any(Plugin)}").build();
                        if (stat instanceof J.MethodInvocation) {
                            J.MethodInvocation m = (J.MethodInvocation) stat;
                            if (settingsPluginsMatcher.matches(m) || buildPluginsMatcher.matches(m)) {
                                Set<J> pluginDef = FindMethods.find(statements.get(0), "Plugin version(..)");
                                m = m.withTemplate(
                                        addPlugin,
                                        ((J.Block) ((J.Lambda) m.getArguments().get(0))
                                                .getBody()).getCoordinates().firstStatement(),
                                        pluginDef.iterator().next()
                                );
                                return m;
                            }
                        }
                        return stat;
                    }));
                }

            }
        }
        return super.visitCompilationUnit(cu, ctx);
    }
}
