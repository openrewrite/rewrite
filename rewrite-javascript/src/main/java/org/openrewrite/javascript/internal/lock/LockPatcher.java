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
 * Turns a proven-safe {@link LockEditSet} into byte-exact new lock content, one implementation per format:
 * it patches only the entries the edit set names and preserves all other bytes, or {@link EngineFailure
 * fails loud} when the format cannot express the edit byte-exactly.
 */
public interface LockPatcher {

    /**
     * @return the new lock content, byte-for-byte what the corresponding real install would write.
     * @throws EngineFailure when the lock cannot be patched (surfaced as a structured failure).
     */
    String patch(LockEditSet edits);
}
