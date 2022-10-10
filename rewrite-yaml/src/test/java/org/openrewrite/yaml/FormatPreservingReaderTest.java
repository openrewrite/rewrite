package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FormatPreservingReaderTest {

    @Test
    void allInCurrentBuffer() throws IOException {
        var text = "0123456789";
        var formatPreservingReader = new FormatPreservingReader(new StringReader(text));

        char[] charArray = new char[10];
        formatPreservingReader.read(charArray, 0, 10);
        assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
    }

    @Test
    void allInPreviousBuffer() throws IOException {
        var text = "0123456789";
        var formatPreservingReader = new FormatPreservingReader(new StringReader(text));

        char[] charArray = new char[10];

        formatPreservingReader.read(charArray, 0, 5);
        formatPreservingReader.read(charArray, 0, 5);

        assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
    }

    @Test
    void splitBetweenPrevAndCurrentBuffer() throws IOException {
        var text = "0123456789";
        var formatPreservingReader = new FormatPreservingReader(new StringReader(text));

        char[] charArray = new char[10];

        formatPreservingReader.read(charArray, 0, 1);
        formatPreservingReader.read(charArray, 0, 9);

        assertThat(formatPreservingReader.prefix(0, 3)).isEqualTo("012");
    }
}
