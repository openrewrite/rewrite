package org.openrewrite.kotlin.internal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;

@EqualsAndHashCode
@Data
public class PsiToken {
    TextRange range;
    String type;
    String text;

    @Override
    public String toString() {
        return range + " | Type: " + type + " | Text: \"" + text.replace("\n", "\\n") + "\"";
    }
}
