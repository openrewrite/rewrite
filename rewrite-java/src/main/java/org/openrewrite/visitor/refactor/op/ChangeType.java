/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.visitor.refactor.op;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.*;
import org.openrewrite.tree.*;
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.RefactorVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.visitor.refactor.AstTransform;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static org.openrewrite.tree.J.randomId;
import static java.util.Collections.emptyList;

/**
 * NOTE: Does not currently transform all possible type references, and accomplishing this would be non-trivial.
 * For example, a method invocation select might refer to field `A a` whose type has now changed to `A2`, and so the type
 * on the select should change as well. But how do we identify the set of all method selects which refer to `a`? Suppose
 * it were prefixed like `this.a`, or `MyClass.this.a`, or indirectly via a separate method call like `getA()` where `getA()`
 * is defined on the super class.
 */
public class ChangeType extends RefactorVisitor {
    private final String from;
    private final Type.Class toClassType;

    public ChangeType(String from, String to) {
        this.from = from;
        this.toClassType = Type.Class.build(to);
    }

    // NOTE: a type change is possible anywhere a J.FieldAccess or J.Ident is possible, but not every FieldAccess or Ident
    // represents a type (could represent a variable name, etc.)

    @Override
    public String getRuleName() {
        return MessageFormatter.arrayFormat("core.ChangeType{from={},to={}}",
                new String[] { from, toClassType.getFullyQualifiedName() }).toString();
    }

    @Override
    public List<AstTransform> visitEnd() {
        maybeAddImport(toClassType);
        maybeRemoveImport(from);
        return super.visitEnd();
    }

    @Override
    public List<AstTransform> visitTypeName(NameTree name) {
        Type.Class oldTypeAsClass = TypeUtils.asClass(name.getType());
        return maybeTransform(name,
                !(name instanceof TypeTree) && oldTypeAsClass != null && oldTypeAsClass.getFullyQualifiedName().equals(from),
                super::visitTypeName,
                n -> n.withType(toClassType));
    }

    @Override
    public List<AstTransform> visitAnnotation(J.Annotation annotation) {
        List<AstTransform> changes = super.visitAnnotation(annotation);
        changes.addAll(transformName(annotation, annotation.getAnnotationType(), J.Annotation::withAnnotationType));
        return changes;
    }

    @Override
    public List<AstTransform> visitArrayType(J.ArrayType arrayType) {
        List<AstTransform> changes = super.visitArrayType(arrayType);
        changes.addAll(transformName(arrayType, arrayType.getElementType(), J.ArrayType::withElementType));
        return changes;
    }

    @Override
    public List<AstTransform> visitClassDecl(J.ClassDecl classDecl) {
        List<AstTransform> changes = super.visitClassDecl(classDecl);
        changes.addAll(transformName(classDecl, classDecl.getExtends(), J.ClassDecl::withExtendings));
        changes.addAll(transformNames(classDecl, classDecl.getImplements(), J.ClassDecl::withImplementings));
        return changes;
    }

    @Override
    public List<AstTransform> visitFieldAccess(J.FieldAccess fieldAccess) {
        List<AstTransform> changes = super.visitFieldAccess(fieldAccess);
        changes.addAll(transformName(fieldAccess, fieldAccess.asClassReference(), J.FieldAccess::withTarget));
        return changes;
    }

