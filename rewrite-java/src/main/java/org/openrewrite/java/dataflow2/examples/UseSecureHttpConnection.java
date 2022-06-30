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
package org.openrewrite.java.dataflow2.examples;

import lombok.AllArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.dataflow2.DataFlowGraph;
import org.openrewrite.java.dataflow2.ProgramPoint;
import org.openrewrite.java.dataflow2.ProgramState;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class UseSecureHttpConnection extends Recipe {
    private static final MethodMatcher URI_CREATE_MATCHER = new MethodMatcher("java.net.URI create(String)");

    @Override
    public String getDisplayName() {
        return "Use secure HTTP connection";
    }

    @Override
    public String getDescription() {
        return "Use HTTPS instead of HTTP wherever possible.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesMethod<>(URI_CREATE_MATCHER);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                if (URI_CREATE_MATCHER.matches(mi)) {
                    J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                    HttpAnalysis httpAnalysis = new HttpAnalysis(new DataFlowGraph(cu));
                    ProgramState<HttpAnalysisValue> state = httpAnalysis.getStateAfter(mi.getArguments().get(0));
                    HttpAnalysisValue stateValue = state.expr();
                    if (stateValue.getName() == HttpAnalysisValue.Understanding.NOT_SECURE) {
                        doAfterVisit(new HttpToHttpsVisitor((J.Literal) stateValue.getLiteral()));
                    }
                }
                return mi;
            }
        };
    }

    @AllArgsConstructor
    private static class HttpToHttpsVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final J.Literal insecureHttps;

        @Override
        public J.Literal visitLiteral(J.Literal literal, ExecutionContext executionContext) {
            if (literal == insecureHttps) {
                assert literal.getValueSource() != null;
                return literal.withValueSource(literal.getValueSource().replace("http", "https"));
            }
            return literal;
        }
    }

}
