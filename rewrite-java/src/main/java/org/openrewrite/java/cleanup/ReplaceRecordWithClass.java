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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesJavaVersion<>(14);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceRecordWithClassVisitor();
    }

    private static class ReplaceRecordWithClassVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final JavaTemplate fieldTemplate = JavaTemplate
                .builder(this::getCursor, "private static final #{} #{};").build();

        private final JavaTemplate constructorTemplate = JavaTemplate
                .builder(this::getCursor, "public #{}(#{}) { #{} }").build();

        private final JavaTemplate getterTemplate = JavaTemplate
                .builder(this::getCursor, "public #{} #{}() { return #{}; }").build();

        private final JavaTemplate equalsTemplate = JavaTemplate
                .builder(this::getCursor,
                        "@Override\n" +
                        "public boolean equals(Object obj) {\n" +
                        "    if (this == obj) { return true; }\n" +
                        "    if (obj == null) { return false; }\n" +
                        "    if (getClass() != obj.getClass()) { return false; }\n" +
                        "    #{} other = (#{}) obj;\n" +
                        "    return #{};\n" +
                        "}\n")
                .imports("java.util.Objects")
                .build();

        private final JavaTemplate hashCodeTemplate = JavaTemplate
                .builder(this::getCursor,
                        "@Override\n" +
                        "public int hashCode() {\n" +
                        "    return Objects.hash(#{});\n" +
                        "}\n")
                .imports("java.util.Objects")
                .build();

        private final JavaTemplate toStringTemplate = JavaTemplate
                .builder(this::getCursor,
                        "@Override\n" +
                        "public String toString() {\n" +
                        "    return \"#{}[#{}]\";\n" +
                        "}\n")
                .build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration result = super.visitClassDeclaration(classDecl, ctx);
            if (result.getKind() != J.ClassDeclaration.Kind.Type.Record) {
                return result;
            }

            // Records with statements not supported yet
            if (!result.getBody().getStatements().isEmpty()) {
                return result;
            }

            // Get record fields
            List<J.VariableDeclarations> fields = Objects.requireNonNull(result.getPrimaryConstructor()).stream()
                    .map(J.VariableDeclarations.class::cast)
                    .collect(Collectors.toList());

            // Change a kind from record to class
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

            // Add fields
            for (J.VariableDeclarations field : fields) {
                result = result.withBody(result.getBody()
                        .withTemplate(fieldTemplate,
                                result.getBody().getCoordinates().lastStatement(),
                                Objects.requireNonNull(field.getTypeExpression()).toString(),
                                field.getVariables().get(0).getSimpleName()));
            }

            // Remove static modifiers from fields.
            // Something is broken in template for non-static fields,
            // so we have to add static fields first and then remove static modifiers
            result = result.withBody(result.getBody().withStatements(result.getBody().getStatements().stream()
                    .map(J.VariableDeclarations.class::cast)
                    .map(fieldDeclaration -> fieldDeclaration.withModifiers(
                            ListUtils.map(fieldDeclaration.getModifiers(),
                                    m -> m.getType() != J.Modifier.Type.Static ? m : null)))
                    .collect(Collectors.toList())));

            // Add a constructor
            result = result.withBody(result.getBody()
                    .withTemplate(constructorTemplate,
                            result.getBody().getCoordinates().lastStatement(),
                            result.getSimpleName(),
                            fields.stream()
                                    .map(this::createConstructorParameter)
                                    .collect(Collectors.joining(", ")),
                            fields.stream()
                                    .map(this::createFieldAssignment)
                                    .collect(Collectors.joining("\n"))));

            // Add getters
            for (J.VariableDeclarations field : fields) {
                result = result.withBody(result.getBody()
                        .withTemplate(getterTemplate,
                                result.getBody().getCoordinates().lastStatement(),
                                Objects.requireNonNull(field.getTypeExpression()).toString(),
                                field.getVariables().get(0).getSimpleName(),
                                field.getVariables().get(0).getSimpleName()));
            }

            // Add equals() method
            result = result.withBody(result.getBody()
                    .withTemplate(equalsTemplate,
                            result.getBody().getCoordinates().lastStatement(),
                            result.getSimpleName(),
                            result.getSimpleName(),
                            fields.stream()
                                    .map(this::createFieldComparison)
                                    .collect(Collectors.joining(" && "))));

            // Add hashCode() method
            result = result.withBody(result.getBody()
                    .withTemplate(hashCodeTemplate,
                            result.getBody().getCoordinates().lastStatement(),
                            fields.stream()
                                    .map(field -> field.getVariables().get(0).getSimpleName())
                                    .collect(Collectors.joining(", "))));

            // Add toString() method
            result = result.withBody(result.getBody()
                    .withTemplate(toStringTemplate,
                            result.getBody().getCoordinates().lastStatement(),
                            result.getSimpleName(),
                            fields.stream()
                                    .map(this::createFieldPrinting)
                                    .collect(Collectors.joining(", "))));

            maybeAddImport("java.util.Objects");

            return autoFormat(result, ctx);
        }

        private String createConstructorParameter(J.VariableDeclarations field) {
            String fieldType = Objects.requireNonNull(field.getTypeExpression()).toString();
            String fieldName = field.getVariables().get(0).getSimpleName();
            return String.format("%s %s", fieldType, fieldName);
        }

        private String createFieldAssignment(J.VariableDeclarations field) {
            String fieldName = field.getVariables().get(0).getSimpleName();
            return String.format("this.%s = %s", fieldName, fieldName);
        }

        private String createFieldComparison(J.VariableDeclarations field) {
            String fieldName = field.getVariables().get(0).getSimpleName();
            if (field.getType() instanceof JavaType.Primitive) {
                return String.format("%s == other.%s", fieldName, fieldName);
            } else {
                return String.format("Objects.equals(%s, other.%s)", fieldName, fieldName);
            }
        }

        private String createFieldPrinting(J.VariableDeclarations field) {
            String fieldName = field.getVariables().get(0).getSimpleName();
            return String.format("%s=\" + %s + \"", fieldName, fieldName);
        }

    }

}
