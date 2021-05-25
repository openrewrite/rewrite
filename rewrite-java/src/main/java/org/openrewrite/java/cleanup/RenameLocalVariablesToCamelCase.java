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

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.tree.J;

import java.util.regex.Pattern;

/**
 * This recipe converts local variables and method parameters to camel case convention.
 * The recipe will not rename variables declared in for loop controls or catches with a single character.
 *
 * The first character is set to lower case and existing capital letters are preserved.
 * Special characters that are allowed in java field names `$` and `_` are removed.
 * If a special character is removed the next valid alpha-numeric will be capitalized.
 *
 * Currently, unsupported:
 *  - The recipe will not rename variables declared in a class.
 *  - The recipe will not rename variables if the result already exists in a class or the result will be a java reserved keyword.
 */
public class RenameLocalVariablesToCamelCase extends Recipe {

    @Override
    public String getDisplayName() {
        return "Rename local variables to camel case.";
    }

    @Override
    public String getDescription() {
        return "Renames local variables and method parameters to java camel case naming convention.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RenameNonCompliantNames();
    }

    private static class RenameNonCompliantNames extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            Cursor parentScope = getCursorToParentScope(getCursor());

            // Does not currently support renaming fields in a J.ClassDeclaration.
            if (!(parentScope.getParent() != null && parentScope.getParent().getValue() instanceof J.ClassDeclaration)) {
                // Does not apply to loop counters.
                if (!(parentScope.getValue() instanceof J.ForLoop.Control)) {
                    // Does not apply to one-character catch variables.
                    if (!((parentScope.getValue() instanceof J.Try.Catch || parentScope.getValue() instanceof J.MultiCatch) &&
                            variable.getSimpleName().length() == 1)) {
                        // Regex to match java naming convention.
                        Pattern pattern = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
                        if (!pattern.matcher(variable.getSimpleName()).matches()) {
                            String toName = convertToCamelCase(variable.getSimpleName());
                            // An identifier does not exit in the class.
                            if (!toNameFound(toName, getCursor(), ctx)) {
                                doAfterVisit(new RenameVariable<>(variable, toName));
                                return variable;
                            }
                        }
                    }
                }
            }
            return super.visitVariable(variable, ctx);
        }
    }

    private static boolean toNameFound(String toName, Cursor cursor, ExecutionContext ctx) {
        new FindName(toName).visit(cursor.firstEnclosing(J.CompilationUnit.class), ctx);
        return ctx.getMessage("NAME_FOUND_KEY") != null;
    }

    private static class FindName extends JavaIsoVisitor<ExecutionContext> {
        private final String name;

        public FindName(String name) {
            this.name = name;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            String nameFoundKey = "NAME_FOUND_KEY";
            if (ctx.getMessage(nameFoundKey) == null && ident.getSimpleName().equals(name)) {
                ctx.putMessage(nameFoundKey, true);
            }
            return super.visitIdentifier(ident, ctx);
        }
    }

    /**
     * Returns either the current block or a J.Type that may create a reference to a variable.
     * I.E. for(int target = 0; target < N; target++) creates a new name scope for `target`.
     * The name scope in the next J.Block `{}` cannot create new variables with the name `target`.
     * <p>
     * J.* types that may only reference an existing name and do not create a new name scope are excluded.
     */
    private static Cursor getCursorToParentScope(Cursor cursor) {
        return cursor.dropParentUntil(is ->
                is instanceof J.Block ||
                        is instanceof J.MethodDeclaration ||
                        is instanceof J.ForLoop ||
                        is instanceof J.ForEachLoop ||
                        is instanceof J.ForLoop.Control ||
                        is instanceof J.Case ||
                        is instanceof J.Try ||
                        is instanceof J.Try.Catch ||
                        is instanceof J.MultiCatch ||
                        is instanceof J.Lambda
        );
    }

    /**
     * Renames `oldName` to java camel case naming convention.
     * - The first character will be lower case.
     * - Preserves other existing capitalized characters.
     * - Special characters `$` and `_` will be removed.
     *      - If a special character is removed the following alphanumeric will be capitalized.
     */
    private static String convertToCamelCase(String oldName) {
        StringBuilder builder = new StringBuilder();
        boolean setCaps = false;
        for (int i = 0; i < oldName.length(); i++) {
            char c = oldName.charAt(i);
            switch (c) {
                case '$':
                case '_':
                    if (builder.length() > 0) {
                        setCaps = true;
                    }
                    continue;
                default:
                    break;
            }

            if (builder.length() == 0) {
                builder.append(String.valueOf(oldName.charAt(i)).toLowerCase());
            } else {
                if (setCaps) {
                    builder.append(String.valueOf(oldName.charAt(i)).toUpperCase());
                    setCaps = false;
                } else {
                    builder.append(oldName.charAt(i));
                }
            }
        }
        return builder.toString();
    }
}
