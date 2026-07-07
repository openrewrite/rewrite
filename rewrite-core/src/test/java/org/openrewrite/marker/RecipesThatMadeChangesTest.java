/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RecipesThatMadeChangesTest {

    @Test
    void rpcCodecRoundTripsStacksAndSharesRepeatedRecipesWithinAMarker() {
        // given a marker whose stacks repeat the same recipe instance
        Recipe recipe = Recipe.noop();
        RecipesThatMadeChanges marker = new RecipesThatMadeChanges(UUID.randomUUID(), List.of(
          List.of(recipe, recipe),
          List.of(recipe)
        ));

        // when it is sent
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue send = new RpcSendQueue(100, batches::addLast, new IdentityHashMap<>(), null, false);
        send.send(marker, null, null);
        send.flush();

        // then a repeated recipe is written to the wire table only once (referenced by the stacks)
        List<RpcObjectData> messages = batches.removeFirst();
        long recipeEntries = messages.stream()
          .filter(m -> RecipeDescriptor.class.getName().equals(m.getValueType()))
          .count();
        assertThat(recipeEntries).isEqualTo(1);

        // and the round trip rebuilds the stack structure, sharing the single reconstructed instance
        batches.addLast(messages);
        RpcReceiveQueue receive = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, null, null);
        RecipesThatMadeChanges roundTrip = receive.receive(null);

        List<List<Recipe>> recipes = new ArrayList<>(roundTrip.getRecipes());
        assertThat(recipes).hasSize(2);
        assertThat(recipes.get(0)).hasSize(2);
        assertThat(recipes.get(1)).hasSize(1);
        assertThat(recipes.get(0).get(0)).isSameAs(recipes.get(0).get(1));
        assertThat(recipes.get(0).get(0)).isSameAs(recipes.get(1).get(0));
        assertThat(recipes.get(0).get(0).getName()).isEqualTo(recipe.getName());
    }

    @Test
    void rpcCodecSendsShallowDescriptorNotRecipeSubtree() {
        // given a composite recipe whose descriptor carries a recursive recipeList
        Recipe parent = new ParentRecipe();
        assertThat(parent.getDescriptor().getRecipeList()).isNotEmpty();

        RecipesThatMadeChanges marker = new RecipesThatMadeChanges(UUID.randomUUID(), List.of(List.of(parent)));

        // when the marker is sent
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue send = new RpcSendQueue(100, batches::addLast, new IdentityHashMap<>(), null, false);
        send.send(marker, null, null);
        send.flush();

        // then the recipe is transmitted as an identity-only descriptor with the recursive subtree cleared
        List<RpcObjectData> messages = batches.removeFirst();
        RecipeDescriptor sent = null;
        for (RpcObjectData m : messages) {
            Object value = m.getValue();
            if (value instanceof RecipeDescriptor) {
                sent = (RecipeDescriptor) value;
                break;
            }
        }
        assertThat(sent).isNotNull();
        assertThat(sent.getName()).isEqualTo(parent.getName());
        assertThat(sent.getRecipeList()).isEmpty();
        assertThat(sent.getDataTables()).isEmpty();
        assertThat(sent.getPreconditions()).isEmpty();

        // and the round trip reconstructs the recipe with the same identity
        batches.addLast(messages);
        RpcReceiveQueue receive = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, null, null);
        RecipesThatMadeChanges roundTrip = receive.receive(null);
        Recipe received = new ArrayList<>(roundTrip.getRecipes()).get(0).get(0);
        assertThat(received.getName()).isEqualTo(parent.getName());
        assertThat(received.getDisplayName()).isEqualTo(parent.getDisplayName());
    }

    static class ParentRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "Parent recipe";
        }

        @Override
        public String getDescription() {
            return "A composite recipe with a child, so its descriptor carries a recipeList.";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return List.of(new ChildRecipe());
        }
    }

    static class ChildRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "Child recipe";
        }

        @Override
        public String getDescription() {
            return "A leaf recipe.";
        }
    }
}
