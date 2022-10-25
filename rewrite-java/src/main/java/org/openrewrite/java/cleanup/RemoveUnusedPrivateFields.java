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
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveUnusedPrivateFields extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove unused private fields";
    }

    @Override
    public String getDescription() {
        return "If a private field is declared but not used in the program, it can be considered dead code and should therefore be removed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1068");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);

                List<J.VariableDeclarations> checkFields = new ArrayList<>();
                // Do not remove fields with `serialVersionUID` name.
                boolean skipSerialVersionUID = cd.getType() == null ||
                        cd.getType().isAssignableTo("java.io.Serializable");
                for (Statement statement : cd.getBody().getStatements()) {
                    if (statement instanceof J.VariableDeclarations) {
                        J.VariableDeclarations vd = (J.VariableDeclarations) statement;
                        // RSPEC-1068 does not apply serialVersionUID of Serializable classes, or fields with annotations.
                        if (!(skipSerialVersionUID && isSerialVersionUid(vd)) &&
                                vd.getLeadingAnnotations().isEmpty() &&
                                vd.hasModifier(J.Modifier.Type.Private)) {
                            checkFields.add(vd);
                        }
                    } else if (statement instanceof J.MethodDeclaration) {
                        // RSPEC-1068 does not apply fields from classes with native methods.
                        J.MethodDeclaration md = (J.MethodDeclaration) statement;
                        if (md.hasModifier(J.Modifier.Type.Native)) {
                            return cd;
                        }
                    }
                }

                if (checkFields.isEmpty()) {
                    return cd;
                }

                for (J.VariableDeclarations fields : checkFields) {
                    // Find variable uses.
                    Map<J.VariableDeclarations.NamedVariable, List<J.Identifier>> inUse = VariableUses.find(fields, cd);
                    for (Map.Entry<J.VariableDeclarations.NamedVariable, List<J.Identifier>> entry : inUse.entrySet()) {
                        if (entry.getValue().isEmpty()) {
                            cd = (J.ClassDeclaration) new RemoveUnusedField(entry.getKey()).visitNonNull(cd, executionContext);
                        }
                    }
                }

                return cd;
            }

            private boolean isSerialVersionUid(J.VariableDeclarations vd) {
                return vd.hasModifier(J.Modifier.Type.Private) &&
                        vd.hasModifier(J.Modifier.Type.Static) &&
                        vd.hasModifier(J.Modifier.Type.Final) &&
                        TypeUtils.isOfClassType(vd.getType(), "long") &&
                        vd.getVariables().stream().anyMatch(it -> "serialVersionUID".equals(it.getSimpleName()));
            }
        };
    }

    private static class VariableUses {
        public static Map<J.VariableDeclarations.NamedVariable, List<J.Identifier>> find(J.VariableDeclarations declarations, J.ClassDeclaration parent) {
            Map<J.VariableDeclarations.NamedVariable, List<J.Identifier>> found = new IdentityHashMap<>(declarations.getVariables().size());
            Map<String, J.VariableDeclarations.NamedVariable> signatureMap = new HashMap<>();

            for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                if (variable.getVariableType() != null) {
                    found.computeIfAbsent(variable, k -> new ArrayList<>());
                    // Note: Using a variable type signature is only safe to find uses of class fields.
                    signatureMap.put(variable.getVariableType().toString(), variable);
                }
            }

            JavaIsoVisitor<Map<J.VariableDeclarations.NamedVariable, List<J.Identifier>>> visitor =
                    new JavaIsoVisitor<Map<J.VariableDeclarations.NamedVariable, List<J.Identifier>>>() {

                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier,
                                                    Map<J.VariableDeclarations.NamedVariable, List<J.Identifier>> identifiers) {
                    if (identifier.getFieldType() != null && signatureMap.containsKey(identifier.getFieldType().toString())) {
                        Cursor parent = getCursor().dropParentUntil(is ->
                                is instanceof J.VariableDeclarations ||
                                is instanceof J.ClassDeclaration);

                        if (!(parent.getValue() instanceof J.VariableDeclarations && parent.getValue() == declarations)) {
                            J.VariableDeclarations.NamedVariable name = signatureMap.get(identifier.getFieldType().toString());
                            if (declarations.getVariables().contains(name)) {
                                J.VariableDeclarations.NamedVariable used = signatureMap.get(identifier.getFieldType().toString());
                                identifiers.computeIfAbsent(used, k -> new ArrayList<>())
                                        .add(identifier);
                            }
                        }
                    }
                    return super.visitIdentifier(identifier, identifiers);
                }
            };

            visitor.visit(parent, found);
            return found;
        }
    }

    private static class RemoveUnusedField extends JavaVisitor<ExecutionContext> {
        private final J.VariableDeclarations.NamedVariable namedVariable;

        public RemoveUnusedField(J.VariableDeclarations.NamedVariable namedVariable) {
            this.namedVariable = namedVariable;
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            if (multiVariable.getVariables().size() == 1 && multiVariable.getVariables().contains(namedVariable)) {
                //noinspection ConstantConditions
                return null;
            }
            return super.visitVariableDeclarations(multiVariable, executionContext);
        }

        @Override
        public J visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
            if (variable == namedVariable) {
                //noinspection ConstantConditions
                return null;
            }
            return super.visitVariable(variable, executionContext);
        }
    }
}
