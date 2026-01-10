/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python.marker;

import lombok.Getter;
import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Used for `J.Identifier` nodes that are quoted in the source.
 */
@Value
@With
public class Quoted implements Marker {
    UUID id;
    Style style;

    @Getter
    public enum Style {
        SINGLE("'"),
        DOUBLE("\""),
        TRIPLE_SINGLE("'''"),
        TRIPLE_DOUBLE("\"\"\""),
        ;

        final String quote;

        Style(String quote) {
            this.quote = quote;
        }
    }
}
