/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.ClassHierarchy;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindClassHierarchy extends Recipe {
    transient ClassHierarchy classHierarchy = new ClassHierarchy(this);

    @Override
    public String getDisplayName() {
        return "Find class hierarchy";
    }

    @Override
    public String getDescription() {
        return "Discovers all class declarations within a project, recording which files they appear in, their superclasses, and interfaces. " +
               "That information is then recorded in a data table.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                JavaType.FullyQualified type = cd.getType();
                if(type == null) {
                    return cd;
                }
                classHierarchy.insertRow(ctx, new ClassHierarchy.Row(
                        getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                        type.getFullyQualifiedName(),
                        type.getSupertype() == null ? null : type.getSupertype().getFullyQualifiedName(),
                        type.getInterfaces().isEmpty() ? null : type.getInterfaces().stream()
                                .map(JavaType.FullyQualified::getFullyQualifiedName)
                                .collect(Collectors.joining(",")))
                );
                return cd;
            }
        };
    }
}
