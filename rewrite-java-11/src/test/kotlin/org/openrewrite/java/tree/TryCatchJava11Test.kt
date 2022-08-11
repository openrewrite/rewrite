/*
 * Copyright 2021 the original author or authors.
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
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.Issue
import org.openrewrite.java.Java11Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaParserResolver

@ExtendWith(JavaParserResolver::class)
class TryCatchJava11Test : JavaTreeTest, Java11Test {


    @Issue("https://github.com/openrewrite/rewrite/issues/763")
    @Test
    fun tryWithResourcesIdentifier(jp: JavaParser) = assertParsePrintAndProcess(
        jp,
        JavaTreeTest.NestingLevel.Block,
        """
            InputStream in;
            try (in) {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1027")
    @Test
    fun tryWithResourcesIdentifierAndVariables(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.Block, """
            FileInputStream fis = new FileInputStream(new File("file.txt"));
            try (fis; Scanner sc = new Scanner("")) {
            }
        """, "java.io.*", "java.util.Scanner"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1027")
    @Test
    fun tryWithResourcesIdentifierAndSemicolon(jp: JavaParser) = assertParsePrintAndProcess(
        jp, JavaTreeTest.NestingLevel.Block, """
            FileInputStream fis = new FileInputStream(new File("file.txt"));
            try (fis;) {
            }
        """, "java.io.*", "java.util.Scanner"
    )

}
