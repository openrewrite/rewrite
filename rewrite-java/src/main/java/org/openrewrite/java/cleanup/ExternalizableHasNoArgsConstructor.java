/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ExternalizableHasNoArgsConstructor extends Recipe {

    @Override
    public String getDisplayName() {
        return "`Externalizable` classes have no-arguments constructor";
    }

    @Override
    public String getDescription() {
        return "`Externalizable` classes handle both serialization and deserialization and must have a no-args constructor for the deserialization process.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(20);
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2060");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("java.io.Externalizable", false),
                new ExternalizableHasNoArgsConstructorVisitor());
    }

    private static class ExternalizableHasNoArgsConstructorVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final JavaType externalizableType = JavaType.buildType("java.io.Externalizable");

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (TypeUtils.isAssignableTo(externalizableType, cd.getType())) {
                boolean hasFinalUninitializedFieldVar = false;
                Integer firstMethodDeclarationIndex = null;
                List<Statement> statements = cd.getBody().getStatements();

                for (int i = 0; i < statements.size(); i++) {
                    Statement statement = statements.get(i);
                    if (statement instanceof J.VariableDeclarations) {
                        J.VariableDeclarations varDecls = (J.VariableDeclarations) statement;
                        if (J.Modifier.hasModifier(varDecls.getModifiers(), J.Modifier.Type.Final)
                            && varDecls.getVariables().stream().anyMatch(v -> v.getInitializer() == null)) {
                            hasFinalUninitializedFieldVar = true;
                            break;
                        }
                    }
                    // The new no-args constructor should be the first methodDeclaration
                    if (statement instanceof J.MethodDeclaration && firstMethodDeclarationIndex == null) {
                        firstMethodDeclarationIndex = i;
                    }
                }
                if (!hasFinalUninitializedFieldVar && !hasNoArgsConstructor(cd) && parentClassHasNoArgsConstructor(cd)) {
                    cd = cd.withTemplate(JavaTemplate.builder(this::getCursor, "public " + cd.getSimpleName() + "() {}").build(), cd.getBody().getCoordinates().lastStatement());
                    if (firstMethodDeclarationIndex != null) {
                        statements.add(firstMethodDeclarationIndex, cd.getBody().getStatements().remove(cd.getBody().getStatements().size() - 1));
                        cd = cd.withBody(cd.getBody().withStatements(statements));
                    }
                }
            }
            return cd;
        }

        private boolean hasNoArgsConstructor(J.ClassDeclaration cd) {
            boolean hasNoArgsConstructor = false;
            boolean hasDefaultConstructor = true;
            for (Statement statement : cd.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration) {
                    J.MethodDeclaration md = (J.MethodDeclaration) statement;
                    if (md.isConstructor()) {
                        if (md.getParameters().isEmpty() || md.getParameters().get(0) instanceof J.Empty) {
                            hasNoArgsConstructor = true;
                        } else {
                            hasDefaultConstructor = false;
                        }
                    }
                }
            }
            return hasDefaultConstructor || hasNoArgsConstructor;
        }

        private boolean parentClassHasNoArgsConstructor(J.ClassDeclaration cd) {
            if (cd.getExtends() == null) {
                return true;
            }

            JavaType.FullyQualified parentFq = TypeUtils.asFullyQualified(cd.getExtends().getType());
            if (parentFq == null) {
                return false;
            }

            boolean hasNoArgsConstructor = false;
            boolean hasDefaultConstructor = true;

            for (JavaType.Method method : parentFq.getMethods()) {
                if ("<constructor>".equals(method.getName())) {
                    if (method.getParameterNames().isEmpty()) {
                        hasNoArgsConstructor = true;
                    } else {
                        hasDefaultConstructor = false;
                    }
                }
            }
            return hasDefaultConstructor || hasNoArgsConstructor;
        }
    }
}
