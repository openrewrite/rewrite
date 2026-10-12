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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;

/**
 * The errors every package manager raises alike under its own codes, each reported under one reason.
 */
@Getter
@RequiredArgsConstructor
public enum SharedError {
    /**
     * The registry has no such package (npm E404, pnpm ERR_PNPM_FETCH_404, Yarn's and Bun's 404).
     */
    PACKAGE_NOT_FOUND(Reason.PACKAGE_NOT_FOUND),

    /**
     * No version of the package satisfies the range (npm ETARGET, pnpm ERR_PNPM_NO_MATCHING_VERSION).
     */
    NO_MATCHING_VERSION(Reason.VERSION_NOT_FOUND),

    /**
     * The package's engines exclude the host (npm EBADENGINE, pnpm ERR_PNPM_UNSUPPORTED_ENGINE).
     */
    UNSUPPORTED_ENGINE(Reason.UNSUPPORTED_ENTRY_TYPE),

    /**
     * The package's os, cpu or libc exclude the host (npm EBADPLATFORM, pnpm ERR_PNPM_UNSUPPORTED_PLATFORM).
     */
    UNSUPPORTED_PLATFORM(Reason.UNSUPPORTED_ENTRY_TYPE);

    private final Reason reason;
}
