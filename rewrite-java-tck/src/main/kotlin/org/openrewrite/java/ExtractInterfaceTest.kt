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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.internal.ListUtils
import org.openrewrite.java.tree.JavaSourceFile

interface ExtractInterfaceTest : JavaRecipeTest {

    @Test
    fun extractInterface(jp: JavaParser) {
        val cus = jp.parse(
            """
                package org.openrewrite;
                
                class Test {
                    int f;
                    
                    public Test() {
                    }
                    
                    public final int test() {
                        return 0;
                    }
                    
                    private int privateTest() {
                    }
                    
                    public static int staticTest() {
                    }
                }
            """.trimIndent()
        )

        val results = object: Recipe() {
            override fun getDisplayName(): String = "Extract interface"

            override fun visit(before: List<SourceFile>, ctx: ExecutionContext): List<SourceFile> {
                return ListUtils.flatMap(before) { b ->
                    ExtractInterface.extract(b as JavaSourceFile, "org.openrewrite.interfaces.ITest")
                }
            }
        }.run(cus).results

        assertThat(results[0].after!!.printAll()).isEqualTo(
            //language=java
            """
                package org.openrewrite.interfaces;
                
                interface ITest {
                
                    int test();
                }
            """.trimIndent()
        )

        assertThat(results[1].after!!.printAll()).isEqualTo(
            //language=java
            """
                package org.openrewrite;
                
                import org.openrewrite.interfaces.ITest;
                
                class Test implements ITest {
                    int f;
                    
                    public Test() {
                    }
                
                    @Override
                    public final int test() {
                        return 0;
                    }
                    
                    private int privateTest() {
                    }
                    
                    public static int staticTest() {
                    }
                }
            """.trimIndent()
        )
    }
}
