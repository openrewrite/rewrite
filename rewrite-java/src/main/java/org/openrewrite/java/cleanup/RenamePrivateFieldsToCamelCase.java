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
package org.openrewrite.java.cleanup;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.internal.NameCaseConvention.LOWER_CAMEL;

/**
 * This recipe converts private fields to camel case convention.
 * <p>
 * The first character is set to lower case and existing capital letters are preserved.
 * Special characters that are allowed in java field names `$` and `_` are removed.
 * If a special character is removed the next valid alpha-numeric will be capitalized.
 * <p>
 * Currently, unsupported:
 * - The recipe will not rename fields if the result already exists in a class or the result will be a java reserved keyword.
 */
public class RenamePrivateFieldsToCamelCase extends Recipe {

    @Override
    public String getDisplayName() {
        return "Reformat private field names to camelCase";
    }

    @Override
    public String getDescription() {
        return "Reformat private field names to camelCase to comply with Java naming convention. " +
               "The recipe will not rename fields with default, protected or public access modifiers." +
               "The recipe will not rename private constants." +
               "The first character is set to lower case and existing capital letters are preserved. " +
               "Special characters that are allowed in java field names `$` and `_` are removed. " +
               "If a special character is removed the next valid alphanumeric will be capitalized. " +
               "The recipe will not rename a field if the result already exists in the class, conflicts with a java reserved keyword, or the result is blank.";
    }

    @Override
    public Set<String> getTags() {
        return new LinkedHashSet<>(Arrays.asList("RSPEC-116", "RSPEC-3008"));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RenameToCamelCase() {
            @Override
            protected boolean shouldRename(Set<String> hasNameKey, J.VariableDeclarations.NamedVariable variable, String toName) {
                return !hasNameKey.contains(toName) && !hasNameKey.contains(variable.getSimpleName());
            }

            @SuppressWarnings("all")
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                Cursor parentScope = getCursorToParentScope(getCursor());

                // Does support renaming fields in a J.ClassDeclaration.
                // We must have a variable type to make safe changes.
                // Only make changes to private fields that are not constants.
                // Does not apply for instance variables of inner classes
                // Only make a change if the variable does not conform to lower camelcase format.
                if (parentScope.getParent() != null &&
                    parentScope.getParent().getValue() instanceof J.ClassDeclaration &&
                    !(parentScope.getValue() instanceof J.ClassDeclaration) &&
                    variable.getVariableType() != null &&
                    variable.getVariableType().hasFlags(Flag.Private) &&
                    !(variable.getVariableType().hasFlags(Flag.Static, Flag.Final)) &&
                    !((J.ClassDeclaration) parentScope.getParent().getValue()).getType().getFullyQualifiedName().contains("$") &&
                    !LOWER_CAMEL.matches(variable.getSimpleName())) {

                    String toName = LOWER_CAMEL.format(variable.getSimpleName());
                    renameVariable(variable, toName);
                } else {
                    hasNameKey(variable.getSimpleName());
                }

                return super.visitVariable(variable, ctx);
            }

            /**
             * Returns either the current block or a J.Type that may create a reference to a variable.
             * I.E. for(int target = 0; target < N; target++) creates a new name scope for `target`.
             * The name scope in the next J.Block `{}` cannot create new variables with the name `target`.
             * <p>
             * J.* types that may only reference an existing name and do not create a new name scope are excluded.
             */
            private Cursor getCursorToParentScope(Cursor cursor) {
                return cursor.dropParentUntil(is -> is instanceof J.ClassDeclaration || is instanceof J.Block);
            }
        };
    }
}
