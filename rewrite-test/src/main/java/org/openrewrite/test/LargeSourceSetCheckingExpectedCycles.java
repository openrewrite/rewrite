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

import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.quark.Quark;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class LargeSourceSetCheckingExpectedCycles extends InMemoryLargeSourceSet {
    private final int expectedCyclesThatMakeChanges;
    private int cyclesThatResultedInChanges = 0;

    private Map<Path, SourceFile> lastCycleChanges = emptyMap();

    LargeSourceSetCheckingExpectedCycles(int expectedCyclesThatMakeChanges, List<SourceFile> ls) {
        super(ls);
        this.expectedCyclesThatMakeChanges = expectedCyclesThatMakeChanges;
    }

    private LargeSourceSetCheckingExpectedCycles(LargeSourceSetCheckingExpectedCycles from, @Nullable Map<SourceFile, List<Recipe>> deletions, List<SourceFile> mapped) {
        super(from.getInitialState(), deletions, mapped);
        this.expectedCyclesThatMakeChanges = from.expectedCyclesThatMakeChanges;
    }

    @Override
    protected InMemoryLargeSourceSet withChanges(@Nullable Map<SourceFile, List<Recipe>> deletions, List<SourceFile> mapped) {
        return new LargeSourceSetCheckingExpectedCycles(this, deletions, mapped);
    }

    @Override
    public void afterCycle(boolean lastCycle) {
        boolean detectedChangeInThisCycle = false;
        Map<Path, SourceFile> thisCycleChanges = new HashMap<>();

        for (Result result : getChangeset().getAllResults()) {
            SourceFile before; // this source file as it existed after the last cycle
            Path sourcePath = result.getAfter() != null ? result.getAfter().getSourcePath() : result.getBefore().getSourcePath();
            if (result.getBefore() == null) {
                // a source file generated on a prior cycle
                before = result.getAfter() == null ? null : lastCycleChanges.get(sourcePath);
            } else {
                before = lastCycleChanges.getOrDefault(sourcePath, result.getBefore());
            }

            SourceFile after = result.getAfter();
            if (before != null && after != null && !(after instanceof Quark)) {
                if (!detectedChangeInThisCycle && before != after) {
                    detectedChangeInThisCycle = true;
                    cyclesThatResultedInChanges++;
                }
                if (cyclesThatResultedInChanges > expectedCyclesThatMakeChanges) {
                    assertThat(after.printAllTrimmed())
                            .as(
                                    "Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges == 1 ? "" : "s") + ", " +
                                    "but took at least one more cycle. Between the last two executed cycles there were changes to \"" + before.getSourcePath() + "\""
                            )
                            .isEqualTo(before.printAllTrimmed());
                }
            }

            if (result.getAfter() != null) {
                thisCycleChanges.put(sourcePath, result.getAfter());
            }
        }
        lastCycleChanges = thisCycleChanges;
        if (lastCycle) {
            if (cyclesThatResultedInChanges == 0 && expectedCyclesThatMakeChanges > 0) {
                fail("Recipe was expected to make a change but made no changes.");
            } else if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges) {
                fail("Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges > 1 ? "s" : "") + ", " +
                     "but took " + cyclesThatResultedInChanges + " cycle" + (cyclesThatResultedInChanges > 1 ? "s" : "") + ". " +
                     "This usually indicates the recipe is making changes after it should have stabilized.");
            }
        }
    }
}
