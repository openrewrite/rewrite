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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of bun's test/js/bun/glob/match.test.ts (bun-v1.3.10): every match the test makes, with bun's answer
 * ({@code glob-match.tsv}); its "braces" case reads fixture files outside the test and records nothing.
 */
class GlobTest {

    @Test
    void matchesAsBunDoes() {
        List<String[]> rows = BunTestData.rows("glob-match.tsv");
        assertThat(rows).hasSizeGreaterThan(1000);
        List<String> failures = new ArrayList<>();
        for (String[] row : rows) {
            boolean actual = Glob.match(row[0], row[1]).matches();
            if (!row[2].equals(String.valueOf(actual))) {
                failures.add("match(\"" + row[0] + "\", \"" + row[1] + "\") is " + actual + ", bun says " + row[2]);
            }
        }
        assertThat(failures).isEmpty();
    }

    @Test
    void detectsGlobSyntax() {
        assertThat(Glob.detectGlobSyntax("packages/*")).isTrue();
        assertThat(Glob.detectGlobSyntax("!packages/a")).isTrue();
        assertThat(Glob.detectGlobSyntax("packages/{a,b}")).isTrue();
        assertThat(Glob.detectGlobSyntax("packages/a")).isFalse();
        assertThat(Glob.detectGlobSyntax("packages/\\*")).isFalse();
    }
}
