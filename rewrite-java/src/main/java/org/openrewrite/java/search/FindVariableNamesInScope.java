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
package org.openrewrite.java.search;

import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.*;

@Incubating(since = "7.25.0")
@Value
public class FindVariableNamesInScope {

    // The SourceFile is required to ensure all J.ClassDeclaration fields may be found.
    public static Set<String> find(SourceFile compilationUnit, Cursor scope) {
        Set<String> names = new HashSet<>();
        VariableNameScopeVisitor variableNameScopeVisitor = new VariableNameScopeVisitor(scope);
        variableNameScopeVisitor.visit(compilationUnit, names);
        return names;
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
                    is instanceof J.CompilationUnit ||
                            is instanceof J.ClassDeclaration ||
                            is instanceof J.MethodDeclaration ||
                            is instanceof J.Block ||
                            is instanceof J.ForLoop ||
                            is instanceof J.Case ||
                            is instanceof J.Try ||
                            is instanceof J.Try.Catch ||
                            is instanceof J.If ||
                            is instanceof J.If.Else ||
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
                if (scope.getValue() instanceof J.Identifier) {
                    Cursor aggregatedScope = aggregateNameScope();
                    Set<String> names = nameScopes.get(aggregatedScope);

                    // Add the names created in the target scope.
                    Set<String> namesInCursorScope = nameScopes.get(scope);
                    if (namesInCursorScope != null) {
                        names.addAll(nameScopes.get(scope));
                    }
                    namesInScope.addAll(names);
                } else {
                    nameScopes.forEach((key, value) -> {
                        if (key.isScopeInPath(scope.getValue())) {
                            namesInScope.addAll(value);
                        }
                    });
                }
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
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Set<String> strings) {
            // Placeholder: variable declarations in a scope may be tracked to distinguish when a namespace conflict is a valid shadow vs. compilation error.
            return super.visitVariableDeclarations(multiVariable, strings);
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

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> namesInScope) {
            if (isValidIdentifier()) {
                Set<String> names = nameScopes.get(currentScope.peek());
                if (names != null) {
                    names.add(identifier.getSimpleName());
                }
            }
            return super.visitIdentifier(identifier, namesInScope);
        }

        // Filter out identifiers that won't create namespace conflicts.
        private boolean isValidIdentifier() {
            J parent = getCursor().dropParentUntil(is -> is instanceof J).getValue();
            return !(parent instanceof J.ClassDeclaration) &&
                    !(parent instanceof J.MethodDeclaration) &&
                    !(parent instanceof J.MethodInvocation) &&
                    !(parent instanceof J.VariableDeclarations) &&
                    !(parent instanceof J.NewClass) &&
                    !(parent instanceof J.Annotation) &&
                    !(parent instanceof J.MultiCatch) &&
                    !(parent instanceof J.ParameterizedType) &&
                    !(parent instanceof J.Case && getCursor().getValue() instanceof J.Identifier && "default".equals(((J.Identifier) getCursor().getValue()).getSimpleName()));
        }
    }
}
