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
package org.openrewrite.config;

import org.jspecify.annotations.Nullable;
import org.openrewrite.style.GeneralFormatStyle;

import java.util.Map;

/**
 * Static helpers for mapping universal {@code .editorconfig} properties to
 * language-neutral style values. Language modules compose these into their
 * own style objects.
 */
public final class EditorConfigStyles {
    private EditorConfigStyles() {
    }

    /**
     * @return {@code true} for tabs, {@code false} for spaces, {@code null} if unset
     */
    public static @Nullable Boolean useTabCharacter(Map<String, String> props) {
        String value = props.get("indent_style");
        if ("tab".equals(value)) {
            return true;
        } else if ("space".equals(value)) {
            return false;
        }
        return null;
    }

    /**
     * @return the indent size, or {@code null} if unset. Handles the special case
     * where {@code indent_size=tab} means use the {@code tab_width} value.
     */
    public static @Nullable Integer indentSize(Map<String, String> props) {
        String value = props.get("indent_size");
        if (value == null) {
            return null;
        }
        if ("tab".equals(value)) {
            return tabSize(props);
        }
        return parsePositiveInt(value);
    }

    /**
     * @return the tab size, or {@code null} if unset. Falls back to {@code indent_size}
     * if {@code tab_width} is not explicitly set.
     */
    public static @Nullable Integer tabSize(Map<String, String> props) {
        String value = props.get("tab_width");
        if (value != null) {
            return parsePositiveInt(value);
        }
        // Per spec, tab_width defaults to indent_size when not set
        String indentSizeValue = props.get("indent_size");
        if (indentSizeValue != null && !"tab".equals(indentSizeValue)) {
            return parsePositiveInt(indentSizeValue);
        }
        return null;
    }

    /**
     * @return a {@link GeneralFormatStyle} based on {@code end_of_line}, or {@code null} if unset
     */
    public static @Nullable GeneralFormatStyle generalFormatStyle(Map<String, String> props) {
        String value = props.get("end_of_line");
        if ("crlf".equals(value)) {
            return new GeneralFormatStyle(true);
        } else if ("lf".equals(value) || "cr".equals(value)) {
            return new GeneralFormatStyle(false);
        }
        return null;
    }

    private static @Nullable Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
