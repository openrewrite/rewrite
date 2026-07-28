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

/**
 * Turns a proven-safe {@link LockEditSet} into the byte-exact new lock-file content, one
 * implementation per package-manager format (npm v2/v3, pnpm v9/v6, yarn-classic, bun).
 * Implementations are format-faithful: they read the raw lock the {@link NativeLockEngine}
 * captured and patch only the entries the edit set names, preserving all other bytes.
 * <p>
 * A patcher may still {@link EngineFailure fail loud} when the raw lock cannot be parsed into
 * its format model or the edit touches a surface the format cannot express byte-exactly.
 */
public interface LockPatcher {

    /**
     * @return the new lock content, byte-for-byte what the corresponding real install would write.
     * @throws EngineFailure when the lock cannot be patched (surfaced as a structured failure).
     */
    String patch(LockEditSet edits);
}
