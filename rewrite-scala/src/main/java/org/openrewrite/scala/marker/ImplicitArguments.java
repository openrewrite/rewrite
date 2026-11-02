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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.util.List;
import java.util.UUID;

/**
 * Marks an expression the compiler passes implicit arguments to, such as {@code Future { 1 }},
 * which Scala calls as {@code Future.apply { 1 }(global)} when
 * {@code scala.concurrent.ExecutionContext.Implicits.global} is imported. Arguments written
 * in the source, even to an implicit or {@code using} parameter list, aren't listed.
 */
@Value
@With
public class ImplicitArguments implements Marker {
    UUID id;

    /**
     * The definitions the compiler passes, in the order it passes them: a variable for a
     * value or an object, a method for a method. A definition that takes implicit arguments
     * of its own is followed by the definitions passed to it.
     */
    List<JavaType> arguments;

    public ImplicitArguments(UUID id, List<JavaType> arguments) {
        this.id = id;
        this.arguments = arguments;
    }
}
