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

import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.trait.Element;
import org.openrewrite.java.trait.TraitFactory;
import org.openrewrite.java.trait.expr.VarAccess;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.util.Validation;
import org.openrewrite.java.tree.JavaType;

import java.util.Collection;

/**
 * A variable is a field, a local variable or a parameter.
 */
public interface Variable extends Element {

    @Nullable
    JavaType getType();

    /**
     * Gets all access to this variable.
     */
    Collection<VarAccess> getVarAccesses();

    enum Factory implements TraitFactory<Variable> {
        F;

        @Override
        public Validation<TraitErrors, Variable> viewOf(Cursor cursor) {
            Validation<TraitErrors, Variable> localScopeVariable = LocalScopeVariable.viewOf(cursor).map(l -> l);
            return localScopeVariable.f().bind(localScopeVariableFail -> {
                Validation<TraitErrors, Variable> field = Field.viewOf(cursor).map(f -> f);
                return field.f().bind(fieldFail -> Validation.fail(TraitErrors.semigroup.sum(
                        localScopeVariableFail,
                        fieldFail
                )));
            });

        }
    }

    static Validation<TraitErrors, Variable> viewOf(Cursor cursor) {
        return Factory.F.viewOf(cursor);
    }

}
