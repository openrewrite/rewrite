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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveImplements extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove interface implementations";
    }

    @Override
    public String getDescription() {
        return "Removes `implements` clauses from classes implementing the specified interface. " +
               "Removes `@Overrides` annotations from methods which no longer override anything.";
    }

    @Option(displayName = "Interface Type",
            description = "The fully qualified name of the interface to remove.",
            example = "java.io.Serializable")
    String interfaceType;

    @Option(displayName = "Filter",
            description = "Only apply the interface removal to classes with fully qualified names that begin with this filter. " +
                          "`null` or empty matches all classes.",
            example = "com.yourorg.")
    @Nullable
    String filter;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext ctx) {
                if (!(classDeclaration.getType() instanceof JavaType.Class) || classDeclaration.getImplements() == null) {
                    return super.visitClassDeclaration(classDeclaration, ctx);
                }
                JavaType.Class cdt = (JavaType.Class) classDeclaration.getType();
                if ((filter == null || cdt.getFullyQualifiedName().startsWith(filter)) && cdt.getInterfaces().stream().anyMatch(it -> isOfClassType(it, interfaceType))) {
                    return SearchResult.found(classDeclaration, "");
                }
                return super.visitClassDeclaration(classDeclaration, ctx);
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
                if (!(cd.getType() instanceof JavaType.Class) || cd.getImplements() == null) {
                    return super.visitClassDeclaration(cd, ctx);
                }
                JavaType.Class cdt = (JavaType.Class) cd.getType();
                if ((filter == null || cdt.getFullyQualifiedName().startsWith(filter)) && cdt.getInterfaces().stream().anyMatch(it -> isOfClassType(it, interfaceType))) {
                    cd = cd.withImplements(cd.getImplements().stream()
                            .filter(implement -> !isOfClassType(implement.getType(), interfaceType))
                            .collect(toList()));
                    cdt = cdt.withInterfaces(cdt.getInterfaces().stream()
                            .filter(it -> !isOfClassType(it, interfaceType))
                            .collect(toList()));
                    cd = cd.withType(cdt);
                    maybeRemoveImport(interfaceType);
                    getCursor().putMessage(cdt.getFullyQualifiedName(), cdt);
                }
                return super.visitClassDeclaration(cd, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration md, ExecutionContext context) {
                if (md.getMethodType() == null) {
                    return super.visitMethodDeclaration(md, context);
                }
                Object maybeClassType = getCursor().getNearestMessage(md.getMethodType().getDeclaringType().getFullyQualifiedName());
                if (!(maybeClassType instanceof JavaType.Class)) {
                    return super.visitMethodDeclaration(md, context);
                }
                JavaType.Class cdt = (JavaType.Class) maybeClassType;
                md = md.withMethodType(md.getMethodType().withDeclaringType(cdt));
                if (md.getAllAnnotations().stream().noneMatch(ann -> isOfClassType(ann.getType(), "java.lang.Override")) || TypeUtils.isOverride(md.getMethodType())) {
                    return super.visitMethodDeclaration(md, context);
                }
                md = (J.MethodDeclaration) new RemoveAnnotation("@java.lang.Override").getVisitor().visitNonNull(md, context, getCursor().getParentOrThrow());
                return super.visitMethodDeclaration(md, context);
            }
        });
    }
}
