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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class RecipesThatMadeChanges implements Marker, RpcCodec<RecipesThatMadeChanges> {
    @EqualsAndHashCode.Include
    UUID id;

    Collection<List<Recipe>> recipes;

    public static RecipesThatMadeChanges create(List<Recipe> recipeStack) {
        List<List<Recipe>> recipeStackList = new ArrayList<>(1);
        recipeStackList.add(recipeStack);
        return new RecipesThatMadeChanges(Tree.randomId(), recipeStackList);
    }

    @Override
    public void rpcSend(RecipesThatMadeChanges after, RpcSendQueue q) {
        q.getAndSend(after, RecipesThatMadeChanges::getId);
        q.getAndSendList(after, RecipesThatMadeChanges::identities, RecipesThatMadeChanges::stackKey, stack ->
                q.getAndSendList(stack, Function.identity(), RecipeThatMadeChanges::getName, null));
    }

    @Override
    public RecipesThatMadeChanges rpcReceive(RecipesThatMadeChanges before, RpcReceiveQueue q) {
        UUID receivedId = q.receiveAndGet(before.getId(), UUID::fromString);
        List<List<RecipeThatMadeChanges>> received = requireNonNull(
                q.receiveList(before.identities(), stack -> q.receiveList(stack, null)));
        return before
                .withId(receivedId)
                .withRecipes(received.stream()
                        .map(stack -> (List<Recipe>) new ArrayList<Recipe>(stack))
                        .collect(toList()));
    }

    /** Identifies a stack for the sender's own list diff; it never travels. */
    private static String stackKey(List<RecipeThatMadeChanges> stack) {
        StringBuilder key = new StringBuilder();
        for (RecipeThatMadeChanges recipe : stack) {
            key.append(recipe.getName()).append('\u0000');
        }
        return key.toString();
    }

    /**
     * The recipe stacks as they travel: identity only. Frames of a received marker pass through
     * unchanged, so forwarding one diffs against what arrived rather than degrading it.
     */
    private List<List<RecipeThatMadeChanges>> identities() {
        if (recipes == null) {
            return emptyList();
        }
        List<List<RecipeThatMadeChanges>> identities = new ArrayList<>(recipes.size());
        for (List<Recipe> stack : recipes) {
            List<RecipeThatMadeChanges> stackIdentities = new ArrayList<>(stack.size());
            for (Recipe recipe : stack) {
                stackIdentities.add(RecipeThatMadeChanges.of(recipe));
            }
            identities.add(stackIdentities);
        }
        return identities;
    }
}
