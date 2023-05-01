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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.openrewrite.Cursor;

import java.util.UUID;
import java.util.function.UnaryOperator;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
public interface Marker {
    /**
     * An id that can be used to identify a particular marker, even after transformations have taken place on it
     *
     * @return A unique identifier
     */
    UUID getId();

    <M extends Marker> M withId(UUID id);

    /**
     * @param cursor The cursor at the point where the marker is being visited.
     * @param commentWrapper A function that wraps arbitrary text in a multi-line comment that is language-specific.
     * @return The printed representation of the marker.
     */
    default String print(Cursor cursor, UnaryOperator<String> commentWrapper, boolean verbose) {
        return "";
    }
}
