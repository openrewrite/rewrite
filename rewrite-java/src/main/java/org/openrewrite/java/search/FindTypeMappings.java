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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.TypeMappings;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypedTree;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindTypeMappings extends ScanningRecipe<Map<FindTypeMappings.TypeAssociation, Integer>> {
    transient TypeMappings typeMappingsPerSource = new TypeMappings(this);

    @Override
    public String getDisplayName() {
        return "Find type mappings";
    }

    @Override
    public String getDescription() {
        return "Study the frequency of `J` types and their `JavaType` type attribution.";
    }

    @Override
    public Map<TypeAssociation, Integer> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<TypeAssociation, Integer> acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
                Cursor cursor = getCursor();
                acc.compute(new TypeAssociation(
                                cursor.firstEnclosingOrThrow(JavaSourceFile.class).getClass(),
                                cursor.getValue().getClass(),
                                javaType == null ? null : javaType.getClass(),
                                javaType == null ?
                                        cursor.getPathAsStream()
                                                .filter(t -> t instanceof TypedTree &&
                                                             ((TypedTree) t).getType() != null)
                                                .findFirst()
                                                .map(Object::getClass)
                                                .orElse(null) :
                                        null),
                        (k, v) -> v == null ? 1 : v + 1);
                return javaType;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Map<TypeAssociation, Integer> acc, ExecutionContext ctx) {
        acc.forEach((assoc, count) -> {
            String j = assoc.getJ().getName();
            Class<?> nearJ = assoc.getNearestNonNullJ();
            typeMappingsPerSource.insertRow(ctx, new TypeMappings.Row(
                    assoc.getCompilationUnit().getEnclosingClass().getSimpleName(),
                    j.substring(j.lastIndexOf('.') + 1),
                    assoc.getJavaType() == null ? "null" : assoc.getJavaType().getSimpleName(),
                    count,
                    nearJ == null ? null : nearJ.getName().substring(nearJ.getName().lastIndexOf('.') + 1)
            ));
        });
        return emptyList();
    }

    @Value
    public static class TypeAssociation {
        Class<? extends JavaSourceFile> compilationUnit;

        Class<?> j;

        @Nullable
        Class<? extends JavaType> javaType;

        /**
         * When {@link #j} is null, this is the nearest non-null {@link J} type
         * in the cursor stack.
         */
        @Nullable
        Class<?> nearestNonNullJ;
    }
}
