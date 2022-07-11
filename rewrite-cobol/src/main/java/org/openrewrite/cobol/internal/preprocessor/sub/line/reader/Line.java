package org.openrewrite.cobol.internal.preprocessor.sub.line.reader;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.openrewrite.internal.lang.Nullable;

@Data
@AllArgsConstructor
public class Line {
    String text;
    String newLine;
}
