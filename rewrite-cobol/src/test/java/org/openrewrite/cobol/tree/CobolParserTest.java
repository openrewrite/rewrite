/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.cobol.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

public class CobolParserTest implements RewriteTest {

    @Test
    void helloWorld() {
        rewriteRun(
                cobol("" +
                        "IDENTIFICATION  DIVISION .\n" +
                        "PROGRAM-ID    . HELLO     .\n" +
                        "PROCEDURE DIVISION.\n" +
                        "   DISPLAY 'Hello world!'.\n" +
                        "   STOP RUN."
                )
        );
    }

    @Test
    void arithmetic() {
        rewriteRun(
                cobol("" +
                        "IDENTIFICATION DIVISION .\n" +
                        "PROGRAM-ID . HELLO-WORLD .\n" +
                        "DATA DIVISION .\n" +
                        "    WORKING-STORAGE SECTION .\n" +
                        "        77 X PIC 99.\n" +
                        "        77 Y PIC 99.\n" +
                        "        77 Z PIC 99.\n" +
                        "PROCEDURE DIVISION .\n" +
                        "    SET X TO 10 .\n" +
                        "    SET Y TO 25 .\n" +
                        "    ADD X Y GIVING Z .\n" +
                        "    DISPLAY \"X + Y = \"Z .\n" +
                        "STOP RUN .\n")
        );
    }
}
