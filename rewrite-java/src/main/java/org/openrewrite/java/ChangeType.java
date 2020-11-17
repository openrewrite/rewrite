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
public class ChangeType extends JavaIsoRefactorVisitor {
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
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
        maybeAddImport(targetType);
        maybeRemoveImport(type);
        return super.visitCompilationUnit(cu);
    }

    @Override
    public NameTree visitTypeName(NameTree name) {
        JavaType.Class oldTypeAsClass = TypeUtils.asClass(name.getType());
        NameTree n = super.visitTypeName(name);
        if (!(name instanceof TypeTree) && oldTypeAsClass != null && oldTypeAsClass.getFullyQualifiedName().equals(type)) {
            n = n.withType(targetType);
        }
        return n;
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation) {
        J.Annotation a = super.visitAnnotation(annotation);
        return a.withAnnotationType(transformName(a.getAnnotationType()));
    }

    @Override
    public J.ArrayType visitArrayType(J.ArrayType arrayType) {
        J.ArrayType a = super.visitArrayType(arrayType);
        return a.withElementType(transformName(a.getElementType()));
    }

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
        J.ClassDecl c = super.visitClassDecl(classDecl);

        if (c.getExtends() != null) {
            c = c.withExtends(c.getExtends().withFrom(transformName(c.getExtends().getFrom())));
        }

        if (c.getImplements() != null) {
            c = c.withImplements(c.getImplements().withFrom(transformNames(c.getImplements().getFrom())));
        }

        return c;
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess) {
        J.FieldAccess f = super.visitFieldAccess(fieldAccess);
        if (f.isFullyQualifiedClassReference(type)) {
            return TreeBuilder.buildName(targetType.getFullyQualifiedName(), f.getFormatting(), f.getId());
        }
        return f;
    }

    @Override
    public J.Ident visitIdentifier(J.Ident ident) {
        // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
        // Then transform it, otherwise leave it alone
        J.Ident i = super.visitIdentifier(ident);
        JavaType.Class originalType = JavaType.Class.build(type);

        if (TypeUtils.isOfClassType(i.getType(), type) && i.getSimpleName().equals(originalType.getClassName())) {
            i = i.withName(targetType.getClassName());
            i = i.withType(targetType);
        }

        return i;
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method) {
        J.MethodDecl m = super.visitMethod(method);
        m = m.withReturnTypeExpr(transformName(m.getReturnTypeExpr()));
        return m.withThrows(m.getThrows() == null ? null :
                m.getThrows().withExceptions(transformNames(m.getThrows().getExceptions())));
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        J.MethodInvocation m = super.visitMethodInvocation(method);

        if (m.getSelect() instanceof NameTree && m.getType() != null && m.getType().hasFlags(Flag.Static)) {
            m = m.withSelect(transformName(m.getSelect()));
        }

        if (m.getSelect() != null) {
            JavaType.Class selectType = TypeUtils.asClass(m.getSelect().getType());
            if (selectType != null && selectType.getFullyQualifiedName().equals(type)) {
                m = m.withSelect(m.getSelect().withType(targetType));
            }
        }

        if (m.getType() != null) {
            if(m.getType().getDeclaringType().getFullyQualifiedName().equals(type)) {
                m = m.withDeclaringType(targetType);
            }
        }

        return m;
    }

    @Override
    public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch) {
        J.MultiCatch m = super.visitMultiCatch(multiCatch);
        return m.withAlternatives(transformNames(m.getAlternatives()));
    }

    @Override
    public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable) {
        if (multiVariable.getTypeExpr() instanceof J.MultiCatch) {
            return super.visitMultiVariable(multiVariable);
        }

        J.VariableDecls m = super.visitMultiVariable(multiVariable);
        return m.withTypeExpr(transformName(m.getTypeExpr()));
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable) {
        J.VariableDecls.NamedVar v = super.visitVariable(variable);

        JavaType.Class varType = TypeUtils.asClass(variable.getType());
        if (varType != null && varType.getFullyQualifiedName().equals(type)) {
            v = v.withType(targetType).withName(v.getName().withType(targetType));
        }

        return v;
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray) {
        J.NewArray n = super.visitNewArray(newArray);
        return n.withTypeExpr(transformName(n.getTypeExpr()));
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass) {
        J.NewClass n = super.visitNewClass(newClass);
        return n.withClazz(transformName(n.getClazz()));
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast) {
        J.TypeCast t = super.visitTypeCast(typeCast);
        return t.withClazz(t.getClazz().withTree(transformName(t.getClazz().getTree())));
    }

    @Override
    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam) {
        J.TypeParameter t = super.visitTypeParameter(typeParam);
        t = t.withBounds(t.getBounds() == null ? null :
                t.getBounds().withTypes(transformNames(t.getBounds().getTypes())));
        return t.withName(transformName(t.getName()));
    }

    @Override
    public J.Wildcard visitWildcard(J.Wildcard wildcard) {
        J.Wildcard w = super.visitWildcard(wildcard);
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
