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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Paths;

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

    private final boolean recursive;

    @JsonCreator
    public ChangePackage(String oldFullyQualifiedPackageName, String newFullyQualifiedPackageName, boolean recursive) {
        this.oldFullyQualifiedPackageName = oldFullyQualifiedPackageName;
        this.newFullyQualifiedPackageName = newFullyQualifiedPackageName;
        this.recursive = recursive;
    }

    public ChangePackage(String oldFullyQualifiedPackageName, String newFullyQualifiedPackageName) {
        this(oldFullyQualifiedPackageName, newFullyQualifiedPackageName, true);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final JavaTemplate newPackageExpr = template("package #{}").build();

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext context) {
                J.CompilationUnit c = super.visitCompilationUnit(cu, context);
                String changingTo = getCursor().getMessage("changingTo");
                if (changingTo != null) {
                    String path = c.getSourcePath().toString();
                    c = c.withSourcePath(Paths.get(path.replaceFirst(
                            oldFullyQualifiedPackageName.replace('.', '/'),
                            changingTo.replace('.', '/')
                    )));
                }
                return c;
            }

            @Override
            public J.Package visitPackage(J.Package pkg, ExecutionContext context) {
                String original = pkg.getExpression().printTrimmed().replaceAll("\\s", "");
                if (original.equals(oldFullyQualifiedPackageName)) {
                    getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "changingTo", newFullyQualifiedPackageName);
                    return pkg.withTemplate(newPackageExpr, pkg.getCoordinates().replace(), newFullyQualifiedPackageName);
                } else if (recursive && original.startsWith(oldFullyQualifiedPackageName)) {
                    String changingTo = newFullyQualifiedPackageName +
                            original.substring(oldFullyQualifiedPackageName.length());
                    getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "changingTo",
                            changingTo);
                    return pkg.withTemplate(newPackageExpr, pkg.getCoordinates().replace(), changingTo);
                }
                return pkg;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext context) {
                String changingTo = getCursor().getNearestMessage("changingTo");
                JavaType.Class classType = classDecl.getType();
                if (changingTo != null && classType != null) {
                    String fqn = classType.getFullyQualifiedName();
                    doNext(new ChangeType(fqn, changingTo + '.' + classType.getClassName()));
                }
                return super.visitClassDeclaration(classDecl, context);
            }
        };
    }
}
