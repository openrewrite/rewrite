/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.marker;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.openrewrite.Tree.randomId;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@With
public class SearchResult implements Marker {
    UUID id;

    @Nullable
    String description;

    public static <T extends Tree> T found(@Nullable T t) {
        return found(t, null);
    }

    public static <T extends Tree> T found(@Nullable T t, @Nullable String description) {
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        return t.withMarkers(t.getMarkers().computeByType(new SearchResult(randomId(), description),
                (s1, s2) -> s1 == null ? s2 : s1));
    }

    /**
     * @param cursor         The cursor at the point where the marker is being visited.
     * @param commentWrapper A function that wraps arbitrary text in a multi-line comment that is language-specific.
     * @return The printed representation of the marker.
     */
    @Override
    public String print(Cursor cursor, UnaryOperator<String> commentWrapper, boolean verbose) {
        return commentWrapper.apply(description == null ? "" : "(" + description + ")");
    }
}
