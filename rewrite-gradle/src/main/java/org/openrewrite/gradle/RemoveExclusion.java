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
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;

import java.util.List;

import static org.openrewrite.gradle.ExcludeDependency.extractNamedArg;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveExclusion extends Recipe {

    @Option(displayName = "Group",
            description = "The group of the excluded dependency. If omitted, excludes of the artifact from any group are removed.",
            example = "org.springframework.boot",
            required = false)
    @Nullable
    String groupId;

    @Option(displayName = "Artifact",
            description = "The module of the excluded dependency. If omitted, excludes of any module in the group are removed.",
            example = "spring-boot-starter-tomcat",
            required = false)
    @Nullable
    String artifactId;

    String displayName = "Remove Gradle dependency exclusion";

    String description = "Removes `exclude` declarations of the given dependency from Gradle build scripts, the inverse of " +
            "`ExcludeDependency`. Covers excludes on a dependency declaration and on configurations, in both the Groovy and Kotlin DSL. " +
            "An exclude that leaves the group or the module out also matches, as it excludes the given dependency as well. " +
            "A closure the removal leaves empty is removed too.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId == null ? "*" : groupId, artifactId == null ? "*" : artifactId);
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                "coordinates",
                "At least one of `groupId` or `artifactId` must be provided",
                this,
                r -> r.groupId != null || r.artifactId != null));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new RemoveStatementsVisitor<ExecutionContext>() {
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
                if (args.size() == 1) {
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
                boolean groupMatches = group == null || groupId == null || groupId.equals(group);
                boolean moduleMatches = module == null || artifactId == null || artifactId.equals(module);
                boolean named = (groupId != null && groupId.equals(group)) || (artifactId != null && artifactId.equals(module));
                return groupMatches && moduleMatches && named;
            }
        });
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
