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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeFieldName<P> extends JavaIsoVisitor<P> {
    String classType;
    String hasName;
    String toName;

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = variable;
        J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
        if(enclosingClass == null) {
            return v;
        }
        if (variable.isField(getCursor()) && matchesClass(enclosingClass.getType()) &&
                variable.getSimpleName().equals(hasName)) {
            v = v.withName(v.getName().withSimpleName(toName));
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
                i = new J.Identifier(
                        randomId(),
                        i.getPrefix(),
                        i.getMarkers(),
                        toName,
                        i.getType(),
                        new JavaType.Variable(
                                null,
                                Flag.flagsToBitMap(varType.getFlags()),
                                toName,
                                varType.getOwner(),
                                varType.getType(),
                                varType.getAnnotations()
                        )
                );
            }
        }

        return i;
    }

    private boolean matchesClass(@Nullable JavaType test) {
        JavaType.FullyQualified testClassType = TypeUtils.asFullyQualified(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType);
    }
}
