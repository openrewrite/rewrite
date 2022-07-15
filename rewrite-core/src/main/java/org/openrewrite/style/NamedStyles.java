/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.style;

import lombok.*;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * A collection of styles by name, e.g. IntelliJ or Google Java Format.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@With
public class NamedStyles implements Marker {
    UUID id;

    @EqualsAndHashCode.Include
    String name;

    String displayName;

    @Nullable
    String description;

    Set<String> tags;
    Collection<Style> styles;

    @SuppressWarnings("unchecked")
    @Nullable
    public static <S extends Style> S merge(Class<S> styleClass,
                                                                                              Iterable<? extends NamedStyles> namedStyles) {
        S merged = null;
        for (NamedStyles namedStyle : namedStyles) {
            for (Style style : namedStyle.styles) {
                if (styleClass.isInstance(style)) {
                    style = style.applyDefaults();
                    if (merged == null) {
                        merged = (S) style;
                    } else {
                        merged = (S) merged.merge(style);
                    }
                }
            }
        }
        return merged;
    }

    public Validated validate() {
        return Validated.none();
    }
}
