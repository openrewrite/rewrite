/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.xml.style;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.config.EditorConfigStyles;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;

/**
 * Converts resolved {@code .editorconfig} properties to XML-specific {@link NamedStyles}.
 */
public final class EditorConfigStyleMapper {
    private EditorConfigStyleMapper() {
    }

    /**
     * Creates a {@link NamedStyles} from editorconfig properties containing XML-relevant styles.
     *
     * @return a NamedStyles marker, or {@code null} if no relevant properties are present
     */
    public static @Nullable NamedStyles fromEditorConfig(Map<String, String> properties) {
        List<Style> styles = new ArrayList<>(2);

        Boolean useTabs = EditorConfigStyles.useTabCharacter(properties);
        Integer indentSize = EditorConfigStyles.indentSize(properties);
        Integer tabSize = EditorConfigStyles.tabSize(properties);

        if (useTabs != null || indentSize != null || tabSize != null) {
            Integer continuationIndentSize = indentSize != null ? indentSize * 2 : null;
            styles.add(new TabsAndIndentsStyle(useTabs, tabSize, indentSize, continuationIndentSize));
        }

        GeneralFormatStyle generalFormat = EditorConfigStyles.generalFormatStyle(properties);
        if (generalFormat != null) {
            styles.add(generalFormat);
        }

        if (styles.isEmpty()) {
            return null;
        }

        return new NamedStyles(
                Tree.randomId(),
                "org.openrewrite.xml.EditorConfig",
                "EditorConfig",
                "Styles derived from .editorconfig",
                Collections.emptySet(),
                styles
        );
    }
}
