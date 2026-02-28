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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.RandomizeIdVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeRepository extends Recipe {

    String displayName = "Change repository";

    String description = "Replace a repository in Gradle build scripts to standardize repository usage across an organization.";

    @Option(displayName = "Old type",
            description = "The type of the artifact repository to replace. " +
                          "Named types include \"jcenter\", \"mavenCentral\", \"mavenLocal\", \"google\", and \"gradlePluginPortal\". " +
                          "If not specified, matches any repository type with the given URL.",
            required = false,
            example = "jcenter")
    @Nullable
    String oldType;

    @Option(displayName = "Old URL",
            description = "The URL of the artifact repository to replace. If not specified, matches any repository of the given type.",
            required = false,
            example = "https://old-nexus.example.com/releases")
    @Nullable
    String oldUrl;

    @Option(displayName = "New type",
            description = "The type of the new artifact repository. " +
                          "If not specified, the matched repository's type will be preserved.",
            required = false,
            example = "mavenCentral")
    @Nullable
    String newType;

    @Option(displayName = "New URL",
            description = "The URL of the new artifact repository. Required when the new type is not a named repository.",
            required = false,
            example = "https://new-nexus.example.com/releases")
    @Nullable
    String newUrl;

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(Validated.required("newType", newType).or(Validated.required("newUrl", newUrl)))
                .and(Validated.test(
                        "repository",
                        "Old and new repository must be different",
                        this,
                        r -> !(Objects.equals(r.oldType, r.newType) && Objects.equals(r.oldUrl, r.newUrl))
                ));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher repoMatcher = new MethodMatcher("org.gradle.api.artifacts.dsl.RepositoryHandler " + (oldType != null ? oldType : "*") + "(..)", true);

        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (!repoMatcher.matches(m, true)) {
                    return m;
                }

                // Ensure we're inside a repositories {} block
                if (!isInsideRepositoriesBlock()) {
                    return m;
                }

                // If oldUrl is specified, verify the URL matches
                if (oldUrl != null && !urlMatches(m)) {
                    return m;
                }

                // If newType is not specified, keep the matched repository's type
                String effectiveNewType = newType != null ? newType : m.getSimpleName();

                boolean isKotlinDsl = getCursor().firstEnclosingOrThrow(JavaSourceFile.class) instanceof K.CompilationUnit;

                // If the target repository already exists as a sibling, remove the old one instead of replacing
                if (newRepoAlreadyExists(m, effectiveNewType)) {
                    //noinspection DataFlowIssue
                    return null;
                }

                // Named → Named (no URLs involved)
                if (oldUrl == null && newUrl == null) {
                    if (m.getSimpleName().equals(effectiveNewType)) {
                        return m;
                    }
                    return m.withName(m.getName().withSimpleName(effectiveNewType));
                }

                // Custom → Custom with same type: just change the URL
                if (m.getSimpleName().equals(effectiveNewType) && newUrl != null) {
                    if (oldUrl != null && oldUrl.equals(newUrl)) {
                        return m;
                    }
                    return replaceUrl(m, newUrl, isKotlinDsl);
                }

                // All other cases: generate a new repository node and swap it in
                J.MethodInvocation replacement = generateRepositoryInvocation(effectiveNewType, isKotlinDsl, ctx);
                return (J.MethodInvocation) autoFormat(replacement.withPrefix(m.getPrefix()), ctx, getCursor().getParentOrThrow());
            }

            private boolean isInsideRepositoriesBlock() {
                try {
                    getCursor().dropParentUntil(e ->
                            e instanceof J.MethodInvocation &&
                            "repositories".equals(((J.MethodInvocation) e).getSimpleName()));
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            private boolean newRepoAlreadyExists(J.MethodInvocation current, String effectiveNewType) {
                try {
                    Cursor reposCursor = getCursor().dropParentUntil(e ->
                            e instanceof J.MethodInvocation &&
                            "repositories".equals(((J.MethodInvocation) e).getSimpleName()));
                    J.MethodInvocation repos = reposCursor.getValue();
                    if (repos.getArguments().isEmpty() || !(repos.getArguments().get(0) instanceof J.Lambda)) {
                        return false;
                    }
                    J.Lambda lambda = (J.Lambda) repos.getArguments().get(0);
                    if (!(lambda.getBody() instanceof J.Block)) {
                        return false;
                    }
                    J.Block block = (J.Block) lambda.getBody();
                    for (Statement sibling : block.getStatements()) {
                        Statement s = sibling instanceof J.Return ? (Statement) ((J.Return) sibling).getExpression() : sibling;
                        if (!(s instanceof J.MethodInvocation)) {
                            continue;
                        }
                        J.MethodInvocation siblingInvocation = (J.MethodInvocation) s;
                        // Skip the node we're currently visiting
                        if (siblingInvocation.getId().equals(current.getId())) {
                            continue;
                        }
                        if (!effectiveNewType.equals(siblingInvocation.getSimpleName())) {
                            continue;
                        }
                        if (newUrl == null) {
                            // Named repo match (e.g. mavenCentral())
                            return true;
                        }
                        // Custom repo: check URL matches
                        String siblingUrl = extractUrlFromInvocation(siblingInvocation);
                        if (newUrl.equals(siblingUrl)) {
                            return true;
                        }
                    }
                } catch (Exception ignored) {
                }
                return false;
            }

            private @Nullable String extractUrlFromInvocation(J.MethodInvocation m) {
                if (m.getArguments().isEmpty() || !(m.getArguments().get(0) instanceof J.Lambda)) {
                    return null;
                }
                J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
                if (!(lambda.getBody() instanceof J.Block)) {
                    return null;
                }
                J.Block block = (J.Block) lambda.getBody();
                for (Statement statement : block.getStatements()) {
                    Statement s = statement instanceof J.Return ? (Statement) ((J.Return) statement).getExpression() : statement;
                    if (s != null) {
                        String url = extractUrl(s);
                        if (url != null) {
                            return url;
                        }
                    }
                }
                return null;
            }

            private boolean urlMatches(J.MethodInvocation m) {
                if (m.getArguments().isEmpty() || !(m.getArguments().get(0) instanceof J.Lambda)) {
                    return false;
                }
                J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
                if (!(lambda.getBody() instanceof J.Block)) {
                    return false;
                }
                J.Block block = (J.Block) lambda.getBody();
                for (Statement statement : block.getStatements()) {
                    Statement s = statement instanceof J.Return ? (Statement) ((J.Return) statement).getExpression() : statement;
                    if (s == null) {
                        continue;
                    }
                    String extractedUrl = extractUrl(s);
                    if (oldUrl.equals(extractedUrl)) {
                        return true;
                    }
                }
                return false;
            }

            private @Nullable String extractUrl(Statement s) {
                // Handle: url = "..." or url = uri("...")
                if (s instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) s;
                    if (assignment.getVariable() instanceof J.Identifier &&
                        "url".equals(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                        return extractUrlFromExpression(assignment.getAssignment());
                    }
                }
                // Handle: setUrl("...") or url("...") or setUrl(uri("..."))
                if (s instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) s;
                    if ("setUrl".equals(mi.getSimpleName()) || "url".equals(mi.getSimpleName())) {
                        return extractUrlFromExpression(mi.getArguments().get(0));
                    }
                }
                return null;
            }

            private @Nullable String extractUrlFromExpression(J expr) {
                if (expr instanceof J.Literal) {
                    return (String) ((J.Literal) expr).getValue();
                }
                if (expr instanceof J.MethodInvocation && "uri".equals(((J.MethodInvocation) expr).getSimpleName())) {
                    J.MethodInvocation uri = (J.MethodInvocation) expr;
                    if (!uri.getArguments().isEmpty() && uri.getArguments().get(0) instanceof J.Literal) {
                        return (String) ((J.Literal) uri.getArguments().get(0)).getValue();
                    }
                }
                return null;
            }

            private J.MethodInvocation replaceUrl(J.MethodInvocation m, String newUrl, boolean isKotlinDsl) {
                return (J.MethodInvocation) new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                        if (literal.getValue() instanceof String && literal.getValue().equals(oldUrl)) {
                            return ChangeStringLiteral.withStringValue(literal, newUrl);
                        }
                        return literal;
                    }
                }.visitNonNull(m, new InMemoryExecutionContext());
            }

            private J.MethodInvocation generateRepositoryInvocation(String effectiveNewType, boolean isKotlinDsl, ExecutionContext ctx) {
                String code;
                if (newUrl == null) {
                    code = effectiveNewType + "()";
                } else if (isKotlinDsl) {
                    code = effectiveNewType + " {\n    url = uri(\"" + newUrl + "\")\n}";
                } else {
                    code = effectiveNewType + " {\n    url = \"" + newUrl + "\"\n}";
                }

                String template = "repositories {\n    " + code + "\n}";
                Path path = Paths.get(isKotlinDsl ? "build.gradle.kts" : "build.gradle");

                J.MethodInvocation reposBlock;
                if (isKotlinDsl) {
                    K.CompilationUnit cu = GradleParser.builder().build()
                            .parseInputs(singletonList(Parser.Input.fromString(path, template)), null, ctx)
                            .map(K.CompilationUnit.class::cast)
                            .collect(toList()).get(0);
                    J.Block block = (J.Block) cu.getStatements().get(0);
                    reposBlock = (J.MethodInvocation) block.getStatements().get(0);
                } else {
                    G.CompilationUnit cu = GradleParser.builder().build()
                            .parseInputs(singletonList(Parser.Input.fromString(path, template)), null, ctx)
                            .map(G.CompilationUnit.class::cast)
                            .collect(toList()).get(0);
                    reposBlock = (J.MethodInvocation) cu.getStatements().get(0);
                }

                // Extract the repository from inside the repositories {} block
                J.Lambda lambda = (J.Lambda) reposBlock.getArguments().get(0);
                J.Block body = (J.Block) lambda.getBody();
                Statement stmt = body.getStatements().get(0);
                J.MethodInvocation repo;
                if (stmt instanceof J.Return) {
                    repo = (J.MethodInvocation) ((J.Return) stmt).getExpression();
                } else {
                    repo = (J.MethodInvocation) stmt;
                }

                return (J.MethodInvocation) new RandomizeIdVisitor<Integer>().visit(repo, 0);
            }
        });
    }
}
