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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Stack;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeType extends Recipe {

    @Option(displayName = "Old fully-qualified type name",
            description = "Fully-qualified class name of the original type.",
            example = "org.junit.Assume")
    String oldFullyQualifiedTypeName;

    @Option(displayName = "New fully-qualified type name",
            description = "Fully-qualified class name of the replacement type, or the name of a primitive such as \"int\". The `OuterClassName$NestedClassName` naming convention should be used for nested classes.",
            example = "org.junit.jupiter.api.Assumptions")
    String newFullyQualifiedTypeName;

    @Option(displayName = "Ignore type definition",
            description = "When set to `true` the definition of the old type will be left untouched. " +
                    "This is useful when you're replacing usage of a class but don't want to rename it.",
            example = "true",
            required = false)
    @Nullable
    Boolean ignoreDefinition;

    @Override
    public String getDisplayName() {
        return "Change type";
    }

    @Override
    public String getDescription() {
        return "Change a given type to another.";
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new ChangeTypeVisitor(newFullyQualifiedTypeName);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                for (J.ClassDeclaration it : cu.getClasses()) {
                    if (!TypeUtils.isOfClassType(it.getType(), oldFullyQualifiedTypeName)) {
                        continue;
                    }
                    if(Boolean.TRUE.equals(ignoreDefinition)) {
                        return cu;
                    } else {
                        return cu.withMarkers(cu.getMarkers().searchResult());
                    }
                }
                doAfterVisit(new UsesType<>(oldFullyQualifiedTypeName));
                return cu;
            }
        };
    }

    private class ChangeTypeVisitor extends JavaVisitor<ExecutionContext> {
        private final JavaType targetType;
        private final JavaType.Class originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);

        private ChangeTypeVisitor(String targetType) {
            this.targetType = JavaType.buildType(targetType);
        }

        @SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions"})
        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {

            J.CompilationUnit c = visitAndCast(cu, ctx, super::visitCompilationUnit);
            c = (J.CompilationUnit) new RemoveImport<>(oldFullyQualifiedTypeName).visit(c, ctx);
            if (originalType.getOwningClass() != null) {
                c = (J.CompilationUnit) new RemoveImport(originalType.getOwningClass().getFullyQualifiedName()).visit(c, ctx);
            }
            JavaType.FullyQualified fullyQualifiedTarget = TypeUtils.asFullyQualified(targetType);
            if (fullyQualifiedTarget != null) {
                if (fullyQualifiedTarget.getOwningClass() != null && !"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                    c = (J.CompilationUnit) new AddImport(fullyQualifiedTarget.getOwningClass().getFullyQualifiedName(), null, true).visit(c, ctx);
                }
                if (!"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                    c = (J.CompilationUnit) new AddImport(fullyQualifiedTarget.getFullyQualifiedName(), null, true).visit(c, ctx);
                }
            }
            if (c != null) {
                c = c.withImports(ListUtils.map(c.getImports(), i -> visitAndCast(i, ctx, super::visitImport)));
            }
            return c;
        }

        @Override
        public J visitImport(J.Import impoort, ExecutionContext executionContext) {
            // visitCompilationUnit() handles changing the imports.
            // If we call super.visitImport() then visitFieldAccess() will change the imports before AddImport/RemoveImport see them.
            // visitFieldAccess() doesn't have the import-specific formatting logic that AddImport/RemoveImport do.
            return impoort;
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
            if (tree instanceof J.MethodDeclaration) {
                J.MethodDeclaration method = (J.MethodDeclaration) tree;
                tree = method.withMethodType(updateType(method.getMethodType()));
            } else if (tree instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) tree;
                tree = method.withMethodType(updateType(method.getMethodType()));
            }

            if (tree instanceof TypedTree) {
                if (tree instanceof J.MethodDeclaration) {
                    J.MethodDeclaration m = (J.MethodDeclaration) tree;
                    return m.withMethodType(updateType(m.getMethodType()));
                } else if (tree instanceof J.MethodInvocation) {
                    J.MethodInvocation m = (J.MethodInvocation) tree;
                    return m.withMethodType(updateType(m.getMethodType()));
                } else if (tree instanceof J.NewClass) {
                    J.NewClass n = (J.NewClass) tree;
                    return n.withConstructorType(updateType(n.getConstructorType()));
                }
                return ((TypedTree) tree).withType(updateType(((TypedTree) tree).getType()));
            }
            return tree;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            if (fieldAccess.isFullyQualifiedClassReference(oldFullyQualifiedTypeName)) {
                if (targetType instanceof JavaType.FullyQualified) {
                    return updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getFullyQualifiedName())
                            .withPrefix(fieldAccess.getPrefix()));
                } else if (targetType instanceof JavaType.Primitive) {
                    return new J.Primitive(
                            fieldAccess.getId(),
                            fieldAccess.getPrefix(),
                            Markers.EMPTY,
                            (JavaType.Primitive) targetType
                    );
                }
            } else {
                StringBuilder maybeClass = new StringBuilder();
                for (Expression target = fieldAccess; target != null; ) {
                    if (target instanceof J.FieldAccess) {
                        J.FieldAccess fa = (J.FieldAccess) target;
                        maybeClass.insert(0, fa.getSimpleName()).insert(0, '.');
                        target = fa.getTarget();
                    } else if (target instanceof J.Identifier) {
                        maybeClass.insert(0, ((J.Identifier) target).getSimpleName());
                        target = null;
                    } else {
                        maybeClass = new StringBuilder("__NOT_IT__");
                        break;
                    }
                }
                JavaType.Class oldType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
                if (maybeClass.toString().equals(oldType.getClassName())) {
                    maybeRemoveImport(oldType.getOwningClass());
                    Expression e = updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                            .withPrefix(fieldAccess.getPrefix()));
                    // If a FieldAccess like Map.Entry has been replaced with an Identifier, ensure that identifier has the correct type
                    if (e instanceof J.Identifier && e.getType() == null) {
                        J.Identifier i = (J.Identifier) e;
                        e = i.withType(targetType);
                    }
                    return e;
                }
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            J.Identifier i = visitAndCast(ident, ctx, super::visitIdentifier);

            if (TypeUtils.isOfClassType(i.getType(), oldFullyQualifiedTypeName)) {
                String className = originalType.getClassName();
                JavaType.FullyQualified iType = TypeUtils.asFullyQualified(i.getType());
                if (iType != null && iType.getOwningClass() != null) {
                    className = originalType.getFullyQualifiedName().substring(iType.getOwningClass().getFullyQualifiedName().length() + 1);
                }

                if (i.getSimpleName().equals(className)) {
                    if (targetType instanceof JavaType.FullyQualified) {
                        if (((JavaType.FullyQualified) targetType).getOwningClass() != null) {
                            return updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                                    .withType(null)
                                    .withPrefix(i.getPrefix()));
                        } else {
                            i = i.withSimpleName(((JavaType.FullyQualified) targetType).getClassName());
                        }
                    } else if (targetType instanceof JavaType.Primitive) {
                        i = i.withSimpleName(((JavaType.Primitive) targetType).getKeyword());
                    }
                }
            }
            return i.withType(updateType(i.getType()));
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static)) {
                if (method.getMethodType().getDeclaringType().isAssignableFrom(originalType)) {
                    J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);

                    for (J.Import anImport : cu.getImports()) {
                        if (anImport.isStatic() && anImport.getQualid().getTarget().getType() != null) {
                            JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                            if (fqn != null && TypeUtils.isOfClassType(fqn, originalType.getFullyQualifiedName()) &&
                                    method.getSimpleName().equals(anImport.getQualid().getSimpleName())) {
                                maybeAddImport(((JavaType.FullyQualified) targetType).getFullyQualifiedName(), method.getName().getSimpleName());
                                break;
                            }
                        }
                    }
                }
            }
            return super.visitMethodInvocation(method, ctx);
        }

        private Expression updateOuterClassTypes(Expression typeTree) {
            if (typeTree instanceof J.FieldAccess) {
                JavaType.FullyQualified type = (JavaType.FullyQualified) targetType;

                if (type.getOwningClass() == null) {
                    // just a performance shortcut when this isn't an inner class
                    typeTree.withType(updateType(targetType));
                }

                Stack<Expression> typeStack = new Stack<>();
                typeStack.push(typeTree);

                Stack<JavaType.FullyQualified> attrStack = new Stack<>();
                attrStack.push(type);

                for (Expression t = ((J.FieldAccess) typeTree).getTarget(); ; ) {
                    typeStack.push(t);
                    if (t instanceof J.FieldAccess) {
                        if (Character.isUpperCase(((J.FieldAccess) t).getSimpleName().charAt(0))) {
                            if (attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        t = ((J.FieldAccess) t).getTarget();
                    } else if (t instanceof J.Identifier) {
                        if (Character.isUpperCase(((J.Identifier) t).getSimpleName().charAt(0))) {
                            if (attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        break;
                    }
                }

                Expression attributed = null;
                for (Expression e = typeStack.pop(); ; e = typeStack.pop()) {
                    if (e instanceof J.Identifier) {
                        if (attrStack.size() == typeStack.size() + 1) {
                            attributed = ((J.Identifier) e).withType(attrStack.pop());
                        } else {
                            attributed = e;
                        }
                    } else if (e instanceof J.FieldAccess) {
                        if (attrStack.size() == typeStack.size() + 1) {
                            attributed = ((J.FieldAccess) e).withTarget(attributed)
                                    .withType(attrStack.pop());
                        } else {
                            attributed = ((J.FieldAccess) e).withTarget(attributed);
                        }
                    }
                    if (typeStack.isEmpty()) {
                        break;
                    }
                }

                assert attributed != null;
                return attributed;
            }
            return typeTree;
        }

        private JavaType updateType(@Nullable JavaType type) {
            JavaType.GenericTypeVariable gtv = TypeUtils.asGeneric(type);
            if (gtv != null) {
                return gtv.withBounds(ListUtils.map(gtv.getBounds(), bound -> {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(bound);
                    if (fq != null) {
                        if (fq.getFullyQualifiedName().equals(oldFullyQualifiedTypeName) && targetType instanceof JavaType.FullyQualified) {
                            return targetType;
                        }
                    }
                    return bound;
                }));
            }

            JavaType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if (fqt != null && fqt.getFullyQualifiedName().equals(oldFullyQualifiedTypeName)) {
                return targetType;
            }

            //noinspection ConstantConditions
            return type;
        }

        @Nullable
        private JavaType.Method updateType(@Nullable JavaType.Method mt) {
            if (mt != null) {
                return mt.withDeclaringType((JavaType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withReturnType(updateType(mt.getReturnType()))
                        .withParameterTypes(ListUtils.map(mt.getParameterTypes(), this::updateType));
            }
            return null;
        }
    }
}
