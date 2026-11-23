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
 * The lock as read from the project: its text, parsed once for everything that reads it, whether or not it parses.
 */
public final class LockFile {

    @Getter
    private final String text;

    private JsonParseEvenBetterErrors.@Nullable Parsed parsed;

    private @Nullable RuntimeException error;

    public LockFile(String text) {
        this.text = text;
    }

    JsonParseEvenBetterErrors.Parsed parsed() {
        if (error != null) {
            throw error;
        }
        if (parsed == null) {
            try {
                parsed = JsonParseEvenBetterErrors.parse(text);
            } catch (RuntimeException e) {
                error = e;
                throw e;
            }
        }
        return parsed;
    }
}
