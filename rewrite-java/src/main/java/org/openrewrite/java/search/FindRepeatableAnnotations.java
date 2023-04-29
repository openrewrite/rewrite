/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

public class FindRepeatableAnnotations extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find uses of `@Repeatable` annotations";
    }

    @Override
    public String getDescription() {
        return "Java 8 introduced the concept of `@Repeatable` annotations.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
                    if (isRepeatable(javaType)) {
                        return SearchResult.found(cu);
                    }
                }
                return cu;
            }
        }, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                if (isRepeatable(annotation.getType())) {
                    return SearchResult.found(annotation);
                }
                return super.visitAnnotation(annotation, ctx);
            }
        });
    }

    public static boolean isRepeatable(@Nullable JavaType javaType) {
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(javaType);
        if (type != null && TypeUtils.isAssignableTo("java.lang.annotation.Annotation", type)) {
            for (JavaType.FullyQualified ann : type.getAnnotations()) {
                if (TypeUtils.isOfClassType(ann, "java.lang.annotation.Repeatable")) {
                    return true;
                }
            }
        }
        return false;
    }
}
