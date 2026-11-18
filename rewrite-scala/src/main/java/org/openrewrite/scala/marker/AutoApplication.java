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
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks a {@link org.openrewrite.java.tree.J.MethodInvocation} that leaves out the empty
 * argument list its method is declared with, as {@code a.run} does for {@code def run(): Int}.
 * Scala 2 supplies the {@code ()}. Scala 3 rejects the call unless the method is
 * {@code javaDefined}, and only warns for a method of a class compiled by Scala 2.
 */
@Value
@With
public class AutoApplication implements Marker {
    UUID id;

    /**
     * Whether the method, or a method it overrides, is defined in Java or is a member of
     * {@code Any}, as {@code toString} and {@code hashCode} are. Scala 3 keeps accepting
     * these calls without {@code ()}.
     */
    boolean javaDefined;

    public AutoApplication(UUID id, boolean javaDefined) {
        this.id = id;
        this.javaDefined = javaDefined;
    }
}
