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
package org.openrewrite.java

import org.junit.jupiter.api.Test

interface RemoveImplementsTest : JavaRecipeTest {

    @Test
    fun removeBasic(jp: JavaParser) = assertChanged(
        jp,
        recipe = RemoveImplements("java.io.Serializable", null),
        before = """
            import java.io.Serializable;
            
            class A implements Serializable {
            }
        """,
        after = """
            class A {
            }
        """
    )

    @Test
    fun preservesOtherInterfaces(jp: JavaParser) = assertChanged(
        jp,
        recipe =RemoveImplements("java.io.Serializable", ""),
        before = """
            import java.io.Closeable;
            import java.io.Serializable;
            
            class A implements Serializable, Closeable {
            }
        """,
        after = """
            import java.io.Closeable;
            
            class A implements Closeable {
            }
        """
    )

    @Test
    fun removeFullyQualifiedInterface(jp: JavaParser) = assertChanged(
        jp,
        recipe = RemoveImplements("java.io.Serializable", null),
        before = """
            class A implements java.io.Serializable {
            }
        """,
        after = """
            class A {
            }
        """
    )

    @Test
    fun innerClassOnly(jp: JavaParser) = assertChanged(
        jp,
        recipe = RemoveImplements("java.io.Serializable", "com.yourorg.Outer${'$'}Inner"),
        before = """
            package com.yourorg;
            
            import java.io.Serializable;
            
            class Outer implements Serializable {
                class Inner implements Serializable {
                }
            }
        """,
        after = """
            package com.yourorg;
            
            import java.io.Serializable;
            
            class Outer implements Serializable {
                class Inner {
                }
            }
        """
    )

    @Suppress("RedundantThrows")
    @Test
    fun removeOverrideFromMethods(jp: JavaParser) = assertChanged(
        jp,
        recipe = RemoveImplements("java.io.Closeable", null),
        before = """
            import java.io.Closeable;
            import java.io.IOException;
            
            class A implements Closeable {
                @Override
                public void close() throws IOException {}
            }
        """,
        after = """
            import java.io.IOException;
            
            class A {
                public void close() throws IOException {}
            }
        """
    )
}
