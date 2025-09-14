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
package org.openrewrite.groovy.internal;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
public enum Delimiter {
    SINGLE_QUOTE_STRING("'", "'"),
    DOUBLE_QUOTE_STRING("\"", "\""),
    TRIPLE_SINGLE_QUOTE_STRING("'''", "'''"),
    TRIPLE_DOUBLE_QUOTE_STRING("\"\"\"", "\"\"\""),
    SLASHY_STRING("/", "/"),
    DOLLAR_SLASHY_STRING("$/", "/$"),
    PATTERN_SINGLE_QUOTE_STRING("~'", "'"),
    PATTERN_DOUBLE_QUOTE_STRING("~\"", "\""),
    PATTERN_TRIPLE_SINGLE_QUOTE_STRING("~'''", "'''"),
    PATTERN_TRIPLE_DOUBLE_QUOTE_STRING("~\"\"\"", "\"\"\""),
    PATTERN_SLASHY_STRING("~/", "/"),
    PATTERN_DOLLAR_SLASHY_STRING("~$/", "$/"),
    SINGLE_LINE_COMMENT("//", "\n"),
    MULTILINE_COMMENT("/*", "*/"),
    ARRAY("[", "]"),
    CLOSURE("{", "}");

    public final String open;
    public final String close;

    public static @Nullable Delimiter of(String open) {
        for (Delimiter delim : Delimiter.values()) {
            if (delim.open.equals(open)) {
                return delim;
            }
        }
        return null;
    }
}
