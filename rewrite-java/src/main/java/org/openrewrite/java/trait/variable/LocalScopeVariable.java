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
import org.openrewrite.java.trait.TraitFactory;
import org.openrewrite.java.trait.member.Callable;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.util.Validation;

/** A locally scoped variable, that is, either a local variable or a parameter. */
public interface LocalScopeVariable extends Variable {
    Callable getCallable();

    enum Factory implements TraitFactory<LocalScopeVariable> {
        F;

        @Override
        public Validation<TraitErrors, LocalScopeVariable> viewOf(Cursor cursor) {
            Validation<TraitErrors, LocalScopeVariable> parameter = Parameter.viewOf(cursor).map(p -> p);
            return parameter.f().bind(parameterFail -> {
                Validation<TraitErrors, LocalScopeVariable> localVariableDecl = LocalVariableDecl.viewOf(cursor).map(l -> l);
                return localVariableDecl.f().bind(localVariableDeclFail -> Validation.fail(TraitErrors.semigroup.sum(
                        parameterFail,
                        localVariableDeclFail
                )));
            });
        }
    }

    static Validation<TraitErrors, LocalScopeVariable> viewOf(Cursor cursor) {
        return LocalScopeVariable.Factory.F.viewOf(cursor);
    }
}
