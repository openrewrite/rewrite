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
package org.openrewrite.python.internal.poetrylock;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

/**
 * A package's {@code [package.source]} table, present for non-registry (git/directory/file/url,
 * or a custom {@code legacy} index) packages. Keys are emitted only when present, in the order
 * type, url, reference, resolved_reference, subdirectory.
 */
@Value
@With
public class PoetryLockSource {
    @Nullable String type;
    String url;
    @Nullable String reference;
    @Nullable String resolvedReference;
    @Nullable String subdirectory;
}
