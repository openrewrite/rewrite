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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
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
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit compilationUnit, ExecutionContext ctx) {
                List<GroupArtifactVersionBecause> gavs = new ArrayList<>();
                Cursor parent = requireNonNull(getCursor().getParent());
                G.CompilationUnit cu = (G.CompilationUnit) new RemoveConstraints().visitNonNull(compilationUnit, gavs, parent);
                if (gavs.isEmpty()) {
                    return compilationUnit;
                }
                cu = (G.CompilationUnit) new MaybeAddEachDependency().visitNonNull(cu, 0, parent);
                cu = (G.CompilationUnit) new UpdateEachDependency().visitNonNull(cu, gavs, parent);
                return cu;
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

    static class RemoveConstraints extends GroovyIsoVisitor<List<GroupArtifactVersionBecause>> {

        @SuppressWarnings("DataFlowIssue")
        @Override
        public  J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<GroupArtifactVersionBecause> groupArtifactVersions) {
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
                    new GroovyIsoVisitor<Integer>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                            J.MethodInvocation m1 = super.visitMethodInvocation(method, integer);
                            if("because".equals(m1.getSimpleName()) && m1.getArguments().get(0) instanceof J.Literal) {
                                because.set(((J.Literal) m1.getArguments().get(0)).getValue().toString());
                            }
                            return m1;
                        }
                    }.visit(constraint.getArguments(), 0);

                    groupArtifactVersions.add(new GroupArtifactVersionBecause(gav[0], gav[1], gav[2], because.get()));
                    return null;
                });
                // If nothing remains in the constraints{} it can be removed entirely
                if(withoutConvertableConstraints.isEmpty()) {
                    return null;
                } else {
                    return m.withArguments(singletonList(closure.withBody(((J.Block) closure.getBody()).withStatements(withoutConvertableConstraints))));
                }
            }
            return m;
        }
    }

    static class UpdateEachDependency extends GroovyIsoVisitor<List<GroupArtifactVersionBecause>> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<GroupArtifactVersionBecause> groupArtifactVersions) {
            J.MethodInvocation m = super.visitMethodInvocation(method, groupArtifactVersions);
            if (isEachDependency(m)) {
                Cursor parent = requireNonNull(getCursor().getParent());
                for (GroupArtifactVersionBecause gav : groupArtifactVersions) {
                    m = (J.MethodInvocation) new MaybeAddIf().visitNonNull(m, gav, parent);
                    m = (J.MethodInvocation) new UpdateIf().visitNonNull(m, gav, parent);
                }
            }
            return m;
        }
    }

    static class MaybeAddIf extends GroovyIsoVisitor<GroupArtifactVersionBecause> {
        boolean containsAnyIfStatement;
        boolean containsMatchingIfStatement;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, GroupArtifactVersionBecause groupArtifactVersion) {
            J.MethodInvocation m = super.visitMethodInvocation(method, groupArtifactVersion);
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
            @SuppressWarnings("GroovyEmptyStatementBody") @Language("groovy")
            String snippet = "Object " + p + " = null\n" +
                             "if (" + p + ".requested.group == '" + groupArtifactVersion.getGroupId() + "' && " +
                             p + ".requested.name == '" + groupArtifactVersion.getArtifactId() + "') {\n}";
            J.If newIf = GroovyParser.builder().build()
                    .parse(snippet)
                    .map(G.CompilationUnit.class::cast)
                    .map(cu -> cu.getStatements().get(1))
                    .map(J.If.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to produce a new if statement"));
            if (containsAnyIfStatement) {
                m = (J.MethodInvocation) new GroovyIsoVisitor<Integer>() {
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
                J.Block newBody = autoFormat(closureBody.withStatements(ListUtils.concat(newIf, closureBody.getStatements())), groupArtifactVersion, getCursor());
                m = m.withArguments(singletonList(closure.withBody(newBody)));
            }
            return m;
        }


        @Override
        public J.If visitIf(J.If iff, GroupArtifactVersionBecause groupArtifactVersion) {
            containsAnyIfStatement = true;
            J.If f = super.visitIf(iff, groupArtifactVersion);
            if (predicateRelatesToGav(f, groupArtifactVersion)) {
                containsMatchingIfStatement = true;
            }
            return iff;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, GroupArtifactVersionBecause groupArtifactVersion) {
            // Avoid wasting time if we've already found it
            if (containsMatchingIfStatement) {
                return (J) tree;
            }
            return super.visit(tree, groupArtifactVersion);
        }
    }

    static class UpdateIf extends GroovyIsoVisitor<GroupArtifactVersionBecause> {
        @Override
        public J.If visitIf(J.If iff, GroupArtifactVersionBecause groupArtifactVersionBecause) {
            J.If anIf = super.visitIf(iff, groupArtifactVersionBecause);
            if (predicateRelatesToGav(anIf, groupArtifactVersionBecause)) {
                // The predicate of the if condition will already contain the relevant variable name
                AtomicReference<String> variableName = new AtomicReference<>();
                new GroovyIsoVisitor<Integer>() {
                    @Override
                    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer integer) {
                        // Comparison will involve "<variable name>.requested.group"
                        J.FieldAccess field = super.visitFieldAccess(fieldAccess, integer);
                        if(field.getTarget() instanceof J.Identifier) {
                            variableName.set(((J.Identifier) field.getTarget()).getSimpleName());
                        }
                        return fieldAccess;
                    }
                }.visit(anIf.getIfCondition(), 0);
                @Language("groovy")
                String snippet = variableName + ".useVersion('" + groupArtifactVersionBecause.getVersion() + "')\n";
                if(groupArtifactVersionBecause.getBecause() != null) {
                    snippet += variableName + ".because('" + groupArtifactVersionBecause.getBecause() + "')\n";
                }
                List<Statement> newStatements = GroovyParser.builder()
                        .build()
                        .parse(snippet)
                        .map(G.CompilationUnit.class::cast)
                        .map(G.CompilationUnit::getStatements)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to produce a new block statement"));
                J.Block block = (J.Block) anIf.getThenPart();
                block = block.withStatements(newStatements);
                block = autoFormat(block, groupArtifactVersionBecause, getCursor());
                anIf = anIf.withThenPart(block);
            }
            return anIf;
        }
    }

    static class MaybeAddEachDependency extends GroovyIsoVisitor<Integer> {
        boolean alreadyExists;

        @Override
        public G.CompilationUnit visitCompilationUnit(G.CompilationUnit compilationUnit, Integer integer) {
            G.CompilationUnit cu = super.visitCompilationUnit(compilationUnit, integer);
            if (alreadyExists) {
                return cu;
            }
            // Prefer to insert before the dependencies block for readability
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
                    .parse("\n" +
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
            cu = cu.withStatements(ListUtils.insert(cu.getStatements(), m, insertionIndex));
            return cu;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
            J.MethodInvocation m = super.visitMethodInvocation(method, integer);
            if (isEachDependency(m)) {
                alreadyExists = true;
            }
            return m;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, Integer integer) {
            // Avoid wasting time if we've already found it
            if (alreadyExists) {
                return (J) tree;
            }
            return super.visit(tree, integer);
        }
    }

    private static boolean isInDependenciesBlock(Cursor cursor) {
        Cursor c = cursor.dropParentUntil(value ->
                value == Cursor.ROOT_VALUE ||
                (value instanceof J.MethodInvocation && DEPENDENCIES_DSL_MATCHER.matches((J.MethodInvocation) value)));
        return c.getValue() instanceof J.MethodInvocation;
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
        new GroovyIsoVisitor<GroupArtifactVersionBecause>() {
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
