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
 * Marks an expression the compiler passes through an implicit conversion, such as the
 * receiver of {@code "42".toInt}, which Scala converts to {@code StringOps}. The type of
 * the marked element remains the type of the expression as written.
 */
@Value
@With
public class ImplicitConversion implements Marker {
    UUID id;

    /**
     * The implicit method, or the {@code apply} method of the {@code Conversion} instance,
     * that converts the expression.
     */
    JavaType.@Nullable Method conversion;

    /**
     * The type of the expression after the conversion.
     */
    @Nullable
    JavaType type;

    public ImplicitConversion(UUID id, JavaType.@Nullable Method conversion, @Nullable JavaType type) {
        this.id = id;
        this.conversion = conversion;
        this.type = type;
    }
}
