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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

/**
 * NOTE: Does not currently transform all possible type references, and accomplishing this would be non-trivial.
 * For example, a method invocation select might refer to field `A a` whose type has now changed to `A2`, and so the type
 * on the select should change as well. But how do we identify the set of all method selects which refer to `a`? Suppose
 * it were prefixed like `this.a`, or `MyClass.this.a`, or indirectly via a separate method call like `getA()` where `getA()`
 * is defined on the super class.
 */
public class ChangeType extends Recipe {
    private String type;
    private JavaType.Class targetType;

    public ChangeType() {
        this.processor = () -> new ChangeTypeProcessor(type, targetType);
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTargetType(String targetType) {
        this.targetType = JavaType.Class.build(targetType);
    }

    public void setTargetType(JavaType.Class targetType) {
        this.targetType = targetType;
    }

    @Override
    public Validated validate() {
        return required("type", type)
                .and(required("target.type", targetType.getFullyQualifiedName()));
    }

    private static class ChangeTypeProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final String type;
        private final JavaType.Class targetType;

        private ChangeTypeProcessor(String type, JavaType.Class targetType) {
            this.type = type;
            this.targetType = targetType;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            maybeAddImport(targetType);
            maybeRemoveImport(type);
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
            JavaType.Class oldTypeAsClass = TypeUtils.asClass(name.getType());
            N n = super.visitTypeName(name, ctx);
            if (!(name instanceof TypeTree) && oldTypeAsClass != null && oldTypeAsClass.getFullyQualifiedName().equals(type)) {
                n = n.withType(targetType);
            }
            return n;
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = super.visitAnnotation(annotation, ctx);
            return a.withAnnotationType(transformName(a.getAnnotationType()));
        }

        @Override
        public J.ArrayType visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
            J.ArrayType a = super.visitArrayType(arrayType, ctx);
            return a.withElementType(transformName(a.getElementType()));
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, ExecutionContext ctx) {
            J.ClassDecl c = super.visitClassDecl(classDecl, ctx);

            if (c.getExtends() != null) {
                c = c.withExtends(c.getExtends().withElem(transformName(c.getExtends().getElem())));
            }

            if (c.getImplements() != null) {
                c = c.withImplements(c.getImplements().map(this::transformName));
            }

            return c;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);
            if (f.isFullyQualifiedClassReference(type)) {
                return TypeTree.build(targetType.getFullyQualifiedName())
                        .withPrefix(f.getPrefix());
            }
            return f;
        }

        @Override
        public J.Ident visitIdentifier(J.Ident ident, ExecutionContext ctx) {
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            J.Ident i = super.visitIdentifier(ident, ctx);
            JavaType.Class originalType = JavaType.Class.build(type);

            if (TypeUtils.isOfClassType(i.getType(), type) && i.getSimpleName().equals(originalType.getClassName())) {
                i = i.withName(targetType.getClassName());
                i = i.withType(targetType);
            }

            return i;
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl method, ExecutionContext ctx) {
            J.MethodDecl m = super.visitMethod(method, ctx);
            m = m.withReturnTypeExpr(transformName(m.getReturnTypeExpr()));
            return m.withThrows(m.getThrows() == null ? null : m.getThrows().map(this::transformName));
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            if (m.getSelect() instanceof NameTree && m.getType() != null && m.getType().hasFlags(Flag.Static)) {
                m = m.withSelect(m.getSelect().map(this::transformName));
            }

            if (m.getSelect() != null) {
                JavaType.Class selectType = TypeUtils.asClass(m.getSelect().getElem().getType());
                if (selectType != null && selectType.getFullyQualifiedName().equals(type)) {
                    m = m.withSelect(m.getSelect().map(s -> s.withType(targetType)));
                }
            }

            if (m.getType() != null) {
                if (m.getType().getDeclaringType().getFullyQualifiedName().equals(type)) {
                    m = m.withDeclaringType(targetType);
                }
            }

            return m;
        }

        @Override
        public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, ExecutionContext ctx) {
            J.MultiCatch m = super.visitMultiCatch(multiCatch, ctx);
            return m.withAlternatives(ListUtils.map(m.getAlternatives(), a -> a.map(this::transformName)));
        }

        @Override
        public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable, ExecutionContext ctx) {
            J.VariableDecls m = super.visitMultiVariable(multiVariable, ctx);
            if (!(multiVariable.getTypeExpr() instanceof J.MultiCatch)) {
                m = m.withTypeExpr(transformName(m.getTypeExpr()));
            }
            return m;
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, ExecutionContext ctx) {
            J.VariableDecls.NamedVar v = super.visitVariable(variable, ctx);

            JavaType.Class varType = TypeUtils.asClass(variable.getType());
            if (varType != null && varType.getFullyQualifiedName().equals(type)) {
                v = v.withType(targetType).withName(v.getName().withType(targetType));
            }

            return v;
        }

        @Override
        public J.NewArray visitNewArray(J.NewArray newArray, ExecutionContext ctx) {
            J.NewArray n = super.visitNewArray(newArray, ctx);
            return n.withTypeExpr(transformName(n.getTypeExpr()));
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            return n.withClazz(transformName(n.getClazz()));
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
            J.TypeCast t = super.visitTypeCast(typeCast, ctx);
            return t.withClazz(t.getClazz().withTree(t.getClazz().getTree().map(this::transformName)));
        }

        @Override
        public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, ExecutionContext ctx) {
            J.TypeParameter t = super.visitTypeParameter(typeParam, ctx);
            t = t.withBounds(t.getBounds() == null ? null : t.getBounds().map(this::transformName));
            return t.withName(transformName(t.getName()));
        }

        @Override
        public J.Wildcard visitWildcard(J.Wildcard wildcard, ExecutionContext ctx) {
            J.Wildcard w = super.visitWildcard(wildcard, ctx);
            return w.withBoundedType(transformName(w.getBoundedType()));
        }

        @SuppressWarnings("unchecked")
        private <T extends J> T transformName(@Nullable T nameField) {
            if (nameField instanceof NameTree) {
                JavaType.Class nameTreeClass = TypeUtils.asClass(((NameTree) nameField).getType());
                if (nameTreeClass != null && nameTreeClass.getFullyQualifiedName().equals(type)) {
                    return (T) J.Ident.build(randomId(),
                            nameField.getPrefix(),
                            Markers.EMPTY,
                            targetType.getClassName(),
                            targetType
                    );
                }
            }
            return nameField;
        }
    }
}
