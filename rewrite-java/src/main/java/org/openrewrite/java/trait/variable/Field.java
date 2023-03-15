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

import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.trait.Element;
import org.openrewrite.java.trait.Top;
import org.openrewrite.java.trait.TraitFactory;
import org.openrewrite.java.trait.expr.VarAccess;
import org.openrewrite.java.trait.member.FieldDeclaration;
import org.openrewrite.java.trait.member.Member;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.util.Validation;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * A class or instance field.
 */
public interface Field extends Member, Variable {
    /**
     * Gets the field declaration in which this field is declared.
     * <p/>
     * Note that this declaration is only available if the field occurs in source code.
     */
    Optional<FieldDeclaration> getDeclaration();

    enum Factory implements TraitFactory<Field> {
        F;

        @Override
        public Validation<TraitErrors, Field> viewOf(Cursor cursor) {
            if (cursor.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                Cursor maybeVariableDecl = cursor.getParentTreeCursor();
                Cursor maybeBlock = maybeVariableDecl.getParentTreeCursor();
                Cursor maybeClassDecl = maybeBlock.getParentTreeCursor();
                if (maybeClassDecl.getValue() instanceof J.ClassDeclaration || maybeClassDecl.getValue() instanceof J.NewClass) {
                    return Validation.success(new FieldFromCursor(cursor, cursor.getValue(), maybeBlock));
                }
                return TraitErrors.invalidTraitCreationError("Field must be declared in a class, interface, or anonymous class");
            }
            return TraitErrors.invalidTraitCreationType(Field.class, cursor, J.VariableDeclarations.NamedVariable.class);
        }
    }

    static Validation<TraitErrors, Field> viewOf(Cursor cursor) {
        return Factory.F.viewOf(cursor);
    }
}

@AllArgsConstructor
class FieldFromCursor implements Field {
    Cursor cursor;
    J.VariableDeclarations.NamedVariable variable;
    Cursor parentBlock;

    @Override
    public String getName() {
        return variable.getSimpleName();
    }

    @Override
    public UUID getId() {
        return variable.getId();
    }

    @Override
    public Optional<FieldDeclaration> getDeclaration() {
        return Optional.empty();
    }

    @Override
    public @Nullable JavaType getType() {
        return variable.getType();
    }

    @Override
    public Collection<VarAccess> getVarAccesses() {
        // Searching starts at the J.CompilationUnit because we want to find all references to this field, within the file,
        // not just within the class (which may contain multiple classes).
        Cursor searchScope = parentBlock.dropParentUntil(J.CompilationUnit.class::isInstance);
        return VarAccess.findAllInScope(searchScope, this);
    }

    @Override
    public boolean equals(Object obj) {
        return Top.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Top.hashCode(this);
    }
}
