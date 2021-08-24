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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RemoveUnusedPrivateMethods extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove unused private methods";
    }

    @Override
    public String getDescription() {
        return "`private` methods that are never executed are dead code: unnecessary, inoperative code that should be removed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1144");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, executionContext);
                JavaType.Method methodType = TypeUtils.asMethod(method.getType());

                if (methodType != null && methodType.hasFlags(Flag.Private) &&
                        !method.isConstructor() &&
                        methodType.getGenericSignature() != null &&
                        method.getAllAnnotations().isEmpty()) {

                    J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    if (classDeclaration != null && classDeclaration.getImplements() != null) {
                        for (TypeTree implement : classDeclaration.getImplements()) {
                            if (implement instanceof J.Identifier) {
                                JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(implement.getType());
                                if (fqn != null && "java.io.Serializable".equals(fqn.getFullyQualifiedName()) &&
                                        ("readObject".equals(m.getName().getSimpleName()) ||
                                                "readObjectNoData".equals(m.getName().getSimpleName()) ||
                                                "readResolve".equals(m.getName().getSimpleName()) ||
                                                "writeObject".equals(m.getName().getSimpleName()))) {
                                    return m;
                                }
                            }
                        }
                    }

                    J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                    for (JavaType type : cu.getTypesInUse()) {
                        if(type instanceof JavaType.Method) {
                            JavaType.Method usedMethodType = (JavaType.Method) type;
                            if(methodType.getName().equals(usedMethodType.getName()) && methodType.getGenericSignature().equals(usedMethodType.getGenericSignature())) {
                                return m;
                            }
                        }
                        if (type instanceof JavaType.Class &&
                                "org.junit.jupiter.params.provider.MethodSource".equals(((JavaType.Class)type).getFullyQualifiedName())) {
                            return m;
                        }
                    }

                    //noinspection ConstantConditions
                    return null;
                }

                return m;
            }
        };
    }
}
