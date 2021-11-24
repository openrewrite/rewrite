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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress("CStyleArrayDeclaration")
interface ExplicitInitializationTest: JavaRecipeTest {
    override val recipe: Recipe?
        get() = ExplicitInitialization()

    @Test
    fun ignoreInterfaces() = assertUnchanged(
        before = """
            interface Test {
                private int a = 0;
                default void s() {
                    int i = 0;
                }
            }
        """
    )

    @Test
    fun blockStatement() = assertUnchanged(
        before = """
            class Test {
                void doSomething() {
                    for (int i=0; i<10; i++) {
                        System.out.println(i);
                    }
                }
            }
        """
    )
    @Test
    fun removeExplicitInitialization() = assertChanged(
        before = """
            class Test {
                private int a = 0;
                private long b = 0L;
                private short c = 0;
                private int d = 1;
                private long e = 2L;
                private int f;
                private char g = '\0';

                private boolean h = false;
                private boolean i = true;

                private Object j = new Object();
                private Object k = null;

                int l[] = null;
                int m[] = new int[0];
                
                private final Long n = null;
            }
        """,
        after = """
            class Test {
                private int a;
                private long b;
                private short c;
                private int d = 1;
                private long e = 2L;
                private int f;
                private char g;
            
                private boolean h;
                private boolean i = true;
            
                private Object j = new Object();
                private Object k;
            
                int l[];
                int m[] = new int[0];
                
                private final Long n = null;
            }
        """
    )
}
