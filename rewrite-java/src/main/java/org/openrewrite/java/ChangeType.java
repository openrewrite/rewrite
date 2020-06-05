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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

/**
 * NOTE: Does not currently transform all possible type references, and accomplishing this would be non-trivial.
 * For example, a method invocation select might refer to field `A a` whose type has now changed to `A2`, and so the type
 * on the select should change as well. But how do we identify the set of all method selects which refer to `a`? Suppose
 * it were prefixed like `this.a`, or `MyClass.this.a`, or indirectly via a separate method call like `getA()` where `getA()`
 * is defined on the super class.
 */
public class ChangeType extends JavaRefactorVisitor {
    private String type;
    private JavaType.Class targetType;

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

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("type", type, "target.type", targetType.getFullyQualifiedName());
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu) {
        maybeAddImport(targetType);
        maybeRemoveImport(type);
        return super.visitCompilationUnit(cu);
    }

    @Override
    public J visitTypeName(NameTree name) {
        JavaType.Class oldTypeAsClass = TypeUtils.asClass(name.getType());
        NameTree n = refactor(name, super::visitTypeName);
        if (!(name instanceof TypeTree) && oldTypeAsClass != null && oldTypeAsClass.getFullyQualifiedName().equals(type)) {
            n = n.withType(targetType);
        }
        return n;
    }

    @Override
    public J visitAnnotation(J.Annotation annotation) {
        J.Annotation a = refactor(annotation, super::visitAnnotation);
        return a.withAnnotationType(transformName(a.getAnnotationType()));
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType) {
        J.ArrayType a = refactor(arrayType, super::visitArrayType);
        return a.withElementType(transformName(a.getElementType()));
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl) {
        J.ClassDecl c = refactor(classDecl, super::visitClassDecl);

        if(c.getExtends() != null) {
            c = c.withExtends(c.getExtends().withFrom(transformName(c.getExtends().getFrom())));
        }

        if(c.getImplements() != null) {
            c = c.withImplements(c.getImplements().withFrom(transformNames(c.getImplements().getFrom())));
        }

        return c;
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess) {
        J.FieldAccess f = refactor(fieldAccess, super::visitFieldAccess);
        return f.withTarget(transformName(f.getTarget()));
    }

    @Override
    public J visitMethod(J.MethodDecl method) {
        J.MethodDecl m = refactor(method, super::visitMethod);
        m = m.withReturnTypeExpr(transformName(m.getReturnTypeExpr()));
        return m.withThrows(m.getThrows() == null ? null :
                        m.getThrows().withExceptions(transformNames(m.getThrows().getExceptions())));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        J.MethodInvocation m = refactor(method, super::visitMethodInvocation);

        if (method.getSelect() instanceof NameTree && m.getType() != null && m.getType().hasFlags(Flag.Static)) {
            m = m.withSelect(transformName(m.getSelect()));
        }

        if (m.getSelect() != null) {
            JavaType.Class selectType = TypeUtils.asClass(m.getSelect().getType());
            if (selectType != null && selectType.getFullyQualifiedName().equals(type)) {
                m = m.withSelect(m.getSelect().withType(targetType));
            }
        }

        return m;
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch) {
        J.MultiCatch m = refactor(multiCatch, super::visitMultiCatch);
        return m.withAlternatives(transformNames(m.getAlternatives()));
    }

    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable) {
        if (multiVariable.getTypeExpr() instanceof J.MultiCatch) {
            return super.visitMultiVariable(multiVariable);
        }

        J.VariableDecls m = refactor(multiVariable, super::visitMultiVariable);
        return m.withTypeExpr(transformName(m.getTypeExpr()));
    }

    @Override
    public J visitVariable(J.VariableDecls.NamedVar variable) {
        J.VariableDecls.NamedVar v = refactor(variable, super::visitVariable);

        JavaType.Class varType = TypeUtils.asClass(variable.getType());
        if (varType != null && varType.getFullyQualifiedName().equals(type)) {
            v = v.withType(targetType).withName(v.getName().withType(targetType));
        }

        return v;
    }

    @Override
    public J visitNewArray(J.NewArray newArray) {
        J.NewArray n = refactor(newArray, super::visitNewArray);
        return n.withTypeExpr(transformName(n.getTypeExpr()));
    }

    @Override
    public J visitNewClass(J.NewClass newClass) {
        J.NewClass n = refactor(newClass, super::visitNewClass);
        return n.withClazz(transformName(n.getClazz()));
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast) {
        J.TypeCast t = refactor(typeCast, super::visitTypeCast);
        return t.withClazz(t.getClazz().withTree(transformName(t.getClazz().getTree())));
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam) {
        J.TypeParameter t = refactor(typeParam, super::visitTypeParameter);
        t = t.withBounds(t.getBounds() == null ? null :
                t.getBounds().withTypes(transformNames(t.getBounds().getTypes())));
        return t.withName(transformName(t.getName()));
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard) {
        J.Wildcard w = refactor(wildcard, super::visitWildcard);
        return w.withBoundedType(transformName(w.getBoundedType()));
    }

    @SuppressWarnings("unchecked")
    private <T extends Tree> T transformName(@Nullable T nameField) {
        if (nameField instanceof NameTree) {
            JavaType.Class nameTreeClass = TypeUtils.asClass(((NameTree) nameField).getType());
            if (nameTreeClass != null && nameTreeClass.getFullyQualifiedName().equals(type)) {
                return (T) J.Ident.build(randomId(), targetType.getClassName(), targetType, nameField.getFormatting());
            }
        }
        return nameField;
    }

    @SuppressWarnings("unchecked")
    private <T extends Tree> List<T> transformNames(@Nullable List<T> trees) {
        if (trees == null) {
            return null;
        }

        boolean atLeastOneChanged = false;
        List<T> transformed = new ArrayList<>();
        for (T tree : trees) {
            if (tree instanceof NameTree) {
                JavaType.Class nodeTypeAsClass = TypeUtils.asClass(((NameTree) tree).getType());
                if (nodeTypeAsClass != null && nodeTypeAsClass.getFullyQualifiedName().equals(type)) {
                    atLeastOneChanged = true;
                    transformed.add((T) J.Ident.build(randomId(), targetType.getClassName(), targetType, tree.getFormatting()));
                    continue;
                }
            }
            transformed.add(tree);
        }

        return atLeastOneChanged ? transformed : trees;
    }
}
