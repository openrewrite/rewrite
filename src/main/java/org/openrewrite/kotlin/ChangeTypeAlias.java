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
package org.openrewrite.kotlin;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.VariableNameUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.marker.Modifier;
import org.openrewrite.kotlin.tree.K;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeTypeAlias extends Recipe {

    @Option(displayName = "Old alias name",
            description = "Name of the alias type.",
            example = "OldAlias")
    String aliasName;

    @Option(displayName = "New alias name",
            description = "Name of the alias type.",
            example = "NewAlias")
    String newName;

    @Option(displayName = "Target fully qualified type",
            description = "Fully-qualified class name of the aliased type.",
            example = "org.junit.Assume")
    String fullyQualifiedAliasedType;

    @Override
    public String getDisplayName() {
        return "Change type alias";
    }

    @Override
    public String getDescription() {
        return "Change a given type alias to another.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinIsoVisitor<ExecutionContext>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext executionContext) {
                K.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
                J.VariableDeclarations.NamedVariable variable = getCursor().pollMessage("RENAME_VARIABLE");
                if (variable != null) {
                    String uniqueName = VariableNameUtils.generateVariableName(newName, getCursor(), VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER);
                    c = (K.CompilationUnit) new RenameVariable<>(variable, uniqueName).visit(c, executionContext, getCursor());
                    assert c != null;
                }
                return c;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                if (isTypeAlias(multiVariable.getLeadingAnnotations()) && TypeUtils.isOfClassType(multiVariable.getType(), fullyQualifiedAliasedType)) {
                    return super.visitVariableDeclarations(multiVariable, executionContext);
                }
                return multiVariable;
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                if (aliasName.equals(variable.getSimpleName())) {
                    getCursor().putMessageOnFirstEnclosing(K.CompilationUnit.class, "RENAME_VARIABLE", variable);
                }
                return variable;
            }

            private boolean isTypeAlias(List<J.Annotation> annotationList) {
                return annotationList.stream()
                        .anyMatch(a -> "typealias".equals(a.getSimpleName()) && a.getMarkers().findFirst(Modifier.class).isPresent());
            }
        };
    }
}
