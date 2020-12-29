package org.openrewrite;

import org.openrewrite.internal.StringUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ParserTest {
    public void assertPrintEquals(Parser<SourceFile> parser, String before) {
        SourceFile source = parser.parse(before).iterator().next();
        assertThat(source.printTrimmed()).isEqualTo(StringUtils.trimIndent(before));
    }
}
