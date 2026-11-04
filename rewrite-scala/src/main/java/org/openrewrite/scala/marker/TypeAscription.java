/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.marker;

import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks a {@link org.openrewrite.java.tree.J.TypeCast} that represents a Scala type
 * ascription ({@code expr: Type}) rather than a Java-style cast ({@code (Type) expr}).
 * <p>
 * When this marker is present, the printer emits {@code expr: Type} instead of
 * {@code (Type) expr}.
 */
public class TypeAscription implements Marker {
    private final UUID id;

    public TypeAscription(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public TypeAscription withId(UUID id) {
        return new TypeAscription(id);
    }

    public static TypeAscription create() {
        return new TypeAscription(Tree.randomId());
    }
}
