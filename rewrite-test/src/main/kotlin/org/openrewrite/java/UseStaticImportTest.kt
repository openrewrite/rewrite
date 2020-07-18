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
package org.openrewrite.java

import org.junit.jupiter.api.Test

interface UseStaticImportTest {
    @Test
    fun replaceWithStaticImports(jp: JavaParser) {
        val asserts = """
            package asserts;
            
            public class Assert {
                public static void assertTrue(boolean b) {}
                public static void assertFalse(boolean b) {}
                public static void assertEquals(int m, int n) {}
            }
        """.trimIndent()

        val test = """
            package test;
            
            import asserts.Assert;
                        
            class Test {
                void test() {
                    Assert.assertTrue(true);
                    Assert.assertFalse(false);
                    Assert.assertEquals(1, 2);
                }
            }
        """.trimIndent()

        val fixed = jp.parse(test, asserts).refactor().visit(UseStaticImport().apply {
            setMethod("asserts.Assert assert*(..)")
        }).fix().fixed

        assertRefactored(fixed, """
            package test;
            
            import static asserts.Assert.*;
            
            class Test {
                void test() {
                    assertTrue(true);
                    assertFalse(false);
                    assertEquals(1, 2);
                }
            }
        """.trimIndent())
    }
}
