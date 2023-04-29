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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ReplaceDuplicateStringLiterals extends Recipe {

    @Option(displayName = "Apply recipe to test source set",
            description = "Changes only apply to main by default. `includeTestSources` will apply the recipe to `test` source files.",
            required = false)
    @Nullable
    Boolean includeTestSources;

    @Override
    public String getDisplayName() {
        return "Replace duplicate `String` literals";
    }

    @Override
    public String getDescription() {
        return "Replaces `String` literals with a length of 5 or greater repeated a minimum of 3 times. Qualified `String` literals include final Strings, method invocations, and new class invocations. Adds a new `private static final String` or uses an existing equivalent class field. A new variable name will be generated based on the literal value if an existing field does not exist. The generated name will append a numeric value to the variable name if a name already exists in the compilation unit.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1192");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("java.lang.String", false), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                Optional<JavaSourceSet> sourceSet = cu.getMarkers().findFirst(JavaSourceSet.class);
                if (Boolean.TRUE.equals(includeTestSources) || (sourceSet.isPresent() && "main".equals(sourceSet.get().getName()))) {
                    return super.visitJavaSourceFile(cu, executionContext);
                }
                return cu;
            }

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                if (classDecl.getType() == null) {
                    return classDecl;
                }

                Map<String, Set<J.Literal>> duplicateLiteralsMap = FindDuplicateStringLiterals.find(classDecl);
                if (duplicateLiteralsMap.isEmpty()) {
                    return classDecl;
                }

                Set<String> variableNames = FindVariableNames.find(classDecl);
                Map<String, String> fieldValueToFieldName = FindExistingPrivateStaticFinalFields.find(classDecl);

                String classFqn = classDecl.getType().getFullyQualifiedName();
                for (String valueOfLiteral : duplicateLiteralsMap.keySet()) {
                    String variableName;
                    if (fieldValueToFieldName.containsKey(valueOfLiteral)) {
                        String classFieldName = fieldValueToFieldName.get(valueOfLiteral);
                        variableName = getNameWithoutShadow(classFieldName, variableNames);
                        if (StringUtils.isBlank(variableName)) {
                            continue;
                        }
                        if (!classFieldName.equals(variableName)) {
                            doAfterVisit(new ChangeFieldName<>(classFqn, classFieldName, variableName));
                        }
                    } else {
                        variableName = getNameWithoutShadow(transformToVariableName(valueOfLiteral), variableNames);
                        if (StringUtils.isBlank(variableName)) {
                            continue;
                        }
                        J.Literal replaceLiteral = ((J.Literal) duplicateLiteralsMap.get(valueOfLiteral).toArray()[0]).withId(Tree.randomId());
                        String insertStatement = "private static final String " + variableName + " = #{any(String)};";
                        if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Enum) {
                            J.EnumValueSet enumValueSet = classDecl.getBody().getStatements().stream()
                                    .filter(it -> it instanceof J.EnumValueSet)
                                    .map(it -> (J.EnumValueSet) it)
                                    .findFirst()
                                    .orElse(null);

                            if (enumValueSet != null) {
                                // Temporary work around due to an issue in the JavaTemplate related to BlockStatementTemplateGenerator#enumClassDeclaration.
                                Space singleSpace = Space.build(" ", emptyList());
                                Expression literal = duplicateLiteralsMap.get(valueOfLiteral).toArray(new J.Literal[0])[0].withId(randomId());
                                J.Modifier privateModifier = new J.Modifier(randomId(), Space.build("\n", emptyList()), Markers.EMPTY, J.Modifier.Type.Private, emptyList());
                                J.Modifier staticModifier = new J.Modifier(randomId(), singleSpace, Markers.EMPTY, J.Modifier.Type.Static, emptyList());
                                J.Modifier finalModifier = new J.Modifier(randomId(), singleSpace, Markers.EMPTY, J.Modifier.Type.Final, emptyList());
                                J.VariableDeclarations variableDeclarations = autoFormat(new J.VariableDeclarations(
                                        randomId(),
                                        Space.EMPTY,
                                        Markers.EMPTY,
                                        emptyList(),
                                        Arrays.asList(privateModifier, staticModifier, finalModifier),
                                        new J.Identifier(
                                                randomId(),
                                                singleSpace,
                                                Markers.EMPTY,
                                                "String",
                                                JavaType.ShallowClass.build("java.lang.String"),
                                                null),
                                        null,
                                        emptyList(),
                                        singletonList(JRightPadded.build(new J.VariableDeclarations.NamedVariable(
                                                randomId(),
                                                Space.EMPTY,
                                                Markers.EMPTY,
                                                new J.Identifier(
                                                        randomId(),
                                                        Space.EMPTY,
                                                        Markers.EMPTY,
                                                        variableName,
                                                        JavaType.ShallowClass.build("java.lang.String"),
                                                        null),
                                                emptyList(),
                                                JLeftPadded.build(literal).withBefore(singleSpace),
                                                null)))
                                ), executionContext, new Cursor(getCursor(), classDecl.getBody()));

                                // Insert the new statement after the EnumValueSet.
                                List<Statement> statements = new ArrayList<>(classDecl.getBody().getStatements().size() + 1);
                                boolean addedNewStatement = false;
                                for (Statement statement : classDecl.getBody().getStatements()) {
                                    if (!(statement instanceof J.EnumValueSet) && !addedNewStatement) {
                                        statements.add(variableDeclarations);
                                        addedNewStatement = true;
                                    }
                                    statements.add(statement);
                                }
                                classDecl = classDecl.withBody(classDecl.getBody().withStatements(statements));
                            }
                        } else {
                            classDecl = classDecl.withBody(
                                    classDecl.getBody().withTemplate(
                                            JavaTemplate.builder(this::getCursor, insertStatement).build(),
                                            classDecl.getBody().getCoordinates().firstStatement(), replaceLiteral));
                        }
                    }
                    variableNames.add(variableName);
                    doAfterVisit(new ReplaceStringLiterals(classDecl, variableName, duplicateLiteralsMap.get(valueOfLiteral)));
                }
                return classDecl;
            }

            /**
             * Generate a variable name that does not create a name space conflict.
             * @param name variable name to replace duplicate literals with.
             * @param variableNames variable names that exist in the compilation unit.
             * @return unique variable name.
             */
            private String getNameWithoutShadow(String name, Set<String> variableNames) {
                String transformedName = transformToVariableName(name);
                String newName = transformedName;
                int append = 0;
                while (variableNames.contains(newName)) {
                    append++;
                    newName = transformedName + "_" + append;
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
                    if (i > 0 && newName.lastIndexOf("_") != newName.length() - 1 &&
                        (Character.isUpperCase(c) && prevIsLower || !prevIsCharacter)) {
                        newName.append("_");
                    }
                    prevIsCharacter = Character.isLetterOrDigit(c);
                    if (!prevIsCharacter) {
                        continue;
                    }
                    if (newName.length() == 0 && Character.isDigit(c)) {
                        newName.append("A_");
                    }
                    newName.append(Character.toUpperCase(c));
                    prevIsLower = Character.isLowerCase(c);
                }
                return VariableNameUtils.normalizeName(newName.toString());
            }
        });
    }

    private static class FindDuplicateStringLiterals extends JavaIsoVisitor<Map<String, Set<J.Literal>>> {

        /**
         * Find duplicate `String` literals repeated 3 or more times and with a length of at least 3.
         *
         * @param inClass subtree to search in.
         * @return `Map` of `String` literal values to the `J.Literal` AST elements.
         */
        public static Map<String, Set<J.Literal>> find(J.ClassDeclaration inClass) {
            Map<String, Set<J.Literal>> literalsMap = new HashMap<>();
            Map<String, Set<J.Literal>> filteredMap = new TreeMap<>(Comparator.reverseOrder());
            new FindDuplicateStringLiterals().visit(inClass, literalsMap);
            for (String valueOfLiteral : literalsMap.keySet()) {
                if (literalsMap.get(valueOfLiteral).size() >= 3) {
                    filteredMap.put(valueOfLiteral, literalsMap.get(valueOfLiteral));
                }
            }
            return filteredMap;
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
                // EnumValue can accept constructor arguments, including string literals
                // But the static field can't be placed before them, so these literals are ineligible for replacement
                if (parent.getValue() instanceof J.NewClass && parent.firstEnclosing(J.EnumValueSet.class) != null) {
                    return literal;
                }

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
         *
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
            if (parentScope.getValue() instanceof J.MethodDeclaration ||
                (parentScope.getValue() instanceof J.ClassDeclaration && declaration != null &&
                 // `private static final String`(s) are handled separately by `FindExistingPrivateStaticFinalFields`.
                 !(isPrivateStaticFinalVariable(declaration) && variable.getInitializer() instanceof J.Literal &&
                   ((J.Literal) variable.getInitializer()).getValue() instanceof String))) {
                variableNames.add(variable.getSimpleName());
            }
            return variable;
        }
    }

    private static class FindExistingPrivateStaticFinalFields extends JavaIsoVisitor<Map<String, String>> {

        /**
         * Find existing `private static final String`(s) in a class.
         */
        public static Map<String, String> find(J j) {
            Map<String, String> fieldValueToFieldName = new LinkedHashMap<>();
            new FindExistingPrivateStaticFinalFields().visit(j, fieldValueToFieldName);
            return fieldValueToFieldName;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Map<String, String> stringStringMap) {
            Cursor parentScope = getCursor().dropParentUntil(is -> is instanceof J.ClassDeclaration ||
                                                                   // Prevent checks on most of the literals.
                                                                   is instanceof J.MethodDeclaration);
            J.VariableDeclarations declaration = getCursor().firstEnclosing(J.VariableDeclarations.class);
            if (parentScope.getValue() instanceof J.ClassDeclaration &&
                declaration != null && isPrivateStaticFinalVariable(declaration) &&
                variable.getInitializer() instanceof J.Literal &&
                ((J.Literal) variable.getInitializer()).getValue() instanceof String) {
                String value = (String) (((J.Literal) variable.getInitializer()).getValue());
                stringStringMap.putIfAbsent(value, variable.getSimpleName());
            }
            return variable;
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
        public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
            if (literals.contains(literal)) {
                assert isClass.getType() != null;
                return new J.Identifier(
                        Tree.randomId(),
                        literal.getPrefix(),
                        literal.getMarkers(),
                        variableName,
                        JavaType.Primitive.String,
                        new JavaType.Variable(
                                null,
                                Flag.flagsToBitMap(new HashSet<>(Arrays.asList(Flag.Private, Flag.Static, Flag.Final))),
                                variableName,
                                isClass.getType(),
                                JavaType.Primitive.String,
                                emptyList()
                        )
                );
            }
            return literal;
        }
    }
}
