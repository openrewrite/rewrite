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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class RemoveCallsToSystemGc extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove calls to `System.gc()` method";
    }

    @Override
    public String getDescription() {
        return "Removes calls to System.gc()";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1215");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new NoGcVisitor();
    }
    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>("java.lang.System gc()");
    }
    private static class NoGcVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
            J.MethodInvocation invocation = super.visitMethodInvocation(method, context);
            if (invocation.getSelect() != null) {
                final JavaType.FullyQualified fq = TypeUtils.asFullyQualified(invocation.getSelect().getType());
                if (fq != null) {
                    final Boolean isSystem = "java.lang.System".equals(fq.getFullyQualifiedName());
                    final Boolean isGc = "gc".equals(method.getName().getSimpleName());
                    final Boolean isEmptyArgs = method.getArguments().get(0) instanceof J.Empty;
                    if (isSystem && isGc && isEmptyArgs) {
                        return null;
                    }
                }
            }
            return invocation;
        }
    }

}
