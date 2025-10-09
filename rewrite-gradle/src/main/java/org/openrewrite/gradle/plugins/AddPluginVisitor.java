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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Tree;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.search.FindPlugins;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.tree.ParseError;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddPluginVisitor extends JavaIsoVisitor<ExecutionContext> {
    String pluginId;

    @Nullable
    String newVersion;

    @Nullable
    String versionPattern;

    @Nullable
    Boolean apply;

    @Nullable
    Boolean acceptTransitive;

    private static @Nullable Comment getLicenseHeader(Tree tree) {
        if (tree instanceof G.CompilationUnit) {
            G.CompilationUnit cu = (G.CompilationUnit) tree;
            if (!cu.getStatements().isEmpty()) {
                Statement firstStatement = cu.getStatements().get(0);
                if (!firstStatement.getComments().isEmpty()) {
                    Comment firstComment = firstStatement.getComments().get(0);
                    if (isLicenseHeader(firstComment)) {
                        return firstComment;
                    }
                }
            } else if (cu.getEof() != null && !cu.getEof().getComments().isEmpty()) {
                Comment firstComment = cu.getEof().getComments().get(0);
                if (isLicenseHeader(firstComment)) {
                    // Adding suffix so when we later use it, formats well.
                    return firstComment.withSuffix("\n\n");
                }
            }
        }
        if (tree instanceof K.CompilationUnit) {
            K.CompilationUnit cu = (K.CompilationUnit) tree;
            if (!cu.getStatements().isEmpty()) {
                Statement firstStatement = cu.getStatements().get(0);
                if (firstStatement instanceof J.Block && !((J.Block) firstStatement).getStatements().isEmpty()) {
                    firstStatement = ((J.Block) firstStatement).getStatements().get(0);
                }
                if (!firstStatement.getComments().isEmpty()) {
                    Comment firstComment = firstStatement.getComments().get(0);
                    if (isLicenseHeader(firstComment)) {
                        return firstComment;
                    }
                }
            } else if (cu.getEof() != null && !cu.getEof().getComments().isEmpty()) {
                Comment firstComment = cu.getEof().getComments().get(0);
                if (isLicenseHeader(firstComment)) {
                    // Adding suffix so when we later use it, formats well.
                    return firstComment.withSuffix("\n\n");
                }
            }
        }
        return null;
    }

    private static boolean isLicenseHeader(Comment comment) {
        return comment instanceof TextComment && comment.isMultiline() &&
               ((TextComment) comment).getText().contains("License");
    }

    private static Tree removeLicenseHeader(Tree tree) {
        if (tree instanceof G.CompilationUnit) {
            G.CompilationUnit cu = (G.CompilationUnit) tree;
            if (!cu.getStatements().isEmpty()) {
                return cu.withStatements(ListUtils.mapFirst(cu.getStatements(),
                        s -> s.withComments(s.getComments().subList(1, s.getComments().size()))
                ));
            } else {
                List<Comment> eofComments = cu.getEof().getComments();
                return cu.withEof(cu.getEof().withComments(eofComments.subList(1, eofComments.size())));
            }
        }
        if (tree instanceof K.CompilationUnit) {
            K.CompilationUnit cu = (K.CompilationUnit) tree;
            if (!cu.getStatements().isEmpty()) {
                if (cu.getStatements().get(0) instanceof J.Block && !((J.Block) cu.getStatements().get(0)).getStatements().isEmpty()) {
                    return cu.withStatements(ListUtils.mapFirst(cu.getStatements(), b ->
                            ((J.Block) b).withStatements(ListUtils.mapFirst(((J.Block) b).getStatements(), s -> {
                                if (!s.getComments().isEmpty()) {
                                    if (isLicenseHeader(s.getComments().get(0))) {
                                        return s.withComments(s.getComments().subList(1, s.getComments().size()));
                                    }
                                }
                                return s;
                            }))
                    ));
                }
            } else {
                List<Comment> eofComments = cu.getEof().getComments();
                return cu.withEof(cu.getEof().withComments(eofComments.subList(1, eofComments.size())));
            }
        }
        return tree;
    }

    @Override
    public @Nullable J preVisit(@NonNull J tree, ExecutionContext ctx) {
        if (tree instanceof JavaSourceFile) {
            if (tree instanceof G.CompilationUnit) {
                return addPluginToGroovyCompilationUnit((G.CompilationUnit) tree, ctx);
            }
            if (tree instanceof K.CompilationUnit) {
                return addPluginToKotlinCompilationUnit((K.CompilationUnit) tree, ctx);
            }
        }
        return super.preVisit(tree, ctx);
    }

    private G.CompilationUnit addPluginToGroovyCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
        if (!FindPlugins.find(cu, pluginId).isEmpty() && (acceptTransitive == null || acceptTransitive)) {
            return cu;
        }

        String version;
        if (newVersion == null) {
            // We have been requested to add a versionless plugin
            version = null;
        } else {
            Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
            Optional<GradleSettings> maybeGs = cu.getMarkers().findFirst(GradleSettings.class);
            if (!maybeGp.isPresent() && !maybeGs.isPresent()) {
                return cu;
            }

            try {
                version = new DependencyVersionSelector(null, maybeGp.orElse(null), maybeGs.orElse(null)).select(new GroupArtifact(pluginId, pluginId + ".gradle.plugin"), "classpath", newVersion, versionPattern, ctx);
            } catch (MavenDownloadingException e) {
                return e.warn(cu);
            }
        }

        AtomicInteger singleQuote = new AtomicInteger();
        AtomicInteger doubleQuote = new AtomicInteger();
        AtomicBoolean pluginAlreadyApplied = new AtomicBoolean();
        new JavaIsoVisitor<Integer>() {
            final MethodMatcher pluginIdMatcher = new MethodMatcher("PluginSpec id(..)");

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                J.MethodInvocation m = super.visitMethodInvocation(method, integer);
                if (pluginIdMatcher.matches(m)) {
                    if (m.getArguments().get(0) instanceof J.Literal) {
                        J.Literal l = (J.Literal) m.getArguments().get(0);
                        if (pluginId.equals(l.getValue())) {
                            pluginAlreadyApplied.set(true);
                        }
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
        }.visit(cu, 0);

        if (pluginAlreadyApplied.get()) {
            return cu;
        }

        String delimiter = singleQuote.get() < doubleQuote.get() ? "\"" : "'";
        String source = "plugins {\n" +
                        "    id " + delimiter + pluginId + delimiter + (version != null ? " version " + delimiter + version + delimiter : "") + (version != null && Boolean.FALSE.equals(apply) ? " apply " + apply : "") + "\n" +
                        "}";
        Statement statement = GradleParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(source)), null, ctx)
                .findFirst()
                .map(parsed -> {
                    if (parsed instanceof ParseError) {
                        throw ((ParseError) parsed).toException();
                    }
                    return ((G.CompilationUnit) parsed);
                })
                .map(parsed -> parsed.getStatements().get(0))
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"));

        if (FindMethods.find(cu, "RewriteGradleProject plugins(..)").isEmpty() && FindMethods.find(cu, "RewriteSettings plugins(..)").isEmpty()) {
            if (cu.getSourcePath().endsWith(Paths.get("settings.gradle")) &&
                !cu.getStatements().isEmpty() &&
                cu.getStatements().get(0) instanceof J.MethodInvocation &&
                "pluginManagement".equals(((J.MethodInvocation) cu.getStatements().get(0)).getSimpleName())) {
                return cu.withStatements(ListUtils.insert(cu.getStatements(), autoFormat(statement.withPrefix(Space.format("\n\n")), ctx, getCursor()), 1));
            } else {
                int insertAtIdx = 0;
                for (int i = 0; i < cu.getStatements().size(); i++) {
                    Statement existingStatement = cu.getStatements().get(i);
                    if (existingStatement instanceof J.MethodInvocation && "buildscript".equals(((J.MethodInvocation) existingStatement).getSimpleName())) {
                        insertAtIdx = i + 1;
                        break;
                    }
                }
                if (insertAtIdx == 0) {
                    Comment licenseHeader = getLicenseHeader(cu);
                    if (licenseHeader != null) {
                        cu = (G.CompilationUnit) removeLicenseHeader(cu);
                        statement = statement.withComments(singletonList(licenseHeader));
                    }
                    Space leadingSpace = Space.firstPrefix(cu.getStatements());
                    return cu.withStatements(ListUtils.insert(
                            Space.formatFirstPrefix(cu.getStatements(), leadingSpace.withWhitespace("\n\n" + leadingSpace.getWhitespace())),
                            autoFormat(statement, ctx, getCursor()),
                            insertAtIdx));
                } else {
                    return cu.withStatements(ListUtils.insert(cu.getStatements(), autoFormat(statement.withPrefix(Space.format("\n\n")), ctx, getCursor()), insertAtIdx));
                }
            }
        } else {
            MethodMatcher buildPluginsMatcher = new MethodMatcher("RewriteGradleProject plugins(groovy.lang.Closure)");
            MethodMatcher settingsPluginsMatcher = new MethodMatcher("RewriteSettings plugins(groovy.lang.Closure)");
            J.MethodInvocation pluginDef = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) ((J.MethodInvocation) autoFormat(statement, ctx, getCursor())).getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
            return cu.withStatements(ListUtils.map(cu.getStatements(), stat -> {
                if (stat instanceof J.MethodInvocation) {
                    J.MethodInvocation m = (J.MethodInvocation) stat;
                    if (buildPluginsMatcher.matches(m) || settingsPluginsMatcher.matches(m)) {
                        return m.withArguments(ListUtils.map(m.getArguments(), a -> {
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
                    }
                }
                return stat;
            }));
        }
    }

    private K.CompilationUnit addPluginToKotlinCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
        MethodMatcher pluginsMatcher = new MethodMatcher("*..* plugins(..)");
        MethodMatcher pluginIdMatcher = new MethodMatcher("*..* id(..)");
        AtomicBoolean hasPlugin = new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                J.MethodInvocation m = super.visitMethodInvocation(method, found);
                if (pluginIdMatcher.matches(m, true)) {
                    if (m.getArguments().get(0) instanceof J.Literal) {
                        J.Literal l = (J.Literal) m.getArguments().get(0);
                        if (pluginId.equals(l.getValue())) {
                            found.set(true);
                            return m;
                        }
                    }
                }
                return m;
            }
        }.reduce(cu, new AtomicBoolean());
        if (hasPlugin.get()) {
            return cu;
        }

        String version;
        if (newVersion == null) {
            // We have been requested to add a versionless plugin
            version = null;
        } else {
            Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
            Optional<GradleSettings> maybeGs = cu.getMarkers().findFirst(GradleSettings.class);
            if (!maybeGp.isPresent() && !maybeGs.isPresent()) {
                return cu;
            }

            try {
                version = new DependencyVersionSelector(null, maybeGp.orElse(null), maybeGs.orElse(null)).select(new GroupArtifact(pluginId, pluginId + ".gradle.plugin"), "classpath", newVersion, versionPattern, ctx);
            } catch (MavenDownloadingException e) {
                return e.warn(cu);
            }
        }

        String source = "plugins {\n" +
                "    id(\"" + pluginId + "\")" + (version != null ? " version \"" + version + "\"" : "") + (version != null && Boolean.FALSE.equals(apply) ? " apply " + apply : "") + "\n" +
                "}";
        Statement statement = GradleParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(Paths.get("build.gradle.kts"), source)), null, ctx)
                .findFirst()
                .map(parsed -> {
                    if (parsed instanceof ParseError) {
                        throw ((ParseError) parsed).toException();
                    }
                    return ((K.CompilationUnit) parsed);
                })
                .map(parsed -> parsed.getStatements().get(0))
                .map(block -> ((J.Block)block).getStatements().get(0))
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"));

        AtomicBoolean hasPluginsBlock = new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                J.MethodInvocation m = super.visitMethodInvocation(method, found);
                if (pluginsMatcher.matches(m, true)) {
                    found.set(true);
                }
                return m;
            }
        }.reduce(cu, new AtomicBoolean());
        if (!hasPluginsBlock.get()) {
            J.Block block = (J.Block) cu.getStatements().get(0);
            if (cu.getSourcePath().endsWith(Paths.get("settings.gradle")) &&
                    !block.getStatements().isEmpty() &&
                    block.getStatements().get(0) instanceof J.MethodInvocation &&
                    "pluginManagement".equals(((J.MethodInvocation) block.getStatements().get(0)).getSimpleName())) {
                block = block.withStatements(ListUtils.insert(block.getStatements(), autoFormat(statement.withPrefix(Space.format("\n\n")), ctx, getCursor()), 1));
            } else {
                int insertAtIdx = 0;
                for (int i = 0; i < cu.getStatements().size(); i++) {
                    Statement existingStatement = cu.getStatements().get(i);
                    if (existingStatement instanceof J.MethodInvocation && "buildscript".equals(((J.MethodInvocation) existingStatement).getSimpleName())) {
                        insertAtIdx = i + 1;
                        break;
                    }
                }
                if (insertAtIdx == 0) {
                    Comment licenseHeader = getLicenseHeader(cu);
                    if (licenseHeader != null) {
                        cu = (K.CompilationUnit) removeLicenseHeader(cu);
                        block = (J.Block) cu.getStatements().get(0);
                        statement = statement.withComments(singletonList(licenseHeader));
                    }
                    Space leadingSpace = Space.firstPrefix(block.getStatements());
                    block = block.withStatements(ListUtils.insert(
                            Space.formatFirstPrefix(block.getStatements(), leadingSpace.withWhitespace("\n\n" + leadingSpace.getWhitespace())),
                            statement,
                            insertAtIdx));
                } else {
                    block = block.withStatements(ListUtils.insert(block.getStatements(), statement.withPrefix(Space.format("\n\n")), insertAtIdx));
                }
            }
            J.Block newStatement = block;
            return cu.withStatements(ListUtils.mapFirst(cu.getStatements(), __ -> newStatement));
        } else {
            J.MethodInvocation pluginDef = (J.MethodInvocation)(((J.Block) ((J.Lambda) ((J.MethodInvocation) statement).getArguments().get(0)).getBody()).getStatements().get(0));
            return cu.withStatements(ListUtils.mapFirst(cu.getStatements(), b -> {
                if (b instanceof J.Block) {
                    J.Block block = (J.Block) b;
                    return block.withStatements(ListUtils.map(block.getStatements(), stat -> {
                        if (stat instanceof J.MethodInvocation) {
                            J.MethodInvocation m = (J.MethodInvocation) stat;
                            if (pluginsMatcher.matches(m, true)) {
                                return m.withArguments(ListUtils.map(m.getArguments(), a -> {
                                    if (a instanceof J.Lambda) {
                                        J.Lambda l = (J.Lambda) a;
                                        J.Block body = (J.Block) l.getBody();
                                        List<Statement> pluginStatements = body.getStatements();
                                        if (!pluginStatements.isEmpty() && pluginStatements.get(pluginStatements.size() - 1) instanceof J.Return) {
                                            Statement last = pluginStatements.remove(pluginStatements.size() - 1);
                                            Expression lastExpr = requireNonNull(((J.Return) last).getExpression());
                                            pluginStatements.add(lastExpr.withPrefix(last.getPrefix()));
                                        } else if (pluginStatements.isEmpty()) {
                                            body = body.withPrefix(Space.EMPTY).withEnd(Space.build("\n", emptyList()));
                                        }
                                        pluginStatements.add(pluginDef);
                                        return l.withBody(body.withStatements(pluginStatements));
                                    }
                                    return a;
                                }));
                            }
                        }
                        return stat;
                    }));
                }
                return b;
            }));
        }
    }
}
