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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindEmptyMethods extends Recipe {

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Find methods with empty bodies";
    }

    @Override
    public String getDescription() {
        return "Find methods with empty bodies and single public no arg constructors.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1186");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (classDecl.hasModifier(J.Modifier.Type.Abstract)) {
                    return classDecl;
                }

                if (hasSinglePublicNoArgsConstructor(classDecl.getBody().getStatements())) {
                    getCursor().putMessage("CHECK_CONSTRUCTOR", true);
                }

                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                Boolean checkConstructor = null;
                if (method.isConstructor()) {
                    checkConstructor = getCursor().getNearestMessage("CHECK_CONSTRUCTOR") != null;
                }
                if (checkConstructor != null && checkConstructor || isEmptyMethod(method)) {
                    method = SearchResult.found(method);
                }
                return super.visitMethodDeclaration(method, ctx);
            }

            private boolean isEmptyMethod(J.MethodDeclaration method) {
                return !method.isConstructor() && !isInterfaceMethod(method) &&
                       (matchOverrides == null || !matchOverrides && !TypeUtils.isOverride(method.getMethodType()) || matchOverrides) &&
                       (method.getBody() == null || method.getBody().getStatements().isEmpty() && method.getBody().getEnd().getComments().isEmpty());
            }

            private boolean isInterfaceMethod(J.MethodDeclaration method) {
                //noinspection ConstantConditions
                return method.getMethodType().getDeclaringType() != null
                       && method.getMethodType().getDeclaringType().getKind() == JavaType.FullyQualified.Kind.Interface
                       && !method.hasModifier(J.Modifier.Type.Default);
            }

            private boolean hasSinglePublicNoArgsConstructor(List<Statement> classStatements) {
                List<J.MethodDeclaration> constructors = classStatements.stream()
                        .filter(o -> o instanceof J.MethodDeclaration)
                        .map(o -> (J.MethodDeclaration) o)
                        .filter(J.MethodDeclaration::isConstructor)
                        .collect(Collectors.toList());
                return constructors.size() == 1 &&
                       constructors.get(0).hasModifier(J.Modifier.Type.Public) &&
                       constructors.get(0).getParameters().size() == 1 &&
                       constructors.get(0).getParameters().get(0) instanceof J.Empty;
            }
        };
    }
}
