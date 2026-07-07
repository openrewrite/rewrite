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
package org.openrewrite.marker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class RecipesThatMadeChanges implements Marker, RpcCodec<RecipesThatMadeChanges> {
    @EqualsAndHashCode.Include
    UUID id;

    Collection<List<Recipe>> recipes;

    private static final ObjectMapper DESCRIPTOR_MAPPER =
            ObjectMappers.propertyBasedMapper(null).copy().registerModule(new JavaTimeModule());

    public static RecipesThatMadeChanges create(List<Recipe> recipeStack) {
        List<List<Recipe>> recipeStackList = new ArrayList<>(1);
        recipeStackList.add(recipeStack);
        return new RecipesThatMadeChanges(Tree.randomId(), recipeStackList);
    }

    @Override
    public void rpcSend(RecipesThatMadeChanges after, RpcSendQueue q) {
        q.getAndSend(after, r -> r.getId().toString());
        q.getAndSendList(after, r -> WireForm.from(r).recipeTable, System::identityHashCode, null);
        q.getAndSend(after, r -> WireForm.from(r).stacks);
    }

    @Override
    public RecipesThatMadeChanges rpcReceive(RecipesThatMadeChanges before, RpcReceiveQueue q) {
        UUID id = q.receiveAndGet(before.getId(), UUID::fromString);
        WireForm beforeWire = WireForm.from(before);
        List<Object> recipeTable = q.receiveList(beforeWire.recipeTable, null);
        List<List<Integer>> stacks = q.receive(beforeWire.stacks, null);
        List<Recipe> recipesById = new ArrayList<>(recipeTable.size());
        for (Object recipe : recipeTable) {
            recipesById.add(SnapshotRecipe.from(recipe));
        }

        List<List<Recipe>> recipes = new ArrayList<>(stacks.size());
        for (List<Integer> stack : stacks) {
            List<Recipe> recipeStack = new ArrayList<>(stack.size());
            for (Integer recipeIndex : stack) {
                recipeStack.add(recipesById.get(recipeIndex));
            }
            recipes.add(recipeStack);
        }
        return new RecipesThatMadeChanges(id, recipes);
    }

    private static class WireForm {
        final List<Object> recipeTable = new ArrayList<>();
        final List<List<Integer>> stacks = new ArrayList<>();
        final IdentityHashMap<Recipe, Integer> recipeIds = new IdentityHashMap<>();

        static WireForm from(@Nullable RecipesThatMadeChanges marker) {
            WireForm wire = new WireForm();
            if (marker == null || marker.getRecipes() == null) {
                return wire;
            }

            for (List<Recipe> stack : marker.getRecipes()) {
                List<Integer> stackIds = new ArrayList<>(stack.size());
                for (Recipe recipe : stack) {
                    Integer id = wire.recipeIds.get(recipe);
                    if (id == null) {
                        id = wire.recipeTable.size();
                        wire.recipeIds.put(recipe, id);
                        wire.recipeTable.add(recipe instanceof SnapshotRecipe ?
                                ((SnapshotRecipe) recipe).getWireValue() : shallowDescriptor(recipe));
                    }
                    stackIds.add(id);
                }
                wire.stacks.add(stackIds);
            }
            return wire;
        }

        /**
         * The marker only conveys which recipes made a change; the receiver reconstructs each as a
         * {@link SnapshotRecipe} from its descriptor and never reads the recipe's sub-tree or
         * visitors. Sending the full recipe drags along the recursive {@code recipeList}/{@code
         * childResponses} and precondition-visitor state, which for a large composite recipe
         * expands into hundreds of MB over RPC. Emitting an identity-only descriptor (the recursive
         * and data-table fields cleared) is exactly what the receiver rebuilds, and the stack
         * structure already encodes the recipe hierarchy.
         *
         * <p>The descriptor is serialized as a plain {@link Map} so it crosses the wire without a
         * value type; every runtime then receives and re-sends it as raw data, needing no factory
         * for the descriptor type.
         */
        private static Map<String, Object> shallowDescriptor(Recipe recipe) {
            RecipeDescriptor descriptor = recipe.getDescriptor()
                    .withRecipeList(emptyList())
                    .withPreconditions(emptyList())
                    .withDataTables(emptyList());
            //noinspection unchecked
            return DESCRIPTOR_MAPPER.convertValue(descriptor, Map.class);
        }
    }

    static class SnapshotRecipe extends Recipe {
        private final Object wireValue;
        private final RecipeDescriptor descriptor;

        private SnapshotRecipe(Object wireValue, RecipeDescriptor descriptor) {
            this.wireValue = wireValue;
            this.descriptor = descriptor;
        }

        Object getWireValue() {
            return wireValue;
        }

        static Recipe from(Object wireValue) {
            if (wireValue instanceof Recipe) {
                return (Recipe) wireValue;
            }
            return new SnapshotRecipe(wireValue, descriptorFrom(wireValue));
        }

        @Override
        public String getName() {
            return descriptor.getName();
        }

        @Override
        public String getDisplayName() {
            return descriptor.getDisplayName();
        }

        @Override
        public String getDescription() {
            return descriptor.getDescription();
        }

        @Override
        public Set<String> getTags() {
            return descriptor.getTags() == null ? emptySet() : descriptor.getTags();
        }

        @Override
        public @Nullable Duration getEstimatedEffortPerOccurrence() {
            return descriptor.getEstimatedEffortPerOccurrence();
        }

        @Override
        public List<Recipe> getRecipeList() {
            return emptyList();
        }

        @Override
        protected RecipeDescriptor createRecipeDescriptor() {
            return descriptor;
        }

        private static RecipeDescriptor descriptorFrom(Object wireValue) {
            if (wireValue instanceof RecipeDescriptor) {
                return (RecipeDescriptor) wireValue;
            }
            return DESCRIPTOR_MAPPER.convertValue(wireValue, RecipeDescriptor.class);
        }
    }
}
