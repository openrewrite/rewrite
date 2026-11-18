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
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeFieldName<P> extends JavaIsoVisitor<P> {
    String classType;
    String hasName;
    String toName;

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
        J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
        if (enclosingClass == null) {
            return v;
        }
        if (variable.isField(getCursor()) && matchesClass(enclosingClass.getType()) &&
                variable.getSimpleName().equals(hasName)) {
            if (v.getVariableType() != null) {
                v = v.withVariableType(v.getVariableType().withName(toName));
            }
        }
        if (variable.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(visitLeftPadded(variable.getPadding().getInitializer(),
                    JLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v;
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = super.visitFieldAccess(fieldAccess, p);
        if (matchesClass(fieldAccess.getTarget().getType()) &&
                fieldAccess.getSimpleName().equals(hasName)) {
            f = f.getPadding().withName(f.getPadding().getName().withElement(f.getPadding().getName().getElement().withSimpleName(toName)));
        }
        return f;
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier ident, P p) {
        J.Identifier i = super.visitIdentifier(ident, p);

        if (i.getFieldType() != null) {
            JavaType.Variable varType = i.getFieldType();
            if (varType.getName().equals(hasName) && TypeUtils.isOfClassType(varType.getOwner(), classType)) {
                if (varType.getOwner() instanceof JavaType.Method) {
                    return i;
                }
                J.Identifier renamed = i.withSimpleName(toName).withFieldType(varType.withName(toName));
                Expression qualifier = qualifierWhereShadowed(varType);
                if (qualifier != null) {
                    doAfterVisit(new Qualify<>(renamed, qualifier));
                }
                return renamed;
            }
        }

        return i;
    }

    /**
     * What the field has to be selected from at the cursor once it has its new name, or {@code null} where the bare
     * name still reads it. A local variable of the new name would otherwise be read in its place.
     */
    private @Nullable Expression qualifierWhereShadowed(JavaType.Variable field) {
        Object parent = getCursor().getParentTreeCursor().getValue();
        if (parent instanceof J.FieldAccess || parent instanceof J.VariableDeclarations.NamedVariable ||
            !Scope.at(getCursor()).lookup(toName).filter(Scope.Binding::isLocal).isPresent()) {
            return null;
        }
        JavaType.FullyQualified owner = TypeUtils.asFullyQualified(field.getOwner());
        if (owner == null) {
            return null;
        }
        J.Identifier className = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), owner.getClassName(), owner, null);
        if (field.hasFlags(Flag.Static)) {
            return className;
        }
        J.Identifier this_ = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "this", owner, null);
        Object innermost = getCursor().dropParentUntil(v -> v instanceof J.ClassDeclaration ||
                v instanceof J.NewClass && ((J.NewClass) v).getBody() != null).getValue();
        if (innermost instanceof J.ClassDeclaration && TypeUtils.isOfType(((J.ClassDeclaration) innermost).getType(), owner)) {
            return this_;
        }
        return new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, className, JLeftPadded.build(this_), owner);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class Qualify<P> extends JavaVisitor<P> {
        J.Identifier name;
        Expression qualifier;

        @Override
        public J visitIdentifier(J.Identifier identifier, P p) {
            if (identifier.getId().equals(name.getId())) {
                return new J.FieldAccess(randomId(), identifier.getPrefix(), Markers.EMPTY, qualifier,
                        JLeftPadded.build(identifier.withPrefix(Space.EMPTY)), identifier.getType());
            }
            return identifier;
        }
    }

    private boolean matchesClass(@Nullable JavaType test) {
        JavaType.FullyQualified testClassType = TypeUtils.asFullyQualified(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType);
    }
}
