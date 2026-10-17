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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class LargeSourceSetCheckingExpectedCycles extends InMemoryLargeSourceSet {
    private final int expectedCyclesThatMakeChanges;
    private final @Nullable AddedMarkers addedMarkers;
    private final @Nullable UnchangedTreeIdentity unchangedTreeIdentity;
    private int cyclesThatResultedInChanges = 0;

    private Map<SourceFile, SourceFile> lastCycleEdits = emptyMap();
    private Set<SourceFile> lastCycleGenerated = emptySet();
    private Set<SourceFile> lastCycleDeleted = emptySet();

    LargeSourceSetCheckingExpectedCycles(int expectedCyclesThatMakeChanges, List<SourceFile> ls) {
        this(expectedCyclesThatMakeChanges, ls, null, null);
    }

    LargeSourceSetCheckingExpectedCycles(int expectedCyclesThatMakeChanges, List<SourceFile> ls,
                                         @Nullable AddedMarkers addedMarkers,
                                         @Nullable UnchangedTreeIdentity unchangedTreeIdentity) {
        super(ls);
        this.expectedCyclesThatMakeChanges = expectedCyclesThatMakeChanges;
        this.addedMarkers = addedMarkers;
        this.unchangedTreeIdentity = unchangedTreeIdentity;
    }

    private LargeSourceSetCheckingExpectedCycles(LargeSourceSetCheckingExpectedCycles from, @Nullable Map<SourceFile, List<Recipe>> deletions, List<SourceFile> mapped) {
        super(from.getInitialState(), deletions, mapped, null);
        this.expectedCyclesThatMakeChanges = from.expectedCyclesThatMakeChanges;
        this.addedMarkers = from.addedMarkers;
        this.unchangedTreeIdentity = from.unchangedTreeIdentity;
        this.cyclesThatResultedInChanges = from.cyclesThatResultedInChanges;
        this.lastCycleEdits = from.lastCycleEdits;
        this.lastCycleGenerated = from.lastCycleGenerated;
        this.lastCycleDeleted = from.lastCycleDeleted;
    }

    @Nullable
    UnchangedTreeIdentity getUnchangedTreeIdentity() {
        return unchangedTreeIdentity;
    }

    @Override
    protected InMemoryLargeSourceSet withChanges(@Nullable Map<SourceFile, List<Recipe>> deletions, List<SourceFile> mapped) {
        return new LargeSourceSetCheckingExpectedCycles(this, deletions, mapped);
    }

    @Override
    public void onGenerateCollision(Path sourcePath, boolean existingFile) {
        if (existingFile) {
            fail("Recipe generated a source file that already exists in the source set: " + sourcePath);
        } else {
            fail("Recipe generated multiple source files at the same path: " + sourcePath);
        }
    }

    @Override
    public void onGenerateInvalidPath(Path sourcePath) {
        fail("Recipe generated a source file with an invalid path: " + sourcePath);
    }

    @Override
    public void afterCycle(boolean lastCycle) {
        boolean detectedChangeInThisCycle = false;
        Map<SourceFile, SourceFile> thisCycleEdits = new HashMap<>();
        Set<SourceFile> thisCycleGenerated = new HashSet<>();
        Set<SourceFile> thisCycleDeleted = new HashSet<>();

        for (Result result : getChangeset().getAllResults()) {
            SourceFile before = null; // this source file as it existed after the last cycle
            SourceFile after = result.getAfter();

            if (result.getBefore() == null) {
                // a source file generated on a prior cycle
                before = after == null ? null : lastCycleGenerated.stream()
                        .filter(it -> Objects.equals(it.getId(), result.getAfter().getId()))
                        .findFirst()
                        .orElse(null);
            } else {
                if (after == null && lastCycleDeleted.contains(result.getBefore())) {
                    before = result.getBefore();
                    after = before;
                }
                if (before == null) {
                    before = lastCycleEdits.getOrDefault(result.getBefore(), result.getBefore());
                }
            }

            if (before != null && after != null) {
                thisCycleEdits.put(before, after);
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
            } else if (before == null && after != null) {
                thisCycleGenerated.add(after);
                if (!detectedChangeInThisCycle) {
                    detectedChangeInThisCycle = true;
                    cyclesThatResultedInChanges++;
                }
            } else if (before != null) {
                thisCycleDeleted.add(before);
                if (!detectedChangeInThisCycle) {
                    detectedChangeInThisCycle = true;
                    cyclesThatResultedInChanges++;
                }
            }
        }

        lastCycleEdits = thisCycleEdits;
        lastCycleGenerated = thisCycleGenerated;
        lastCycleDeleted = thisCycleDeleted;
        if (lastCycle) {
            if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges && addedMarkers != null) {
                // A recipe that only adds markers changes no text, which would otherwise surface below as a
                // puzzling complaint about the number of cycles
                addedMarkers.assertNoneAdded(getChangeset().getAllResults());
            }
            if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges && unchangedTreeIdentity != null) {
                // An equal copy of a source file counts as a change, which would otherwise surface below as a
                // complaint about the number of cycles
                unchangedTreeIdentity.assertNoneFound();
            }
            if (cyclesThatResultedInChanges == 0 && expectedCyclesThatMakeChanges > 0) {
                fail("Recipe was expected to make a change but made no changes.");
            } else if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges) {
                fail("Expected recipe to complete in " + expectedCyclesThatMakeChanges + " cycle" + (expectedCyclesThatMakeChanges > 1 ? "s" : "") + ", " +
                     "but took " + cyclesThatResultedInChanges + " cycle" + (cyclesThatResultedInChanges > 1 ? "s" : "") + ". " +
                     "This usually indicates the recipe is making changes after it should have stabilized.\n" +
                     "https://docs.openrewrite.org/authoring-recipes/recipe-conventions-and-best-practices#stay-single-cycle");
            }
        }
    }
}
