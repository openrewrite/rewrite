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
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.Stack;

import static org.openrewrite.Tree.randomId;

/**
 * NOTE: Does not currently transform all possible type references, and accomplishing this would be non-trivial.
 * For example, a method invocation select might refer to field `A a` whose type has now changed to `A2`, and so the type
 * on the select should change as well. But how do we identify the set of all method selects which refer to `a`? Suppose
 * it were prefixed like `this.a`, or `MyClass.this.a`, or indirectly via a separate method call like `getA()` where `getA()`
 * is defined on the super class.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeType extends Recipe {
    @SuppressWarnings("ConstantConditions")
    private static final Marker FOUND_TYPE = new JavaSearchResult(randomId(), null, null);

    /**
     * Fully-qualified class name of the original type.
     */
    @Option(displayName = "Old fully-qualified type name",
            description = "Fully-qualified class name of the original type.",
            example = "org.junit.Assume")
    String oldFullyQualifiedTypeName;

    /**
     * Fully-qualified class name of the replacement type, the replacement type can also defined as a primitive.
     */
    @Option(displayName = "New fully-qualified type name",
            description = "Fully-qualified class name of the replacement type, or the name of a primitive such as \"int\".",
            example = "org.junit.jupiter.api.Assumptions")
    String newFullyQualifiedTypeName;

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
                    if (TypeUtils.isOfClassType(it.getType(), oldFullyQualifiedTypeName)) {
                        return cu.withMarkers(cu.getMarkers().addIfAbsent(FOUND_TYPE));
                    }
                }
                doAfterVisit(new UsesType<>(oldFullyQualifiedTypeName));
                return cu;
            }
        };
    }

    private class ChangeTypeVisitor extends JavaVisitor<ExecutionContext> {
        private final JavaType targetType;
        private final JavaType.Class originalType = JavaType.Class.build(oldFullyQualifiedTypeName);

        private ChangeTypeVisitor(String targetType) {
            this.targetType = JavaType.buildType(targetType);
        }

        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            maybeRemoveImport(oldFullyQualifiedTypeName);
            if (targetType instanceof JavaType.FullyQualified) {
                if (((JavaType.FullyQualified) targetType).getOwningClass() != null) {
                    maybeAddImport(((JavaType.FullyQualified) targetType).getOwningClass());
                } else {
                    maybeAddImport((JavaType.FullyQualified) targetType);
                }
            }
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J visitImport(J.Import impoort, ExecutionContext executionContext) {
            // visitCompilationUnit() handles changing the imports.
            // If we call super.visitImport() then visitFieldAccess() will change the imports before AddImport/RemoveImport see them.
            // visitFieldAccess() doesn't have the import-specific formatting logic that AddImport/RemoveImport do.
            return impoort;
        }

        @Override
        public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
            N n = visitAndCast(name, ctx, super::visitTypeName);
            return n.withType(updateType(n.getType()));
        }

        @Override
        public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = visitAndCast(annotation, ctx, super::visitAnnotation);
            return a.withAnnotationType(transformName(a.getAnnotationType()));
        }

        @Override
        public J visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
            J.ArrayType a = visitAndCast(arrayType, ctx, super::visitArrayType);
            return a.withElementType(transformName(a.getElementType()));
        }

        @Override
        public J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            J.Assignment a = visitAndCast(assignment, ctx, super::visitAssignment);
            return updateType(a);
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = visitAndCast(classDecl, ctx, super::visitClassDeclaration);

            if (c.getExtends() != null) {
                c = c.withExtends(transformName(c.getExtends()));
            }

            if (c.getImplements() != null) {
                c = c.withImplements(ListUtils.map(c.getImplements(), this::transformName));
            }

            return c;
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
                JavaType.Class oldType = JavaType.Class.build(oldFullyQualifiedTypeName);
                if (maybeClass.toString().equals(oldType.getClassName())) {
                    maybeRemoveImport(oldType.getOwningClass());
                    Expression e = updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                            .withPrefix(fieldAccess.getPrefix()));
                    // If a FieldAccess like Map.Entry has been replaced with an Identifier, ensure that identifier has the correct type
                    if(e instanceof J.Identifier && e.getType() == null) {
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
                            i = i.withName(((JavaType.FullyQualified) targetType).getClassName());
                        }
                    } else if (targetType instanceof JavaType.Primitive) {
                        i = i.withName(((JavaType.Primitive) targetType).getKeyword());
                    }
                }
            }
            return i.withType(updateType(i.getType()));
        }

        @Override
        public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
            J.Lambda l = visitAndCast(lambda, ctx, super::visitLambda);
            return l.withType(updateType(l.getType()));
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration m = visitAndCast(method, ctx, super::visitMethodDeclaration);
            m = m.withReturnTypeExpression(transformName(m.getReturnTypeExpression()));
            return m.withThrows(m.getThrows() == null ? null : ListUtils.map(m.getThrows(), this::transformName));
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = method;
            boolean isStatic = m.getType() != null && m.getType().hasFlags(Flag.Static);
            if (m.getSelect() instanceof NameTree && isStatic) {
                m = m.withSelect(transformName(m.getSelect()));
            }

            Expression select = updateType(m.getSelect());
            m = m.withSelect(select).withType(updateType(method.getType()));

            if (m != method && isStatic && targetType instanceof JavaType.FullyQualified) {
                maybeAddImport(((JavaType.FullyQualified) targetType).getFullyQualifiedName(), m.getName().getSimpleName());
            }
            return super.visitMethodInvocation(m, ctx);
        }

        @Override
        public J visitMultiCatch(J.MultiCatch multiCatch, ExecutionContext ctx) {
            J.MultiCatch m = visitAndCast(multiCatch, ctx, super::visitMultiCatch);
            return m.withAlternatives(ListUtils.map(m.getAlternatives(), this::transformName));
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations m = visitAndCast(multiVariable, ctx, super::visitVariableDeclarations);
            if (!(m.getTypeExpression() instanceof J.MultiCatch)) {
                m = m.withTypeExpression(transformName(m.getTypeExpression()));
            }
            return m;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = visitAndCast(variable, ctx, super::visitVariable);
            return v.withType(updateType(v.getType()))
                    .withName(v.getName().withType(updateType(v.getName().getType())));
        }

        @Override
        public J visitNewArray(J.NewArray newArray, ExecutionContext ctx) {
            J.NewArray n = visitAndCast(newArray, ctx, super::visitNewArray);
            return n.withTypeExpression(transformName(n.getTypeExpression()));
        }

        @Override
        public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = visitAndCast(newClass, ctx, super::visitNewClass);
            return n.withClazz(transformName(n.getClazz()))
                    .withType(updateType(n.getType()));
        }

        @Override
        public J visitTernary(J.Ternary ternary, ExecutionContext ctx) {
            J.Ternary t = visitAndCast(ternary, ctx, super::visitTernary);
            return updateType(t);
        }

        @Override
        public J visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
            J.TypeCast t = visitAndCast(typeCast, ctx, super::visitTypeCast);
            return t.withClazz(t.getClazz().withTree(transformName(t.getClazz().getTree())));
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParam, ExecutionContext ctx) {
            J.TypeParameter t = visitAndCast(typeParam, ctx, super::visitTypeParameter);
            t = t.withBounds(t.getBounds() == null ? null : ListUtils.map(t.getBounds(), this::transformName));
            return t.withName(transformName(t.getName()));
        }

        @Override
        public J visitWildcard(J.Wildcard wildcard, ExecutionContext ctx) {
            J.Wildcard w = visitAndCast(wildcard, ctx, super::visitWildcard);
            return w.withBoundedType(transformName(w.getBoundedType()));
        }

        @SuppressWarnings({"unchecked", "ConstantConditions"})
        private <T extends J> T transformName(@Nullable T nameField) {
            if (nameField instanceof NameTree) {
                JavaType.FullyQualified nameTreeClass = TypeUtils.asFullyQualified(((NameTree) nameField).getType());
                String name;
                if (targetType instanceof JavaType.FullyQualified) {
                    name = ((JavaType.FullyQualified) targetType).getClassName();
                } else {
                    name = ((JavaType.Primitive) targetType).getKeyword();
                }
                if (nameTreeClass != null && nameTreeClass.getFullyQualifiedName().equals(oldFullyQualifiedTypeName)) {
                    return (T) J.Identifier.build(randomId(),
                            nameField.getPrefix(),
                            nameField.getMarkers(),
                            name,
                            targetType
                    );
                }
            }
            return nameField;
        }

        private Expression updateOuterClassTypes(Expression typeTree) {
            if (typeTree instanceof J.FieldAccess) {
                JavaType.FullyQualified type = (JavaType.FullyQualified) targetType;

                if(type.getOwningClass() == null) {
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
                            if(attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        t = ((J.FieldAccess) t).getTarget();
                    } else if (t instanceof J.Identifier) {
                        if (Character.isUpperCase(((J.Identifier) t).getSimpleName().charAt(0))) {
                            if(attrStack.peek().getOwningClass() != null) {
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

        private Expression updateType(@Nullable Expression typeTree) {
            if (typeTree == null) {
                // updateType/updateSignature are always used to swap things in-place
                // The true nullability is that the return has the same nullability as the input
                // Because it's always an in-place operation it isn't problematic to tell a white lie about the nullability of the return value

                //noinspection ConstantConditions
                return null;
            }

            return typeTree.withType(updateType(typeTree.getType()));
        }

        private JavaType updateType(@Nullable JavaType type) {
            JavaType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if (fqt != null && fqt.getFullyQualifiedName().equals(oldFullyQualifiedTypeName)) {
                return targetType;
            }
            JavaType.GenericTypeVariable gtv = TypeUtils.asGeneric(type);
            if (gtv != null && gtv.getBound() != null
                    && gtv.getBound().getFullyQualifiedName().equals(oldFullyQualifiedTypeName)
                    && targetType instanceof JavaType.FullyQualified) {
                return gtv.withBound((JavaType.FullyQualified) targetType);
            }
            JavaType.Method mt = TypeUtils.asMethod(type);
            if (mt != null) {
                return mt.withDeclaringType((JavaType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withResolvedSignature(updateSignature(mt.getResolvedSignature()))
                        .withGenericSignature(updateSignature(mt.getGenericSignature()));
            }

            //noinspection ConstantConditions
            return type;
        }

        private JavaType.Method.Signature updateSignature(@Nullable JavaType.Method.Signature signature) {
            if (signature == null) {
                //noinspection ConstantConditions
                return signature;
            }

            return signature.withReturnType(updateType(signature.getReturnType()))
                    .withParamTypes(ListUtils.map(signature.getParamTypes(), this::updateType));
        }
    }
}
