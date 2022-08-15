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
import org.openrewrite.java.tree.JavaTreeTest.NestingLevel.CompilationUnit

interface LambdaTest : JavaTreeTest {

    @Test
    fun lambda(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            Function<String, String> func = (String s) -> "";
        """, "java.util.function.Function"
    )

    @Test
    fun untypedLambdaParameter(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            List<String> list = new ArrayList<>();
            list.stream().filter(s -> s.isEmpty());
        """, "java.util.*"
    )

    @Test
    fun optionalSingleParameterParentheses(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            List<String> list = new ArrayList<>();
            list.stream().filter((s) -> s.isEmpty());
        """, "java.util.*"
    )

    @Test
    fun rightSideBlock(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public class A {
                Action a = ( ) -> { };
            }

            interface Action {
                void call();
            }
        """
    )

    @Test
    fun multipleParameters(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            BiConsumer<String, String> a = (s1, s2) -> { };
        """, "java.util.function.BiConsumer"
    )
}
