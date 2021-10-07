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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.ChangeFieldName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ReplaceDuplicateStringLiterals extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace duplicate `String` literals";
    }

    @Override
    public String getDescription() {
        return "Replaces `String` literals with a length of 5 or greater repeated a minimum of 3 times. Qualified `String` literals include final Strings, method invocations, and new class invocations. Adds a new `private static final String` or uses an existing equivalent class field. A new variable name will be generated based on the literal value if an existing field does not exist. The generated name will appends a numeric value to the variable name if a name already exists in the compilation unit.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1192");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Nullable
    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("java.lang.String");
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, executionContext);
                if (cd.getType() == null) {
                    return classDecl;
                }

                Map<String, Set<J.Literal>> duplicateLiteralsMap = FindDuplicateStringLiterals.find(cd);
                if (duplicateLiteralsMap.isEmpty()) {
                    return cd;
                }

                Set<String> variableNames = FindVariableNames.find(cd);
                Map<String, String> fieldValueToFieldName = FindExistingPrivateStaticFinalFields.find(cd, cd);

                for (String valueOfLiteral : duplicateLiteralsMap.keySet()) {
                    String variableName;
                    if (fieldValueToFieldName.containsKey(valueOfLiteral)) {
                        String classFieldName = fieldValueToFieldName.get(valueOfLiteral);
                        variableName = getNameWithoutShadow(classFieldName, variableNames);
                        if (!classFieldName.equals(variableName)) {
                            assert cd.getType() != null;
                            doAfterVisit(new ChangeFieldName<>(JavaType.Class.build(cd.getType().getFullyQualifiedName()), classFieldName, variableName));
                        }
                    } else {
                        variableName = getNameWithoutShadow(transformToVariableName(valueOfLiteral), variableNames);
                        J.Literal replaceLiteral = ((J.Literal) duplicateLiteralsMap.get(valueOfLiteral).toArray()[0]).withId(Tree.randomId());
                        cd = cd.withBody(
                                cd.getBody().withTemplate(
                                        JavaTemplate.builder(this::getCursor, "private static final String " + variableName + " = #{any(String)}").build(),
                                        cd.getBody().getCoordinates().firstStatement(), replaceLiteral));
                    }

                    doAfterVisit(new ReplaceStringLiterals(cd, variableName, duplicateLiteralsMap.get(valueOfLiteral)));
                }
                return cd;
            }

            /**
             * Generate a variable name that does not create a name space conflict.
             * @param name variable name to replace duplicate literals with.
             * @param variableNames variable names that exist in the compilation unit.
             * @return unique variable name.
             */
            private String getNameWithoutShadow(String name, Set<String> variableNames) {
                int append = 0;
                String newName;
                while (true) {
                    newName = transformToVariableName(name) + (append == 0 ? "" : "_" + append);
                    if (!variableNames.contains(newName)) {
                        break;
                    }
                    append++;
                }
                return newName;
            }

            /**
             * Convert a `String` value to a variable name with naming convention of all caps delimited by `_`.
             * Special characters are filtered out to meet regex convention: ^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$
             */
            private String transformToVariableName(String valueOfLiteral) {
                boolean prevIsLower = false;
                boolean prevIsCharacter = false;
                StringBuilder newName = new StringBuilder();
                for (int i = 0; i < valueOfLiteral.length(); i++) {
                    char c = valueOfLiteral.charAt(i);
                    if (i == 0 && Character.isDigit(c)) {
                        newName.append("A");
                    }
                    if (i > 0 && newName.lastIndexOf("_") != newName.length() - 1 &&
                            (Character.isUpperCase(c) && prevIsLower || !prevIsCharacter)) {
                        newName.append("_");
                    }
                    prevIsCharacter = Character.isLetterOrDigit(c);
                    if (!prevIsCharacter) {
                        continue;
                    }
                    newName.append(Character.toUpperCase(c));
                    prevIsLower = Character.isLowerCase(c);
                }
                return newName.toString();
            }
        };
    }

    private static class FindDuplicateStringLiterals extends JavaIsoVisitor<Map<String, Set<J.Literal>>> {
        private final J.ClassDeclaration searchInClass;

        private FindDuplicateStringLiterals(J.ClassDeclaration searchInClass) {
            this.searchInClass = searchInClass;
        }

        /**
         * Find duplicate `String` literals repeated 3 or more times and with a length of at least 3.
         * @param inClass subtree to search in.
         * @return `Map` of `String` literal values to the `J.Literal` AST elements.
         */
        public static Map<String, Set<J.Literal>> find(J.ClassDeclaration inClass) {
            // Comparator is reversed to add constants in alphabetical order.
            Map<String, Set<J.Literal>> literalsMap = new TreeMap<>(Comparator.reverseOrder());
            new FindDuplicateStringLiterals(inClass).visit(inClass, literalsMap);
            return literalsMap.entrySet().stream()
                    .filter(m -> literalsMap.get(m.getKey()).size() >= 3)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Map<String, Set<J.Literal>> literalsMap) {
            if (classDecl.equals(searchInClass)) {
                return super.visitClassDeclaration(classDecl, literalsMap);
            }
            return classDecl;
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, Map<String, Set<J.Literal>> literalsMap) {
            if (JavaType.Primitive.String.equals(literal.getType()) &&
                    literal.getValue() instanceof String &&
                    ((String) literal.getValue()).length() >= 5) {

                Cursor parent = getCursor().dropParentUntil(is -> is instanceof J.ClassDeclaration ||
                        is instanceof J.Annotation ||
                        is instanceof J.VariableDeclarations ||
                        is instanceof J.NewClass ||
                        is instanceof J.MethodInvocation);

                if ((parent.getValue() instanceof J.VariableDeclarations &&
                        ((J.VariableDeclarations) parent.getValue()).hasModifier(J.Modifier.Type.Final) &&
                        !(((J.VariableDeclarations) parent.getValue()).hasModifier(J.Modifier.Type.Private) && ((J.VariableDeclarations) parent.getValue()).hasModifier(J.Modifier.Type.Static))) ||
                        parent.getValue() instanceof J.NewClass ||
                        parent.getValue() instanceof J.MethodInvocation) {
                    literalsMap.computeIfAbsent(((String) literal.getValue()), k -> new HashSet<>());
                    literalsMap.get((String) literal.getValue()).add(literal);
                }
            }
            return literal;
        }
    }

    private static boolean isPrivateStaticFinalVariable(J.VariableDeclarations declaration) {
        return declaration.hasModifier(J.Modifier.Type.Private) &&
                declaration.hasModifier(J.Modifier.Type.Static) &&
                declaration.hasModifier(J.Modifier.Type.Final);
    }

    private static class FindVariableNames extends JavaIsoVisitor<Set<String>> {

        /**
         * Find all the variable names that exist in the provided subtree.
         * @param inClass subtree to search in.
         * @return variable names that exist in the subtree.
         */
        public static Set<String> find(J.ClassDeclaration inClass) {
            Set<String> variableNames = new HashSet<>();
            new FindVariableNames().visit(inClass, variableNames);
            return variableNames;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<String> variableNames) {
            Cursor parentScope = getCursor().dropParentUntil(is -> is instanceof J.ClassDeclaration || is instanceof J.MethodDeclaration);
            J.VariableDeclarations declaration = getCursor().firstEnclosing(J.VariableDeclarations.class);
            // `private static final String`(s) are handled separately by `FindExistingPrivateStaticFinalFields`.
            if ((parentScope.getValue() instanceof J.ClassDeclaration && declaration != null &&
                    !(isPrivateStaticFinalVariable(declaration) && variable.getInitializer() instanceof J.Literal &&
                            ((J.Literal) variable.getInitializer()).getValue() instanceof String)) ||
                    parentScope.getValue() instanceof J.MethodDeclaration) {
                variableNames.add(variable.getSimpleName());
            }
            return super.visitVariable(variable, variableNames);
        }
    }

    private static class FindExistingPrivateStaticFinalFields extends JavaIsoVisitor<Map<String, String>> {
        private final J.ClassDeclaration searchInClass;

        private FindExistingPrivateStaticFinalFields(J.ClassDeclaration searchInClass) {
            this.searchInClass = searchInClass;
        }

        /**
         * Find existing `private static final String`(s) in a class.
         */
        public static Map<String, String> find(J j, J.ClassDeclaration searchInClass) {
            Map<String, String> fieldValueToFieldName = new LinkedHashMap<>();
            new FindExistingPrivateStaticFinalFields(searchInClass).visit(j, fieldValueToFieldName);
            return fieldValueToFieldName;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Map<String, String> stringStringMap) {
            if (classDecl.equals(searchInClass)) {
                return super.visitClassDeclaration(classDecl, stringStringMap);
            }
            return classDecl;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Map<String, String> stringStringMap) {
            Cursor parentScope = getCursor().dropParentUntil(is -> is instanceof J.ClassDeclaration || is instanceof J.MethodDeclaration);
            J.VariableDeclarations declaration = getCursor().firstEnclosing(J.VariableDeclarations.class);
            if (parentScope.getValue() instanceof J.ClassDeclaration &&
                    declaration != null && isPrivateStaticFinalVariable(declaration) &&
                    variable.getInitializer() instanceof J.Literal &&
                    ((J.Literal) variable.getInitializer()).getValue() instanceof String) {
                String value = ((String) (((J.Literal) variable.getInitializer()).getValue()));
                stringStringMap.putIfAbsent(value, variable.getSimpleName());
            }
            return super.visitVariable(variable, stringStringMap);
        }
    }

    /**
     * ReplaceStringLiterals in a class with a reference to a `private static final String` with the provided variable name.
     */
    private static class ReplaceStringLiterals extends JavaVisitor<ExecutionContext> {
        private final J.ClassDeclaration isClass;
        private final String variableName;
        private final Set<J.Literal> literals;

        private ReplaceStringLiterals(J.ClassDeclaration isClass, String variableName, Set<J.Literal> literals) {
            this.isClass = isClass;
            this.variableName = variableName;
            this.literals = literals;
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            if (classDecl.equals(isClass)) {
                return super.visitClassDeclaration(classDecl, executionContext);
            }
            return classDecl;
        }

        @Override
        public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
            J.Literal l = (J.Literal) super.visitLiteral(literal, executionContext);
            if (literals.contains(literal)) {
                assert isClass.getType() != null;
                return J.Identifier.build(
                        Tree.randomId(),
                        l.getPrefix(),
                        l.getMarkers(),
                        variableName,
                        JavaType.Primitive.String,
                        JavaType.Variable.build(variableName,
                                isClass.getType(),
                                JavaType.Primitive.String,
                                Collections.emptyList(),
                                Flag.flagsToBitMap(new HashSet<>(Arrays.asList(Flag.Private, Flag.Static, Flag.Final)))));
            }
            return l;
        }
    }
}
