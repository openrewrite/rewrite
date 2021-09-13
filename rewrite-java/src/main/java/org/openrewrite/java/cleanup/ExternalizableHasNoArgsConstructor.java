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
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ExternalizableHasNoArgsConstructor extends Recipe {

    @Override
    public String getDisplayName() {
        return "`Externalizable` classes must have a no-arguments constructor";
    }

    @Override
    public String getDescription() {
        return "`Externalizable` classes handle both serialization and deserialization and must have a no-args constructor for the deserialization process.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2060");
    }

    @Override
    protected UsesType<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("java.io.Externalizable");
    }

    @Override
    protected ExternalizableHasNoArgsConstructorVisitor getVisitor() {
        return new ExternalizableHasNoArgsConstructorVisitor();
    }

    private static class ExternalizableHasNoArgsConstructorVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final JavaType externalizableType = JavaType.buildType("java.io.Externalizable");

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (TypeUtils.isAssignableTo(externalizableType, cd.getType())) {
                boolean hasNoArgsConstructor = false;
                boolean hasDefaultConstructor = true;
                int firstMethodDeclarationIndex = 0;
                List<Statement> statements = cd.getBody().getStatements();
                for (int i = 0; i < statements.size(); i++) {
                    Statement statement = statements.get(i);
                    if (statement instanceof J.MethodDeclaration) {
                        J.MethodDeclaration md = (J.MethodDeclaration) statement;
                        if (md.isConstructor()) {
                            if (md.getParameters().isEmpty() || (md.getParameters().size() == 1 && md.getParameters().get(0) instanceof J.Empty)) {
                                hasNoArgsConstructor = true;
                            } else {
                                hasDefaultConstructor = false;
                            }
                        }
                        if (firstMethodDeclarationIndex == 0) {
                            firstMethodDeclarationIndex = i;
                        }
                    }
                }
                if (!hasNoArgsConstructor && !hasDefaultConstructor) {
                    cd = cd.withTemplate(JavaTemplate.builder(this::getCursor, "public " + cd.getSimpleName() + "() {}").build(), cd.getBody().getCoordinates().lastStatement());
                    statements.add(firstMethodDeclarationIndex, cd.getBody().getStatements().remove(cd.getBody().getStatements().size() - 1));
                    cd = cd.withBody(cd.getBody().withStatements(statements));
                }
            }
            return cd;
        }
    }
}
