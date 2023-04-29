/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class UnnecessaryCloseInTryWithResources extends Recipe {
    @Override
    public String getDisplayName() {
        return "Unnecessary close in try-with-resources";
    }

    @Override
    public String getDescription() {
        return "Remove unnecessary `AutoCloseable#close()` statements in try-with-resources.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-4087");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UnnecessaryAutoCloseableVisitor();
    }

    private static class UnnecessaryAutoCloseableVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher AUTO_CLOSEABLE_METHOD_MATCHER = new MethodMatcher("java.lang.AutoCloseable close()", true);

        @Override
        public J.Try visitTry(J.Try aTry, ExecutionContext executionContext) {
            J.Try tr = super.visitTry(aTry, executionContext);
            if (tr.getResources() != null) {
                String[] resourceNames = new String[tr.getResources().size()];
                for (int i = 0; i < tr.getResources().size(); i++) {
                    J.Try.Resource tryResource = tr.getResources().get(i);
                    if (tryResource.getVariableDeclarations() instanceof J.VariableDeclarations) {
                        J.VariableDeclarations varDecls = (J.VariableDeclarations) tryResource.getVariableDeclarations();
                        resourceNames[i] = varDecls.getVariables().get(0).getSimpleName();
                    } else if (tryResource.getVariableDeclarations() instanceof J.Identifier) {
                        J.Identifier identifier = (J.Identifier) tryResource.getVariableDeclarations();
                        resourceNames[i] = identifier.getSimpleName();
                    }
                }

                tr = tr.withBody(tr.getBody().withStatements(ListUtils.map(tr.getBody().getStatements(), statement -> {
                    if (statement instanceof J.MethodInvocation) {
                        J.MethodInvocation mi = (J.MethodInvocation) statement;
                        if (AUTO_CLOSEABLE_METHOD_MATCHER.matches(mi) && mi.getSelect() instanceof J.Identifier) {
                            String selectName = ((J.Identifier) mi.getSelect()).getSimpleName();
                            for (String resourceName : resourceNames) {
                                if (resourceName.equals(selectName)) {
                                    return null;
                                }
                            }
                        }
                    }
                    return statement;
                })));
            }
            return tr;
        }
    }
}
