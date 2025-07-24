/*
 * Copyright 2024 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.style.Style;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateGradleEnterpriseToDevelocity extends Recipe {

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Plugin version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors). " +
                          "Defaults to `latest.release`.",
            example = "3.x",
            required = false)
    @Nullable
    String version;

    @Override
    public String getDisplayName() {
        //noinspection DialogTitleCapitalization
        return "Migrate from Gradle Enterprise to Develocity";
    }

    @Override
    public String getDescription() {
        return "Migrate from the Gradle Enterprise Gradle plugin to the Develocity Gradle plugin.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new IsSettingsGradle<>(),
                new GroovyIsoVisitor<ExecutionContext>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                        Optional<GradleSettings> maybeGs = cu.getMarkers().findFirst(GradleSettings.class);
                        if (!maybeGs.isPresent()) {
                            return cu;
                        }

                        try {
                            String newVersion = new DependencyVersionSelector(metadataFailures, null, maybeGs.get())
                                    .select(new GroupArtifact("com.gradle.develocity", "com.gradle.develocity.gradle.plugin"), "classpath", version, null, ctx);
                            if (newVersion == null) {
                                // The develocity plugin was first published as of 3.17
                                return cu;
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(cu);
                        }

                        G.CompilationUnit g = cu;
                        g = (G.CompilationUnit) new ChangePlugin("com.gradle.enterprise", "com.gradle.develocity", version).getVisitor()
                                .visitNonNull(g, ctx);
                        g = (G.CompilationUnit) new UpgradePluginVersion("com.gradle.common-custom-user-data-gradle-plugin", "2.x", null).getVisitor()
                                .visitNonNull(g, ctx);
                        return (G.CompilationUnit) new MigrateConfigurationVisitor().visitNonNull(g, ctx);
                    }
                }
        );
    }

    private static class MigrateConfigurationVisitor extends GroovyIsoVisitor<ExecutionContext> {
        @Override
        public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (m.getSimpleName().equals("gradleEnterprise") && m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Lambda) {
                return m.withName(m.getName().withSimpleName("develocity"));
            }

            if (m.getSimpleName().startsWith("publishAlways") && withinMethodInvocations(Arrays.asList("gradleEnterprise", "buildScan"))) {
                if (m.getSimpleName().equals("publishAlways") && noArguments(m.getArguments())) {
                    // As of 3.17+, `publishAlways` is the default, so it is recommended to not configure anything
                    return null;
                }

                if (m.getSimpleName().equals("publishAlwaysIf")) {
                    J.MethodInvocation publishingTemplate = develocityPublishAlwaysIfDsl(getIndent(getCursor().firstEnclosing(G.CompilationUnit.class)), ctx);
                    if (publishingTemplate == null) {
                        return m;
                    }

                    return publishingTemplate.withArguments(ListUtils.mapFirst(publishingTemplate.getArguments(), arg -> {
                        if (arg instanceof J.Lambda) {
                            J.Lambda lambda = (J.Lambda) arg;
                            J.Block block = (J.Block) lambda.getBody();
                            return lambda.withBody(block.withStatements(ListUtils.mapFirst(block.getStatements(), s -> {
                                if (s instanceof J.Return) {
                                    J.Return _return = (J.Return) s;
                                    return _return.withExpression(m.getArguments().get(0));
                                }
                                return s;
                            })));
                        }
                        return arg;
                    }));
                }
            }

            if (m.getSimpleName().startsWith("publishOnFailure") && withinMethodInvocations(Arrays.asList("gradleEnterprise", "buildScan"))) {
                J.MethodInvocation publishingTemplate = develocityPublishOnFailureIfDsl(getIndent(getCursor().firstEnclosing(G.CompilationUnit.class)), ctx);
                if (publishingTemplate == null) {
                    return m;
                }

                if (m.getSimpleName().equals("publishOnFailure") && noArguments(m.getArguments())) {
                    return publishingTemplate;
                }

                if (m.getSimpleName().equals("publishOnFailureIf") && m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Binary) {
                    return publishingTemplate.withArguments(ListUtils.mapFirst(publishingTemplate.getArguments(), arg -> {
                        if (arg instanceof J.Lambda) {
                            J.Lambda lambda = (J.Lambda) arg;
                            J.Block block = (J.Block) lambda.getBody();
                            return lambda.withBody(block.withStatements(ListUtils.mapFirst(block.getStatements(), s -> {
                                if (s instanceof J.Return && ((J.Return) s).getExpression() instanceof J.Unary) {
                                    J.Return _return = (J.Return) s;
                                    return _return.withExpression(new J.Binary(
                                            Tree.randomId(),
                                            Space.EMPTY,
                                            Markers.EMPTY,
                                            _return.getExpression(),
                                            JLeftPadded.build(J.Binary.Type.And).withBefore(Space.SINGLE_SPACE),
                                            Space.formatFirstPrefix(m.getArguments(), Space.SINGLE_SPACE).get(0),
                                            JavaType.Primitive.Boolean
                                    ));
                                }
                                return s;
                            })));
                        }
                        return arg;
                    }));
                }
            }

            if (m.getSimpleName().equals("remote") && withinMethodInvocations(Arrays.asList("gradleEnterprise", "buildCache"))) {
                return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                    if (arg instanceof J.FieldAccess) {
                        J.FieldAccess field = (J.FieldAccess) arg;
                        if (field.getSimpleName().equals("buildCache") && field.getTarget() instanceof J.Identifier && ((J.Identifier) field.getTarget()).getSimpleName().equals("gradleEnterprise")) {
                            return field.withTarget(((J.Identifier) field.getTarget()).withSimpleName("develocity"));
                        }
                    }
                    return arg;
                }));
            }
            return m;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            J.Assignment a = super.visitAssignment(assignment, ctx);

            if (a.getVariable() instanceof J.Identifier && ((J.Identifier) a.getVariable()).getSimpleName().equals("taskInputFiles") && withinMethodInvocations(Arrays.asList("gradleEnterprise", "buildScan", "capture"))) {
                return a.withVariable(((J.Identifier) a.getVariable()).withSimpleName("fileFingerprints"));
            }

            return a;
        }

        private boolean noArguments(List<Expression> arguments) {
            return arguments.isEmpty() || (arguments.size() == 1 && arguments.get(0) instanceof J.Empty);
        }

        private boolean withinMethodInvocations(List<String> methods) {
            Cursor current = getCursor().getParent();
            for (int i = methods.size() - 1; i >= 0; i--) {
                current = findMethodInvocation(current);
                if (current == null) {
                    return false;
                }

                if (!((J.MethodInvocation) current.getValue()).getSimpleName().equals(methods.get(i))) {
                    return false;
                }

                current = current.getParent();
            }

            return true;
        }

        private @Nullable Cursor findMethodInvocation(@Nullable Cursor start) {
            if (start == null) {
                return null;
            }

            Cursor current = start;
            while (current.getParent() != null) {
                if (current.getValue() instanceof J.MethodInvocation) {
                    return current;
                }

                current = current.getParent();
            }

            return null;
        }

        private J.@Nullable MethodInvocation develocityPublishAlwaysIfDsl(String indent, ExecutionContext ctx) {
            StringBuilder ge = new StringBuilder("\ndevelocity {\n");
            ge.append(indent).append("buildScan {\n");
            ge.append(indent).append(indent).append("publishing.onlyIf { true }\n");
            ge.append(indent).append("}\n");
            ge.append("}\n");

            G.CompilationUnit cu = GradleParser.builder().build()
                    .parseInputs(singletonList(
                            Parser.Input.fromString(Paths.get("settings.gradle"), ge.toString())), null, ctx)
                    .map(G.CompilationUnit.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"));

            J.MethodInvocation develocity = (J.MethodInvocation) cu.getStatements().get(0);
            J.MethodInvocation buildScan = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) develocity.getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
            return (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) buildScan.getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
        }

        private J.@Nullable MethodInvocation develocityPublishOnFailureIfDsl(String indent, ExecutionContext ctx) {
            StringBuilder ge = new StringBuilder("\ndevelocity {\n");
            ge.append(indent).append("buildScan {\n");
            ge.append(indent).append(indent).append("publishing.onlyIf { !it.buildResult.failures.empty }\n");
            ge.append(indent).append("}\n");
            ge.append("}\n");

            G.CompilationUnit cu = GradleParser.builder().build()
                    .parseInputs(singletonList(
                            Parser.Input.fromString(Paths.get("settings.gradle"), ge.toString())), null, ctx)
                    .map(G.CompilationUnit.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"));

            J.MethodInvocation develocity = (J.MethodInvocation) cu.getStatements().get(0);
            J.MethodInvocation buildScan = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) develocity.getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
            return (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) buildScan.getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
        }

        private String getIndent(G.CompilationUnit cu) {
            TabsAndIndentsStyle style = Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents);
            if (style.getUseTabCharacter()) {
                return "\t";
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < style.getIndentSize(); i++) {
                    sb.append(" ");
                }
                return sb.toString();
            }
        }
    }
}
