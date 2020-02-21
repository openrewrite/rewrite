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
package com.netflix.rewrite.visitor.refactor.op;

import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.*;
import com.netflix.rewrite.visitor.refactor.AstTransform;
import com.netflix.rewrite.visitor.refactor.RefactorVisitor;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static com.netflix.rewrite.tree.Tr.randomId;
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

    // NOTE: a type change is possible anywhere a Tr.FieldAccess or Tr.Ident is possible, but not every FieldAccess or Ident
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
    public List<AstTransform> visitAnnotation(Tr.Annotation annotation) {
        List<AstTransform> changes = super.visitAnnotation(annotation);
        changes.addAll(transformName(annotation, annotation.getAnnotationType(), Tr.Annotation::withAnnotationType));
        return changes;
    }

    @Override
    public List<AstTransform> visitArrayType(Tr.ArrayType arrayType) {
        List<AstTransform> changes = super.visitArrayType(arrayType);
        changes.addAll(transformName(arrayType, arrayType.getElementType(), Tr.ArrayType::withElementType));
        return changes;
    }

    @Override
    public List<AstTransform> visitClassDecl(Tr.ClassDecl classDecl) {
        List<AstTransform> changes = super.visitClassDecl(classDecl);
        changes.addAll(transformName(classDecl, classDecl.getExtends(), Tr.ClassDecl::withExtendings));
        changes.addAll(transformNames(classDecl, classDecl.getImplements(), Tr.ClassDecl::withImplementings));
        return changes;
    }

    @Override
    public List<AstTransform> visitFieldAccess(Tr.FieldAccess fieldAccess) {
        List<AstTransform> changes = super.visitFieldAccess(fieldAccess);
        changes.addAll(transformName(fieldAccess, fieldAccess.asClassReference(), Tr.FieldAccess::withTarget));
        return changes;
    }

    @Override
    public List<AstTransform> visitMethod(Tr.MethodDecl method) {
        List<AstTransform> changes = super.visitMethod(method);
        changes.addAll(transformName(method, method.getReturnTypeExpr(), Tr.MethodDecl::withReturnTypeExpr));
        if (method.getThrows() != null) {
            changes.addAll(transformNames(method, method.getThrows().getExceptions(), (m, exceptions) -> m.getThrows() == null ?
                    m.withThrowz(new Tr.MethodDecl.Throws(randomId(), exceptions, Formatting.format(" "))) :
                    m.withThrowz(m.getThrows().withExceptions(exceptions))));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitMethodInvocation(Tr.MethodInvocation method) {
        List<AstTransform> changes = super.visitMethodInvocation(method);

        if (method.getSelect() instanceof NameTree && method.getType() != null && method.getType().hasFlags(Flag.Static)) {
            changes.addAll(transformName(method, method.getSelect(), Tr.MethodInvocation::withSelect));
        }

        if (method.getTypeParameters() != null) {
            for (Tr.TypeParameter param : method.getTypeParameters().getParams()) {
                changes.addAll(transformName(param, param.getName(), Tr.TypeParameter::withName));
            }
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitMultiCatch(Tr.MultiCatch multiCatch) {
        List<AstTransform> changes = super.visitMultiCatch(multiCatch);
        changes.addAll(transformNames(multiCatch, multiCatch.getAlternatives(), Tr.MultiCatch::withAlternatives));
        return changes;
    }

    @Override
    public List<AstTransform> visitMultiVariable(Tr.VariableDecls multiVariable) {
        List<AstTransform> changes = super.visitMultiVariable(multiVariable);

        if (multiVariable.getTypeExpr() instanceof Tr.MultiCatch) {
            return changes;
        }

        changes.addAll(transformName(multiVariable, multiVariable.getTypeExpr(), Tr.VariableDecls::withTypeExpr));

        List<Tr.VariableDecls.NamedVar> vars = multiVariable.getVars();
        for (int i = 0; i < vars.size(); i++) {
            Tr.VariableDecls.NamedVar var = vars.get(i);
            final int innerI = i;
            changes.addAll(transformName(multiVariable, var, (m, transformedName) -> {
                List<Tr.VariableDecls.NamedVar> transformedVars = new ArrayList<>(vars.size());
                for (int j = 0; j < vars.size(); j++) {
                    transformedVars.add(innerI == j ? var.withName(transformedName) : var);
                }
                return m.withVars(transformedVars);
            }));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitNewArray(Tr.NewArray newArray) {
        List<AstTransform> changes = super.visitNewArray(newArray);
        changes.addAll(transformName(newArray, newArray.getTypeExpr(), Tr.NewArray::withTypeExpr));
        return changes;
    }

    @Override
    public List<AstTransform> visitNewClass(Tr.NewClass newClass) {
        List<AstTransform> changes = super.visitNewClass(newClass);
        changes.addAll(transformName(newClass, newClass.getClazz(), Tr.NewClass::withClazz));
        return changes;
    }

    @Override
    public List<AstTransform> visitTypeCast(Tr.TypeCast typeCast) {
        List<AstTransform> changes = super.visitTypeCast(typeCast);
        changes.addAll(transformName(typeCast, typeCast.getClazz().getTree(),
                (t, name) -> t.withClazz(typeCast.getClazz().withTree(name))));
        return changes;
    }

    @Override
    public List<AstTransform> visitTypeParameter(Tr.TypeParameter typeParam) {
        List<AstTransform> changes = super.visitTypeParameter(typeParam);
        if (typeParam.getBounds() != null) {
            changes.addAll(transformNames(typeParam, typeParam.getBounds().getTypes(), (t, types) -> t.getBounds() == null ?
                    t.withBounds(new Tr.TypeParameter.Bounds(randomId(), types, Formatting.EMPTY)) :
                    t.withBounds(typeParam.getBounds().withTypes(types))));
        }
        else {
            changes.addAll(transformName(typeParam, typeParam.getName(), Tr.TypeParameter::withName));
        }
        return changes;
    }

    @Override
    public List<AstTransform> visitWildcard(Tr.Wildcard wildcard) {
        List<AstTransform> changes = super.visitWildcard(wildcard);
        changes.addAll(transformName(wildcard, wildcard.getBoundedType(), Tr.Wildcard::withBoundedType));
        return changes;
    }

    private <T extends Tree> List<AstTransform> transformName(T containsName, @Nullable Tree nameField, BiFunction<T, Tr.Ident, T> change) {
        if (nameField instanceof NameTree) {
            Type.Class nameTreeClass = TypeUtils.asClass(((NameTree) nameField).getType());
            if (nameTreeClass != null && nameTreeClass.getFullyQualifiedName().equals(from)) {
                return transform(containsName, t -> change.apply(t,
                        Tr.Ident.build(randomId(), toClassType.getClassName(), toClassType, nameField.getFormatting())));
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
                    transformed.add((U) Tr.Ident.build(randomId(), toClassType.getClassName(), toClassType, node.getFormatting()));
                    continue;
                }
            }
            transformed.add(node);
        }

        return atLeastOneChanged ? transform(containsName, t -> (T) change.apply(t, transformed)) : emptyList();
    }
}
