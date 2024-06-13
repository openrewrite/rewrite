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
package org.openrewrite.java;

import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.text.Normalizer;
import java.util.*;

@Incubating(since = "7.25.0")
@Value
public class VariableNameUtils {

    private VariableNameUtils() {
    }

    /**
     * Generates a variable name without namespace conflicts in the scope of a cursor.
     * <p>
     * A JavaSourceFile must be available in the Cursor path to account for all names
     * available in the cursor scope.
     * <p>
     * Since the client recipe may have modified the LST element represented by {@code scope}, this method will
     * visit the value held by {@code scope} once it encounters it in the tree. For this to work, the client recipe
     * must use {@link Cursor#Cursor(Cursor, Object)} to pass a cursor holding the modified LST element.
     *
     * @param baseName baseName for the variable.
     * @param scope The cursor position of a JavaVisitor, {@link org.openrewrite.java.JavaVisitor#getCursor()}.
     * @param strategy {@link GenerationStrategy} to use if a name already exists with the baseName.
     * @return either the baseName if a namespace conflict does not exist or a name generated with the provided strategy.
     */
    public static String generateVariableName(String baseName, Cursor scope, GenerationStrategy strategy) {
        Set<String> namesInScope = findNamesInScope(scope);
        // Generate a new name to prevent namespace shadowing.
        String newName = baseName;
        if (GenerationStrategy.INCREMENT_NUMBER.equals(strategy)) {
            StringBuilder postFix = new StringBuilder();
            char[] charArray = baseName.toCharArray();
            for (int i = charArray.length - 1; i >= 0; i--) {
                char c = charArray[i];
                if (Character.isDigit(c)) {
                    postFix.append(c);
                } else {
                    break;
                }
            }

            baseName = baseName.substring(0, baseName.length() - postFix.length());
            int count = postFix.length() == 0 ? 0 : Integer.parseInt(postFix.reverse().toString());
            while (namesInScope.contains(newName) || JavaKeywords.isReserved(newName)) {
                newName = baseName + (count += 1);
            }
        }

        return newName;
    }

    /**
     * Replace accent and diacritics with normalized characters.
     * @param name variable name to normalize.
     * @return normalized name.
     */
    public static String normalizeName(String name) {
        if (name.isEmpty() || Normalizer.isNormalized(name, Normalizer.Form.NFKD)) {
            return name;
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKD);
        return normalized.replaceAll("\\p{M}", "");
    }

    /**
     * Find the names of variables that already exist in the scope of a cursor.
     * A JavaSourceFile must be available in the Cursor path to account for all names
     * available in the cursor scope.
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

    /**
     * Collects class field names inherited from super classes.
     * <p>
     * Note: Does not currently account for {@link JavaType.Unknown}.
     */
    public static Set<String> findInheritedNames(J.ClassDeclaration classDeclaration) {
        Set<String> names = new HashSet<>();
        if (classDeclaration.getType() != null) {
            addInheritedClassFields(classDeclaration, classDeclaration.getType().getSupertype(), names);
        }
        return names;
    }

    private static void addInheritedClassFields(J.ClassDeclaration classDeclaration, @Nullable JavaType.FullyQualified superClass, Set<String> names) {
        if (superClass != null) {
            boolean isSamePackage = classDeclaration.getType() != null && classDeclaration.getType().getPackageName().equals(superClass.getPackageName());
            superClass.getMembers().forEach(m -> {
                if ((Flag.hasFlags(m.getFlagsBitMap(), Flag.Public) ||
                        Flag.hasFlags(m.getFlagsBitMap(), Flag.Protected)) ||
                        // Member is accessible as package-private.
                        !Flag.hasFlags(m.getFlagsBitMap(), Flag.Private) && isSamePackage) {
                    names.add(m.getName());
                }
            });
            addInheritedClassFields(classDeclaration, superClass.getSupertype(), names);
        }
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

        @Override
        public @Nullable J preVisit(J tree, Set<String> namesInScope) {
            // visit value from scope rather than `tree`, since calling recipe may have modified it already
            return scope.<J> getValue().isScope(tree) ? scope.getValue() : super.preVisit(tree, namesInScope);
        }

        // Stop after the tree has been processed to ensure all the names in scope have been collected.
        @Override
        public @Nullable J postVisit(J tree, Set<String> namesInScope) {
            if (!currentScope.isEmpty() && currentScope.peek().getValue().equals(tree)) {
                currentScope.pop();
            }

            if (scope.getValue().equals(tree)) {
                Cursor aggregatedScope = getCursor().getValue() instanceof JavaSourceFile ? getCursor() : aggregateNameScope();
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

            addImportedStaticFieldNames(getCursor().firstEnclosing(JavaSourceFile.class), getCursor());
            if (classDecl.getType() != null) {
                namesInScope.addAll(findInheritedNames(classDecl));
            }
            return super.visitClassDeclaration(classDecl, namesInScope);
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, Set<String> strings) {
            if (instanceOf.getPattern() instanceof J.Identifier) {
                Set<String> names = nameScopes.get(currentScope.peek());
                if (names != null) {
                    names.add(((J.Identifier)instanceOf.getPattern()).getSimpleName());
                }
            }
            return super.visitInstanceOf(instanceOf, strings);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<String> strings) {
            Set<String> names = nameScopes.get(currentScope.peek());
            if (names != null) {
                names.add(variable.getSimpleName());
            }
            return super.visitVariable(variable, strings);
        }

        private void addImportedStaticFieldNames(@Nullable JavaSourceFile cu, Cursor classCursor) {
            if (cu != null) {
                List<J.Import> imports = cu.getImports();
                imports.forEach(i -> {
                    if (i.isStatic()) {
                        // Note: Currently, adds all statically imported identifiers including method and classes rather than restricting the names to static fields.
                        Set<String> namesAtCursor = nameScopes.computeIfAbsent(classCursor, k -> new HashSet<>());
                        if (isValidImportName(i.getQualid().getTarget().getType(), i.getQualid().getSimpleName())) {
                            namesAtCursor.add(i.getQualid().getSimpleName());
                        }
                    }
                });
            }
        }

        private boolean isValidImportName(@Nullable JavaType targetType, String name) {
            // Consider the id a valid field if the type is null since it is indistinguishable from a method name or class name.
            return targetType == null || (targetType instanceof JavaType.FullyQualified && ((JavaType.FullyQualified) targetType).getMembers().stream().anyMatch(o -> o.getName().equals(name)));
        }
    }

    static final class JavaKeywords {
        JavaKeywords() {
        }

        private static final String[] RESERVED_WORDS = new String[]{
                "abstract",
                "assert",
                "boolean",
                "break",
                "byte",
                "case",
                "catch",
                "char",
                "class",
                "const",
                "continue",
                "default",
                "do",
                "double",
                "else",
                "enum",
                "extends",
                "final",
                "finally",
                "float",
                "for",
                "goto",
                "if",
                "implements",
                "import",
                "instanceof",
                "int",
                "interface",
                "long",
                "native",
                "new",
                "package",
                "private",
                "protected",
                "public",
                "return",
                "short",
                "static",
                "strictfp",
                "super",
                "switch",
                "synchronized",
                "this",
                "throw",
                "throws",
                "transient",
                "try",
                "void",
                "volatile",
                "while",
        };

        private static final Set<String> RESERVED_WORDS_SET = new HashSet<>(Arrays.asList(RESERVED_WORDS));

        public static boolean isReserved(String word) {
            return RESERVED_WORDS_SET.contains(word);
        }
    }
}
