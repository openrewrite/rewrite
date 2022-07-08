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
package org.openrewrite.cobol.tree

import org.junit.jupiter.api.Test

class CobolBasicsTest : CobolTreeTest {
    @Test
    fun helloWorld() = roundTrip(
        """
        IDENTIFICATION  DIVISION .
        PROGRAM-ID    . HELLO     .
        PROCEDURE DIVISION.
        DISPLAY 'Hello world!'.
        STOP RUN.
        """)

    @Test
    fun arithmetic() = roundTrip(
        """
        IDENTIFICATION DIVISION .
        PROGRAM-ID . HELLO-WORLD .
        DATA DIVISION .
            WORKING-STORAGE SECTION .
                77 X PIC 99.
                77 Y PIC 99.
                77 Z PIC 99.
        PROCEDURE DIVISION .
            SET X TO 10 .
            SET Y TO 25 .
            ADD X Y GIVING Z .
            DISPLAY "X + Y = "Z .
        STOP RUN .
        """)
}

