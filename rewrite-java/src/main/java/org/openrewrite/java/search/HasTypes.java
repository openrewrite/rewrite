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
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.RecipeSearchResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.Tree.randomId;

/**
 * This recipe finds {@link J.CompilationUnit}s having any one of the supplied types
 */
@Incubating(since = "7.2.0")
@Value
@EqualsAndHashCode(callSuper = true)
public class HasTypes extends Recipe {

    @Option(displayName = "Fully-qualified type names",
            description = "Find J.CompilationUnits having one of the supplied types. Types should be identified by fully qualified class name or a glob expression",
            example = "com.google.guava.*")
    List<String> fullyQualifiedTypeNames;

    UUID id = randomId();

    @Override
    public String getDisplayName() {
        return "Has types";
    }

    @Override
    public String getDescription() {
        return "Find any type references by name.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                if (find(cu, fullyQualifiedTypeNames)){
                    cu = cu.withMarkers(cu.getMarkers().addOrUpdate(new RecipeSearchResult(id, HasTypes.this)));
                }
                return cu;
            }
        };

    }

    public static Boolean find(J j, List<String> fullyQualifiedClassNames) {
        JavaIsoVisitor<AtomicBoolean> hasTypeVisitor = new JavaIsoVisitor<AtomicBoolean>() {

            @Override
            public <N extends NameTree> N visitTypeName(N name, AtomicBoolean typeExists) {
                if (typeExists.get()) {
                    return name;
                }
                N n = super.visitTypeName(name, typeExists);
                JavaType.Class asClass = TypeUtils.asClass(n.getType());
                for (String fullyQualifiedClassName : fullyQualifiedClassNames) {
                    if (asClass != null && targetClassMatches(asClass, fullyQualifiedClassName) &&
                            getCursor().firstEnclosing(J.Import.class) == null) {
                        typeExists.set(true);
                        return n;
                    }
                }
                return n;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, AtomicBoolean typeExists) {
                if (typeExists.get()) {
                    return fieldAccess;
                }
                J.FieldAccess fa = super.visitFieldAccess(fieldAccess, typeExists);
                JavaType.Class targetClass = TypeUtils.asClass(fa.getTarget().getType());
                for (String fullyQualifiedTypeName : fullyQualifiedClassNames) {
                    if (targetClass != null && targetClassMatches(targetClass, fullyQualifiedTypeName) &&
                            fa.getName().getSimpleName().equals("class")) {
                        typeExists.set(true);
                        return fa;
                    }
                }
                return fa;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean typeExists) {
                if (typeExists.get()) {
                    return method;
                }
                J.MethodInvocation methodInvocation = super.visitMethodInvocation(method, typeExists);
                JavaType.Class targetClass = methodInvocation.getType() != null ? TypeUtils.asClass(methodInvocation.getType().getDeclaringType()) : null;
                for (String fullyQualifiedTypeName : fullyQualifiedClassNames) {
                    if (targetClass != null && targetClassMatches(targetClass, fullyQualifiedTypeName)) {
                        typeExists.set(true);
                        return methodInvocation;
                    }
                }
                return methodInvocation;
            }

            private boolean targetClassMatches(JavaType.Class targetClass, String fullyQualifiedTypeName) {
                if (fullyQualifiedTypeName.endsWith(".*")) {
                    fullyQualifiedTypeName = fullyQualifiedTypeName.replaceAll("\\.\\*", "");
                }
                return targetClass.getFullyQualifiedName().equals(fullyQualifiedTypeName) || targetClass.getFullyQualifiedName().startsWith(fullyQualifiedTypeName);
            }
        };

        AtomicBoolean typeExists = new AtomicBoolean(false);
        hasTypeVisitor.visit(j, typeExists);
        return typeExists.get();
    }
}
