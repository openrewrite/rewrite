/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A replay of a family's recorded edits, each held to its expect line, one result line per edit in the file the
 * results variable names, which a corpus run tallies.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ScenarioTest {

    private final String resultsEnv;
    private final List<String> results = Collections.synchronizedList(new ArrayList<>());

    protected ScenarioTest(String resultsEnv) {
        this.resultsEnv = resultsEnv;
    }

    @AfterAll
    void writeResults() {
        String file = System.getenv(resultsEnv);
        if (file == null || file.isEmpty()) {
            return;
        }
        try {
            List<String> sorted = new ArrayList<>(results);
            Collections.sort(sorted);
            Files.write(Paths.get(file), (String.join("\n", sorted) + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void record(String label, String outcome, String detail) {
        results.add(label + "\t" + outcome + "\t" + detail.replace('\n', ' ').replace('\t', ' '));
    }

    /**
     * Skips an edit before its replay: one a re-recording supersedes, or one its expect line says the engine does not
     * reproduce, with the reason.
     */
    protected void skipUnlessReplayed(String label, LockScenario scenario, Expect expect) {
        LockScenario supersededBy = scenario.getSupersededBy();
        if (supersededBy != null) {
            record(label, "skipped", "superseded by " + supersededBy.getName());
            assumeTrue(false, label + " is superseded by " + supersededBy.getName());
        }
        if (Expect.NOT_REPRODUCED.equals(expect.getKind())) {
            record(label, "skipped", "not reproduced: " + expect.getRest());
            assumeTrue(false, label + " is not reproduced: " + expect.getRest());
        }
    }

    /**
     * Records a departure from the recording and fails with {@code message} when it has one; a replay the stub could
     * not serve is skipped instead.
     */
    protected void failed(String label, RecordedRegistry<?> http, String outcome, String detail, String message) {
        if (http.unservedElsewhereRecording()) {
            record(label, "skipped", "not served offline:" + http.describe());
            assumeTrue(false, label + " cannot be replayed offline" + http.describe());
        }
        record(label, outcome, detail + http.describe());
        if (!message.isEmpty()) {
            fail(label + message + http.describe());
        }
    }
}
