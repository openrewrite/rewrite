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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.marker.Marker;

import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * A collection of styles by name, e.g., IntelliJ IDEA or Google Java Format.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@With
public class NamedStyles implements Marker {
    UUID id;

    @EqualsAndHashCode.Include
    String name;

    @NlsRewrite.DisplayName
    String displayName;

    @Nullable
    @NlsRewrite.Description
    String description;

    Set<String> tags;
    Collection<Style> styles;

    /**
     * Returns the effective style for a given style class.
     * There may be multiple named styles which provide the same style class, in whole or in part.
     * Consider the example of a formatting recipe which want so to know whether to use tabs or spaces for indentation.
     * The build plugins will attach one or more NamedStyles which provide TabsAndIndents.
     * Autodetect and Checkstyle may both provide some or all of TabsAndBraces.
     * Autodetect is a weaker signal than explicit configuration so it is attached earlier in the parsing process.
     * Therefore, this method gives precedence to non-null values from styles which appear later in the list.
     *
     * @param styleClass the style class whose effective value is being queried for.
     * @param namedStyles the list of named styles to extract a specific effective style from.
     * @return the effective style as computed from the list of named styles.
     * @param <S> a style class.
     */
    @SuppressWarnings("unchecked")
    public static <S extends Style> @Nullable S merge(Class<S> styleClass,
                                            Iterable<? extends NamedStyles> namedStyles) {
        S merged = null;
        for (NamedStyles namedStyle : namedStyles) {
            Collection<Style> styles = namedStyle.styles;
            //noinspection ConstantValue
            if (styles != null) {
                for (Style style : styles) {
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
        }
        return merged;
    }

    /**
     * Merge many NamedStyles into one NamedStyle.
     *
     * @param styles The styles to be merged. Styles earlier in the list take precedence over styles later in the list.
     * @return A single merged style with the aggregate configuration of all inputs. null if the list is empty.
     */
    public static @Nullable NamedStyles merge(List<NamedStyles> styles) {
        if (styles.isEmpty()) {
            return null;
        } else if (styles.size() == 1) {
            return styles.get(0);
        }
        Set<Class<? extends Style>> styleClasses = new HashSet<>();
        for (NamedStyles namedStyles : styles) {
            for (Style style : namedStyles.getStyles()) {
                styleClasses.add(style.getClass());
            }
        }

        List<Style> mergedStyles = new ArrayList<>(styleClasses.size());
        for (Class<? extends Style> styleClass : styleClasses) {
            //noinspection DataFlowIssue
            mergedStyles.add(NamedStyles.merge(styleClass, styles));
        }

        return new NamedStyles(Tree.randomId(), "MergedStyles",
                "Merged styles",
                "Merged Styles from " + styles.stream().map(NamedStyles::getName).collect(joining(", ")),
                styles.stream().map(NamedStyles::getTags).flatMap(Set::stream).collect(toSet()),
                mergedStyles);
    }

    public Validated<Object> validate() {
        return Validated.none();
    }

    public <S extends Style> S getStyle(Class<S> styleClass) {
        S ret = merge(styleClass, singletonList(this));
        assert(ret != null);
        return ret;
    }
}
