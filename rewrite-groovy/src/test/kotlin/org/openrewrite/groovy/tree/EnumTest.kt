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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

class xEnumTest : GroovyTreeTest {

    @Disabled
    @Test
    fun enum() = assertParsePrintAndProcess(
        """
            enum A {
                B, C,
                D;
            }
        """
    )

    @Disabled
    @Test
    fun innerEnum() = assertParsePrintAndProcess(
        """
            class A {
                enum B {
                    C
                }
            }
        """
    )

    @Disabled
    @Test
    fun enumWithAnnotations() = assertParsePrintAndProcess(
        """
            enum Test {
                @Deprecated(since = "now")
                One,
                
                @Deprecated(since = "now")
                Two;
            }
        """
    )

    @Disabled
    @Test
    fun anonymousClassInitializer() = assertParsePrintAndProcess(
        """
            enum A {
                A1(1) {
                    @Override
                    void foo() {}
                },

                A2 {
                    @Override
                    void foo() {}
                };
                
                A() {}
                A(int n) {}
                
                abstract void foo();
            }
        """
    )

    @Disabled
    @Test
    fun enumConstructor() = assertParsePrintAndProcess(
        """
            class Outer {
                enum A {
                    A1(1);
    
                    A(int n) {}
                }
                
                private static final class ContextFailedToStart {
                    private static Object[] combineArguments(String context, Throwable ex, Object[] arguments) {
                        return new Object[arguments.length + 2];
                    }
                }
            }
        """
    )

    @Disabled
    @Test
    fun noArguments() = assertParsePrintAndProcess(
        """
            enum A {
                A1, A2();
            }
        """
    )

    @Disabled
    @Test
    fun enumWithParameters() = assertParsePrintAndProcess(
        """
            enum A {
                ONE(1),
                TWO(2);
            
                A(int n) {}
            }
        """
    )

    @Disabled
    @Test
    fun enumWithoutParameters() = assertParsePrintAndProcess(
        "enum A { ONE, TWO }"
    )

    @Disabled
    @Test
    fun enumUnnecessarilyTerminatedWithSemicolon() = assertParsePrintAndProcess(
        "enum A { ONE ; }"
    )

    @Disabled
    @Test
    fun enumWithEmptyParameters() = assertParsePrintAndProcess(
        "enum A { ONE ( ), TWO ( ) }"
    )
}
