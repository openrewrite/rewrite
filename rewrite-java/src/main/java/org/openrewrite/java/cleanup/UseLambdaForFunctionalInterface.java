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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.RemoveUnusedImports;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;

public class UseLambdaForFunctionalInterface extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use lambda expressions instead of anonymous classes";
    }

    @Override
    public String getDescription() {
        return "Instead of anonymous class declarations, use a lambda where possible. Using lambdas to replace " +
               "anonymous classes can lead to more expressive and maintainable code, improve code readability, reduce " +
               "code duplication, and achieve better performance in some cases.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-1604");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (n.getBody() != null &&
                    n.getBody().getStatements().size() == 1 &&
                    n.getBody().getStatements().get(0) instanceof J.MethodDeclaration &&
                    n.getClazz() != null) {
                    JavaType.@Nullable FullyQualified type = TypeUtils.asFullyQualified(n.getClazz().getType());
                    if (type != null && type.getKind().equals(JavaType.Class.Kind.Interface)) {
                        JavaType.Method sam = null;
                        for (JavaType.Method method : type.getMethods()) {
                            if (method.hasFlags(Flag.Default) || method.hasFlags(Flag.Static)) {
                                continue;
                            }
                            if (sam != null) {
                                return n;
                            }
                            sam = method;
                        }
                        if (sam == null) {
                            return n;
                        }

                        if (usesThis(getCursor())
                            || shadowsLocalVariable(getCursor())
                            || usedAsStatement(getCursor())
                            || fieldInitializerReferencingUninitializedField(getCursor())) {
                            return n;
                        }

                        //The interface may be parameterized and that is needed to maintain type attribution:
                        JavaType.FullyQualified typedInterface = null;
                        JavaType.FullyQualified anonymousClass = TypeUtils.asFullyQualified(n.getType());
                        if (anonymousClass != null) {
                            typedInterface = anonymousClass.getInterfaces().stream().filter(i -> i.getFullyQualifiedName().equals(type.getFullyQualifiedName())).findFirst().orElse(null);
                        }
                        if (typedInterface == null) {
                            return n;
                        }

                        StringBuilder templateBuilder = new StringBuilder();
                        J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) n.getBody().getStatements().get(0);

                        if (methodDeclaration.getParameters().get(0) instanceof J.Empty) {
                            templateBuilder.append("() -> {");
                        } else {
                            templateBuilder.append(methodDeclaration.getParameters().stream()
                                    .map(param -> ((J.VariableDeclarations) param).getVariables().get(0).getSimpleName())
                                    .collect(Collectors.joining(",", "(", ") -> {")));
                        }

                        JavaType returnType = sam.getReturnType();
                        if (!JavaType.Primitive.Void.equals(returnType)) {
                            templateBuilder.append("return ").append(valueOfType(returnType)).append(';');
                        }
                        templateBuilder.append('}');

                        J.Lambda lambda = n.withTemplate(
                                JavaTemplate.builder(this::getCursor, templateBuilder.toString())
                                        .build(),
                                n.getCoordinates().replace()
                        );
                        lambda = lambda.withType(typedInterface);
                        lambda = (J.Lambda) new UnnecessaryParenthesesVisitor<ExecutionContext>(Checkstyle.unnecessaryParentheses())
                                .visitNonNull(lambda, ctx);

                        J.Block lambdaBody = methodDeclaration.getBody();
                        assert lambdaBody != null;

                        lambda = lambda.withBody(lambdaBody.withPrefix(Space.format(" ")));

                        lambda = (J.Lambda) new LambdaBlockToExpression().getVisitor().visitNonNull(lambda, ctx, getCursor().getParentOrThrow());

                        doAfterVisit(new RemoveUnusedImports());

                        return autoFormat(lambda, ctx);
                    }
                }
                return n;
            }

            private String valueOfType(@Nullable JavaType type) {
                JavaType.Primitive primitive = TypeUtils.asPrimitive(type);
                if (primitive != null) {
                    switch (primitive) {
                        case Boolean:
                            return "true";
                        case Byte:
                        case Char:
                        case Int:
                        case Double:
                        case Float:
                        case Long:
                        case Short:
                            return "0";
                        case String:
                        case Null:
                            return "null";
                        case None:
                        case Void:
                        default:
                            return "";
                    }
                }

                return "null";
            }
        };
    }

    private static boolean usesThis(Cursor cursor) {
        J.NewClass n = cursor.getValue();
        assert n.getBody() != null;
        AtomicBoolean hasThis = new AtomicBoolean(false);
        new JavaVisitor<Integer>() {
            @Override
            public J visitIdentifier(J.Identifier ident, Integer integer) {
                if (ident.getSimpleName().equals("this")) {
                    hasThis.set(true);
                }
                return super.visitIdentifier(ident, integer);
            }
        }.visit(n.getBody(), 0, cursor);
        return hasThis.get();
    }

    private static List<String> parameterNames(J.MethodDeclaration method) {
        return method.getParameters().stream()
                .filter(s -> s instanceof J.VariableDeclarations)
                .map(v -> ((J.VariableDeclarations) v).getVariables().get(0).getSimpleName())
                .collect(Collectors.toList());
    }

    // This does not recursive descend extended classes for inherited fields.
    private static List<String> classFields(J.ClassDeclaration classDeclaration) {
        return classDeclaration.getBody().getStatements().stream()
                .filter(s -> s instanceof J.VariableDeclarations)
                .map(v -> ((J.VariableDeclarations) v).getVariables().get(0).getSimpleName())
                .collect(Collectors.toList());
    }

    private static boolean usedAsStatement(Cursor cursor) {
        Iterator<Object> path = cursor.getParentOrThrow().getPath();
        for (Object last = cursor.getValue(); path.hasNext(); ) {
            Object next = path.next();
            if (next instanceof J.Block) {
                return true;
            } else if (next instanceof J && !(next instanceof J.MethodInvocation)) {
                return false;
            } else if (next instanceof J.MethodInvocation) {
                for (Expression argument : ((J.MethodInvocation) next).getArguments()) {
                    if (argument == last) {
                        return false;
                    }
                }
            }

            if (next instanceof J) {
                last = next;
            }
        }
        return false;
    }

    private static boolean fieldInitializerReferencingUninitializedField(Cursor cursor) {
        J.NewClass n = cursor.getValue();
        assert n.getBody() != null;
        Cursor parent = cursor.dropParentUntil(is -> is instanceof J.VariableDeclarations.NamedVariable || is instanceof SourceFile);
        Object parentValue = parent.getValue();
        if (!(parentValue instanceof J.VariableDeclarations.NamedVariable)) {
            return false;
        }

        J.VariableDeclarations.NamedVariable variable = cursor.firstEnclosing(J.VariableDeclarations.NamedVariable.class);
        if (variable == null || variable.getInitializer() == null) {
            return false;
        }

        parent = cursor.dropParentUntil(is -> is instanceof J.MethodDeclaration || is instanceof J.ClassDeclaration || is instanceof SourceFile);
        parentValue = parent.getValue();
        if (!(parentValue instanceof J.ClassDeclaration) || ((J.ClassDeclaration) parentValue).getType() == null) {
            return false;
        }

        JavaType.FullyQualified owner = ((J.ClassDeclaration) parentValue).getType();
        AtomicBoolean referencesUninitializedFinalField = new AtomicBoolean(false);
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier ident, Integer integer) {
                if (referencesUninitializedFinalField.get()) {
                    return ident;
                }
                if (ident.getFieldType() != null && ident.getFieldType().hasFlags(Flag.Final)
                    && !ident.getFieldType().hasFlags(Flag.HasInit)
                    && owner.equals(ident.getFieldType().getOwner())) {
                    referencesUninitializedFinalField.set(true);
                }
                return super.visitIdentifier(ident, integer);
            }
        }.visit(n.getBody(), 0, cursor);
        return referencesUninitializedFinalField.get();
    }

    // if the contents of the cursor value shadow a local variable in its containing name scope
    private static boolean shadowsLocalVariable(Cursor cursor) {
        J.NewClass n = cursor.getValue();
        assert n.getBody() != null;
        AtomicBoolean hasShadow = new AtomicBoolean(false);

        List<String> localVariables = new ArrayList<>();
        List<J.Block> nameScopeBlocks = new ArrayList<>();
        J nameScope = cursor.dropParentUntil(p -> {
            if (p instanceof J.Block) {
                nameScopeBlocks.add((J.Block) p);
            }
            return p instanceof J.MethodDeclaration || p instanceof J.ClassDeclaration;
        } ).getValue();
        if (nameScope instanceof J.MethodDeclaration) {
            J.MethodDeclaration m = (J.MethodDeclaration) nameScope;
            localVariables.addAll(parameterNames(m));
            J.ClassDeclaration c = cursor.firstEnclosing(J.ClassDeclaration.class);
            assert c != null;
            localVariables.addAll(classFields(c));
        } else {
            J.ClassDeclaration c = (J.ClassDeclaration) nameScope;
            localVariables.addAll(classFields(c));
        }

        new JavaVisitor<List<String>>() {
            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, List<String> variables) {
                variables.add(variable.getSimpleName());
                return variable;
            }

            @Override
            public J visitBlock(J.Block block, List<String> strings) {
                return nameScopeBlocks.contains(block) ? super.visitBlock(block, strings) : block;
            }

            @Override
            public J visitNewClass(J.NewClass newClass, List<String> variables) {
                if (newClass == n) {
                    getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
                }
                return newClass;
            }

            @Nullable
            @Override
            public J visit(@Nullable Tree tree, List<String> variables) {
                if (getCursor().getNearestMessage("stop") != null) {
                    return (J) tree;
                }
                return super.visit(tree, variables);
            }
        }.visit(nameScope, localVariables);

        new JavaVisitor<Integer>() {
            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, Integer integer) {
                if (localVariables.contains(variable.getSimpleName())) {
                    hasShadow.set(true);
                }
                return super.visitVariable(variable, integer);
            }
        }.visit(n.getBody(), 0, cursor);

        return hasShadow.get();
    }
}
