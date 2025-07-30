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
package org.openrewrite.gradle.trait;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.internal.AddDependencyVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;

@AllArgsConstructor
public class JvmTestSuite implements Trait<Statement> {
    private static final String[] JVM_TEST_SUITE_SUFFIXES = new String[] {
            "annotationProcessor",
            "compileOnly",
            "implementation",
            "runtimeOnly"
    };

    @Getter
    Cursor cursor;

    GradleProject gradleProject;

    @Getter
    String name;

    public TreeVisitor<J, ExecutionContext> addDependency(
            String configuration,
            String groupId,
            String artifactId,
            @Nullable String version,
            @Nullable String versionPattern,
            @Nullable String classifier,
            @Nullable String extension,
            MavenMetadataFailures metadataFailures,
            org.openrewrite.gradle.AddDependencyVisitor.@Nullable DependencyModifier dependencyModifier,
            ExecutionContext ctx
    ) {
        if (!isAcceptable(configuration)) {
            return TreeVisitor.noop();
        }

        String resolvedConfiguration = configuration.startsWith(name) ?
                Character.toLowerCase(configuration.charAt(name.length())) + configuration.substring(name.length() + 1) :
                configuration;

        String targetConfiguration = configuration.startsWith(name) ?
                configuration :
                name + Character.toUpperCase(configuration.charAt(0)) + configuration.substring(1);

        boolean isKotlinDsl = isKotlinDsl();
        try {
            String resolvedVersion = resolveVersion(resolvedConfiguration, groupId, artifactId, version, versionPattern, metadataFailures, ctx);

            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                    J j = new AddDependencyVisitor(resolvedConfiguration, groupId, artifactId, version, classifier, extension, JvmTestSuite.this::isScope, dependencyModifier, isKotlinDsl)
                            .visit(tree, ctx, getCursor());

                    if (j instanceof JavaSourceFile && j != tree) {
                        return AddDependencyVisitor.addDependency(
                                (JavaSourceFile) j,
                                gradleProject.getConfiguration(targetConfiguration),
                                new GroupArtifactVersion(groupId, artifactId, resolvedVersion),
                                classifier,
                                ctx
                        );
                    }

                    return j;
                }
            };
        } catch (MavenDownloadingException e) {
            return new JavaVisitor<ExecutionContext>() {
                @Override
                public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree == null) {
                        return null;
                    }

                    return (J) e.warn(tree);
                }
            };
        }
    }

    private @Nullable String resolveVersion(String configuration, String groupId, String artifactId, @Nullable String version, @Nullable String versionPattern, MavenMetadataFailures metadataFailures, ExecutionContext ctx) throws MavenDownloadingException{
        if (version == null) {
            return null;
        }

        if (version.startsWith("$")) {
            return version;
        }
        return new DependencyVersionSelector(metadataFailures, gradleProject, null)
                .select(new GroupArtifact(groupId, artifactId), configuration, version, versionPattern, ctx);
    }

    public boolean isAcceptable(String configuration) {
        for (String suffix : JVM_TEST_SUITE_SUFFIXES) {
            if (configuration.equals(suffix)) {
                return true;
            } else if (configuration.equals(name + Character.toUpperCase(suffix.charAt(0)) + suffix.substring(1))) {
                return true;
            }
        }
        return false;
    }

    private boolean isKotlinDsl() {
        return getCursor().firstEnclosing(JavaSourceFile.class) instanceof K.CompilationUnit;
    }

    private boolean isScope(Cursor cursor) {
        Statement statement = getTree();
        Cursor c = cursor.dropParentUntil(value -> (value instanceof J && statement.isScope((J) value)) || (value instanceof J.MethodInvocation && !"registering".equals(((J.MethodInvocation) value).getSimpleName()) && !"getting".equals(((J.MethodInvocation) value).getSimpleName())) || value == Cursor.ROOT_VALUE);
        return !c.isRoot() && statement.isScope(c.getValue());
    }

    public static class Matcher extends GradleTraitMatcher<JvmTestSuite> {
        @Nullable
        protected String name;

        private transient Map<GradleProject, Set<String>> sourceSets = new HashMap<>();

        public Matcher name(@Nullable String name) {
            this.name = name;
            return this;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<JvmTestSuite, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    JvmTestSuite suite = test(getCursor());
                    return suite != null ?
                            (J) visitor.visit(suite, p) :
                            super.visitMethodInvocation(method, p);
                }

                @Override
                public J visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
                    JvmTestSuite suite = test(getCursor());
                    return suite != null ?
                            (J) visitor.visit(suite, p) :
                            super.visitVariableDeclarations(multiVariable, p);
                }
            };
        }

        @Override
        protected @Nullable JvmTestSuite test(Cursor cursor) {
            if (!withinSuitesBlock(cursor)) {
                return null;
            }

            if (cursor.getValue() instanceof J.MethodInvocation) {
                J.MethodInvocation m = cursor.getValue();

                if ("register".equals(m.getSimpleName())) {
                    if (!(m.getArguments().get(0) instanceof J.Literal)) {
                        return null;
                    }

                    J.Literal literal = (J.Literal) m.getArguments().get(0);
                    if (literal.getType() != JavaType.Primitive.String) {
                        return null;
                    }

                    if (literal.getValue() == null) {
                        return null;
                    }

                    return maybeJvmTestSuite(cursor, (String) literal.getValue());
                } else {
                    return maybeJvmTestSuite(cursor, m.getSimpleName());
                }
            } else {
                J.VariableDeclarations variables = cursor.getValue();
                J.VariableDeclarations.NamedVariable variable = variables.getVariables().get(0);

                if (!(variable.getInitializer() instanceof J.MethodInvocation)) {
                    return null;
                }

                J.MethodInvocation initializer = (J.MethodInvocation) variable.getInitializer();
                if (!"getting".equals(initializer.getSimpleName()) && !"registering".equals(initializer.getSimpleName())) {
                    return null;
                }

                return maybeJvmTestSuite(cursor, variable.getSimpleName());
            }
        }

        private @Nullable JvmTestSuite maybeJvmTestSuite(Cursor cursor, String simpleName) {
            Set<String> sourceSets = getSourceSets(cursor);
            if (sourceSets.isEmpty()) {
                if (!hasDependenciesBlock(cursor)) {
                    return null;
                }
            } else if (!sourceSets.contains(simpleName)) {
                return null;
            }

            if (!StringUtils.isBlank(name) && !name.equals(simpleName)) {
                return null;
            }

            return new JvmTestSuite(cursor, getGradleProject(cursor), simpleName);
        }

        private boolean withinTestingBlock(Cursor cursor) {
            return withinBlock(cursor, "testing");
        }

        private boolean withinSuitesBlock(Cursor cursor) {
            return withinBlock(cursor, "suites") && withinTestingBlock(cursor);
        }

        private Set<String> getSourceSets(Cursor cursor) {
            GradleProject gp = getGradleProject(cursor);
            if (gp == null) {
                return emptySet();
            }

            return sourceSets.computeIfAbsent(gp, key -> {
                Set<String> sourceSets = new HashSet<>();
                for (GradleDependencyConfiguration configuration : gp.getConfigurations()) {
                    String maybeSourceSet = removeSuffix(configuration.getName());
                    if (maybeSourceSet != null) {
                        sourceSets.add(maybeSourceSet);
                    }
                }
                return sourceSets;
            });
        }

        private boolean hasDependenciesBlock(Cursor cursor) {
            Statement original = cursor.getValue();
            Statement updated = (Statement) new JavaIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer ctx) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                    if ("dependencies".equals(m.getSimpleName())) {
                        return SearchResult.found(m);
                    }
                    return m;
                }
            }.visitNonNull(original, 0);
            return updated != original;
        }

        /**
         * This gives us the best estimate to a project's source sets. This would be better coming from the GradleProject marker.
         */
        private static @Nullable String removeSuffix(String configuration) {
            for (String suffix : JVM_TEST_SUITE_SUFFIXES) {
                if (configuration.equals(suffix)) {
                    return "main";
                } else if (configuration.endsWith(Character.toUpperCase(suffix.charAt(0)) + suffix.substring(1))) {
                    return configuration.substring(0, configuration.length() - suffix.length());
                }
            }
            return null;
        }
    }
}
