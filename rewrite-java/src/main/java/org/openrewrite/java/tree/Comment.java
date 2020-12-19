package org.openrewrite.java.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class Comment {
    @With
    String prefix;

    @With
    Style style;

    @With
    String text;

    enum Style {
        LINE,
        BLOCK,
        JAVADOC,
        WHITESPACE
    }
}
