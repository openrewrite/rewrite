package org.openrewrite;

import lombok.Value;

@Value
public class CodeExample {
    /**
     * Description of the example.
     */
    String description;

    /**
     * The example code before change.
     */
    String before;

    /**
     * The example code after change.
     */
    String after;
}
