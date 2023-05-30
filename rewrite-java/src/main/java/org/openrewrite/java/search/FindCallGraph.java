/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTypeSignatureBuilder;
import org.openrewrite.java.internal.DefaultJavaTypeSignatureBuilder;
import org.openrewrite.java.table.MethodCallGraph;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class FindCallGraph extends Recipe {
    transient final MethodCallGraph methodCallGraph = new MethodCallGraph(this);

    @Override
    public String getDisplayName() {
        return "Build call graph";
    }

    @Override
    public String getDescription() {
        return "Produce the call graph describing the relationships between methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final JavaTypeSignatureBuilder signatureBuilder = new CallGraphSignatureBuilder();
            final Set<JavaType.Method> methodsCalledInDeclaration = Collections.newSetFromMap(new IdentityHashMap<>());

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, executionContext);
                methodsCalledInDeclaration.clear();
                return m;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                recordCall(method.getMethodType(), ctx);
                return super.visitMethodInvocation(method, ctx);
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                recordCall(memberRef.getMethodType(), ctx);
                return super.visitMemberReference(memberRef, ctx);
            }

            private void recordCall(@Nullable JavaType.Method method, ExecutionContext ctx) {
                if (method == null) {
                    return;
                }
                J.MethodDeclaration declaration = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (declaration != null && declaration.getMethodType() != null && methodsCalledInDeclaration.add(method)) {
                    methodsCalledInDeclaration.add(method);
                    methodCallGraph.insertRow(ctx, new MethodCallGraph.Row(
                            signatureBuilder.signature(declaration.getMethodType()),
                            signatureBuilder.signature(method)
                    ));
                }
            }
        };
    }

    private static class CallGraphSignatureBuilder extends DefaultJavaTypeSignatureBuilder {
        @Override
        public String genericSignature(Object type) {
            return super.genericSignature(type)
                    .replace("Generic{", "<")
                    .replace("}", ">");
        }

        public String methodSignature(JavaType.Method method) {
            StringBuilder s = new StringBuilder(signature(method.getDeclaringType()));
            s.append(" ").append(method.getName()).append("(");
            List<JavaType> parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.size(); i++) {
                JavaType parameterType = parameterTypes.get(i);
                s.append(i == 0 ? "" : ", ");
                s.append(signature(parameterType));
            }
            s.append(")");
            return s.toString();
        }
    }
}
