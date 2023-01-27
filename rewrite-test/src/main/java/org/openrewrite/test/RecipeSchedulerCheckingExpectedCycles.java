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
package org.openrewrite.test;

import lombok.RequiredArgsConstructor;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.quark.Quark;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RequiredArgsConstructor
class RecipeSchedulerCheckingExpectedCycles implements RecipeScheduler {
    private final RecipeScheduler delegate;
    private final int expectedCyclesThatMakeChanges;

    private int cyclesThatResultedInChanges = 0;

    @Override
    public <T> CompletableFuture<T> schedule(Callable<T> fn) {
        return delegate.schedule(fn);
    }

    @Override
    public <S extends SourceFile> List<S> scheduleVisit(RecipeRunStats runStats, Stack<Recipe> recipeStack, List<S> before,
                                                        @Nullable List<Boolean> singleSourceApplicableTestResult, ExecutionContext ctx,
                                                        Map<UUID, Stack<Recipe>> recipeThatAddedOrDeletedSourceFile) {
        ctx.putMessage("cyclesThatResultedInChanges", cyclesThatResultedInChanges);
        List<S> afterList = delegate.scheduleVisit(runStats, recipeStack, before, singleSourceApplicableTestResult, ctx, recipeThatAddedOrDeletedSourceFile);
        if (afterList != before) {
            cyclesThatResultedInChanges++;
            if (cyclesThatResultedInChanges > expectedCyclesThatMakeChanges &&
                    !before.isEmpty() && !afterList.isEmpty()) {
                for (int i = 0; i < before.size(); i++) {
                    if(!(afterList.get(i) instanceof Quark)) {
                        assertThat(afterList.get(i).printAllTrimmed())
                                .as(
                                        "Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges == 1 ? "" : "s") + ", " +
                                                "but took at least one more cycle. Between the last two executed cycles there were changes to \"" + before.get(i).getSourcePath() + "\""
                                )
                                .isEqualTo(before.get(i).printAllTrimmed());
                    }
                }
            }
        }
        return afterList;
    }

    public void verify() {
        if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges) {
            fail("Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges > 1 ? "s" : "") + ", " +
                    "but took " + cyclesThatResultedInChanges + " cycle" + (cyclesThatResultedInChanges > 1 ? "s" : "") + ".");
        }
    }
}
