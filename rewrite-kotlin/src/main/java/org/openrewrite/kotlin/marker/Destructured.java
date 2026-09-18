/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * A {@code J.VariableDeclarations} whose names are written as a parenthesized destructuring pattern,
 * as in {@code for ((a, b) in pairs)}. A single-component pattern needs the marker to keep its
 * parentheses, since one name is otherwise indistinguishable from an ordinary declaration.
 */
@Value
@With
public class Destructured implements Marker {
    UUID id;

    public Destructured(UUID id) {
        this.id = id;
    }
}
