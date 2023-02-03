package org.openrewrite.python;


import org.junit.jupiter.api.Test;

import static org.openrewrite.python.Assertions.python;

public class PythonParserTest {

    @Test
    void testParse() {
        python("""
                print(42)
                """);
    }

}
