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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.AbstractRecipe;
import org.openrewrite.Recipe;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.net.URI;
import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

/**
 * One frame of a {@link RecipesThatMadeChanges} stack, reduced to what a consumer of the marker
 * reads: how the recipe is named, how it was configured, and what it is worth. Standing in for the
 * recipe itself keeps run state (cursors, scanning accumulators, and the source files they reach)
 * off the wire. Reading anything beyond the fields below yields a {@link Recipe} default.
 */
@AbstractRecipe
@RequiredArgsConstructor
public class RecipeThatMadeChanges extends Recipe implements RpcCodec<RecipeThatMadeChanges> {
    private static final URI SOURCE = URI.create("rpc:recipe-that-made-changes");

    private final String name;
    private final @Nullable String displayName;
    private final @Nullable String instanceName;
    private final @Nullable Map<String, Object> options;
    private final @Nullable Duration estimatedEffortPerOccurrence;

    public static RecipeThatMadeChanges of(Recipe recipe) {
        if (recipe instanceof RecipeThatMadeChanges) {
            return (RecipeThatMadeChanges) recipe;
        }
        RecipeDescriptor descriptor = recipe.getDescriptor();
        Map<String, Object> options = null;
        for (OptionDescriptor option : descriptor.getOptions()) {
            if (option.getValue() != null) {
                if (options == null) {
                    options = new LinkedHashMap<>();
                }
                options.put(option.getName(), option.getValue());
            }
        }
        return new RecipeThatMadeChanges(recipe.getName(), descriptor.getDisplayName(),
                descriptor.getInstanceName(), options, descriptor.getEstimatedEffortPerOccurrence());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName == null ? name : displayName;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getInstanceName() {
        return instanceName == null ? super.getInstanceName() : instanceName;
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence;
    }

    @Override
    protected RecipeDescriptor createRecipeDescriptor() {
        List<OptionDescriptor> optionDescriptors = new ArrayList<>();
        if (options != null) {
            for (Map.Entry<String, Object> option : options.entrySet()) {
                // Option types don't travel, only their values.
                optionDescriptors.add(new OptionDescriptor(option.getKey(), "", "", "", null, null, false, option.getValue()));
            }
        }
        return new RecipeDescriptor(name, getDisplayName(), getInstanceName(), "", emptySet(),
                estimatedEffortPerOccurrence, optionDescriptors, emptyList(), emptyList(), emptyList(),
                emptyList(), emptyList(), emptyList(), SOURCE);
    }

    /** Field order is the protocol; every peer codec mirrors it. */
    @Override
    public void rpcSend(RecipeThatMadeChanges after, RpcSendQueue q) {
        q.getAndSend(after, RecipeThatMadeChanges::getName);
        q.getAndSend(after, r -> r.displayName);
        q.getAndSend(after, r -> r.instanceName);
        q.getAndSend(after, r -> r.options);
        q.getAndSend(after, r -> r.estimatedEffortPerOccurrence == null ?
                null : r.estimatedEffortPerOccurrence.toMillis());
    }

    @Override
    public RecipeThatMadeChanges rpcReceive(RecipeThatMadeChanges before, RpcReceiveQueue q) {
        return new RecipeThatMadeChanges(
                q.receive(before.name),
                q.receive(before.displayName),
                q.receive(before.instanceName),
                q.receive(before.options),
                q.receiveAndGet(before.estimatedEffortPerOccurrence,
                        (Number millis) -> Duration.ofMillis(millis.longValue())));
    }
}
