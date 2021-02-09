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
package org.openrewrite.java;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChangePackage extends Recipe {
    /**
     * Fully-qualified package name of the old package.
     */
    private final String oldFullyQualifiedPackageName;

    /**
     * Fully-qualified package name of the replacement package.
     */
    private final String newFullyQualifiedPackageName;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            {
                setCursoringOn();
            }

            final JavaTemplate newPackageExpr = template("package " + newFullyQualifiedPackageName)
                    .build();

            @Override
            public J.Package visitPackage(J.Package pkg, ExecutionContext context) {
                if (pkg.getExpression().printTrimmed()
                        .replaceAll("\\s", "")
                        .equals(oldFullyQualifiedPackageName)) {
                    getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "changing", true);
                    return pkg.withTemplate(newPackageExpr, pkg.getCoordinates().replace());
                }
                return pkg;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext context) {
                Boolean changing = getCursor().<Boolean>peekNearestMessage("changing");
                if(changing != null && changing && classDecl.getType() != null) {
                    String fqn = classDecl.getType().getFullyQualifiedName();
                    doNext(new ChangeType(fqn, fqn.replaceFirst("^" + oldFullyQualifiedPackageName,
                            newFullyQualifiedPackageName)));
                }
                return super.visitClassDeclaration(classDecl, context);
            }
        };
    }
}
