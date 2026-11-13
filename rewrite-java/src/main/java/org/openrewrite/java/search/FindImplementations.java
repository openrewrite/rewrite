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

    String displayName = "Find implementing classes";

    String description = "Find class declarations which implement the specified type. " +
               "If the specified type is a class, its subclasses will be matched. " +
               "If the specified type is an interface, classes which implement it will be matched. " +
               "Anonymous classes, lambdas, and method references implementing the specified type are also matched.";

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

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                // Only an anonymous class body is an implementation; a plain constructor invocation is not.
                if (n.getBody() != null && TypeUtils.isAssignableTo(typeName, n.getType())) {
                    n = SearchResult.found(n);
                }
                return n;
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                J.Lambda l = super.visitLambda(lambda, ctx);
                if (TypeUtils.isAssignableTo(typeName, l.getType())) {
                    l = SearchResult.found(l);
                }
                return l;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                if (TypeUtils.isAssignableTo(typeName, m.getType())) {
                    m = SearchResult.found(m);
                }
                return m;
            }
        };
    }
}
