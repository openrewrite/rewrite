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

import org.jspecify.annotations.Nullable;

/**
 * The registry documents a Bun install reads.
 */
public interface ManifestSource {

    /**
     * {@code GET <registry><name>}: the abbreviated packument, or the full one (with {@code time}) when
     * {@code extended}; null when the registry has no such package.
     */
    @Nullable
    String packument(String registry, String name, boolean extended);
}
