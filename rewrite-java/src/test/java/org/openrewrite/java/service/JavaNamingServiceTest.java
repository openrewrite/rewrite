package org.openrewrite.java.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class JavaNamingServiceTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
      foo_bar,fooBar
      foo$bar,fooBar
      foo_bar$,fooBar
      foo$bar$,fooBar
      """)
    void changeMethodName(String before, String after) {
        String actual = new JavaNamingService().standardizeMethodName(before);
        assertThat(actual).isEqualTo(after);
    }

}