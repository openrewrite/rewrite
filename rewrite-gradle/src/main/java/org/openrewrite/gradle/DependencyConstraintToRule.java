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
package org.openrewrite.gradle;

import lombok.*;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class DependencyConstraintToRule extends Recipe {

    private static final MethodMatcher DEPENDENCIES_DSL_MATCHER = new MethodMatcher("RewriteGradleProject dependencies(..)");
    private static final String CONSTRAINT_MATCHER = "org.gradle.api.artifacts.dsl.DependencyHandler *(..)";

    @Override
    public String getDisplayName() {
        return "Dependency constraint to resolution rule";
    }

    @Override
    public String getDescription() {
        return "Gradle [dependency constraints](https://docs.gradle.org/current/userguide/dependency_constraints.html#dependency-constraints) " +
                "are useful for managing the versions of transitive dependencies. " +
                "Some plugins, such as the Spring Dependency Management plugin, do not respect these constraints. " +
                "This recipe converts constraints into [resolution rules](https://docs.gradle.org/current/userguide/resolution_rules.html), " +
                "which can achieve similar effects to constraints but are harder for plugins to ignore.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return (J) tree;
                }
                List<GroupArtifactVersionBecause> gavs = new ArrayList<>();
                JavaSourceFile cu = (JavaSourceFile) new RemoveConstraints().visitNonNull(tree, gavs);
                if (gavs.isEmpty()) {
                    return (J) tree;
                }
                cu = (JavaSourceFile) new MaybeAddEachDependency().visitNonNull(cu, ctx);
                cu = (JavaSourceFile) new UpdateEachDependency(gavs, cu instanceof K.CompilationUnit).visitNonNull(cu, ctx);
                return (JavaSourceFile) new MaybeRemoveDependencyBlock(cu instanceof K.CompilationUnit).visitNonNull(cu, ctx);
            }
        });
    }

    @Value
    static class GroupArtifactVersionBecause {
        @Nullable
        String groupId;

        String artifactId;

        @Nullable
        String version;

        @Nullable
        String because;
    }

    static class RemoveConstraints extends JavaIsoVisitor<List<GroupArtifactVersionBecause>> {

        @SuppressWarnings({"DataFlowIssue", "NullableProblems"})
        @Override
        public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<GroupArtifactVersionBecause> groupArtifactVersions) {
            J.MethodInvocation m = super.visitMethodInvocation(method, groupArtifactVersions);
            if ("constraints".equals(m.getSimpleName()) && isInDependenciesBlock(getCursor())) {
                if (!(m.getArguments().get(0) instanceof J.Lambda)) {
                    return null;
                }
                J.Lambda closure = (J.Lambda) m.getArguments().get(0);
                if (!(closure.getBody() instanceof J.Block)) {
                    return null;
                }
                List<Statement> withoutConvertableConstraints = ListUtils.map(((J.Block) closure.getBody()).getStatements(), statement -> {
                    J.MethodInvocation constraint = null;
                    if (statement instanceof J.MethodInvocation) {
                        constraint = (J.MethodInvocation) statement;
                    } else if (statement instanceof J.Return) {
                        constraint = (J.MethodInvocation) ((J.Return) statement).getExpression();
                    }
                    if (constraint == null) {
                        return statement;
                    }
                    if (!(constraint.getArguments().get(0) instanceof J.Literal)) {
                        return statement;
                    }
                    J.Literal rawGav = (J.Literal) constraint.getArguments().get(0);
                    String[] gav = rawGav.getValue().toString().split(":");
                    if (gav.length != 3) {
                        return statement;
                    }
                    AtomicReference<String> because = new AtomicReference<>(null);
                    new JavaIsoVisitor<Integer>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                            J.MethodInvocation m1 = super.visitMethodInvocation(method, integer);
                            if ("because".equals(m1.getSimpleName()) && m1.getArguments().get(0) instanceof J.Literal) {
                                because.set(((J.Literal) m1.getArguments().get(0)).getValue().toString());
                            }
                            return m1;
                        }
                    }.visit(constraint.getArguments(), 0);

                    groupArtifactVersions.add(new GroupArtifactVersionBecause(gav[0], gav[1], gav[2], because.get()));
                    return null;
                });
                // If nothing remains in the constraints{} it can be removed entirely
                if (withoutConvertableConstraints.isEmpty()) {
                    return null;
                } else {
                    return m.withArguments(singletonList(closure.withBody(((J.Block) closure.getBody()).withStatements(withoutConvertableConstraints))));
                }
            }
            return m;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class UpdateEachDependency extends JavaIsoVisitor<ExecutionContext> {
        List<GroupArtifactVersionBecause> groupArtifactVersions;
        boolean isKotlinDsl;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (isEachDependency(m) && !isInBuildscriptBlock(getCursor())) {
                Cursor parent = requireNonNull(getCursor().getParent());
                for (GroupArtifactVersionBecause gav : groupArtifactVersions) {
                    m = (J.MethodInvocation) new MaybeAddIf(gav, isKotlinDsl).visitNonNull(m, ctx, parent);
                    m = (J.MethodInvocation) new UpdateIf(gav, isKotlinDsl).visitNonNull(m, ctx, parent);
                }
            }
            return m;
        }
    }

    @RequiredArgsConstructor
    static class MaybeAddIf extends JavaIsoVisitor<ExecutionContext> {
        @NonNull
        GroupArtifactVersionBecause groupArtifactVersion;

        final boolean isKotlinDsl;

        boolean containsAnyIfStatement;
        boolean containsMatchingIfStatement;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (containsMatchingIfStatement) {
                return m;
            }
            if (!isEachDependency(m)) {
                return m;
            }
            // Identify the parameter name used in the eachDependency closure
            Expression maybeClosure = m.getArguments().get(0);
            if (!(maybeClosure instanceof J.Lambda) || !(((J.Lambda) maybeClosure).getBody() instanceof J.Block)) {
                return m;
            }
            J.Lambda closure = (J.Lambda) maybeClosure;
            J.Block closureBody = (J.Block) closure.getBody();
            J rawParam = ((J.Lambda) maybeClosure).getParameters().getParameters().get(0);
            if (!(rawParam instanceof J.VariableDeclarations)) {
                return m;
            }
            String p = ((J.VariableDeclarations) rawParam).getVariables().get(0).getSimpleName();
            J.If newIf;
            if (!isKotlinDsl) {
                @SuppressWarnings("GroovyEmptyStatementBody") @Language("groovy")
                String snippet = "Object " + p + " = null\n" +
                        "if (" + p + ".requested.group == '" + groupArtifactVersion.getGroupId() + "' && " +
                        p + ".requested.name == '" + groupArtifactVersion.getArtifactId() + "') {\n}";
                newIf = GroovyParser.builder().build()
                        .parse(ctx, snippet)
                        .map(G.CompilationUnit.class::cast)
                        .map(cu -> cu.getStatements().get(1))
                        .map(J.If.class::cast)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to produce a new if statement"));
            } else {
                String snippet = "var " + p + ": Any = null\n" +
                        "if (" + p + ".requested.group == \"" + groupArtifactVersion.getGroupId() + "\" && " +
                        p + ".requested.name == \"" + groupArtifactVersion.getArtifactId() + "\") {\n}";
                newIf = KotlinParser.builder().isKotlinScript(true).build()
                        .parse(ctx, snippet)
                        .map(K.CompilationUnit.class::cast)
                        .map(cu -> (J.Block) cu.getStatements().get(0))
                        .map(block -> (J.If) block.getStatements().get(1))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to produce a new if statement"));
            }
            if (containsAnyIfStatement) {
                m = (J.MethodInvocation) new JavaIsoVisitor<Integer>() {
                    boolean inserted;

                    @Override
                    public J.If visitIf(J.If iff, Integer integer) {
                        J.If anIf = super.visitIf(iff, integer);
                        J.If.Else currentElse = anIf.getElsePart();
                        if (!inserted && (currentElse == null || currentElse.getBody() instanceof J.Block)) {
                            inserted = true;
                            J.If.Else newElsePart = new J.If.Else(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    JRightPadded.build(newIf
                                            .withPrefix(Space.SINGLE_SPACE)
                                            .withElsePart(currentElse)));
                            anIf = autoFormat(anIf.withElsePart(newElsePart), 0, requireNonNull(getCursor().getParent()));
                        }
                        return anIf;
                    }
                }.visitNonNull(m, 0, requireNonNull(getCursor().getParent()));
            } else {
                J.Block newBody = autoFormat(closureBody.withStatements(ListUtils.concat(newIf, closureBody.getStatements())), ctx, getCursor());
                m = m.withArguments(singletonList(closure.withBody(newBody)));
            }
            return m;
        }


        @Override
        public J.If visitIf(J.If iff, ExecutionContext ctx) {
            containsAnyIfStatement = true;
            J.If f = super.visitIf(iff, ctx);
            if (predicateRelatesToGav(f, groupArtifactVersion)) {
                containsMatchingIfStatement = true;
            }
            return iff;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            // Avoid wasting time if we've already found it
            if (containsMatchingIfStatement) {
                return (J) tree;
            }
            return super.visit(tree, ctx);
        }
    }

    @AllArgsConstructor
    static class UpdateIf extends JavaIsoVisitor<ExecutionContext> {
        GroupArtifactVersionBecause groupArtifactVersionBecause;
        boolean isKotlinDsl;

        @Override
        public J.If visitIf(J.If iff, ExecutionContext ctx) {
            J.If anIf = super.visitIf(iff, ctx);
            if (predicateRelatesToGav(anIf, groupArtifactVersionBecause)) {
                // The predicate of the if condition will already contain the relevant variable name
                AtomicReference<String> variableName = new AtomicReference<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer integer) {
                        // Comparison will involve "<variable name>.requested.group"
                        J.FieldAccess field = super.visitFieldAccess(fieldAccess, integer);
                        if (field.getTarget() instanceof J.Identifier) {
                            variableName.set(((J.Identifier) field.getTarget()).getSimpleName());
                        }
                        return fieldAccess;
                    }
                }.visit(anIf.getIfCondition(), 0);
                List<Statement> newStatements;
                if (!isKotlinDsl) {
                    @Language("groovy")
                    String snippet = variableName + ".useVersion('" + groupArtifactVersionBecause.getVersion() + "')\n";
                    if (groupArtifactVersionBecause.getBecause() != null) {
                        snippet += variableName + ".because('" + groupArtifactVersionBecause.getBecause() + "')\n";
                    }
                    newStatements = GroovyParser.builder()
                            .build()
                            .parse(ctx, snippet)
                            .map(G.CompilationUnit.class::cast)
                            .map(G.CompilationUnit::getStatements)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Unable to produce a new block statement"));
                } else {
                    @Language("kotlin")
                    String snippet = variableName + ".useVersion(\"" + groupArtifactVersionBecause.getVersion() + "\")\n";
                    if (groupArtifactVersionBecause.getBecause() != null) {
                        snippet += variableName + ".because(\"" + groupArtifactVersionBecause.getBecause() + "\")\n";
                    }
                    newStatements = KotlinParser.builder()
                            .isKotlinScript(true)
                            .build()
                            .parse(ctx, snippet)
                            .map(K.CompilationUnit.class::cast)
                            .map(cu -> (J.Block) cu.getStatements().get(0))
                            .map(J.Block::getStatements)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Unable to produce a new block statement"));
                }
                J.Block block = (J.Block) anIf.getThenPart();
                block = block.withStatements(newStatements);
                block = autoFormat(block, ctx, getCursor());
                anIf = anIf.withThenPart(block);
            }
            return anIf;
        }
    }

    static class MaybeAddEachDependency extends JavaIsoVisitor<ExecutionContext> {
        boolean alreadyExists;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (isEachDependency(m) && !isInBuildscriptBlock(getCursor())) {
                alreadyExists = true;
            }
            return m;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            // Avoid wasting time if we've already found it
            if (alreadyExists) {
                return (J) tree;
            }
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile sourceFile = (JavaSourceFile) super.visit(tree, ctx);
                if (alreadyExists) {
                    return sourceFile;
                }
                // Prefer to insert before the dependencies block for readability
                if (sourceFile instanceof G.CompilationUnit) {
                    G.CompilationUnit cu = (G.CompilationUnit) sourceFile;
                    int insertionIndex = 0;
                    while (insertionIndex < cu.getStatements().size()) {
                        Statement s = cu.getStatements().get(insertionIndex);
                        if (s instanceof J.MethodInvocation && DEPENDENCIES_DSL_MATCHER.matches((J.MethodInvocation) s)) {
                            break;
                        }
                        insertionIndex++;
                    }
                    J.MethodInvocation m = GradleParser.builder()
                            .build()
                            .parse(ctx,
                                    "\n" +
                                            "configurations.all {\n" +
                                            "    resolutionStrategy.eachDependency { details ->\n" +
                                            "    }\n" +
                                            "}")
                            .map(G.CompilationUnit.class::cast)
                            .map(G.CompilationUnit::getStatements)
                            .map(it -> it.get(0))
                            .map(J.MethodInvocation.class::cast)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Unable to create a new configurations.all block"));
                    return cu.withStatements(ListUtils.insert(cu.getStatements(), m, insertionIndex));
                } else {
                    K.CompilationUnit cu = (K.CompilationUnit) sourceFile;
                    assert cu != null;
                    J.Block block = (J.Block) cu.getStatements().get(0);
                    int insertionIndex = 0;
                    while (insertionIndex < block.getStatements().size()) {
                        Statement s = block.getStatements().get(insertionIndex);
                        if (s instanceof J.MethodInvocation && ((J.MethodInvocation) s).getSimpleName().equals("dependencies")) {
                            break;
                        }
                        insertionIndex++;
                    }
                    J.MethodInvocation m = GradleParser.builder()
                            .build()
                            .parseInputs(Collections.singletonList(
                                    new Parser.Input(
                                            Paths.get("build.gradle.kts"),
                                            () -> new ByteArrayInputStream(
                                                    ("\n" +
                                                            "configurations.all {\n" +
                                                            "    resolutionStrategy.eachDependency { details ->}\n" +
                                                            "}").getBytes(StandardCharsets.UTF_8)))
                            ), null, ctx)
                            .map(K.CompilationUnit.class::cast)
                            .map(k -> (J.Block) k.getStatements().get(0))
                            .map(J.Block::getStatements)
                            .map(it -> it.get(0))
                            .map(J.MethodInvocation.class::cast)
                            .findFirst()
                            .map(m2 -> m2.withArguments(ListUtils.mapFirst(m2.getArguments(), arg -> {
                                J.Lambda lambda1 = (J.Lambda) arg;
                                J.Block block1 = (J.Block) lambda1.getBody();
                                return lambda1.withBody(block1.withStatements(ListUtils.mapFirst(block1.getStatements(), arg2 -> {
                                    J.MethodInvocation m3 = (J.MethodInvocation) arg2;
                                    return m3.withArguments(ListUtils.mapFirst(m3.getArguments(), arg3 -> {
                                        J.Lambda lambda2 = (J.Lambda) arg3;
                                        return lambda2.withBody(((J.Block) lambda2.getBody()).withEnd(Space.format("\n")));
                                    }));
                                })));
                            })))
                            .orElseThrow(() -> new IllegalStateException("Unable to create a new configurations.all block"));
                    final int finalInsertionIndex = insertionIndex;
                    return cu.withStatements(ListUtils.mapFirst(cu.getStatements(), arg -> {
                        if (arg == block) {
                            return block.withStatements(ListUtils.insert(block.getStatements(), m, finalInsertionIndex));
                        }
                        return arg;
                    }));
                }
            }
            return super.visit(tree, ctx);
        }
    }

    @Value
    static class MaybeRemoveDependencyBlock extends JavaIsoVisitor<ExecutionContext> {
        boolean isKotlinDsl;

        @Override
        public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (isEmptyDependenciesBlock(m)) {
                return null;
            }
            return m;
        }
    }

    private static boolean isEmptyDependenciesBlock(J.MethodInvocation m) {
        if (!m.getSimpleName().equals("dependencies")) {
            return false;
        }
        if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
            return false;
        }
        // `dependencies` should always take a single "closure"
        if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
            return false;
        }
        J.Lambda l = (J.Lambda) m.getArguments().get(0);
        if (l.getBody() instanceof J.Block) {
            J.Block b = (J.Block) l.getBody();
            if (b.getStatements().size() == 1) {
                return b.getStatements().get(0) instanceof J.Return && ((J.Return) b.getStatements().get(0)).getExpression() == null;
            }
        }
        return false;
    }

    private static boolean isInDependenciesBlock(Cursor cursor) {
        Cursor c = cursor.dropParentUntil(value ->
                value == Cursor.ROOT_VALUE ||
                        (value instanceof J.MethodInvocation && ((J.MethodInvocation) value).getSimpleName().equals("dependencies")));
        if (!(c.getValue() instanceof J.MethodInvocation)) {
            return false;
        }
        // Exclude "dependencies" blocks inside of buildscripts
        // No plugins can prevent the "constraints" block from working there, as they can for regular dependencies block
        return !isInBuildscriptBlock(c);
    }

    private static boolean isInBuildscriptBlock(Cursor c) {
        Cursor maybeBuildscript = c.dropParentUntil(value -> value == Cursor.ROOT_VALUE || (value instanceof J.MethodInvocation && ((J.MethodInvocation) value).getSimpleName().equals("buildscript")));
        return maybeBuildscript.getValue() != Cursor.ROOT_VALUE;
    }

    private static boolean isEachDependency(J.MethodInvocation m) {
        return "eachDependency".equals(m.getSimpleName()) &&
                (m.getSelect() instanceof J.Identifier &&
                        "resolutionStrategy".equals(((J.Identifier) m.getSelect()).getSimpleName()));
    }

    private static boolean predicateRelatesToGav(J.If iff, GroupArtifactVersionBecause groupArtifactVersion) {
        Expression predicate = iff.getIfCondition().getTree();
        if (!(predicate instanceof J.Binary)) {
            return false;
        }
        J.Binary and = (J.Binary) predicate;
        // Looking for a comparison of group id && artifact id
        if (and.getOperator() != J.Binary.Type.And) {
            return false;
        }
        // GroupId and artifactId might be compared in either order or this could be an unrelated comparison
        AtomicBoolean groupIdCompared = new AtomicBoolean();
        AtomicBoolean artifactIdCompared = new AtomicBoolean();
        new JavaIsoVisitor<GroupArtifactVersionBecause>() {
            @Override
            public J.Binary visitBinary(J.Binary binary, GroupArtifactVersionBecause groupArtifactVersion) {
                J.Binary b = super.visitBinary(binary, groupArtifactVersion);
                if (b.getOperator() != J.Binary.Type.Equal) {
                    return b;
                }
                J.FieldAccess access = null;
                J.Literal literal = null;
                if (b.getLeft() instanceof J.FieldAccess && b.getRight() instanceof J.Literal) {
                    access = (J.FieldAccess) b.getLeft();
                    literal = (J.Literal) b.getRight();
                } else if (b.getRight() instanceof J.FieldAccess && b.getLeft() instanceof J.Literal) {
                    access = (J.FieldAccess) b.getRight();
                    literal = (J.Literal) b.getLeft();
                }
                //noinspection ConstantValue
                if (access == null || literal == null) {
                    return b;
                }
                if ("group".equals(access.getSimpleName()) && Objects.equals(groupArtifactVersion.getGroupId(), literal.getValue())) {
                    groupIdCompared.set(true);
                } else if ("name".equals(access.getSimpleName()) && Objects.equals(groupArtifactVersion.getArtifactId(), literal.getValue())) {
                    artifactIdCompared.set(true);
                }
                return b;
            }
        }.visit(and, groupArtifactVersion);

        return groupIdCompared.get() && artifactIdCompared.get();
    }
}
