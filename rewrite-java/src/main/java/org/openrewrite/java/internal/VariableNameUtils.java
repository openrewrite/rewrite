/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.internal;

import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;

@Incubating(since = "7.25.0")
@Value
public class VariableNameUtils {

    private VariableNameUtils() {
    }

    /**
     * Generates a variable name without namespace conflicts in the scope of a cursor.
     *
     * A JavaSourceFile must be available in the Cursor path to account for all names
     * available in the cursor scope.
     *
     * @param baseName baseName for the variable.
     * @param scope The cursor position of a JavaVisitor, {@link org.openrewrite.java.JavaVisitor#getCursor()}.
     * @param strategy {@link GenerationStrategy} to use if a name already exists with the baseName.
     * @return either the baseName if a namespace conflict does not exist or a name generated with the provided strategy.
     */
    public static String generateVariableName(String baseName, Cursor scope, GenerationStrategy strategy) {
        Set<String> namesInScope = findNamesInScope(scope);
        String newName = baseName;
        // Generate a new name to prevent namespace shadowing.
        if (GenerationStrategy.INCREMENT_NUMBER.equals(strategy)) {
            int count = 0;
            while (namesInScope.contains(newName)) {
                newName = baseName + (count += 1);
            }
        }

        return newName;
    }

    /**
     * Find the names of variables that already exist in the scope of a cursor.
     * A JavaSourceFile must be available in the Cursor path to account for all names
     * available in the cursor scope.
     *
     * Known issue: all names of static imports are imported.
     *
     * @param scope The cursor position of a JavaVisitor, {@link org.openrewrite.java.JavaVisitor#getCursor()}.
     * @return Variable names available in the name scope of the cursor.
     */
    public static Set<String> findNamesInScope(Cursor scope) {
        JavaSourceFile compilationUnit = scope.firstEnclosing(JavaSourceFile.class);
        if (compilationUnit == null) {
            throw new IllegalStateException("A JavaSourceFile is required in the cursor path.");
        }

        Set<String> names = new HashSet<>();
        VariableNameScopeVisitor variableNameScopeVisitor = new VariableNameScopeVisitor(scope);
        variableNameScopeVisitor.visit(compilationUnit, names);
        return names;
    }

    public enum GenerationStrategy {
        INCREMENT_NUMBER
    }

    private static final class VariableNameScopeVisitor extends JavaIsoVisitor<Set<String>> {
        private final Cursor scope;
        private final Map<Cursor, Set<String>> nameScopes;
        private final Stack<Cursor> currentScope;

        public VariableNameScopeVisitor(Cursor scope) {
            this.scope = scope;
            this.nameScopes = new LinkedHashMap<>();
            this.currentScope = new Stack<>();
        }

        /**
         * Aggregates namespaces into specific scopes.
         * I.E. names declared in a {@link J.ControlParentheses} will belong to the parent AST element.
         */
        private Cursor aggregateNameScope() {
            return getCursor().dropParentUntil(is ->
                    is instanceof JavaSourceFile ||
                            is instanceof J.ClassDeclaration ||
                            is instanceof J.MethodDeclaration ||
                            is instanceof J.Block ||
                            is instanceof J.ForLoop ||
                            is instanceof J.ForEachLoop ||
                            is instanceof J.Case ||
                            is instanceof J.Try ||
                            is instanceof J.Try.Catch ||
                            is instanceof J.Lambda);
        }

        @Override
        public Statement visitStatement(Statement statement, Set<String> namesInScope) {
            Statement s = super.visitStatement(statement, namesInScope);
            Cursor aggregatedScope = aggregateNameScope();
            if (currentScope.isEmpty() || currentScope.peek() != aggregatedScope) {
                Set<String> namesInAggregatedScope = nameScopes.computeIfAbsent(aggregatedScope, k -> new HashSet<>());
                // Pass the name scopes available from a parent scope down to the child.
                if (!currentScope.isEmpty() && aggregatedScope.isScopeInPath(currentScope.peek().getValue())) {
                    namesInAggregatedScope.addAll(nameScopes.get(currentScope.peek()));
                }
                currentScope.push(aggregatedScope);
            }
            return s;
        }

