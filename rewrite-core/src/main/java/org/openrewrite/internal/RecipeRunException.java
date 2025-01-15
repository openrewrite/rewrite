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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;

/**
 * This provides a way for us to capture the cursor position where an unexpected exception was thrown so that
 * {@link org.openrewrite.RecipeScheduler} can add an {@link org.openrewrite.marker.Markup.Error} marker on the
 * part of the LST as part of the recipe running lifecycle.
 * <p>
 * Developers should never see this exception propagated out of the recipe running lifecycle.
 */
public class RecipeRunException extends RuntimeException {
    @Getter
    @Nullable
    private final Cursor cursor;

    public RecipeRunException(Throwable cause, @Nullable Cursor cursor) {
        super(cause);
        this.cursor = cursor;
    }
}
