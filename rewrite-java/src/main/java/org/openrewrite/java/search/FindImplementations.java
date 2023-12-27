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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindImplementations extends Recipe {
    @Option(displayName = "Type name",
            description = "The fully qualified name to search for.",
            example = "org.openrewrite.Recipe")
    String typeName;

    @Override
    public String getDisplayName() {
        return "Find implementing classes";
    }

    @Override
    public String getDescription() {
        return "Find class declarations which implement the specified type. " +
               "If the specified type is a class, its subclasses will be matched. " +
               "If the specified type is an interface, classes which implement it will be matched.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (TypeUtils.isAssignableTo(typeName, cd.getType()) && !TypeUtils.isOfClassType(cd.getType(), typeName)) {
                    cd = SearchResult.found(cd);
                }
                return cd;
            }
        };
    }
}
