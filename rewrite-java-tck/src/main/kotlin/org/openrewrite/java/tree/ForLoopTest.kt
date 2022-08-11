/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.JavaTreeTest.NestingLevel.Block

interface ForLoopTest : JavaTreeTest {

    @Test
    fun forLoopMultipleInit(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            int i;
            int j;
            for(i = 0, j = 0;;) {
            }
        """
    )

    @Test
    fun forLoop(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            for(int i = 0; i < 10; i++) {
            }
        """
    )

    @Test
    fun infiniteLoop(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            for(;;) {
            }
        """
    )

    @Test
    fun format(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            for ( int i = 0 ; i < 10 ; i++ ) {
            }
        """
    )

    @Test
    fun formatInfiniteLoop(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            for ( ; ; ) {}
        """
    )

    @Test
    fun formatLoopNoInit(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            for ( ; i < 10 ; i++ ) {}
        """
    )

    @Test
    fun formatLoopNoCondition(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            int i = 0;
            for(; i < 10; i++) {}
        """
    )

    @Test
    fun statementTerminatorForSingleLineForLoops(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            for(;;) test();
        """
    )

    @Test
    fun initializerIsAnAssignment(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
                int[] a;
                int i=0;
                for(i=0; i<a.length; i++) {}
        """
    )

    @Test
    fun multiVariableInitialization(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            for(int i, j = 0;;) {}
        """
    )
}
