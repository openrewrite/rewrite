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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Parser;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.marker.OmitParentheses;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParseError;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExcludeDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Configuration",
            description = "If specified, only direct dependency declarations in that configuration are considered. " +
                    "If omitted, declarations in every configuration are considered.",
            example = "implementation",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Exclude Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Exclude the specified dependency from any direct dependency declaration that transitively includes it. " +
                "The exclusion is attached to each matching direct dependency, mirroring Maven's per-dependency `<exclusions>` behavior. " +
                "Supports both Groovy and Kotlin DSL build scripts.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(),
                new GradleDependency.Matcher()
                        .configuration(configuration)
                        .asVisitor((dep, ctx) -> {
                            J.MethodInvocation m = dep.getTree();

                            // Skip classpath dependencies inside buildscript { }
                            if (insideBuildscript(dep.getCursor())) {
                                return m;
                            }

                            // If the direct dep itself is g:a, leave it alone — RemoveDependency's job.
                            if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                                return m;
                            }

                            // Transitive match? Exact comparison, no glob.
                            if (dep.getResolvedDependency().findDependency(groupId, artifactId) == null) {
                                return m;
                            }

                            boolean kotlinDsl = dep.getCursor().firstEnclosing(K.CompilationUnit.class) != null;

                            if (alreadyExcludedBy(m, kotlinDsl)) {
                                return m;
                            }

                            return attachExclude(m, kotlinDsl, ctx);
                        }));
    }

    private static boolean insideBuildscript(Cursor cursor) {
        Cursor c = cursor.getParent();
        while (c != null) {
            Object v = c.getValue();
            if (v instanceof J.MethodInvocation && "buildscript".equals(((J.MethodInvocation) v).getSimpleName())) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

    private boolean alreadyExcludedBy(J.MethodInvocation m, boolean kotlinDsl) {
        J.Lambda lambda = trailingLambda(m);
        if (lambda == null || !(lambda.getBody() instanceof J.Block)) {
            return false;
        }
        for (Statement raw : ((J.Block) lambda.getBody()).getStatements()) {
            Statement s = unwrapReturn(raw);
            if (coversTransitiveFalse(s, kotlinDsl)) {
                return true;
            }
            if (!(s instanceof J.MethodInvocation)) {
                continue;
            }
            J.MethodInvocation stmt = (J.MethodInvocation) s;
            if (!"exclude".equals(stmt.getSimpleName())) {
                continue;
            }
            String g = extractNamedArg(stmt, "group", kotlinDsl);
            String a = extractNamedArg(stmt, "module", kotlinDsl);
            boolean groupCovers = g == null || Objects.equals(g, groupId);
            boolean moduleCovers = a == null || Objects.equals(a, artifactId);
            if (groupCovers && moduleCovers && (g != null || a != null)) {
                return true;
            }
        }
        return false;
    }

    private static boolean coversTransitiveFalse(Statement s, boolean kotlinDsl) {
        if (kotlinDsl) {
            if (!(s instanceof J.Assignment)) {
                return false;
            }
            J.Assignment assn = (J.Assignment) s;
            if (!(assn.getVariable() instanceof J.Identifier) ||
                    !"isTransitive".equals(((J.Identifier) assn.getVariable()).getSimpleName())) {
                return false;
            }
            return assn.getAssignment() instanceof J.Literal &&
                    Boolean.FALSE.equals(((J.Literal) assn.getAssignment()).getValue());
        }
        if (!(s instanceof J.Assignment)) {
            return false;
        }
        J.Assignment assn = (J.Assignment) s;
        if (!(assn.getVariable() instanceof J.Identifier) ||
                !"transitive".equals(((J.Identifier) assn.getVariable()).getSimpleName())) {
            return false;
        }
        return assn.getAssignment() instanceof J.Literal &&
                Boolean.FALSE.equals(((J.Literal) assn.getAssignment()).getValue());
    }

    private static @Nullable String extractNamedArg(J.MethodInvocation method, String name, boolean kotlinDsl) {
        for (Expression arg : method.getArguments()) {
            if (!kotlinDsl && arg instanceof G.MapEntry) {
                G.MapEntry entry = (G.MapEntry) arg;
                if (entry.getKey() instanceof J.Literal &&
                        name.equals(((J.Literal) entry.getKey()).getValue()) &&
                        entry.getValue() instanceof J.Literal) {
                    Object v = ((J.Literal) entry.getValue()).getValue();
                    return v == null ? null : v.toString();
                }
            }
            if (kotlinDsl && arg instanceof J.Assignment) {
                J.Assignment assn = (J.Assignment) arg;
                if (assn.getVariable() instanceof J.Identifier &&
                        name.equals(((J.Identifier) assn.getVariable()).getSimpleName()) &&
                        assn.getAssignment() instanceof J.Literal) {
                    Object v = ((J.Literal) assn.getAssignment()).getValue();
                    return v == null ? null : v.toString();
                }
            }
        }
        return null;
    }

    private J.MethodInvocation attachExclude(J.MethodInvocation m, boolean kotlinDsl, ExecutionContext ctx) {
        J.Lambda existingLambda = trailingLambda(m);
        if (existingLambda != null && existingLambda.getBody() instanceof J.Block) {
            Statement excludeStmt = parseExcludeStatement(kotlinDsl, ctx);
            J.Block body = (J.Block) existingLambda.getBody();
            Space indentPrefix = body.getStatements().isEmpty() ?
                    Space.format("\n    ") :
                    body.getStatements().get(body.getStatements().size() - 1).getPrefix();
            Statement withPrefix = excludeStmt.withPrefix(indentPrefix);
            J.Block newBody = body.withStatements(ListUtils.concat(body.getStatements(), withPrefix));
            J.Lambda newLambda = existingLambda.withBody(newBody);
            return m.withArguments(ListUtils.map(m.getArguments(), a -> a == existingLambda ? newLambda : a));
        }
        J.Lambda freshLambda = parseFreshLambda(kotlinDsl, ctx);
        List<Expression> args = m.getArguments();
        if (!kotlinDsl) {
            // In Groovy's parens-less form (`implementation 'g:a:v'`), each existing arg carries an
            // OmitParentheses marker that tells the printer to skip the method's parens entirely.
            // Keeping those would print as `implementation 'g:a:v', { ... }`; stripping them restores
            // `implementation('g:a:v') { ... }`. The new trailing-closure lambda keeps its own marker.
            // The first arg's leading space (from the parens-less form) also has to be cleared so the
            // reinstated parens don't print as `implementation( 'g:a:v')`.
            args = ListUtils.map(args, (idx, a) -> {
                Expression stripped = a.withMarkers(a.getMarkers()
                        .removeByType(OmitParentheses.class)
                        .removeByType(org.openrewrite.java.marker.OmitParentheses.class));
                return idx == 0 ? stripped.withPrefix(Space.EMPTY) : stripped;
            });
        }
        return m.withArguments(ListUtils.concat(args, freshLambda));
    }

    private static Statement unwrapReturn(Statement s) {
        if (s instanceof J.Return && ((J.Return) s).getExpression() instanceof Statement) {
            return (Statement) ((J.Return) s).getExpression();
        }
        return s;
    }

    private J.@Nullable Lambda trailingLambda(J.MethodInvocation m) {
        List<Expression> args = m.getArguments();
        for (int i = args.size() - 1; i >= 0; i--) {
            if (args.get(i) instanceof J.Lambda) {
                return (J.Lambda) args.get(i);
            }
        }
        return null;
    }

    private Statement parseExcludeStatement(boolean kotlinDsl, ExecutionContext ctx) {
        J.Lambda stubLambda = parseStubLambda(kotlinDsl, ctx);
        return unwrapReturn(((J.Block) stubLambda.getBody()).getStatements().get(0));
    }

    private J.Lambda parseFreshLambda(boolean kotlinDsl, ExecutionContext ctx) {
        return parseStubLambda(kotlinDsl, ctx).withPrefix(Space.format(" "));
    }

    private J.Lambda parseStubLambda(boolean kotlinDsl, ExecutionContext ctx) {
        if (kotlinDsl) {
            String snippet = String.format(
                    "dependencies {\n" +
                            "    implementation(\"x:x:1\") {\n" +
                            "        exclude(group = \"%s\", module = \"%s\")\n" +
                            "    }\n" +
                            "}",
                    groupId, artifactId);
            J.MethodInvocation deps = (J.MethodInvocation) ((J.Block) parseKotlin(snippet, ctx).getStatements().get(0)).getStatements().get(0);
            J.Lambda depsLambda = (J.Lambda) deps.getArguments().get(deps.getArguments().size() - 1);
            J.MethodInvocation stub = (J.MethodInvocation) unwrapReturn(((J.Block) depsLambda.getBody()).getStatements().get(0));
            return (J.Lambda) stub.getArguments().get(stub.getArguments().size() - 1);
        }
        String snippet = String.format(
                "dependencies {\n" +
                        "    implementation('x:x:1') {\n" +
                        "        exclude group: '%s', module: '%s'\n" +
                        "    }\n" +
                        "}",
                groupId, artifactId);
        J.MethodInvocation deps = (J.MethodInvocation) parseGroovy(snippet, ctx).getStatements().get(0);
        J.Lambda depsLambda = (J.Lambda) deps.getArguments().get(deps.getArguments().size() - 1);
        J.MethodInvocation stub = (J.MethodInvocation) unwrapReturn(((J.Block) depsLambda.getBody()).getStatements().get(0));
        return (J.Lambda) stub.getArguments().get(stub.getArguments().size() - 1);
    }

    private static G.CompilationUnit parseGroovy(String snippet, ExecutionContext ctx) {
        return (G.CompilationUnit) GradleParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(Paths.get("build.gradle"), snippet)), null, ctx)
                .map(p -> {
                    if (p instanceof ParseError) {
                        throw ((ParseError) p).toException();
                    }
                    return (Tree) p;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not parse Groovy snippet"));
    }

    private static K.CompilationUnit parseKotlin(String snippet, ExecutionContext ctx) {
        return (K.CompilationUnit) GradleParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(Paths.get("build.gradle.kts"), snippet)), null, ctx)
                .map(p -> {
                    if (p instanceof ParseError) {
                        throw ((ParseError) p).toException();
                    }
                    return (Tree) p;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not parse Kotlin DSL snippet"));
    }
}