        // Stop after the tree has been processed to ensure all the names in scope have been collected.
        @Override
        public @Nullable J postVisit(J tree, Set<String> namesInScope) {
            if (!currentScope.isEmpty() && currentScope.peek().getValue().equals(tree)) {
                currentScope.pop();
            }

            if (scope.getValue().equals(tree)) {
                Cursor aggregatedScope = aggregateNameScope();
                // Add names from parent scope.
                Set<String> names = nameScopes.get(aggregatedScope);

                // Add the names created in the target scope.
                namesInScope.addAll(names);
                nameScopes.forEach((key, value) -> {
                    if (key.isScopeInPath(scope.getValue())) {
                        namesInScope.addAll(value);
                    }
                });
                return tree;
            }

            return super.postVisit(tree, namesInScope);
        }

        @Override
        public J.Import visitImport(J.Import _import, Set<String> namesInScope) {
            // Skip identifiers from `import`s.
            return _import;
        }

        @Override
        public J.Package visitPackage(J.Package pkg, Set<String> namesInScope) {
            // Skip identifiers from `package`.
            return pkg;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> namesInScope) {
            // Collect class fields first, because class fields are always visible regardless of what order the statements are declared.
            classDecl.getBody().getStatements().forEach(o -> {
                if (o instanceof J.VariableDeclarations) {
                    J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) o;
                    variableDeclarations.getVariables().forEach(v ->
                            nameScopes.computeIfAbsent(getCursor(), k -> new HashSet<>()).add(v.getSimpleName()));
                }
            });

            addImportedStaticFieldNames(getCursor().firstEnclosing(J.CompilationUnit.class), getCursor());
            if (classDecl.getType() != null) {
                addInheritedClassFields(classDecl.getType().getSupertype(), getCursor());
            }
            return super.visitClassDeclaration(classDecl, namesInScope);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<String> strings) {
            Set<String> names = nameScopes.get(currentScope.peek());
            if (names != null) {
                names.add(variable.getSimpleName());
            }
            return super.visitVariable(variable, strings);
        }

        private void addImportedStaticFieldNames(@Nullable J.CompilationUnit cu, Cursor classCursor) {
            if (cu != null) {
                List<J.Import> imports = cu.getImports();
                imports.forEach(i -> {
                    if (i.isStatic()) {
                        // Note: Currently, adds all statically imported identifiers including method and classes rather than restricting the names to static fields.
                        Set<String> namesAtCursor = nameScopes.computeIfAbsent(classCursor, k -> new HashSet<>());
                        namesAtCursor.add(i.getQualid().getSimpleName());
                    }
                });
            }
        }

        private void addInheritedClassFields(@Nullable JavaType.FullyQualified fq, Cursor classCursor) {
            if (fq != null) {
                J.ClassDeclaration cd = classCursor.getValue();
                boolean isSamePackage = cd.getType() != null && cd.getType().getPackageName().equals(fq.getPackageName());
                fq.getMembers().forEach(m -> {
                    if ((Flag.hasFlags(m.getFlagsBitMap(), Flag.Public) ||
                            Flag.hasFlags(m.getFlagsBitMap(), Flag.Protected)) ||
                            // Member is accessible as package-private.
                            !Flag.hasFlags(m.getFlagsBitMap(), Flag.Private) && isSamePackage) {
                        Set<String> namesAtCursor = nameScopes.computeIfAbsent(classCursor, k -> new HashSet<>());
                        namesAtCursor.add(m.getName());
                    }
                });
                addInheritedClassFields(fq.getSupertype(), classCursor);
            }
        }
    }
}
