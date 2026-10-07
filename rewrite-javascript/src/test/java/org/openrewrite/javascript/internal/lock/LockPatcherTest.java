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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Golden tests for the yarn-berry patcher. Each golden was recorded from the real package manager (corepack
 * yarn@4) for a closure-unchanged edit; the patcher is invoked directly.
 */
class LockPatcherTest {

    // --- yarn berry (parse-only, fail loud) ----------------------------------

    @Test
    void yarnBerryFailsLoudChecksumUnavailable() {
        String before = resource("/lock/yarn-berry/basic/before");
        assertThat(before).contains("checksum:");

        PackageEdit edit = PackageEdit.builder()
                .name("is-odd")
                .oldVersion("3.0.1")
                .newVersion("3.0.2")
                .oldConstraint("^3.0.1")
                .scope("dependencies")
                .build();

        LockEditSet edits = new LockEditSet(
                before,
                Paths.get("yarn.lock"),
                PackageManager.YarnBerry,
                resource("/lock/yarn-berry/basic/pkg-before"),
                singletonList(edit));

        assertThatThrownBy(() -> new YarnBerryLockPatcher().patch(edits))
                .isInstanceOfSatisfying(EngineFailure.class, ef -> {
                    assertThat(ef.failure.getReason()).isEqualTo(Reason.CHECKSUM_UNAVAILABLE);
                    assertThat(ef.failure.getPackageName()).isEqualTo("is-odd");
                });
    }

    private static String resource(String path) {
        try (InputStream in = LockPatcherTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
