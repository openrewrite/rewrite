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

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks method invocations that represent Scala function application syntax, where
 * {@code expr(args)} is sugar for {@code expr.apply(args)}. For example, {@code arr(0)},
 * {@code f(1)} or {@code List(1, 2)} are mapped with {@code arr}, {@code f} or {@code List}
 * as the select and {@code apply} as the name, matching their method type.
 * <p>
 * An unqualified call to a method, such as {@code println("hello")}, is not function application:
 * it is mapped without a select and with the method's own name, as in Java.
 */
@Value
@With
public class FunctionApplication implements Marker {
    UUID id;

    public static FunctionApplication create() {
        return new FunctionApplication(UUID.randomUUID());
    }
}
