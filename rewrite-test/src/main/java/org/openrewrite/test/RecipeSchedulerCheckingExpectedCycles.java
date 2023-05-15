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
import org.openrewrite.LargeSourceSet;
import org.openrewrite.RecipeScheduler;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.quark.Quark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RequiredArgsConstructor
class RecipeSchedulerCheckingExpectedCycles extends RecipeScheduler {
    private final int expectedCyclesThatMakeChanges;
    private int cyclesThatResultedInChanges = 0;
    @Nullable
    private LargeSourceSet previousSourceSet;

    @Override
    public void afterCycle(LargeSourceSet sourceSet) {
        if (sourceSet != previousSourceSet && sourceSet.getChangeset().size() > 0) {
            cyclesThatResultedInChanges++;
            if (cyclesThatResultedInChanges > expectedCyclesThatMakeChanges) {
                for (Result result : sourceSet.getChangeset().getAllResults()) {
                    SourceFile before = result.getBefore();
                    SourceFile after = result.getAfter();
                    if (before != null && after != null && !(after instanceof Quark)) {
                        assertThat(after.printAllTrimmed())
                                .as(
                                        "Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges == 1 ? "" : "s") + ", " +
                                        "but took at least one more cycle. Between the last two executed cycles there were changes to \"" + before.getSourcePath() + "\""
                                )
                                .isEqualTo(before.printAllTrimmed());
                    }
                }
            }
        }
        previousSourceSet = sourceSet;
    }

    public void verify() {
        if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges) {
            fail("Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges > 1 ? "s" : "") + ", " +
                 "but took " + cyclesThatResultedInChanges + " cycle" + (cyclesThatResultedInChanges > 1 ? "s" : "") + ".");
        }
    }
}
