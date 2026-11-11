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
package org.openrewrite.python.internal.uvlock;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * The {@code [options]} table, emitted by uv only for non-default options.
 */
@Value
@With
public class UvLockOptions {

    /** uv writes this in place of a relative {@code exclude-newer}, with the comment on the same line. */
    static final String RELATIVE_EXCLUDE_NEWER_TIMESTAMP = "0001-01-01T00:00:00Z";

    static final String RELATIVE_EXCLUDE_NEWER_COMMENT =
            "# This has no effect and is included for backwards compatibility when using relative exclude-newer values.";

    /** Non-default resolution, prerelease and fork settings, as uv writes them ahead of exclude-newer. */
    @Nullable
    String resolutionMode;

    @Nullable
    String prereleaseMode;

    @Nullable
    String forkStrategy;

    @Nullable
    String excludeNewer;

    /** The ISO 8601 duration of a relative exclude-newer, beside the sentinel or, from an older uv, a timestamp. */
    @Nullable
    String excludeNewerSpan;

    /** The {@code [options.exclude-newer-package]} sub-table: a timestamp string, or {@code false}, per package. */
    @Nullable
    Map<String, Object> excludeNewerPackage;
}
