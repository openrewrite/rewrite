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
import org.jspecify.annotations.Nullable;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * A declaration written as a tag, as {@code <cffunction name="init">} is. Its attributes print in the start tag,
 * and its body, if it has one, between the start tag and the end tag.
 */
@Value
@With
public class TagSyntax implements Marker {
    UUID id;

    /**
     * The tag's name as written, as {@code cffunction} or {@code CFFUNCTION}.
     */
    String name;

    /**
     * The space before the {@code >} or {@code />} that ends the start tag.
     */
    Space beforeEnd;

    boolean selfClosing;

    /**
     * The end tag, or null for a tag that has none.
     */
    Cf.Tag.@Nullable Closing closing;
}
