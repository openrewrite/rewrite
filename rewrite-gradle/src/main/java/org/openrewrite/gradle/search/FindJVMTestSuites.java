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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.table.JVMTestSuitesDefined;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindJVMTestSuites extends Recipe {

    transient JVMTestSuitesDefined jvmTestSuitesDefined = new JVMTestSuitesDefined(this);

    @Option(displayName = "Requires dependencies",
            description = "Whether the test suite configuration defines dependencies to be resolved. Defaults to false.")
    @Nullable
    Boolean definesDependencies;

    @Option(displayName = "Insert rows",
            description = "Whether to insert rows into the table. Defaults to true.")
    @Nullable
    Boolean insertRows;

    @Override
    public String getDisplayName() {
        return "Find Gradle JVMTestSuite plugin configuration";
    }

    @Override
    public String getDescription() {
        return "Find Gradle JVMTestSuite plugin configurations and produce a data table.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean requireDependencies = definesDependencies != null && definesDependencies;
        boolean tableAvailable = this.insertRows == null || this.insertRows;
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            private boolean isJVMTestSuitesBlock() {
                Cursor parent = getCursor().getParent();
                if (parent != null) {
                    Iterator<Object> path = parent.getPath(J.MethodInvocation.class::isInstance);
                    if (path.hasNext() && "suites".equals(((J.MethodInvocation) path.next()).getSimpleName())) {
                        return path.hasNext() && "testing".equals(((J.MethodInvocation) path.next()).getSimpleName());
                    }
                }
                return false;
            }

            private boolean definesDependencies(J.MethodInvocation suite) {
                for (Expression suiteDefinition : suite.getArguments()) {
                    if (suiteDefinition instanceof J.Lambda) {
                        for (Statement statement : ((J.Block) ((J.Lambda) suiteDefinition).getBody()).getStatements()) {
                            if (statement instanceof J.Return) {
                                Expression expression = ((J.Return) statement).getExpression();
                                if (expression instanceof J.MethodInvocation) {
                                    if ("dependencies".equals(((J.MethodInvocation) expression).getSimpleName())) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (isJVMTestSuitesBlock()) {
                    if (tableAvailable) {
                        jvmTestSuitesDefined.insertRow(ctx, new JVMTestSuitesDefined.Row(method.getSimpleName()));
                    }
                    return !requireDependencies || definesDependencies(method) ? SearchResult.found(method) : method;
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }

    public static Set<String> jvmTestSuiteNames(Tree tree, boolean definesDependencies) {
        return TreeVisitor.collect(new FindJVMTestSuites(definesDependencies, false).getVisitor(), tree, new HashSet<>())
                .stream()
                .filter(J.MethodInvocation.class::isInstance)
                .map(J.MethodInvocation.class::cast)
                .filter(m -> m.getMarkers().findFirst(SearchResult.class).isPresent())
                .map(J.MethodInvocation::getSimpleName)
                .collect(Collectors.toSet());
    }
}
