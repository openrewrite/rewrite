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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bun.lock snapshots of bun's own bun-lock.test.ts and catalogs.test.ts (bun-v1.3.10), each loaded, cleaned and
 * written again to the same bytes, without a manifest beside it.
 */
class TextLockfileWriterTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "bun-lock-should-be-the-default-save-format-1",
      "bun-lock-should-be-the-default-save-format-2",
      "bun-lock-should-convert-a-binary-lockfile-with-invalid-optional-peers-1",
      "bun-lock-should-escape-names-1",
      "bun-lock-should-include-unused-resolutions-in-the-lockfile-1",
      "bun-lock-should-not-change-formatting-unexpectedly-2",
      "bun-lock-should-save-the-lockfile-if-save-text-lockfile-and-frozen-lockfile-are-used-1",
      "bun-lock-should-save-the-lockfile-if-save-text-lockfile-and-frozen-lockfile-are-used-2",
      "bun-lock-should-sort-overrides-before-comparing-1",
      "bun-lock-should-write-plaintext-lockfiles-1",
      "catalogs-basic-detect-changes-bun-lock-1",
      "catalogs-basic-detect-changes-bun-lock-2",
      "catalogs-basic-detect-changes-bun-lock-3"
    })
    void writesBunsSnapshotBack(String snapshot) {
        String lock = BunTestData.resource("snapshots/" + snapshot + ".lock");
        Lockfile lockfile = TextLockfileReader.load(lock,
          BunOptions.load(null, null, null, HostEnvironment.UNKNOWN));
        int configVersion = lockfile.savedConfigVersion != null ? lockfile.savedConfigVersion : 0;
        assertThat(TextLockfileWriter.saveFromBinary(Cleaner.clean(lockfile), configVersion)).isEqualTo(lock);
    }
}
