/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.internal;

import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;

public class RecipeRunException extends RuntimeException {
    /**
     * Null if the exception occurs outside a visitor in an applicable test, etc.
     */
    @Getter
    @Nullable
    private final Cursor cursor;

    public RecipeRunException(Throwable cause, @Nullable Cursor cursor) {
        this(cause, cursor, null);
    }

    public RecipeRunException(Throwable cause, @Nullable Cursor cursor, @Nullable String visitedLocation) {
        super(message(visitedLocation, cause), cause);
        this.cursor = cursor;
    }

    @Nullable
    private static String message(@Nullable String visitedLocation, Throwable cause) {
        return visitedLocation == null ? null
                : String.format("Exception while visiting project file '%s', caused by: %s, at %s",
                visitedLocation, cause, cause.getStackTrace()[0]);
    }
}
