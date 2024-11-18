/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.trait;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

@Value
public class ScopedVariable implements Trait<J.VariableDeclarations.NamedVariable> {
    Cursor cursor;
    Cursor scope;
    J.Identifier identifier;

    public boolean isField(Cursor cursor) {
        return scope.getValue() instanceof J.Block &&
               scope.getParentTreeCursor().getValue() instanceof J.ClassDeclaration;
    }

    @RequiredArgsConstructor
    public static class Matcher extends SimpleTraitMatcher<ScopedVariable> {

        @Override
        protected @Nullable ScopedVariable test(Cursor cursor) {
            if (cursor.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                J.VariableDeclarations.NamedVariable variable = cursor.getValue();
                return new ScopedVariable(cursor, this.getDeclaringScope(cursor), variable.getName());
            }
            return null;
        }

        public Cursor getDeclaringScope(Cursor cursor) {
            return cursor.dropParentUntil(it ->
                    it instanceof J.Block ||
                    it instanceof J.Lambda ||
                    it instanceof J.MethodDeclaration ||
                    it == Cursor.ROOT_VALUE);
        }
    }
}
