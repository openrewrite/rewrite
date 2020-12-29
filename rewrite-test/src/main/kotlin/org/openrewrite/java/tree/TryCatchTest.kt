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
import org.openrewrite.java.JavaParserTest
import org.openrewrite.java.JavaParserTest.NestingLevel.Block

interface TryCatchTest : JavaParserTest {

    @Test
    fun tryFinally(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            try {
            }
            finally {
            }
        """
    )

    @Test
    fun tryCatchNoFinally(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            try {
            }
            catch(Throwable t) {
            }
        """
    )

    @Test
    fun tryWithResources(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            File f = new File("file.txt");
            try(FileInputStream fis = new FileInputStream(f)) {
            }
            catch(IOException e) {
            }
        """, "java.io.*"
    )

    @Test
    fun multiCatch(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            File f = new File("file.txt");
            try(FileInputStream fis = new FileInputStream(f)) {}
            catch(FileNotFoundException | RuntimeException e) {}
        """, "java.io.*"
    )

    @Test
    fun tryCatchFinally(jp: JavaParser) = assertParseAndPrint(
        jp, Block, """
            try {}
            catch(Exception e) {}
            catch(RuntimeException e) {}
            catch(Throwable t) {}
            finally {}
        """
    )
}
