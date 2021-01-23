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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
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
@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeType extends Recipe {

    @NonNull
    private final String originalType;

    @NonNull
    private final String replacementType;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new ChangeTypeProcessor(replacementType);
    }


    private class ChangeTypeProcessor extends JavaProcessor<ExecutionContext> {
        private JavaType targetType;

        private ChangeTypeProcessor(String targetType) {
            this.targetType = JavaType.Primitive.fromKeyword(targetType);
            if (this.targetType == null) {
                this.targetType = JavaType.Class.build(targetType);
            }
        }

        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            if (targetType instanceof JavaType.FullyQualified) {
                maybeAddImport((JavaType.FullyQualified) targetType);
            }
            maybeRemoveImport(originalType);
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
            JavaType.Class oldTypeAsClass = TypeUtils.asClass(name.getType());
            N n = call(name, ctx, super::visitTypeName);
            if (!(name instanceof TypeTree) && oldTypeAsClass != null && oldTypeAsClass.getFullyQualifiedName().equals(originalType)) {
                n = n.withType(targetType);
            }
            return n;
        }

        @Override
        public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = call(annotation, ctx, super::visitAnnotation);
            return a.withAnnotationType(transformName(a.getAnnotationType()));
        }

        @Override
        public J visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
            J.ArrayType a = call(arrayType, ctx, super::visitArrayType);
            return a.withElementType(transformName(a.getElementType()));
        }

        @Override
        public J visitClassDecl(J.ClassDecl classDecl, ExecutionContext ctx) {
            J.ClassDecl c = call(classDecl, ctx, super::visitClassDecl);

            if (c.getExtends() != null) {
                c = c.withExtends(c.getExtends().map(this::transformName));
            }

            if (c.getImplements() != null) {
                c = c.withImplements(c.getImplements().map(this::transformName));
            }

            return c;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess f = call(fieldAccess, ctx, super::visitFieldAccess);
            if (f.isFullyQualifiedClassReference(originalType)) {
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
        public J visitIdentifier(J.Ident ident, ExecutionContext ctx) {
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            J.Ident i = call(ident, ctx, super::visitIdentifier);
            JavaType.Class originalType = JavaType.Class.build(ChangeType.this.originalType);

            if (TypeUtils.isOfClassType(i.getType(), ChangeType.this.originalType) && i.getSimpleName().equals(originalType.getClassName())) {
                if (targetType instanceof JavaType.FullyQualified) {
                    i = i.withName(((JavaType.FullyQualified) targetType).getClassName());
                } else if (targetType instanceof JavaType.Primitive) {
                    i = i.withName(((JavaType.Primitive) targetType).getKeyword());
                }
                i = i.withType(targetType);
            }

            return i;
        }

        @Override
        public J visitMethod(J.MethodDecl method, ExecutionContext ctx) {
            J.MethodDecl m = call(method, ctx, super::visitMethod);
            m = m.withReturnTypeExpr(transformName(m.getReturnTypeExpr()));
            return m.withThrows(m.getThrows() == null ? null : m.getThrows().map(this::transformName));
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = call(method, ctx, super::visitMethodInvocation);

            if (m.getSelect() instanceof NameTree && m.getType() != null && m.getType().hasFlags(Flag.Static)) {
                m = m.withSelect(m.getSelect().map(this::transformName));
            }

            if (m.getSelect() != null) {
                JavaType.Class selectType = TypeUtils.asClass(m.getSelect().getElem().getType());
                if (selectType != null && selectType.getFullyQualifiedName().equals(originalType)) {
                    m = m.withSelect(m.getSelect().map(s -> s.withType(targetType)));
                }
            }

            if (m.getType() != null) {
                if (m.getType().getDeclaringType().getFullyQualifiedName().equals(originalType) &&
                        targetType instanceof JavaType.FullyQualified) {
                    m = m.withDeclaringType((JavaType.FullyQualified) targetType);
                }
            }

            return m;
        }

        @Override
        public J visitMultiCatch(J.MultiCatch multiCatch, ExecutionContext ctx) {
            J.MultiCatch m = call(multiCatch, ctx, super::visitMultiCatch);
            return m.withAlternatives(ListUtils.map(m.getAlternatives(), a -> a.map(this::transformName)));
        }

        @Override
        public J visitMultiVariable(J.VariableDecls multiVariable, ExecutionContext ctx) {
            J.VariableDecls m = call(multiVariable, ctx, super::visitMultiVariable);
            if (!(multiVariable.getTypeExpr() instanceof J.MultiCatch)) {
                m = m.withTypeExpr(transformName(m.getTypeExpr()));
            }
            return m;
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, ExecutionContext ctx) {
            J.VariableDecls.NamedVar v = call(variable, ctx, super::visitVariable);

            JavaType.Class varType = TypeUtils.asClass(variable.getType());
            if (varType != null && varType.getFullyQualifiedName().equals(originalType)) {
                v = v.withType(targetType).withName(v.getName().withType(targetType));
            }

            return v;
        }

        @Override
        public J visitNewArray(J.NewArray newArray, ExecutionContext ctx) {
            J.NewArray n = call(newArray, ctx, super::visitNewArray);
            return n.withTypeExpr(transformName(n.getTypeExpr()));
        }

        @Override
        public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = call(newClass, ctx, super::visitNewClass);
            return n.withClazz(transformName(n.getClazz()));
        }

        @Override
        public J visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
            J.TypeCast t = call(typeCast, ctx, super::visitTypeCast);
            return t.withClazz(t.getClazz().withTree(t.getClazz().getTree().map(this::transformName)));
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParam, ExecutionContext ctx) {
            J.TypeParameter t = call(typeParam, ctx, super::visitTypeParameter);
            t = t.withBounds(t.getBounds() == null ? null : t.getBounds().map(this::transformName));
            return t.withName(transformName(t.getName()));
        }

        @Override
        public J visitWildcard(J.Wildcard wildcard, ExecutionContext ctx) {
            J.Wildcard w = call(wildcard, ctx, super::visitWildcard);
            return w.withBoundedType(transformName(w.getBoundedType()));
        }

        @Nullable
        @SuppressWarnings("unchecked")
        private <T extends J> T transformName(@Nullable T nameField) {
            if (nameField instanceof NameTree) {
                JavaType.Class nameTreeClass = TypeUtils.asClass(((NameTree) nameField).getType());
                String name;
                if (targetType instanceof JavaType.FullyQualified) {
                    name = ((JavaType.FullyQualified) targetType).getClassName();
                } else {
                    name = ((JavaType.Primitive) targetType).getKeyword();
                }
                if (nameTreeClass != null && nameTreeClass.getFullyQualifiedName().equals(originalType)) {
                    return (T) J.Ident.build(randomId(),
                            nameField.getPrefix(),
                            Markers.EMPTY,
                            name,
                            targetType
                    );
                }
            }
            return nameField;
        }
    }
}