    @Override
    public List<AstTransform> visitMethod(J.MethodDecl method) {
        List<AstTransform> changes = super.visitMethod(method);
        changes.addAll(transformName(method, method.getReturnTypeExpr(), J.MethodDecl::withReturnTypeExpr));
        if (method.getThrows() != null) {
            changes.addAll(transformNames(method, method.getThrows().getExceptions(), (m, exceptions) -> m.getThrows() == null ?
                    m.withThrowz(new J.MethodDecl.Throws(randomId(), exceptions, Formatting.format(" "))) :
                    m.withThrowz(m.getThrows().withExceptions(exceptions))));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitMethodInvocation(J.MethodInvocation method) {
        List<AstTransform> changes = super.visitMethodInvocation(method);

        if (method.getSelect() instanceof NameTree && method.getType() != null && method.getType().hasFlags(Flag.Static)) {
            changes.addAll(transformName(method, method.getSelect(), J.MethodInvocation::withSelect));
        }

        Type.Class selectType = TypeUtils.asClass(method.getSelect().getType());
        if(selectType != null && selectType.getFullyQualifiedName().equals(from)) {
            changes.addAll(transform(method.getSelect(), s -> s.withType(toClassType)));
        }

        if (method.getTypeParameters() != null) {
            for (J.TypeParameter param : method.getTypeParameters().getParams()) {
                changes.addAll(transformName(param, param.getName(), J.TypeParameter::withName));
            }
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitMultiCatch(J.MultiCatch multiCatch) {
        List<AstTransform> changes = super.visitMultiCatch(multiCatch);
        changes.addAll(transformNames(multiCatch, multiCatch.getAlternatives(), J.MultiCatch::withAlternatives));
        return changes;
    }

    @Override
    public List<AstTransform> visitMultiVariable(J.VariableDecls multiVariable) {
        List<AstTransform> changes = super.visitMultiVariable(multiVariable);

        if (multiVariable.getTypeExpr() instanceof J.MultiCatch) {
            return changes;
        }

        changes.addAll(transformName(multiVariable, multiVariable.getTypeExpr(), J.VariableDecls::withTypeExpr));

        for (J.VariableDecls.NamedVar var : multiVariable.getVars()) {
            Type.Class varType = TypeUtils.asClass(var.getType());
            if (varType != null && varType.getFullyQualifiedName().equals(from)) {
                changes.addAll(transform(var, v -> v.withType(toClassType).withName(v.getName().withType(toClassType))));
            }
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitNewArray(J.NewArray newArray) {
        List<AstTransform> changes = super.visitNewArray(newArray);
        changes.addAll(transformName(newArray, newArray.getTypeExpr(), J.NewArray::withTypeExpr));
        return changes;
    }

    @Override
    public List<AstTransform> visitNewClass(J.NewClass newClass) {
        List<AstTransform> changes = super.visitNewClass(newClass);
        changes.addAll(transformName(newClass, newClass.getClazz(), J.NewClass::withClazz));
        return changes;
    }

    @Override
    public List<AstTransform> visitTypeCast(J.TypeCast typeCast) {
        List<AstTransform> changes = super.visitTypeCast(typeCast);
        changes.addAll(transformName(typeCast, typeCast.getClazz().getTree(),
                (t, name) -> t.withClazz(typeCast.getClazz().withTree(name))));
        return changes;
    }

    @Override
    public List<AstTransform> visitTypeParameter(J.TypeParameter typeParam) {
        List<AstTransform> changes = super.visitTypeParameter(typeParam);
        if (typeParam.getBounds() != null) {
            changes.addAll(transformNames(typeParam, typeParam.getBounds().getTypes(), (t, types) -> t.getBounds() == null ?
                    t.withBounds(new J.TypeParameter.Bounds(randomId(), types, Formatting.EMPTY)) :
                    t.withBounds(typeParam.getBounds().withTypes(types))));
        }
        else {
            changes.addAll(transformName(typeParam, typeParam.getName(), J.TypeParameter::withName));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitWildcard(J.Wildcard wildcard) {
        List<AstTransform> changes = super.visitWildcard(wildcard);
        changes.addAll(transformName(wildcard, wildcard.getBoundedType(), J.Wildcard::withBoundedType));
        return changes;
    }

    private <T extends Tree> List<AstTransform> transformName(T containsName, @Nullable Tree nameField, BiFunction<T, J.Ident, T> change) {
        if (nameField instanceof NameTree) {
            Type.Class nameTreeClass = TypeUtils.asClass(((NameTree) nameField).getType());
            if (nameTreeClass != null && nameTreeClass.getFullyQualifiedName().equals(from)) {
                return transform(containsName, t -> change.apply(t,
                        J.Ident.build(randomId(), toClassType.getClassName(), toClassType, nameField.getFormatting())));
            }
        }
        return emptyList();
    }

    @SuppressWarnings("unchecked")
    private <T extends Tree, U extends Tree> List<AstTransform> transformNames(T containsName, @Nullable Iterable<U> nodes, BiFunction<T, List<U>, Tree> change) {
        if (nodes == null) {
            return emptyList();
        }

        boolean atLeastOneChanged = false;
        List<U> transformed = new ArrayList<>();
        for (U node : nodes) {
            if (node instanceof NameTree) {
                Type.Class nodeTypeAsClass = TypeUtils.asClass(((NameTree) node).getType());
                if (nodeTypeAsClass != null && nodeTypeAsClass.getFullyQualifiedName().equals(from)) {
                    atLeastOneChanged = true;
                    transformed.add((U) J.Ident.build(randomId(), toClassType.getClassName(), toClassType, node.getFormatting()));
                    continue;
                }
            }
            transformed.add(node);
        }

        return atLeastOneChanged ? transform(containsName, t -> (T) change.apply(t, transformed)) : emptyList();
    }
}
