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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * An error npm raises while fetching a package, with its code: {@code E404}, {@code EALLOWGIT},
 * {@code EINCOMPLETEMANIFEST}, ... The code is null where npm exits on an error that carries none.
 */
@Getter
public final class NpmRejection extends IllegalStateException implements NpmError {
    private final @Nullable String code;

    /**
     * The spec npm names in the error ({@code err.package}), when it does.
     */
    private final @Nullable String pkg;

    /**
     * The location of the dependent npm reports the failure for ({@code err.requiredBy}).
     */
    private @Nullable String requiredBy;

    public NpmRejection(@Nullable String code, String message, @Nullable String pkg) {
        super(message);
        this.code = code;
        this.pkg = pkg;
    }

    void setRequiredBy(String requiredBy) {
        this.requiredBy = requiredBy;
    }
}
