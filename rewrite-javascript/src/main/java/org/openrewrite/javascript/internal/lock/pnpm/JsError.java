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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * A JavaScript {@code Error} without a pnpm code that ends the install, with Node's code when it has one.
 */
@Getter
final class JsError extends RuntimeException {

    private final @Nullable String code;

    JsError(String message) {
        this(null, message);
    }

    JsError(@Nullable String code, String message) {
        super(message);
        this.code = code;
    }
}
