/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.trait.variable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.trait.Top;
import org.openrewrite.java.trait.TraitFactory;
import org.openrewrite.java.trait.expr.VarAccess;
import org.openrewrite.java.trait.member.Callable;
import org.openrewrite.java.trait.member.Method;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.util.Validation;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.*;

/**
 * Represents a parameter in a {@link org.openrewrite.java.tree.J.MethodDeclaration} or
 * {@link org.openrewrite.java.tree.J.NewClass}.
 */
public interface Parameter extends LocalScopeVariable {

    /**
     * The zero indexed position of the parameter in the method declaration.
     */
    int getPosition();

    boolean isVarArgs();

    enum Factory implements TraitFactory<Parameter> {
        F;
        @Override
        public Validation<TraitErrors, Parameter> viewOf(Cursor c) {
            if (c.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                Cursor variableDeclarationsCursor = c.getParentTreeCursor();
                Cursor methodDeclarationCursor = variableDeclarationsCursor.getParentTreeCursor();
                return Method.viewOf(methodDeclarationCursor).map(callable ->  new ParameterBase(
                        c,
                        c.getValue(),
                        callable,
                        methodDeclarationCursor,
                        methodDeclarationCursor.getValue()
                ));
            }
            return TraitErrors.invalidTraitCreationType(Parameter.class, c, J.VariableDeclarations.NamedVariable.class);
        }
    }

    static Validation<TraitErrors, Parameter> viewOf(Cursor c) {
        return Factory.F.viewOf(c);
    }
}

@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class ParameterBase implements Parameter {
    Cursor cursor;
    J.VariableDeclarations.NamedVariable namedVariable;
    @Getter(onMethod = @__(@Override))
    Callable callable;
    Cursor methodDeclarationCursor;
    J.MethodDeclaration methodDeclaration;

    @Override
    public UUID getId() {
        return namedVariable.getId();
    }

    @Override
    public String getName() {
        return namedVariable.getSimpleName();
    }

    @Override
    public int getPosition() {
        return callable.getParameters().indexOf(this);
    }

    @Override
    public boolean isVarArgs() {
        if (this.namedVariable.getVariableType() == null) {
            throw new IllegalStateException("Variable type is null for " + this.namedVariable);
        }
        return this.namedVariable.getVariableType().hasFlags(Flag.Varargs);
    }

    @Override
    public boolean equals(Object obj) {
        return Top.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Top.hashCode(this);
    }

    @Override
    public JavaType getType() {
        return namedVariable.getType();
    }

    @Override
    public Collection<VarAccess> getVarAccesses() {
        if (methodDeclaration.getBody() == null) {
            return Collections.emptySet();
        }
        return VarAccess.findAllInScope(new Cursor(methodDeclarationCursor, methodDeclaration.getBody()), this);
    }
}
