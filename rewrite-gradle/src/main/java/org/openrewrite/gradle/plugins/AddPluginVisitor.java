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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.gradle.plugins.GradlePluginUtils.availablePluginVersions;

@Incubating(since = "7.33.0")
@Value
@EqualsAndHashCode(callSuper = false)
public class AddPluginVisitor extends GroovyIsoVisitor<ExecutionContext> {
    String pluginId;

    @Nullable
    String newVersion;

    @Nullable
    String versionPattern;

    public static Optional<String> resolvePluginVersion(String pluginId, @Nullable String newVersion, @Nullable String versionPattern, ExecutionContext ctx) {
        Optional<String> version;
        if (newVersion == null) {
            version = Optional.empty();
        } else {
            VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
            assert versionComparator != null;

            if (versionComparator instanceof ExactVersion) {
                version = versionComparator.upgrade("0", singletonList(newVersion));
            } else {
                version = versionComparator.upgrade("0", availablePluginVersions(pluginId, ctx));
            }
        }
        return version;
    }

    @Override
    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
        if (FindPlugins.find(cu, pluginId).isEmpty()) {
            Optional<String> version = resolvePluginVersion(pluginId, newVersion, versionPattern, ctx);

            AtomicInteger singleQuote = new AtomicInteger();
            AtomicInteger doubleQuote = new AtomicInteger();
            new GroovyIsoVisitor<Integer>() {
                final MethodMatcher pluginIdMatcher = new MethodMatcher("PluginSpec id(..)");

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, integer);
                    if (pluginIdMatcher.matches(m)) {
                        if (m.getArguments().get(0) instanceof J.Literal) {
                            J.Literal l = (J.Literal) m.getArguments().get(0);
                            assert l.getValueSource() != null;
                            if (l.getValueSource().startsWith("'")) {
                                singleQuote.incrementAndGet();
                            } else {
                                doubleQuote.incrementAndGet();
                            }
                        }
                    }
                    return m;
                }
            }.visitCompilationUnit(cu, 0);

            String delimiter = singleQuote.get() < doubleQuote.get() ? "\"" : "'";
            List<Statement> statements = GradleParser.builder().build()
                    .parseInputs(
                            singletonList(
                                    Parser.Input.fromString("plugins {\n" +
                                                            "    id " + delimiter + pluginId + delimiter + (version.map(s -> " version " + delimiter + s + delimiter).orElse("")) + "\n" +
                                                            "}")),
                            null,
                            ctx
                    )
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"))
                    .getStatements();

            if (FindMethods.find(cu, "RewriteGradleProject plugins(..)").isEmpty() && FindMethods.find(cu, "RewriteSettings plugins(..)").isEmpty()) {
                Space leadingSpace = Space.firstPrefix(cu.getStatements());
                if (cu.getSourcePath().endsWith(Paths.get("settings.gradle"))
                    && !cu.getStatements().isEmpty()
                    && cu.getStatements().get(0) instanceof J.MethodInvocation
                    && ((J.MethodInvocation) cu.getStatements().get(0)).getSimpleName().equals("pluginManagement")) {
                    return cu.withStatements(ListUtils.concatAll(ListUtils.concat(cu.getStatements().get(0), Space.formatFirstPrefix(statements, leadingSpace.withWhitespace("\n\n" + leadingSpace.getWhitespace()))),
                            Space.formatFirstPrefix(cu.getStatements().subList(1, cu.getStatements().size()), leadingSpace.withWhitespace("\n\n" + leadingSpace.getWhitespace()))));
                } else {
                    return cu.withStatements(ListUtils.concatAll(statements,
                            Space.formatFirstPrefix(cu.getStatements(), leadingSpace.withWhitespace("\n\n" + leadingSpace.getWhitespace()))));
                }
            } else {
                MethodMatcher buildPluginsMatcher = new MethodMatcher("RewriteGradleProject plugins(groovy.lang.Closure)");
                MethodMatcher settingsPluginsMatcher = new MethodMatcher("RewriteSettings plugins(groovy.lang.Closure)");
                return cu.withStatements(ListUtils.map(cu.getStatements(), stat -> {
                    if (stat instanceof J.MethodInvocation) {
                        J.MethodInvocation m = (J.MethodInvocation) stat;
                        if (buildPluginsMatcher.matches(m) || settingsPluginsMatcher.matches(m)) {
                            J.MethodInvocation pluginDef = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) ((J.MethodInvocation) statements.get(0)).getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
                            m = m.withArguments(ListUtils.map(m.getArguments(), a -> {
                                if (a instanceof J.Lambda) {
                                    J.Lambda l = (J.Lambda) a;
                                    J.Block b = (J.Block) l.getBody();
                                    List<Statement> pluginStatements = b.getStatements();
                                    if (!pluginStatements.isEmpty() && pluginStatements.get(pluginStatements.size() - 1) instanceof J.Return) {
                                        Statement last = pluginStatements.remove(pluginStatements.size() - 1);
                                        Expression lastExpr = requireNonNull(((J.Return) last).getExpression());
                                        pluginStatements.add(lastExpr.withPrefix(last.getPrefix()));
                                    }
                                    pluginStatements.add(pluginDef);
                                    return l.withBody(autoFormat(b.withStatements(pluginStatements), ctx, getCursor()));
                                }
                                return a;
                            }));
                            return m;
                        }
                    }
                    return stat;
                }));
            }
        }
        return super.visitCompilationUnit(cu, ctx);
    }
}
