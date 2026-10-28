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
import org.openrewrite.*;
import org.openrewrite.gradle.internal.RemoveStatementsVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.openrewrite.gradle.ExcludeDependency.effective;
import static org.openrewrite.gradle.ExcludeDependency.extractNamedArg;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveExclusion extends Recipe {

    @Option(displayName = "Group",
            description = "The group of the excluded dependency. If omitted (or `*`), excludes of the artifact from any group are removed. " +
                    "Partial wildcards are not supported.",
            example = "org.springframework.boot",
            required = false)
    @Nullable
    String groupId;

    @Option(displayName = "Artifact",
            description = "The module of the excluded dependency. If omitted (or `*`), excludes of any module in the group are removed, " +
                    "including those of the whole group. Partial wildcards are not supported.",
            example = "spring-boot-starter-tomcat",
            required = false)
    @Nullable
    String artifactId;

    String displayName = "Remove Gradle dependency exclusion";

    String description = "Removes `exclude` declarations of the given dependency from Gradle build scripts, the inverse of " +
            "`ExcludeDependency`. Covers excludes on a dependency declaration and on configurations, in both the Groovy and Kotlin DSL. " +
            "An exclude that leaves out the group also matches, as it excludes the given artifact as well. " +
            "An exclude of a whole group only matches when the artifact is left out, as removing it would bring back every other module of the group too. " +
            "A closure the removal leaves empty is removed too.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId == null ? "*" : groupId, artifactId == null ? "*" : artifactId);
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                        "coordinates",
                        "At least one of `groupId` or `artifactId` must be provided (and not `*`)",
                        this,
                        r -> effective(r.groupId) != null || effective(r.artifactId) != null))
                .and(Validated.test("groupId", "Must be an exact group (letters, digits, `.`, `-`, `_`) or `*`; partial wildcards are not supported",
                        groupId, ExcludeDependency::isExactOrBareWildcard))
                .and(Validated.test("artifactId", "Must be an exact artifact (letters, digits, `.`, `-`, `_`) or `*`; partial wildcards are not supported",
                        artifactId, ExcludeDependency::isExactOrBareWildcard));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                List<RemovedExclusion> removed = new ArrayList<>();
                Tree after = removeVisitor(removed).visit(tree, ctx);
                if (after == tree || !(after instanceof JavaSourceFile) || removed.isEmpty()) {
                    return after;
                }
                JavaSourceFile sourceFile = (JavaSourceFile) after;
                Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
                if (maybeGp.isPresent()) {
                    sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().setByType(
                            updateGradleModel(maybeGp.get(), removed, ctx)));
                    JavaSourceSet.markDirty(ctx, sourceFile);
                }
                return sourceFile;
            }
        });
    }

    private RemoveStatementsVisitor<ExecutionContext> removeVisitor(List<RemovedExclusion> removed) {
        String effG = effective(groupId);
        String effA = effective(artifactId);
        return new RemoveStatementsVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (excludesDependency(method)) {
                    remove(method);
                    return method;
                }
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (m == method || !endsWithEmptyClosure(m) || endsWithEmptyClosure(method)) {
                    return m;
                }
                List<Expression> args = m.getArguments();
                if (args.size() == 1 || isConfigurationLookup(m, getCursor())) {
                    remove(m);
                    return m;
                }
                return m.withArguments(args.subList(0, args.size() - 1));
            }

            private boolean excludesDependency(J.MethodInvocation method) {
                if (!"exclude".equals(method.getSimpleName())) {
                    return false;
                }
                if (hasNonLiteralArgument(method)) {
                    return false;
                }
                boolean kotlinDsl = getCursor().firstEnclosing(K.CompilationUnit.class) != null;
                String group = extractNamedArg(method, "group", kotlinDsl);
                String module = extractNamedArg(method, "module", kotlinDsl);
                boolean matches = effA == null ?
                        Objects.equals(effG, group) :
                        effA.equals(module) && (group == null || effG == null || effG.equals(group));
                if (matches) {
                    recordOnDeclaration(group, module);
                }
                return matches;
            }

            private void recordOnDeclaration(@Nullable String group, @Nullable String module) {
                Cursor enclosing = getCursor().dropParentUntil(v -> v instanceof J.MethodInvocation || v == Cursor.ROOT_VALUE);
                if (!(enclosing.getValue() instanceof J.MethodInvocation)) {
                    return;
                }
                new GradleDependency.Matcher().get(enclosing).ifPresent(dep -> removed.add(new RemovedExclusion(
                        dep.getConfigurationName(),
                        new GroupArtifact(dep.getGroupId(), dep.getArtifactId()),
                        new GroupArtifact(group == null ? "*" : group, module == null ? "*" : module))));
            }
        };
    }

    @Value
    private static class RemovedExclusion {
        String configuration;
        GroupArtifact declared;
        GroupArtifact exclusion;
    }

    private static GradleProject updateGradleModel(GradleProject gp, List<RemovedExclusion> removed, ExecutionContext ctx) {
        List<MavenRepository> repositories = gp.getMavenRepositories();
        for (RemovedExclusion r : removed) {
            if (gp.getConfiguration(r.getConfiguration()) != null) {
                gp = gp.mapConfiguration(r.getConfiguration(),
                        conf -> conf.mapDependencies(d -> withoutExclusion(d, r), repositories, ctx), ctx);
            }
        }
        return gp;
    }

    private static Dependency withoutExclusion(Dependency d, RemovedExclusion r) {
        if (d.getExclusions() == null ||
                !Objects.equals(d.getGroupId(), r.getDeclared().getGroupId()) ||
                !Objects.equals(d.getArtifactId(), r.getDeclared().getArtifactId())) {
            return d;
        }
        List<GroupArtifact> remaining = ListUtils.filter(d.getExclusions(), e -> !e.equals(r.getExclusion()));
        return remaining == d.getExclusions() ? d : d.withExclusions(remaining);
    }

    // A group or module that isn't a literal could be anything, so it must not match like a left-out one
    private static boolean hasNonLiteralArgument(J.MethodInvocation method) {
        for (Expression arg : method.getArguments()) {
            Expression value = arg instanceof G.MapEntry ? ((G.MapEntry) arg).getValue() :
                    arg instanceof J.Assignment ? ((J.Assignment) arg).getAssignment() : arg;
            if (!(value instanceof J.Literal)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConfigurationLookup(J.MethodInvocation method, Cursor cursor) {
        if (!"named".equals(method.getSimpleName()) && !"getByName".equals(method.getSimpleName())) {
            return false;
        }
        Expression select = method.getSelect();
        if (select == null) {
            Object enclosing = cursor.dropParentUntil(v -> v instanceof J.MethodInvocation || v == Cursor.ROOT_VALUE).getValue();
            return enclosing instanceof J.MethodInvocation && "configurations".equals(((J.MethodInvocation) enclosing).getSimpleName());
        }
        return select instanceof J.Identifier && "configurations".equals(((J.Identifier) select).getSimpleName()) ||
                select instanceof J.FieldAccess && "configurations".equals(((J.FieldAccess) select).getSimpleName());
    }

    private static boolean endsWithEmptyClosure(J.MethodInvocation method) {
        List<Expression> args = method.getArguments();
        Expression last = args.isEmpty() ? null : args.get(args.size() - 1);
        if (!(last instanceof J.Lambda) || !(((J.Lambda) last).getBody() instanceof J.Block)) {
            return false;
        }
        J.Block body = (J.Block) ((J.Lambda) last).getBody();
        // A closure still holding a comment is kept, so that no comment is silently deleted
        return body.getStatements().isEmpty() && body.getEnd().getComments().isEmpty();
    }
}
