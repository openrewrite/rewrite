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
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RecipesThatMadeChangesTest {

    @Test
    void rpcCodecSendsRepeatedRecipesOnce() {
        Recipe recipe = Recipe.noop();
        RecipesThatMadeChanges marker = new RecipesThatMadeChanges(UUID.randomUUID(), List.of(
          List.of(recipe, recipe),
          List.of(recipe)
        ));
        RecipesThatMadeChanges marker2 = new RecipesThatMadeChanges(UUID.randomUUID(), List.of(
          List.of(recipe)
        ));

        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue send = new RpcSendQueue(100, batches::addLast, new IdentityHashMap<>(), null, false);
        send.send(marker, null, null);
        send.send(marker2, null, null);
        send.flush();

        List<RpcObjectData> messages = batches.removeFirst();
        assertThat(messages).hasSize(12);
        assertThat(messages.get(0).getValueType()).isEqualTo(RecipesThatMadeChanges.class.getName());
        assertThat((Object) messages.get(0).getValue()).isNull();

        assertThat((Object) messages.get(2).getValue()).isNull();
        assertThat((Object) messages.get(3).getValue()).isEqualTo(List.of(-1));
        assertThat(messages.get(4).getValueType()).isEqualTo(recipe.getClass().getName());
        assertThat(messages.get(4).getRef()).isNotNull();
        assertThat((Object) messages.get(5).getValue()).isEqualTo(List.of(List.of(0, 0), List.of(0)));

        assertThat((Object) messages.get(8).getValue()).isNull();
        assertThat((Object) messages.get(9).getValue()).isEqualTo(List.of(-1));
        assertThat(messages.get(10).getValueType()).isNull();
        assertThat((Object) messages.get(10).getValue()).isNull();
        assertThat(messages.get(10).getRef()).isEqualTo(messages.get(4).getRef());
        assertThat((Object) messages.get(11).getValue()).isEqualTo(List.of(List.of(0)));

        batches.addLast(messages);
        RpcReceiveQueue receive = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, null, null);
        RecipesThatMadeChanges roundTrip = receive.receive(null);
        RecipesThatMadeChanges roundTrip2 = receive.receive(null);

        List<List<Recipe>> recipes = new ArrayList<>(roundTrip.getRecipes());
        assertThat(recipes).hasSize(2);
        assertThat(recipes.get(0)).hasSize(2);
        assertThat(recipes.get(1)).hasSize(1);
        assertThat(recipes.get(0).get(0)).isSameAs(recipes.get(0).get(1));
        assertThat(recipes.get(0).get(0)).isSameAs(recipes.get(1).get(0));
        assertThat(new ArrayList<>(roundTrip2.getRecipes()).get(0).get(0)).isSameAs(recipes.get(0).get(0));
    }
}
