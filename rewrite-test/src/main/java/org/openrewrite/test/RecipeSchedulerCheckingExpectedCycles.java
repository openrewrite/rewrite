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
import org.openrewrite.SourceFile;
import org.openrewrite.SourceSet;
import org.openrewrite.quark.Quark;
import org.openrewrite.scheduling.DirectScheduler;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RequiredArgsConstructor
class RecipeSchedulerCheckingExpectedCycles extends DirectScheduler {
    private final int expectedCyclesThatMakeChanges;
    private int cyclesThatResultedInChanges = 0;

    @Override
    public void afterCycle(SourceSet sourceSet) {
        if (sourceSet != sourceSet.getInitialState()) {
            cyclesThatResultedInChanges++;
            if (cyclesThatResultedInChanges > expectedCyclesThatMakeChanges) {

                Iterator<SourceFile> afterIter = sourceSet.iterator();
                if (!afterIter.hasNext()) {
                    return;
                }

                for (SourceFile b : sourceSet.getInitialState()) {
                    SourceFile a = afterIter.next();
                    if (!(a instanceof Quark)) {
                        assertThat(a.printAllTrimmed())
                                .as(
                                        "Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges == 1 ? "" : "s") + ", " +
                                        "but took at least one more cycle. Between the last two executed cycles there were changes to \"" + b.getSourcePath() + "\""
                                )
                                .isEqualTo(b.printAllTrimmed());
                    }
                }
            }
        }
    }

    public void verify() {
        if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges) {
            fail("Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges > 1 ? "s" : "") + ", " +
                 "but took " + cyclesThatResultedInChanges + " cycle" + (cyclesThatResultedInChanges > 1 ? "s" : "") + ".");
        }
    }
}
