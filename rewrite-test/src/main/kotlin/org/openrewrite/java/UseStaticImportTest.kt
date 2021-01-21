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
import org.openrewrite.RecipeTest

interface UseStaticImportTest : RecipeTest {
    @Test
    fun replaceWithStaticImports(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package asserts;
                
                public class Assert {
                    public static void assertTrue(boolean b) {}
                    public static void assertFalse(boolean b) {}
                    public static void assertEquals(int m, int n) {}
                }
            """
        ),
        recipe = UseStaticImport("asserts.Assert assert*(..)"),
        before = """
            package test;
            
            import asserts.Assert;
            
            class Test {
                void test() {
                    Assert.assertTrue(true);
                    Assert.assertFalse(false);
                    Assert.assertEquals(1, 2);
                }
            }
        """,
        after = """
            package test;
            
            import static asserts.Assert.*;
            
            class Test {
                void test() {
                    assertTrue(true);
                    assertFalse(false);
                    assertEquals(1, 2);
                }
            }
        """
    )

    @Test
    fun junit5Assertions(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp
            .classpath(JavaParser.dependenciesFromClasspath("junit-jupiter-api", "apiguardian-api"))
            .build(),
        recipe = UseStaticImport("org.junit.jupiter.api.Assertions assert*(..)"),
        before = """
            package org.openrewrite;

            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.Assertions;

            class Sample {
                @Test
                void sample() {
                    Assertions.assertEquals(42, 21*2);
                }
            }
        """,
        after = """
            package org.openrewrite;

            import org.junit.jupiter.api.Test;
            
            import static org.junit.jupiter.api.Assertions.assertEquals;

            class Sample {
                @Test
                void sample() {
                    assertEquals(42, 21*2);
                }
            }
        """
    )
}
