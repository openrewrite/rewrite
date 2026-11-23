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
package org.openrewrite.coldfusion.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * An attribute that names a part of a declaration, which the declaration holds as that part rather than as an
 * annotation: a component's {@code extends="models.Base"}, or a function's or variable's {@code name} or
 * {@code type}. It records what is written around the value: the attribute's name, how many of the declaration's
 * attributes come before it, the space on each side of its {@code =}, and the quote, which is empty when the value
 * is not quoted. The value's own prefix is the space inside the quotes.
 */
@Value
@With
public class AttributeClause implements Marker {
    UUID id;

    /**
     * The space before the attribute. A component's {@code extends} and {@code implements} keep it in their
     * padding instead, so it is empty for them.
     */
    Space prefix;

    String name;
    int position;
    Space beforeEquals;
    Space afterEquals;
    String quote;

    public boolean isNamed(String name) {
        return this.name.equalsIgnoreCase(name);
    }
}
