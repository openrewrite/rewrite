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
        return new UsesType<>(oldFullyQualifiedTypeName);
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
                maybeAddImport((JavaType.FullyQualified) targetType);
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
            J.FieldAccess f = visitAndCast(fieldAccess, ctx, super::visitFieldAccess);
            if (f.isFullyQualifiedClassReference(oldFullyQualifiedTypeName)) {
                if (targetType instanceof JavaType.FullyQualified) {
                    return TypeTree.build(((JavaType.FullyQualified) targetType).getFullyQualifiedName())
                            .withPrefix(f.getPrefix());
                } else if (targetType instanceof JavaType.Primitive) {
                    return new J.Primitive(
                            f.getId(),
                            f.getPrefix(),
                            Markers.EMPTY,
                            (JavaType.Primitive) targetType
                    );
                }
            }
            return f;
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            J.Identifier i = visitAndCast(ident, ctx, super::visitIdentifier);

            if (TypeUtils.isOfClassType(i.getType(), oldFullyQualifiedTypeName) && i.getSimpleName().equals(originalType.getClassName())) {
                if (targetType instanceof JavaType.FullyQualified) {
                    i = i.withName(((JavaType.FullyQualified) targetType).getClassName());
                } else if (targetType instanceof JavaType.Primitive) {
                    i = i.withName(((JavaType.Primitive) targetType).getKeyword());
                }
            }
            return i.withType(updateType(i.getType()));
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
            m = m.withSelect(updateType(m.getSelect()))
                    .withType(updateType(method.getType()));

            if(m != method && isStatic && targetType instanceof JavaType.FullyQualified) {
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

        // updateType/updateSignature are always used to swap things in-place
        // The true nullability is that the return has the same nullability as the input
        // Because it's always an in-place operation it isn't problematic to tell a white lie about the nullability of the return value
        private Expression updateType(@Nullable Expression e) {
            if(e == null) {
                //noinspection ConstantConditions
                return null;
            }
            return e.withType(updateType(e.getType()));
        }

        private JavaType updateType(@Nullable JavaType type) {
            JavaType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if(fqt != null && fqt.getFullyQualifiedName().equals(oldFullyQualifiedTypeName)) {
                return targetType;
            }
            JavaType.GenericTypeVariable gtv = TypeUtils.asGeneric(type);
            if(gtv != null && gtv.getBound() != null
                    && gtv.getBound().getFullyQualifiedName().equals(oldFullyQualifiedTypeName)
                    && targetType instanceof JavaType.FullyQualified) {
                return gtv.withBound((JavaType.FullyQualified) targetType);
            }
            JavaType.Method mt = TypeUtils.asMethod(type);
            if(mt != null) {
                return mt.withDeclaringType((JavaType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withResolvedSignature(updateSignature(mt.getResolvedSignature()))
                        .withGenericSignature(updateSignature(mt.getGenericSignature()));
            }

            //noinspection ConstantConditions
            return type;
        }

        private JavaType.Method.Signature updateSignature(@Nullable JavaType.Method.Signature signature) {
            if(signature == null) {
                //noinspection ConstantConditions
                return signature;
            }

            return signature.withReturnType(updateType(signature.getReturnType()))
                    .withParamTypes(ListUtils.map(signature.getParamTypes(), this::updateType));
        }
    }
}
