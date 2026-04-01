/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ObjectMappers;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class PreparedRecipeCache {
    private static final ObjectMapper mapper = ObjectMappers.propertyBasedMapper(null);

    private final @Getter Map<String, Recipe> instantiated = new HashMap<>();
    private final @Getter Map<Recipe, Cursor> recipeCursors = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public <P> TreeVisitor<?, P> instantiateVisitor(String visitorName, @Nullable Map<String, Object> visitorOptions) {
        if (visitorName.startsWith("scan:")) {
            //noinspection unchecked
            ScanningRecipe<Object> recipe = (ScanningRecipe<Object>) instantiated.get(visitorName.substring("scan:".length()));
            return (TreeVisitor<?, P>) new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public Tree preVisit(@NonNull Tree tree, ExecutionContext ctx) {
                    stopAfterPreVisit();
                    Object acc = recipe.getAccumulator(recipeCursors.computeIfAbsent(recipe, r -> new Cursor(null, Cursor.ROOT_VALUE)), ctx);
                    recipe.getScanner(acc).visit(tree, ctx);
                    return tree;
                }
            };
        } else if (visitorName.startsWith("edit:")) {
            Recipe recipe = instantiated.get(visitorName.substring("edit:".length()));
            return (TreeVisitor<?, P>) recipe.getVisitor();
        }

        Map<Object, Object> withJsonType = visitorOptions == null ? new HashMap<>() : new HashMap<>(visitorOptions);
        withJsonType.put("@c", visitorName);
        try {
            Class<?> visitorType = TypeFactory.defaultInstance().findClass(visitorName);
            return (TreeVisitor<?, P>) mapper.convertValue(withJsonType, visitorType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
