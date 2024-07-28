/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * This recipe finds method invocations matching a method pattern and uses a zero-based argument index to determine
 * which argument is removed.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteMethodArgument extends Recipe {

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "com.yourorg.A foo(int, int)")
    String methodPattern;

    /**
     * A zero-based index that indicates which argument will be removed from the method invocation.
     */
    @Option(displayName = "Argument index",
            description = "A zero-based index that indicates which argument will be removed from the method invocation.",
            example = "0")
    int argumentIndex;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%d in methods `%s`", argumentIndex, methodPattern);
    }

    @Override
    public String getDisplayName() {
        return "Delete method argument";
    }

    @Override
    public String getDescription() {
        return "Delete an argument from method invocations.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new DeleteMethodArgumentVisitor(new MethodMatcher(methodPattern)));
    }

    private class DeleteMethodArgumentVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        public DeleteMethodArgumentVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            return (J.MethodInvocation) visitMethodCall(m);
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            return (J.NewClass) visitMethodCall(n);
        }

        private MethodCall visitMethodCall(MethodCall methodCall) {
            MethodCall m = methodCall;
            List<Expression> originalArgs = m.getArguments();
            if (methodMatcher.matches(m) && originalArgs.stream()
                                                    .filter(a -> !(a instanceof J.Empty))
                                                    .count() >= argumentIndex + 1) {
                List<Expression> args = new ArrayList<>(originalArgs);

                args.remove(argumentIndex);
                if (args.isEmpty()) {
                    args = singletonList(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY));
                }

                m = m.withArguments(args);

                JavaType.Method methodType = m.getMethodType();
                if (methodType != null) {
                    List<String> parameterNames = new ArrayList<>(methodType.getParameterNames());
                    parameterNames.remove(argumentIndex);
                    List<JavaType> parameterTypes = new ArrayList<>(methodType.getParameterTypes());
                    parameterTypes.remove(argumentIndex);

                    m = m.withMethodType(methodType
                            .withParameterNames(parameterNames)
                            .withParameterTypes(parameterTypes));
                    if (m instanceof J.MethodInvocation && ((J.MethodInvocation) m).getName().getType() != null) {
                        m = ((J.MethodInvocation) m).withName(((J.MethodInvocation) m).getName().withType(m.getMethodType()));
                    }
                }
            }
            return m;
        }
    }
}
