/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.internal;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Positive/negative control for {@link CursorEscapeDetector}: proves it flags a cursor
 * deliberately retained past its visit and does not flag ephemeral ones. Without this,
 * a "0 leaks" measurement would be meaningless.
 */
class CursorEscapeDetectorTest {

    @Test
    void detectsAnObjectRetainedPastItsVisit() {
        CursorEscapeDetector.configureForTest(1, 1, 2);

        // a "recipe" that stashes one cursor and lets the rest go
        List<Object> retainedByRecipe = new ArrayList<>();

        Object leaked = new Object();
        retainedByRecipe.add(leaked);
        CursorEscapeDetector.onCursorCreated(leaked, "retained");
        for (int i = 0; i < 200; i++) {
            CursorEscapeDetector.onCursorCreated(new Object(), "ephemeral"); // no strong ref kept
        }
        CursorEscapeDetector.onTopLevelEnd();

        // age the retained object well past GRACE while ephemerals get collected
        for (int e = 0; e < 6; e++) {
            CursorEscapeDetector.onCursorCreated(new Object(), "ephemeral");
            CursorEscapeDetector.onTopLevelEnd();
        }

        assertThat(CursorEscapeDetector.leakCount())
                .as("retained object must be detected as a leak")
                .isGreaterThanOrEqualTo(1);
        assertThat(retainedByRecipe).hasSize(1); // keep alive until after the assertion
    }

    @Test
    void doesNotFlagEphemeralCursors() {
        CursorEscapeDetector.configureForTest(1, 1, 2);
        for (int e = 0; e < 10; e++) {
            for (int i = 0; i < 100; i++) {
                CursorEscapeDetector.onCursorCreated(new Object(), "ephemeral");
            }
            CursorEscapeDetector.onTopLevelEnd();
        }
        assertThat(CursorEscapeDetector.leakCount())
                .as("nothing retained => no leaks")
                .isZero();
    }
}
