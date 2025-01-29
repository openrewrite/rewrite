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
    PATTERN_OPERATOR("~/", "/"),
    SINGLE_LINE_COMMENT("//", "\n"),
    MULTILINE_COMMENT("/*", "*/");

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
