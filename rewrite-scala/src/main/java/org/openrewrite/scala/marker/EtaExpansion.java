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
package org.openrewrite.scala.marker;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks a reference to a method that the compiler turns into a function value, such as
 * {@code inc} in {@code xs.map(inc)}. The type of the marked element remains the type of
 * the method reference as written.
 */
@Value
@With
public class EtaExpansion implements Marker {
    UUID id;

    /**
     * The method that is expanded.
     */
    JavaType.@Nullable Method method;

    /**
     * The function type the method is expanded to.
     */
    @Nullable
    JavaType type;

    public EtaExpansion(UUID id, JavaType.@Nullable Method method, @Nullable JavaType type) {
        this.id = id;
        this.method = method;
        this.type = type;
    }
}
