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
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

public class UseDiamondOperator extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use diamond operator";
    }

    @Override
    public String getDescription() {
        return "The diamond operator (`<>`) should be used. Java 7 introduced the diamond operator (<>) to reduce the verbosity of generics code. For instance, instead of having to declare a List's type in both its declaration and its constructor, you can now simplify the constructor declaration with `<>`, and the compiler will infer the type.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2293");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UseDiamondOperatorVisitor();
    }

    private static class UseDiamondOperatorVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                // don't try to do this for Groovy or Kotlin sources
                return cu instanceof J.CompilationUnit ? visitCompilationUnit((J.CompilationUnit) cu, ctx) : cu;
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            J.VariableDeclarations varDecls = super.visitVariableDeclarations(multiVariable, executionContext);
            final TypedTree varDeclsTypeExpression = varDecls.getTypeExpression();
            if (varDecls.getVariables().size() == 1 && varDecls.getVariables().get(0).getInitializer() != null
                && varDecls.getTypeExpression() instanceof J.ParameterizedType) {
                varDecls = varDecls.withVariables(ListUtils.map(varDecls.getVariables(), nv -> {
                    if (nv.getInitializer() instanceof J.NewClass) {
                        nv = nv.withInitializer(maybeRemoveParams(parameterizedTypes((J.ParameterizedType) varDeclsTypeExpression), (J.NewClass) nv.getInitializer()));
                    }
                    return nv;
                }));
            }
            return varDecls;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
            J.Assignment asgn = super.visitAssignment(assignment, executionContext);
            if (asgn.getAssignment() instanceof J.NewClass) {
                JavaType.Parameterized assignmentType = TypeUtils.asParameterized(asgn.getType());
                J.NewClass nc = (J.NewClass) asgn.getAssignment();
                if (assignmentType != null && nc.getClazz() instanceof J.ParameterizedType) {
                    asgn = asgn.withAssignment(maybeRemoveParams(assignmentType.getTypeParameters(), nc));
                }
            }
            return asgn;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            if (isAParameter()) {
                return method;
            }

            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            JavaType.Method methodType = mi.getMethodType();
            if (methodType != null) {
                mi = mi.withArguments(ListUtils.map(mi.getArguments(), (i, arg) -> {
                    if (arg instanceof J.NewClass) {
                        J.NewClass nc = (J.NewClass) arg;
                        if (nc.getBody() == null && !methodType.getParameterTypes().isEmpty()) {
                            JavaType.Parameterized paramType = TypeUtils.asParameterized(getMethodParamType(methodType, i));
                            if (paramType != null && nc.getClazz() instanceof J.ParameterizedType) {
                                return maybeRemoveParams(paramType.getTypeParameters(), nc);
                            }
                        }
                    }
                    return arg;
                }));
            }
            return mi;
        }

        private JavaType getMethodParamType(JavaType.Method methodType, int paramIndex) {
            if (methodType.hasFlags(Flag.Varargs) && paramIndex >= methodType.getParameterTypes().size() - 1) {
                return ((JavaType.Array) methodType.getParameterTypes().get(methodType.getParameterTypes().size() - 1)).getElemType();
            } else {
                return methodType.getParameterTypes().get(paramIndex);
            }
        }

        @Override
        public J.Return visitReturn(J.Return _return, ExecutionContext executionContext) {
            J.Return rtn = super.visitReturn(_return, executionContext);
            J.NewClass returnExpNewClass = rtn.getExpression() instanceof J.NewClass ? (J.NewClass) rtn.getExpression() : null;
            if (returnExpNewClass != null && returnExpNewClass.getBody() == null && returnExpNewClass.getClazz() instanceof J.ParameterizedType) {
                J parentBlock = getCursor().dropParentUntil(v -> v instanceof J.MethodDeclaration || v instanceof J.Lambda).getValue();
                if (parentBlock instanceof J.MethodDeclaration) {
                    J.MethodDeclaration md = (J.MethodDeclaration) parentBlock;
                    if (md.getReturnTypeExpression() instanceof J.ParameterizedType) {
                        rtn = rtn.withExpression(
                                maybeRemoveParams(parameterizedTypes((J.ParameterizedType) md.getReturnTypeExpression()), returnExpNewClass));
                    }
                }
            }
            return rtn;
        }

        @Nullable
        private List<JavaType> parameterizedTypes(J.ParameterizedType parameterizedType) {
            if (parameterizedType.getTypeParameters() == null) {
                return null;
            }
            List<JavaType> types = new ArrayList<>(parameterizedType.getTypeParameters().size());
            for (Expression typeParameter : parameterizedType.getTypeParameters()) {
                types.add(typeParameter.getType());
            }
            return types;
        }


        private J.NewClass maybeRemoveParams(@Nullable List<JavaType> paramTypes, J.NewClass newClass) {
            if (paramTypes != null && newClass.getBody() == null && newClass.getClazz() instanceof J.ParameterizedType) {
                J.ParameterizedType newClassType = (J.ParameterizedType) newClass.getClazz();
                if (newClassType.getTypeParameters() != null) {
                    if (paramTypes.size() != newClassType.getTypeParameters().size()) {
                        return newClass;
                    } else {
                        for (int i = 0; i < paramTypes.size(); i++) {
                            if (!TypeUtils.isAssignableTo(paramTypes.get(i), newClassType.getTypeParameters().get(i).getType())) {
                                return newClass;
                            }
                        }
                    }
                    newClassType.getTypeParameters().stream()
                            .map(e -> TypeUtils.asFullyQualified(e.getType()))
                            .forEach(this::maybeRemoveImport);
                    newClass = newClass.withClazz(newClassType.withTypeParameters(singletonList(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))));
                }
            }
            return newClass;
        }

        private boolean isAParameter() {
            return getCursor().dropParentUntil(p -> p instanceof J.MethodInvocation ||
                                                    p instanceof J.ClassDeclaration ||
                                                    p instanceof J.CompilationUnit ||
                                                    p instanceof J.Block ||
                                                    p == Cursor.ROOT_VALUE).getValue() instanceof J.MethodInvocation;
        }
    }
}
