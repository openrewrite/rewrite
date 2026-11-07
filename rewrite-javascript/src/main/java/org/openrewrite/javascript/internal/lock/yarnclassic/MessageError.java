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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Port of yarn src/errors.js {@code MessageError} (yarn 1.22.22): the error yarn reports as {@code error <message>}.
 */
@Getter
class MessageError extends RuntimeException {

    private final @Nullable String code;

    /**
     * Whether yarn made this error of its own rejection, which it took for another's because {@code instanceof
     * MessageError} failed it (1.5.x): its words name another cause than the one that ended the install.
     */
    private boolean rewrapped;

    MessageError(String message) {
        this(message, null);
    }

    MessageError(String message, @Nullable String code) {
        super(message);
        this.code = code;
    }

    MessageError rewrapped() {
        rewrapped = true;
        return this;
    }
}
