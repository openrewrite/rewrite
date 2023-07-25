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
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.marker.CompactConstructor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The recipe that replaces records with classes.
 */
public class ReplaceRecordWithClass extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace record with a class";
    }

    @Override
    public String getDescription() {
        return "Replace record with a class.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(14), new ReplaceRecordWithClassVisitor());
    }

    /**
     * The visitor implementing replacement of records with classes.
     */
    private static class ReplaceRecordWithClassVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final JavaTemplate fieldTemplate = JavaTemplate
                .builder("private final #{} #{};").build();

        private final JavaTemplate constructorTemplate = JavaTemplate
                .builder("public #{}(#{}) { #{} }").build();

        private final JavaTemplate getterTemplate = JavaTemplate
                .builder("public #{} #{}() { return #{}; }").build();

        private final JavaTemplate equalsTemplate = JavaTemplate
                .builder("@Override\n" +
                        "public boolean equals(Object obj) {\n" +
                        "    if (this == obj) { return true; }\n" +
                        "    if (obj == null || getClass() != obj.getClass()) { return false; }\n" +
                        "    #{} other = (#{}) obj;\n" +
                        "    return #{};\n" +
                        "}\n")
                .imports("java.util.Objects")
                .build();

        private final JavaTemplate hashCodeTemplate = JavaTemplate
                .builder("@Override\n" +
                        "public int hashCode() {\n" +
                        "    return Objects.hash(#{});\n" +
                        "}\n")
                .imports("java.util.Objects")
                .build();

        private final JavaTemplate toStringTemplate = JavaTemplate
                .builder("@Override\n" +
                        "public String toString() {\n" +
                        "    return \"#{}[#{}]\";\n" +
                        "}\n")
                .build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext ctx) {
            J.ClassDeclaration result = super.visitClassDeclaration(classDeclaration, ctx);
            if (result.getKind() != J.ClassDeclaration.Kind.Type.Record) {
                return result;
            }

            // Generic records not supported yet
            if (result.getTypeParameters() != null && !result.getTypeParameters().isEmpty()) {
                return result;
            }

            // Get record fields
            List<J.VariableDeclarations> fields = Objects.requireNonNull(result.getPrimaryConstructor()).stream()
                    .map(J.VariableDeclarations.class::cast)
                    .collect(Collectors.toList());

            // Change kind from record to class
            result = result.withKind(J.ClassDeclaration.Kind.Type.Class);
            if (result.getType() instanceof JavaType.Class) {
                result = result.withType(((JavaType.Class) result.getType())
                        .withKind(JavaType.FullyQualified.Kind.Class));
            }

            // Remove record parameters
            result = result.withPrimaryConstructor(null);

            // Add final modifier to the class
            result = result.withModifiers(ListUtils.concat(result.getModifiers(), new J.Modifier(
                    Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Final, Collections.emptyList())));

            // Replace a compact constructor by an all-args constructor if any
            result = result.withBody(result.getBody().withStatements(result.getBody().getStatements()
                    .stream()
                    .map(stmt -> mapCompactConstructor(stmt, fields))
                    .collect(Collectors.toList())));

            // We should preserve the following member order in the class (as possible):
            // 1) static final fields
            // 2) final fields
            // 3) all-args constructor
            // 4) other constructors
            // 5) getters
            // 6) other methods
            // 7) equals()
            // 8) hashCode()
            // 9) toString()
            // The record can already have some kinds of members, so we can't
            // just add all members to the end of the class definition
            // For instance, we use existing constructors as anchor points to insert new members
            // Maybe it would better to add equals/hashCode/toString after getters,
            // but it's hard to distinguish getters from other methods in the record

            // Check if the record has a canonical constructor
            // If it doesn't then an all-args constructor will be added later
            boolean hasCanonicalConstructor = false;
            // Let's find any first constructor
            // Later the all-args constructor will be added before it if required
            // The private fields will be added before first constructor as well
            J.MethodDeclaration firstConstructor = null;
            // If there are no any constructors in the record, then it will be
            // added before first method later
            J.MethodDeclaration firstMethod = null;
            // Let's find last constructor
            // Later all getters will be added after it
            J.MethodDeclaration lastConstructor = null;
            for (Statement stmt : result.getBody().getStatements()) {
                if (stmt instanceof J.MethodDeclaration) {
                    J.MethodDeclaration method = ((J.MethodDeclaration) stmt);
                    if (method.isConstructor()) {
                        if (firstConstructor == null) {
                            firstConstructor = method;
                        }
                        lastConstructor = method;
                        if (!hasCanonicalConstructor && isCanonicalConstructor(method, fields)) {
                            hasCanonicalConstructor = true;
                        }
                    } else {
                        if (firstMethod == null) {
                            firstMethod = method;
                        }
                    }
                }
            }

            // Add an all-args constructor if there is no a canonical constructor yet
            if (!hasCanonicalConstructor) {
                // The priorities for inserting a new constructor are as follows:
                // 1) Before first already existing constructor (non-canonical one)
                // 2) Before any first method
                // 3) As a last statement in the class declaration
                JavaCoordinates coordinates = firstConstructor != null
                        ? firstConstructor.getCoordinates().before()
                        : firstMethod != null
                        ? firstMethod.getCoordinates().before()
                        : result.getBody().getCoordinates().lastStatement();
                // TODO Word "Template" added between public and a constructor name
                result = constructorTemplate.apply(updateCursor(result),
                        coordinates,
                        result.getSimpleName(),
                        fields.stream()
                                .map(this::createConstructorParameter)
                                .collect(Collectors.joining(", ")),
                        fields.stream()
                                .map(this::createFieldAssignment)
                                .collect(Collectors.joining("\n")));
                // TODO Is there a simpler way to get a just added constructor?
                // If the record didn't have any constructors yet, then the added all-args
                // constructor will be the first and last constructor in the class
                // It will be used later as an anchor point to insert fields and getters
                for (Statement stmt : result.getBody().getStatements()) {
                    if (stmt instanceof J.MethodDeclaration) {
                        J.MethodDeclaration method = ((J.MethodDeclaration) stmt);
                        if (method.isConstructor()) {
                            firstConstructor = method;
                            break;
                        }
                    }
                }
                if (lastConstructor == null) {
                    lastConstructor = firstConstructor;
                }
            }

            // Add fields before the first constructor
            for (J.VariableDeclarations field : fields) {
                result = fieldTemplate.apply(updateCursor(result),
                        firstConstructor.getCoordinates().before(),
                        Objects.requireNonNull(field.getTypeExpression()).toString(),
                        field.getVariables().get(0).getSimpleName());
            }

            // Add getters after the last constructor
            for (int i = fields.size() - 1; i >= 0; i--) {
                J.VariableDeclarations field = fields.get(i);
                String fieldName = field.getVariables().get(0).getSimpleName();
                // TODO Add @Override if required
                // TODO Check parameter types
                if (!hasMethod(result, fieldName)) {
                    result = getterTemplate.apply(updateCursor(result),
                            lastConstructor.getCoordinates().after(),
                            Objects.requireNonNull(field.getTypeExpression()).toString(),
                            fieldName,
                            fieldName);
                }
            }

            // Add equals() method
            if (!hasMethod(result, "equals", JavaType.buildType("java.lang.Object"))) {
                result = equalsTemplate.apply(updateCursor(result),
                        result.getBody().getCoordinates().lastStatement(),
                        result.getSimpleName(),
                        result.getSimpleName(),
                        fields.stream()
                                .map(this::createFieldComparison)
                                .collect(Collectors.joining(" && ")));
            }

            // Add hashCode() method
            if (!hasMethod(result, "hashCode")) {
                result = hashCodeTemplate.apply(updateCursor(result),
                        result.getBody().getCoordinates().lastStatement(),
                        fields.stream()
                                .map(field -> field.getVariables().get(0).getSimpleName())
                                .collect(Collectors.joining(", ")));
            }

            // Add toString() method
            if (!hasMethod(result, "toString")) {
                result = toStringTemplate.apply(updateCursor(result),
                        result.getBody().getCoordinates().lastStatement(),
                        result.getSimpleName(),
                        fields.stream()
                                .map(this::createFieldPrinting)
                                .collect(Collectors.joining(", ")));
            }

            maybeAddImport("java.util.Objects");

            return autoFormat(result, ctx);
        }

        /**
         * Maps a compact constructor to a canonical (all-args) constructor. All other
         * statements are left as is.
         *
         * @param statement the statement to map
         * @param fields the record fields
         * @return the mapped statement
         */
        private Statement mapCompactConstructor(Statement statement, List<J.VariableDeclarations> fields) {
            if (statement instanceof J.MethodDeclaration) {
                J.MethodDeclaration constructor = (J.MethodDeclaration) statement;
                if (constructor.getMarkers().findFirst(CompactConstructor.class).isPresent()) {
                    constructor = constructor
                            .withMarkers(constructor.getMarkers().removeByType(CompactConstructor.class))
                            .withParameters(fields.stream().map(Statement.class::cast).collect(Collectors.toList()));
                    for (J.VariableDeclarations field : fields) {
                        constructor = JavaTemplate.apply(createFieldAssignment(field),
                                new Cursor(getCursor(), constructor),
                                Objects.requireNonNull(constructor.getBody()).getCoordinates().lastStatement());
                    }
                    return constructor;
                }
            }
            return statement;
        }

        /**
         * Checks if a constructor is canonical.
         *
         * @param constructor the constructor to check
         * @param fields the record fields
         * @return true, if successful
         */
        private boolean isCanonicalConstructor(J.MethodDeclaration constructor, List<J.VariableDeclarations> fields) {
            JavaType[] parameterTypes = fields.stream()
                    .map(J.VariableDeclarations::getType)
                    .toArray(JavaType[]::new);
            return hasParameterTypes(constructor, parameterTypes);
        }

        /**
         * Checks if a class or record has a specified method.
         *
         * @param classDeclaration the class declaration
         * @param methodName the method name
         * @param parameterTypes the method parameter types
         * @return true, if successful
         */
        private boolean hasMethod(J.ClassDeclaration classDeclaration, String methodName, JavaType... parameterTypes) {
            for (Statement stmt : classDeclaration.getBody().getStatements()) {
                if (stmt instanceof J.MethodDeclaration) {
                    J.MethodDeclaration method = (J.MethodDeclaration) stmt;
                    if (methodName.equals(method.getSimpleName()) && hasParameterTypes(method, parameterTypes)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Checks if a method has specified parameter types.
         *
         * @param method the method to check
         * @param types the parameter types
         * @return true, if successful
         */
        private boolean hasParameterTypes(J.MethodDeclaration method, JavaType[] types) {
            JavaType[] parameterTypes = method.getParameters().stream()
                    .filter(J.VariableDeclarations.class::isInstance)
                    .map(J.VariableDeclarations.class::cast)
                    .map(J.VariableDeclarations::getType)
                    .toArray(JavaType[]::new);
            if (parameterTypes.length != types.length) {
                return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!areEqualTypes(parameterTypes[i], types[i])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks if two types are equal.
         *
         * @param a the first type
         * @param b the second type
         * @return true, if successful
         */
        private boolean areEqualTypes(JavaType a, JavaType b) {
            // JavaType.buildType() creates JavaType.ShallowClass
            // But JavaType.Class can be equal to JavaType.Class only
            // So we use the custom equality test
            if (a instanceof JavaType.Class && b instanceof JavaType.Class) {
                JavaType.Class classA = (JavaType.Class) a;
                JavaType.Class classB = (JavaType.Class) b;
                return TypeUtils.fullyQualifiedNamesAreEqual(
                        classA.getFullyQualifiedName(),
                        classB.getFullyQualifiedName()) &&
                        classA.getTypeParameters().equals(classB.getTypeParameters());
            }
            return a.equals(b);
        }

        /**
         * Creates the constructor parameter.
         *
         * @param field the field
         * @return the string
         */
        private String createConstructorParameter(J.VariableDeclarations field) {
            String fieldType = Objects.requireNonNull(field.getTypeExpression()).toString();
            String fieldName = field.getVariables().get(0).getSimpleName();
            return String.format("%s %s", fieldType, fieldName);
        }

        /**
         * Creates the field assignment.
         *
         * @param field the field
         * @return the string
         */
        private String createFieldAssignment(J.VariableDeclarations field) {
            String fieldName = field.getVariables().get(0).getSimpleName();
            return String.format("this.%s = %s;", fieldName, fieldName);
        }

        /**
         * Creates the field comparison.
         *
         * @param field the field
         * @return the string
         */
        private String createFieldComparison(J.VariableDeclarations field) {
            String fieldName = field.getVariables().get(0).getSimpleName();
            if (field.getType() instanceof JavaType.Primitive) {
                return String.format("%s == other.%s", fieldName, fieldName);
            } else {
                return String.format("Objects.equals(%s, other.%s)", fieldName, fieldName);
            }
        }

        /**
         * Creates the field printing.
         *
         * @param field the field
         * @return the string
         */
        private String createFieldPrinting(J.VariableDeclarations field) {
            String fieldName = field.getVariables().get(0).getSimpleName();
            // TODO The following format should be used (without parentheses)
            // But in that case some of the tests are failed with
            // java.lang.IllegalStateException: AST contains missing or invalid type information
            // return String.format("%s=\" + %s + \"", fieldName, fieldName);
            return String.format("%s=\" + (%s) + \"", fieldName, fieldName);
        }

    }

}
