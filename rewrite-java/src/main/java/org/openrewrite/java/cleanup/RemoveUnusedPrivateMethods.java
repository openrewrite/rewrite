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
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.NoMissingTypes;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class RemoveUnusedPrivateMethods extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove unused private methods";
    }

    @Override
    public String getDescription() {
        return "`private` methods that are never executed are dead code and should be removed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1144");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new NoMissingTypes();
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                JavaType.Method methodType = method.getMethodType();
                if (methodType != null && methodType.hasFlags(Flag.Private) &&
                        !method.isConstructor() &&
                        methodType.getGenericSignature() != null &&
                        method.getAllAnnotations().isEmpty()) {

                    J.ClassDeclaration classDeclaration = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                    if (TypeUtils.isAssignableTo("java.io.Serializable", classDeclaration.getType())) {
                        switch (m.getSimpleName()) {
                            case "readObject":
                            case "readObjectNoData":
                            case "readResolve":
                            case "writeObject":
                                return m;
                        }
                    }

                    J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                    for (JavaType.Method usedMethodType : cu.getTypesInUse().getUsedMethods()) {
                        if (methodType.getName().equals(usedMethodType.getName()) && methodType.getGenericSignature().equals(usedMethodType.getGenericSignature())) {
                            return m;
                        }
                    }

                    for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
                        if (TypeUtils.isOfClassType(javaType, "org.junit.jupiter.params.provider.MethodSource")) {
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
