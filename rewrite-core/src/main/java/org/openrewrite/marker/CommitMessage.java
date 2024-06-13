/*
 * Copyright 2024 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;

import static org.openrewrite.Tree.randomId;

/**
 * A marker that indicates an extra message to be appended to the message of any commit generated from the recipe run.
 */
@Value
public class CommitMessage implements Marker {
    @EqualsAndHashCode.Exclude
    @With
    UUID id;

    String recipeName;
    String message;

    public static <T extends Tree> T message(Tree t, Recipe r, @Nullable String message) {
        if(message == null) {
            //noinspection unchecked
            return (T) t;
        }
        return t.withMarkers(t.getMarkers().add(new CommitMessage(randomId(), r.getName(), message)));
    }
}
