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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards on the whole-closure resolve-and-patch path shared by every package manager. The per-format byte
 * contracts live in the {@code <Pm>ResolveAndPatchLockRegenTest} classes; what belongs here is what must hold
 * regardless of format.
 */
class ResolveAndPatchLockRegenTest extends LockRegenTestSupport {

    @Test
    void workspaceStaysDeferredRatherThanTruncated() {
        // Whole-closure resolution reproduces a single importer; resolving just one manifest of a multi-importer
        // workspace would drop the siblings. The engine must keep such an edit deferred (a Failure), never emit
        // a truncated lock.
        String workspaceLock = "__metadata:\n  version: 8\n  cacheKey: 10c0\n\n" +
                "\"root@workspace:.\":\n  version: 0.0.0-use.local\n  resolution: \"root@workspace:.\"\n" +
                "  languageName: unknown\n  linkType: soft\n\n" +
                "\"member@workspace:packages/member\":\n  version: 0.0.0-use.local\n" +
                "  resolution: \"member@workspace:packages/member\"\n  languageName: unknown\n  linkType: soft\n";
        Result result = NativeLockEngine.regenerate(PackageManager.YarnBerry,
                "{\"name\":\"root\",\"dependencies\":{\"ms\":\"2.1.3\"}}", null, workspaceLock,
                null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getLockFileContent()).isNull();
    }
}
